/* 
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.flightmap.parsing.faa.nasr;

import com.google.flightmap.common.data.Airspace;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;
import com.google.flightmap.common.db.CustomGridUtil;
import com.google.flightmap.db.JdbcAviationDbWriter;
import com.google.flightmap.parsing.db.AviationDbWriter;
import com.google.flightmap.parsing.esri.ShapefileReader;
import com.google.flightmap.parsing.esri.data.*;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

/**
 * Parses airports from compiled AFD file.
 */
public class AirspaceParser {
  private final static Pattern SHAPEFILE_NAME_PATTERN = Pattern.compile("class_(.)\\.shp");
  private final static String ATTRIBUTE_FILE_SUFFIX = ".csv";

  // Commandline options
  private final static Options OPTIONS = new Options();
  private final static String HELP_OPTION = "help";
  private final static String SHAPEFILES_OPTION = "shapefiles";
  private final static String AVIATION_DB_OPTION = "aviation_db";

  static {
    OPTIONS.addOption("h", "help", false, "Print this message.");
    OPTIONS.addOption(OptionBuilder.withLongOpt(SHAPEFILES_OPTION)
                                   .withDescription("Shapefiles with airspace data.")
                                   .hasArgs()
                                   .withValueSeparator('=')
                                   .isRequired()
                                   .withArgName("class_b.shp,class_c.shp")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(AVIATION_DB_OPTION)
                                   .withDescription("FlightMap aviation database")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("aviation.db")
                                   .create());
  }

  private final File shapefile;
  private final File attributeFile;
  private final String airspaceClass;
  private final AviationDbWriter dbWriter;
  /**
   * @param twrFile NASR Terminal Communications Services database file.
   * @param iataToIcaoFile IATA to ICAO codes file
   * @param dbFile Aviation database file
   */
  public AirspaceParser(final String shapefile, final String dbFile) throws ClassNotFoundException,
      IOException, SQLException {
    this.shapefile = new File(shapefile);
    // Determine attribute file and airspace class (Bravo, Charlie, etc.) from shapefile
    final String shapefileName = this.shapefile.getName();
    final Matcher matcher = SHAPEFILE_NAME_PATTERN.matcher(shapefileName);
    if (!matcher.matches()) {
      throw new RuntimeException(
          "Unsupported shapefile name: " + shapefileName + " (should be class_X.shp)");
    }
    attributeFile = new File(
        this.shapefile.getParentFile(), "class_" + matcher.group(1) + ATTRIBUTE_FILE_SUFFIX);
    final char airspaceClassAbbreviation = matcher.group(1).toUpperCase().charAt(0);
    airspaceClass = getAirspaceClassFromAbbreviation(airspaceClassAbbreviation);

    dbWriter = new JdbcAviationDbWriter(new File(dbFile));
    dbWriter.open();
  }

  /**
   * Returns airspace class string for database insertion based on single char abbreviation.
   */
  private static String getAirspaceClassFromAbbreviation(final char abbr) {
    return Airspace.Class.valueOf(abbr).toString();
  }

  private void execute() throws Exception {
    dbWriter.beginTransaction();
    try {
      dbWriter.initAirspaceTables();
      addAirspaceData();
      dbWriter.commit();
    } catch (Exception ex) {
      dbWriter.rollback();
      throw ex;
    } finally {
      dbWriter.close();
    }
  }

  private void addAirspaceData() throws IOException, SQLException, InterruptedException {
    final AirspaceAttributeBean[] attributeBeans = AirspaceAttributeUtils.parse(attributeFile);
    final ShapefileReader reader = new ShapefileReader(shapefile);
    final Shapefile data = reader.read();
    for (Polygon polygon: data.getPolygons()) {
      if (polygon.parts.length != 1) {
        throw new RuntimeException(
            "Cannot handle polygon with parts.length != 1.  Record number: " +
            polygon.recordNumber);
      }
      final AirspaceAttributeBean bean = attributeBeans[polygon.recordNumber-1];
      // Get metadata
      final String name = bean.getName();
      final String lowAltString = bean.getLowAlt();
      final int lowAlt = "SFC".equals(lowAltString) ? Airspace.SFC : Integer.parseInt(lowAltString);
      final int highAlt = Integer.parseInt(bean.getHighAlt());
      // Get bounding box
      final LatLng minCorner = LatLng.fromDouble(polygon.yMin, polygon.xMin);
      final LatLng maxCorner = LatLng.fromDouble(polygon.yMax, polygon.xMax);
      final LatLngRect boundingBox = new LatLngRect(minCorner, maxCorner);
      // Get LatLng points
      Polygon.Part part = polygon.parts[0];
      final List<LatLng> points = new LinkedList<LatLng>();
      for (Point point: part.points) {
        points.add(LatLng.fromDouble(point.y, point.x));
      }
      addAirspaceToDb(name, lowAlt, highAlt, boundingBox, points);
    }
  }

  private void addAirspaceToDb(final String name, final int lowAlt, final int highAlt,
      final LatLngRect boundingBox, final List<LatLng> points) throws SQLException {
    // Create airspace entry
    final int airportId = -1;  // Airspace center N/A in NASR.
    final int minLat = boundingBox.getSouth();
    final int maxLat = boundingBox.getNorth();
    final int minLng = boundingBox.getWest();
    final int maxLng = boundingBox.getEast();
    final int id = dbWriter.insertAirspace(
        airportId, name, airspaceClass, minLat, maxLat, minLng, maxLng, lowAlt, highAlt);
    // Insert polygon points
    int i = 0;
    for (LatLng point: points) {
      dbWriter.insertAirspacePoint(id, i++, point.lat, point.lng);
    }
  }

  private static void printHelp(final CommandLine line) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(100);
    formatter.printHelp("AirspaceParser", OPTIONS, true);
  }

  public static void main(String args[]) {
    CommandLine line = null;
    try {
      final CommandLineParser parser = new PosixParser();
      line = parser.parse(OPTIONS, args);
    } catch (ParseException pEx) {
      System.err.println(pEx.getMessage());
      printHelp(line);
      System.exit(1);
    }

    if (line.hasOption(HELP_OPTION)) {
      printHelp(line);
      System.exit(0);
    }

    final String[] shapefiles = line.getOptionValues(SHAPEFILES_OPTION);
    final String dbFile = line.getOptionValue(AVIATION_DB_OPTION);

    try {
      for (String shapefile: shapefiles) {
        (new AirspaceParser(shapefile, dbFile)).execute();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

}

/* 
 * Copyright (C) 2011 Google Inc.
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

package com.google.flightmap.parsing.faa.nfd;

import com.google.flightmap.common.data.Airspace;
import com.google.flightmap.common.data.AirspaceArc;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;
import com.google.flightmap.common.geo.GreatCircleUtils;
import com.google.flightmap.common.geo.NavigationUtil;
import com.google.flightmap.db.JdbcAviationDbAdapter;
import com.google.flightmap.db.JdbcAviationDbWriter;
import com.google.flightmap.parsing.db.AviationDbReader;
import com.google.flightmap.parsing.db.AviationDbWriter;
import com.google.flightmap.parsing.faa.nfd.data.ControlledAirspaceRecord;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


/**
 * Parses airspaces from ARINC 424-18 file and adds them to a SQLite database
 */
public class NfdAirspaceParser {
  private final static Logger LOG = Logger.getLogger(NfdAirspaceParser.class.getName());

  /**
   * Maximum allowable distance between points that should logically coincide, in meters.
   * <p>
   * For instance, the starting location of an airspace arc as encoded in NFD should coincide with
   * the location determined by following a radial corresponding the arc's start angle, at a given
   * distance, from the origin.  In reality however, those points differ slightly.
   */
  private final static int MAX_LOC_DIFF = 100;

  // Command line options
  private final static Options OPTIONS = new Options();
  private final static String HELP_OPTION = "help";
  private final static String NFD_OPTION = "nfd";
  private final static String AVIATION_DB_OPTION = "aviation_db";

  static {
    // Command Line options definitions
    OPTIONS.addOption("h", "help", false, "Print this message.");
    OPTIONS.addOption(OptionBuilder.withLongOpt(NFD_OPTION)
                                   .withDescription("FAA National Flight Database.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("nfd.dat")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(AVIATION_DB_OPTION)
                                   .withDescription("Aviation database.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("aviation_db")
                                   .create());
  }

  /**
   * Low-level interface for writing to the aviation database.
   */
  private final AviationDbWriter dbWriter;

  /**
   * Low-level interface for reading from the aviation database.
   */
  private final AviationDbReader dbReader;

  /**
   * Path to file in ARINC 424-18 format (eg. NFD)
   */
  private final File nfd;

  /**
   * NFD reader
   */
  private BufferedReader in;

  /**
   * Indicates that {@code in} has been closed, most likely after having read all its contents.
   */
  private boolean inClosed = false;

  /**
   * Current airspace center.
   */
  private String center;

  /**
   * Airport id for current airspace center.
   */
  private int airportId;

  /**
   * Current airspace area.
   */
  private char area;

  /**
   * Current airspace (area) id.
   */
  private int areaId;

  /**
   * Current sequence number.
   */
  private int sequenceNumber;

  /**
   * Buffer for peeked records (allows to read next record without consuming it.
   */
  private ControlledAirspaceRecord buf;

  /**
   * @param nfdFile
   *          Source database in ARINC 424-18 format (eg NFD)
   */
  public NfdAirspaceParser(final File nfd, final File db) throws ClassNotFoundException,
         IOException, SQLException {
    this.nfd = nfd;
    dbWriter = new JdbcAviationDbWriter(db);
    dbWriter.open();
    dbReader = new JdbcAviationDbAdapter(dbWriter.getConnection());
  }

  public static void main(String args[]) {
    CommandLine line = null;
    try {
      final CommandLineParser parser = new PosixParser();
      line = parser.parse(OPTIONS, args);
    } catch (ParseException pEx) {
      System.err.println(pEx.getMessage());
      printHelp(line);
      System.exit(2);
    }

    if (line.hasOption(HELP_OPTION)) {
      printHelp(line);
      System.exit(0);
    }

    final String nfdPath = line.getOptionValue(NFD_OPTION);
    final File nfd = new File(nfdPath);
    final String dbPath = line.getOptionValue(AVIATION_DB_OPTION);
    final File db = new File(dbPath);
    try {
      (new NfdAirspaceParser(nfd, db)).execute();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  private static void printHelp(final CommandLine line) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(100);
    formatter.printHelp("NfdAirspaceParser", OPTIONS, true);
  }

  /**
   * Executes all operations required to add airspaces to the aviation database.
   */
  private void execute() throws IOException, SQLException {
    try {
      dbWriter.initAirspaceTables();
      parseAirspaceRecords();
    } finally {
      dbWriter.close();
    }
  }

  /**
   * Parses al airspace records in the NFD and inserts them in the aviation database.
   */
  private void parseAirspaceRecords() throws IOException, SQLException {
    while (parseNextAirspace()) { }
  }

  /**
   * Returns (and consumes) next record in NFD, or null if none left.
   */
  private synchronized ControlledAirspaceRecord readNextRecord() throws IOException {
    if (buf != null) {
      final ControlledAirspaceRecord r = buf;
      buf = null;
      return r;
    }
    if (inClosed) {
      return null;
    }
    if (in == null) {
      in = new BufferedReader(new FileReader(nfd));
    }
    String line;
    while ((line = in.readLine()) != null) {
      if (ControlledAirspaceRecord.matches(line)) {
        return ControlledAirspaceRecord.parse(line);
      }
    }
    in.close();
    inClosed = true;
    return null;
  }
  
  /**
   * Returns next record in NFD without consuming it, or null if none left.
   */
  private synchronized ControlledAirspaceRecord peekNextRecord() throws IOException {
    if (buf == null) {
      buf = readNextRecord();
    }
    return buf;
  }

  /**
   * Parses a single airspace from the NFD.
   */
  private boolean parseNextAirspace() throws IOException, SQLException {
    int seqNr = 0;
    LinkedHashMap<Integer, LatLng> points = new LinkedHashMap<Integer, LatLng>();
    LinkedHashMap<Integer, AirspaceArc> arcs = new LinkedHashMap<Integer, AirspaceArc>();

    final ControlledAirspaceRecord init = peekNextRecord();
    if (init == null) {
      return false;
    }
    // Get airspace information
    final String center = init.airspaceCenter.trim();
    final String name = init.controlledAirspaceName.trim();
    final char airspaceClass = init.airspaceClass.charAt(0);
    final String lowAltString = init.lowerLimit.trim();
    final int lowAlt = "GND".equals(lowAltString) ? Airspace.SFC : Integer.parseInt(lowAltString);
    final int highAlt = Integer.parseInt(init.upperLimit);
    // Check altitudes are MSL
    if ((lowAlt != Airspace.SFC && !"M".equals(init.lowerLimitUnitIndicator)) ||
        !"M".equals(init.upperLimitUnitIndicator)) {
      throw new RuntimeException("Airspace altitude limit indicator not supported: " +
          init.lowerLimitUnitIndicator + " or " + init.upperLimitUnitIndicator);
    }
    final LatLngRect boundingBox = new LatLngRect();
    ControlledAirspaceRecord c;
    LatLng first = null;
    LatLng lastArcEnd = null;
    while ((c = readNextRecord()) != null) {
      final char via = c.boundaryVia.charAt(0);
      final boolean isEnd = c.boundaryVia.charAt(1) == 'E';
      LatLng current;
      LatLng next;
      if (via == 'C') {
        // Circle should be first and only record of airspace.
        assert first == null;
        assert isEnd;
        current = LatLngParsingUtils.parseLatLng(c.arcOriginLatitude, c.arcOriginLongitude);
        next = null;
      } else {
        current = LatLngParsingUtils.parseLatLng(c.latitude, c.longitude);
        final ControlledAirspaceRecord n = peekNextRecord();
        next = isEnd ? first : LatLngParsingUtils.parseLatLng(n.latitude, n.longitude);
        if (first == null) {
          first = current ;
        }
      }
      /* If last record was an arc, try replacing current point with its end point to avoid
       * small, erratic shapes (due to rounding error and geodesic computations.) */
      if (lastArcEnd != null && (via == 'H' || via == 'G')) {
          final double diff = Math.abs(NavigationUtil.computeDistance(lastArcEnd, current));
          if (diff < MAX_LOC_DIFF) {
            current = lastArcEnd;
          } else {
            LOG.warning("Mismatch between last arc end point and current (in meters): " + diff);
          }
      }
      boundingBox.add(current);
      if (via == 'H') {
        if (lastArcEnd != current) {
          points.put(seqNr++, current);
        }
      } else if (via == 'G') {
        final List<LatLng> samples =
            GreatCircleUtils.sampleGreatCircle(current, next, MAX_LOC_DIFF);
        int count = 0;
        int size = samples.size();
        for (LatLng sample: samples) {
          if (count == 0 && lastArcEnd == current) {
            continue;  // Skip first point if it is equal to end of previous arc segment.
          }
          if (++count < size) {  // Skips last point (will be handled by next record).
            points.put(seqNr++, sample);
          }
        }
      } else if (via == 'R' || via == 'L' || via == 'C') {
        final LatLng o = LatLngParsingUtils.parseLatLng(c.arcOriginLatitude, c.arcOriginLongitude);
        float startAngle;
        float sweepAngle;
        double radius;

        if (via == 'C') {
          startAngle = 0;
          sweepAngle = 360;
          radius = Integer.parseInt(c.arcDistance) / 10.0;
        } else {
          final boolean clockwise = via == 'R';
          startAngle = (float) ((NavigationUtil.getInitialCourse(o, current) + 270) % 360);
          final float endAngle = (float) ((NavigationUtil.getInitialCourse(o, next) + 270) % 360);
          final float diffAngle = endAngle - startAngle;
          sweepAngle = clockwise ?
                       NavigationUtil.euclideanMod(diffAngle, 360.0f) :
                       -NavigationUtil.euclideanMod(-diffAngle, 360.0f);
          radius = NavigationUtil.computeDistance(o, current);
          lastArcEnd = next;
        }
        final LatLng north = NavigationUtil.getPointAlongRadial(o, 0, radius);
        final LatLng east = NavigationUtil.getPointAlongRadial(o, 90, radius);
        final LatLng south = NavigationUtil.getPointAlongRadial(o, 180, radius);
        final LatLng west = NavigationUtil.getPointAlongRadial(o, 270, radius);
        final LatLngRect box = new LatLngRect();
        box.add(north);
        box.add(east);
        box.add(south);
        box.add(west);
        final AirspaceArc arc = new AirspaceArc(box, startAngle, sweepAngle);
        arcs.put(seqNr++, arc);

        /* Determine contribution of this arc to the airspace bounding box.
         * Remember: for R or L arcs, the start and end points are added elsewhere. */
        if (via == 'C') {
          boundingBox.add(box);
        } else {
          final boolean clockwise = via == 'R';
          double minAngle;  // Must be in [0, 360)
          double maxAngle;  // Must be in [0, 720)
          if (clockwise) {
            minAngle = startAngle;  // [0, 360)
            maxAngle = startAngle + sweepAngle;  // [0, 720)
          } else {
            minAngle = startAngle - sweepAngle; // [-360, 360)
            maxAngle = startAngle;  // [0, 360)
            if (minAngle < 0) {  // minAngle in [-360, 0): shift to expected range
              minAngle += 360;  // [0, 360)
              maxAngle += 360;  // [360, 720)
            }
          }
          // Check if cardinal directions are included in [minAngle, maxAngle].  Note that the
          // possible range for the angles is [0, 720), so two values have to be checked for each
          // direction (eg. North is 0 and 360).
          if (rangeContains(0, minAngle, maxAngle) || rangeContains(360, minAngle, maxAngle)) {
            boundingBox.add(north);
          }
          if (rangeContains(90, minAngle, maxAngle) || rangeContains(450, minAngle, maxAngle)) {
            boundingBox.add(east);
          }
          if (rangeContains(180, minAngle, maxAngle) || rangeContains(540, minAngle, maxAngle)) {
            boundingBox.add(south);
          }
          if (rangeContains(270, minAngle, maxAngle) || rangeContains(630, minAngle, maxAngle)) {
            boundingBox.add(west);
          }
        }
      } else {
        throw new RuntimeException("Unknown boundary via type: " + via);
      }

      if (isEnd) {
        addAirspaceToDb(center, name, airspaceClass, boundingBox, lowAlt, highAlt, points, arcs);
        return true;
      }

      if (via != 'R' && via != 'L') {
        lastArcEnd = null;
      }
    }
    return false;
  }

  /**
   * Checks if {@code x} falls between {@code min} and {@code max} (included).
   */
  private static boolean rangeContains(final double x, final double min, final double max) {
    return x >= min && x <= max;
  }

  /**
   * Adds airspace to the aviation database.
   */
  private void addAirspaceToDb(final String icao, final String name, final char airspaceClass,
      final LatLngRect boundingBox, final int lowAlt, final int highAlt,
      final Map<Integer, LatLng> points, final Map<Integer, AirspaceArc> arcs)
      throws SQLException {
    System.out.println("Inserting airspace: " + name);
    final int airportId = dbReader.getAirportIdByIcao(icao);
    if (airportId == -1) {
      System.err.println("Could not find airport id for icao: " + icao);
      return;
    }

    final int minLat = boundingBox.getSouth();
    final int maxLat = boundingBox.getNorth();
    final int minLng = boundingBox.getWest();
    final int maxLng = boundingBox.getEast();

    dbWriter.beginTransaction();
    try {
      final int airspaceId = dbWriter.insertAirspace(airportId, name,
           Airspace.Class.valueOf(airspaceClass).toString(), minLat, maxLat, minLng, maxLng,
           lowAlt, highAlt);
      addAirspacePointsToDb(airspaceId, points);
      addAirspaceArcsToDb(airspaceId, arcs);
      dbWriter.commit();
    } catch (SQLException ex) {
      dbWriter.rollback();
      throw ex;
    }
  }

  /**
   * Adds airspace points for a given airspace in the aviation database.
   */
  private void addAirspacePointsToDb(final int id, final Map<Integer, LatLng> points) 
      throws SQLException {
    for (Map.Entry<Integer, LatLng> pointEntry: points.entrySet()) {
      final int seqNr = pointEntry.getKey();
      final LatLng point = pointEntry.getValue();
      dbWriter.insertAirspacePoint(id, seqNr, point.lat, point.lng);
    }
  }

  /**
   * Adds airspace arcs for a given airspace in the aviation database.
   */
  private void addAirspaceArcsToDb(final int id, final Map<Integer, AirspaceArc> arcs)
      throws SQLException {
    for (Map.Entry<Integer, AirspaceArc> arcEntry: arcs.entrySet()) {
      final int seqNr = arcEntry.getKey();
      final AirspaceArc arc = arcEntry.getValue();

      final LatLngRect boundingBox = arc.boundingBox;
      final int minLat = boundingBox.getSouth();
      final int maxLat = boundingBox.getNorth();
      final int minLng = boundingBox.getWest();
      final int maxLng = boundingBox.getEast();
      final int startAngle = (int)Math.round(arc.startAngle * 1E6);
      final int sweepAngle = (int)Math.round(arc.sweepAngle * 1E6);

      dbWriter.insertAirspaceArc(id, seqNr, minLat, maxLat, minLng, maxLng, startAngle, sweepAngle);
    }
  }
}

/* 
 * Copyright (C) 2009 Google Inc.
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

package com.google.flightmap.parsing.faa.amr;

import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.Runway;
import com.google.flightmap.common.db.AviationDbAdapter;
import com.google.flightmap.common.db.CustomGridUtil;
import com.google.flightmap.db.JdbcAviationDbAdapter;
import com.google.flightmap.db.JdbcAviationDbWriter;
import com.google.flightmap.parsing.db.AviationDbWriter;
import com.google.flightmap.parsing.db.AviationDbReader;
import com.google.flightmap.parsing.util.IcaoUtils;
import com.google.flightmap.parsing.util.IndexedArray;
import com.google.flightmap.parsing.util.StringUtils;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses airports from FAA Airport Master Record file and adds them to a SQLite database
 *
 * http://www.faa.gov/airports/airport_safety/airportdata_5010/
 *
 */
public class AviationMasterRecordParser {
  // Command line options
  private final static Options OPTIONS = new Options();
  private final static String HELP_OPTION = "help";
  private final static String AIRPORT_MR_OPTION = "airports";
  private final static String RUNWAY_MR_OPTION = "runways";
  private final static String IATA_TO_ICAO_OPTION = "iata_to_icao";
  private final static String AVIATION_DB_OPTION = "aviation_db";

  // Airport data headers
  private final static String AIRPORT_USE_HEADER = "Use";  // PU, PR
  private final static String AIRPORT_OWNERSHIP_HEADER = "Ownership";  // PU, PR, MA, MN, MR
  private final static String AIRPORT_SITE_NUMBER_HEADER = "SiteNumber";
  private final static String AIRPORT_LOCATION_ID_HEADER = "LocationID";
  private final static String AIRPORT_TYPE_HEADER = "Type";  // AIRPORT, HELIPORT, ...
  private final static String AIRPORT_CITY_HEADER = "City";
  private final static String AIRPORT_FACILITY_NAME_HEADER = "FacilityName";
  private final static String AIRPORT_LONGITUDE_HEADER = "ARPLongitudeS";
  private final static String AIRPORT_LATITUDE_HEADER = "ARPLatitudeS";
  private final static String AIRPORT_STATUS_HEADER = "AirportStatusCode"; // O, CP, CI
  private final static String AIRPORT_CONTROL_TOWER_HEADER = "ATCT";
  private final static String AIRPORT_CTAF_HEADER = "CTAFFrequency";
  // Airport property headers
  private final static String AIRPORT_ELEVATION_HEADER = "ARPElevation";
  private final static String AIRPORT_BEACON_COLOR_HEADER = "BeaconColor";
  private final static String AIRPORT_FUEL_TYPES_HEADER = "FuelTypes";
  private final static String AIRPORT_LANDING_FEE_HEADER = "NonCommercialLandingFee";
  private final static String AIRPORT_SEGMENTED_CIRCLE_HEADER = "SegmentedCircle";
  private final static String AIRPORT_WIND_INDICATOR_HEADER = "WindIndicator";
  private final static String AIRPORT_EFFECTIVE_DATE_HEADER = "EffectiveDate";
  /**
   * Maps airport property headers to user-friendly labels.
   */
  private final static Map<String, String> AIRPORT_PROPERTIES_LABEL_MAP;

  // Runway
  private final static String RUNWAY_SITE_NUMBER_HEADER = "SiteNumber";
  private final static String RUNWAY_LETTERS_HEADER = "RunwayID";
  private final static String RUNWAY_SURFACE_HEADER = "RunwaySurfaceTypeCondition";
  private final static String RUNWAY_LENGTH_HEADER = "RunwayLength";
  private final static String RUNWAY_WIDTH_HEADER = "RunwayWidth";
  // Runway end
  private final static String BASE_RUNWAY_END_HEADER_PREFIX = "BaseEnd";
  private final static String RECIPROCAL_RUNWAY_END_HEADER_PREFIX = "ReciprocalEnd";
  private final static String RUNWAY_END_LETTERS_HEADER_SUFFIX = "ID";
  // Runway end properties
  private final static String RUNWAY_END_REIL_HEADER_SUFFIX = "REIL";
  private final static String RUNWAY_END_RIGHT_TRAFFIC_HEADER_SUFFIX = "RightTrafficPattern";
  private final static String RUNWAY_END_VASI_HEADER_SUFFIX = "VASI";
  private final static String RUNWAY_END_TRUE_ALIGNMENT_HEADER_SUFFIX = "TrueAlignment";
  private final static Map<String, String> RUNWAY_END_PROPERTIES_LABEL_MAP;

  static {
    // Command Line options definitions
    OPTIONS.addOption("h", "help", false, "Print this message.");
    OPTIONS.addOption(OptionBuilder.withLongOpt(AIRPORT_MR_OPTION)
                                   .withDescription("FAA Airport Master Record file.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("airports.xls")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(RUNWAY_MR_OPTION)
                                   .withDescription("FAA Runway Master Record file.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("runways.xls")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(IATA_TO_ICAO_OPTION)
                                   .withDescription("IATA to ICAO codes text file.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("iata_to_icao.txt")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(AVIATION_DB_OPTION)
                                   .withDescription("FlightMap aviation database")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("aviation.db")
                                   .create());

    // Airport property labels
    AIRPORT_PROPERTIES_LABEL_MAP = new HashMap<String, String>();
    AIRPORT_PROPERTIES_LABEL_MAP.put(AIRPORT_BEACON_COLOR_HEADER, "Beacon color");
    AIRPORT_PROPERTIES_LABEL_MAP.put(AIRPORT_CONTROL_TOWER_HEADER, "Control tower");
    AIRPORT_PROPERTIES_LABEL_MAP.put(AIRPORT_EFFECTIVE_DATE_HEADER, "Effective date");
    AIRPORT_PROPERTIES_LABEL_MAP.put(AIRPORT_ELEVATION_HEADER, "Elevation");
    AIRPORT_PROPERTIES_LABEL_MAP.put(AIRPORT_FUEL_TYPES_HEADER, "Fuel types");
    AIRPORT_PROPERTIES_LABEL_MAP.put(AIRPORT_LANDING_FEE_HEADER, "Landing fee");
    AIRPORT_PROPERTIES_LABEL_MAP.put(AIRPORT_SEGMENTED_CIRCLE_HEADER, "Segmented circle");
    AIRPORT_PROPERTIES_LABEL_MAP.put(AIRPORT_WIND_INDICATOR_HEADER, "Wind indicator");

    // Runway end property labels
    RUNWAY_END_PROPERTIES_LABEL_MAP = new HashMap<String, String>();
    RUNWAY_END_PROPERTIES_LABEL_MAP.put(RUNWAY_END_REIL_HEADER_SUFFIX, "REIL");
    RUNWAY_END_PROPERTIES_LABEL_MAP.put(RUNWAY_END_RIGHT_TRAFFIC_HEADER_SUFFIX, "Traffic Pattern");
    RUNWAY_END_PROPERTIES_LABEL_MAP.put(RUNWAY_END_VASI_HEADER_SUFFIX, "VASI");
    RUNWAY_END_PROPERTIES_LABEL_MAP.put(RUNWAY_END_TRUE_ALIGNMENT_HEADER_SUFFIX, "True Alignment");
  }

  private final AviationDbWriter dbWriter;
  private final AviationDbReader dbReader;
  private final String airportSourceFile;
  private final String runwaySourceFile;
  private final Map<String, String> iataToIcao;
  private final Map<String, Integer> siteNumberToId = new HashMap<String, Integer>();

  /**
   * @param airportSourceFile
   *          FAA Form 5010, Airport Master Record file
   * @param dbFile
   *          Target SQLite filename. Existing data is silently overwritten.
   */
  public AviationMasterRecordParser(final String airportSourceFile, final String runwaySourceFile,
      final String iataToIcaoFile, final String dbFile) throws ClassNotFoundException, IOException,
      SQLException {
    this.airportSourceFile = airportSourceFile;
    this.runwaySourceFile = runwaySourceFile;
    dbWriter = new JdbcAviationDbWriter(new File(dbFile));
    dbWriter.open();
    dbReader = new JdbcAviationDbAdapter(dbWriter.getConnection());
    iataToIcao = IcaoUtils.parseIataToIcao(iataToIcaoFile);
  }

  /**
   * Runs FAA Aviation Master Record parser with given command line arguments.
   */
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

    final String airportSourceFile = line.getOptionValue(AIRPORT_MR_OPTION);
    final String runwaySourceFile = line.getOptionValue(RUNWAY_MR_OPTION);
    final String iataToIcaoFile = line.getOptionValue(IATA_TO_ICAO_OPTION);
    final String dbFile = line.getOptionValue(AVIATION_DB_OPTION);

    try {
      (new AviationMasterRecordParser(airportSourceFile, runwaySourceFile, iataToIcaoFile, dbFile))
          .execute();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Executes (drives) parsing methods
   *
   * @throws Exception Something went wrong...
   */
  private void execute() throws Exception {
    System.out.println("dbWriter.initAndroidMetadataTable();");
    dbWriter.initAndroidMetadataTable();
    System.out.println("dbWriter.initMetadataTable();");
    dbWriter.initMetadataTable();
    System.out.println("dbWriter.initConstantsTable();");
    dbWriter.initConstantsTable();
    System.out.println("addAirportDataToDb();");
    addAirportDataToDb();
    System.out.println("addRunwayDataToDb();");
    addRunwayDataToDb();
    System.out.println("rankAirportsInDb();");
    rankAirportsInDb();
    System.out.println("dbWriter.close();");
    dbWriter.close();
  }

  /**
   * Iterates over all airports and updates their rank according to their properties.  Typically,
   * this should be done after the initial addition of airports with bogus rank (e.g. -1).
   *
   * @see #computeAirportRank
   */
  private void rankAirportsInDb() throws SQLException {
    dbWriter.beginTransaction();
    final List<Integer> ids = dbReader.getAllAirportIds();
    for (int id: ids) {
      final Airport airport = dbReader.getAirport(id);
      final int rank = computeAirportRank(airport);
      dbWriter.updateAirportRank(id, rank);
    }
    dbWriter.commit();
  }

  /**
   * Returns the rank of an airport on a 0 (minor airport) - 5 (major airport)
   * scale. Major factors affecting rank include type, runway length, and
   * surface.
   *
   * @return  Airport rank, 0 (minor airport) - 5 (major airport)
   */
  private static int computeAirportRank(final Airport airport) {
    int rank = 0;
    if (airport.isPublic) {
      ++rank;
    }
    if (airport.isTowered) {
      ++rank;
    }
    if (airport.runways != null) {
      final int numRunways = airport.runways.size();
      if (numRunways > 2) {
        ++rank;
      }
      final Runway longestRunway = airport.runways.first();
      final int longestRunwayLength = longestRunway.length;
      if (longestRunwayLength > 5000) {
        ++rank;
        if (longestRunwayLength > 8000) {
          ++rank;
        }
      }
    }
    return Math.min(rank, 5);
  }


  /**
   * Parse file and add airport data to the database.
   */
  private void addAirportDataToDb() throws SQLException, IOException {
    final BufferedReader in = new BufferedReader(new FileReader(airportSourceFile));
    try {
      String line = in.readLine();
      if (line == null) {
        throw new RuntimeException("Airport Master Record file is empty.");
      }
      // Parse first line to determine headers
      final String[] headers = line.split("\\t");
      removeEnclosingQuotes(headers);
      final IndexedArray<String, String> fields = new IndexedArray<String, String>(headers);
      dbWriter.initAirportTables();
      dbWriter.initAirportCommTable();
      dbWriter.beginTransaction();
      // Parse airport lines
      while ((line = in.readLine()) != null) {
        fields.setFields(line.split("\\t"));
        // icao
        final String iata = fields.get(AIRPORT_LOCATION_ID_HEADER);
        final String icao = getIcao(iata);
        // name
        String name = fields.get(AIRPORT_FACILITY_NAME_HEADER);
        name = getAirportNameToDisplay(icao, name);
        // type
        String type = fields.get(AIRPORT_TYPE_HEADER);
        type = StringUtils.capitalize(type.toLowerCase());
        // city
        final String city = fields.get(AIRPORT_CITY_HEADER);
        // lat, lng
        final String latitudeS = fields.get(AIRPORT_LATITUDE_HEADER);
        final String longitudeS = fields.get(AIRPORT_LONGITUDE_HEADER);
        final LatLng position = LatLngParsingUtils.parseLatLng(latitudeS, longitudeS);
        // is_open
        final String status = fields.get(AIRPORT_STATUS_HEADER);
        final boolean isOpen = "O".equals(status);
        // is_public
        final String use = fields.get(AIRPORT_USE_HEADER);
        final boolean isPublic = "PU".equals(use);
        // is_towered
        final String controlTower = fields.get(AIRPORT_CONTROL_TOWER_HEADER);
        final boolean isTowered = "Y".equals(controlTower);
        // is_military
        final String ownership = fields.get(AIRPORT_OWNERSHIP_HEADER);
        final boolean isMilitary =  ownership.startsWith("M");
        // cell_id
        int cellId = CustomGridUtil.getCellId(position);
        // rank: Insert bogus value, will be replaced in second run
        final int rank = -1;
        // Insert in airport db
        dbWriter.insertAirport(icao, name, type, city, position.lat, position.lng, isOpen, isPublic,
            isTowered, isMilitary, cellId, rank);
        // Retrieve db id of freshly added airport
        final int id = dbReader.getAirportIdByIcao(icao); 
        if (id == -1) {
          throw new RuntimeException("Could not determine db id of airport: " + icao);
        }
        // Map SiteNumber to icao code for future reference by runway parser
        final String siteNumber = fields.tryGet(AIRPORT_SITE_NUMBER_HEADER);
        siteNumberToId.put(siteNumber, id);


        // Common traffic advisory frequency. (CTAF)
        String ctaf = fields.tryGet(AIRPORT_CTAF_HEADER);
        if (ctaf != null && !ctaf.isEmpty()) {
          try { // Parse frequency and convert it back to String to eliminate leading/trailing 0s.
            Double freq = Double.valueOf(ctaf);
            ctaf = freq.toString();
          } catch (NumberFormatException nfe) {
            // Safe to ignore exception here: frequency MAY be not parseable.
          }
          dbWriter.insertAirportComm(id, "CTAF", ctaf, null);
        }

        // Additional properties
        // Elevation
        final String elevation = fields.get(AIRPORT_ELEVATION_HEADER);
        addAirportProperty(id, AIRPORT_ELEVATION_HEADER, elevation);
        // Beacon Color
        final String beaconColor =
            AirportParsingUtils.parseAirportBeaconColor(fields.tryGet(AIRPORT_BEACON_COLOR_HEADER));
        addAirportProperty(id, AIRPORT_BEACON_COLOR_HEADER, beaconColor);
        // Fuel Types
        final String fuelTypes =
            AirportParsingUtils.parseAirportFuelTypes(fields.tryGet(AIRPORT_FUEL_TYPES_HEADER));
        addAirportProperty(id, AIRPORT_FUEL_TYPES_HEADER, fuelTypes);
        // Non-Commercial Landing Fee
        final String landingFee =
          AirportParsingUtils.parseBoolean(fields.tryGet(AIRPORT_LANDING_FEE_HEADER));
        addAirportProperty(id, AIRPORT_LANDING_FEE_HEADER, landingFee);
        // Segmented circle
        final String segmentedCircle =
          AirportParsingUtils.parseBoolean(fields.tryGet(AIRPORT_SEGMENTED_CIRCLE_HEADER));
        addAirportProperty(id, AIRPORT_SEGMENTED_CIRCLE_HEADER, segmentedCircle);
        // Effective date
        final String effectiveDate = fields.tryGet(AIRPORT_EFFECTIVE_DATE_HEADER);
        addAirportProperty(id, AIRPORT_EFFECTIVE_DATE_HEADER, effectiveDate);
        // Wind indicator 
        final String windIndicator = AirportParsingUtils.parseAirportWindIndicator(
            fields.tryGet(AIRPORT_WIND_INDICATOR_HEADER));
        addAirportProperty(id, AIRPORT_WIND_INDICATOR_HEADER, windIndicator);
      }
      dbWriter.commit();
    } finally {
      in.close();
    }
  }

  /**
   * Adds a property to an airport.
   * <p>
   * Key is an airport property header, and is mapped to a user friendly label.
   * Both key label and value strings are converted to constants.
   * If the value string can be parsed as an integer, it is not converted.
   *
   * @param id Airport id
   * @param key Property header
   * @param value Property value.
   */
  private void addAirportProperty(final int id, final String key, final String value)
      throws SQLException {
    if (value == null || value.isEmpty()) {
      return;
    }
    final String label = AIRPORT_PROPERTIES_LABEL_MAP.get(key);
    dbWriter.insertAirportProperty(id, label, value);
  }

  /**
   * Parse file and add runway data to the database.
   */
  private void addRunwayDataToDb() throws SQLException, IOException {
    final BufferedReader in = new BufferedReader(new FileReader(runwaySourceFile));
    try {
      String line = in.readLine();
      if (line == null) {
        throw new RuntimeException("Runway Master Record file is empty.");
      }
      // Parse first line to determine headers
      final String[] headers = line.split("\\t");
      removeEnclosingQuotes(headers);
      final IndexedArray<String, String> fields = new IndexedArray<String, String>(headers);
      // Initialize db and prepare statements
      dbWriter.initRunwayTables();
      dbWriter.beginTransaction();
      // Parse runway lines
      while ((line = in.readLine()) != null) {
        fields.setFields(line.split("\\t"));
        // Get corresponding airport
        final String siteNumber = fields.get(RUNWAY_SITE_NUMBER_HEADER);
        final Integer airportId = siteNumberToId.get(siteNumber);
        if (airportId == null) {
          System.err.println("Could not find airport id for site number: " + siteNumber);
          continue;
        }
        // Insert new runway in db
        final String letters = fields.get(RUNWAY_LETTERS_HEADER).substring(1); // Remove first '
        final int length = Integer.parseInt(fields.get(RUNWAY_LENGTH_HEADER));
        final int width = Integer.parseInt(fields.get(RUNWAY_WIDTH_HEADER));
        final String surface =
            AirportParsingUtils.parseRunwaySurface(fields.get(RUNWAY_SURFACE_HEADER));
        dbWriter.insertRunway(airportId, letters, length, width, surface);
        // Retrieve db id of freshly added runway
        final int runwayId = dbReader.getRunwayId(airportId, letters);
        if (runwayId == -1) {
          throw new RuntimeException(
              String.format("Could not determine db id of runway %s (airport db id: %d)",
                  letters, airportId));
        }
        // Add runway ends
        addRunwayEndDataToDb(runwayId, fields);
      }
      dbWriter.commit();
    } finally {
      in.close();
    }
  }

  /**
   * Inserts both base and reciprocal runway end data in database.
   */
  private void addRunwayEndDataToDb(final int runwayId, final IndexedArray<String, String> fields)
      throws SQLException {
    addRunwayEndDataToDb(runwayId, fields, BASE_RUNWAY_END_HEADER_PREFIX);
    addRunwayEndDataToDb(runwayId, fields, RECIPROCAL_RUNWAY_END_HEADER_PREFIX);
  }

  private void addRunwayEndDataToDb(final int runwayId, final IndexedArray<String, String> fields,
      final String prefix) throws SQLException {
    // Add runway end to database
    final String lettersHeader = prefix + RUNWAY_END_LETTERS_HEADER_SUFFIX;
    final String letters = fields.get(lettersHeader).substring(1); // Remove first char "'"
    if (letters.isEmpty()) {
      // Skip runway ends with no letters (heliports)
      return;
    }
    dbWriter.insertRunwayEnd(runwayId, letters);
    // Retrieve db id of freshly added runway end
    final int runwayEndId = dbReader.getRunwayEndId(runwayId, letters);
    if (runwayEndId == -1) {
      throw new RuntimeException(
          String.format("Could not determine db id of runway end %s (runway db id: %d)",
              letters, runwayId));
    }

    // Add runway end properties
    // True Alignment
    final String trueAlignmentHeader = prefix + RUNWAY_END_TRUE_ALIGNMENT_HEADER_SUFFIX;
    final String trueAlignment = fields.tryGet(trueAlignmentHeader);
    addRunwayEndProperty(runwayEndId, RUNWAY_END_TRUE_ALIGNMENT_HEADER_SUFFIX, trueAlignment);
    // Runway end identifier lights
    final String reilHeader = prefix + RUNWAY_END_REIL_HEADER_SUFFIX;
    final String runwayEndReil = AirportParsingUtils.parseBoolean(fields.tryGet(reilHeader));
    addRunwayEndProperty(runwayEndId, RUNWAY_END_REIL_HEADER_SUFFIX, runwayEndReil);
    // Traffic pattern
    final String rightTrafficHeader = prefix + RUNWAY_END_RIGHT_TRAFFIC_HEADER_SUFFIX;
    final String trafficPattern = 
        AirportParsingUtils.parseTrafficPattern(fields.tryGet(rightTrafficHeader));
    addRunwayEndProperty(runwayEndId, RUNWAY_END_RIGHT_TRAFFIC_HEADER_SUFFIX, trafficPattern);
    // Visual glide slope indicators
    final String vasiHeader = prefix + RUNWAY_END_VASI_HEADER_SUFFIX;
    final String vasi = AirportParsingUtils.parseVasi(fields.tryGet(vasiHeader));
    addRunwayEndProperty(runwayEndId, RUNWAY_END_VASI_HEADER_SUFFIX, vasi);
  }

  /**
   * Adds runway end property.  No-op if {@code value} is {@code null} or empty.
   */
  private void addRunwayEndProperty(final int runwayEndId, final String key, final String value)
      throws SQLException {
    if (value == null || value.isEmpty()) {
      return;
    }
    final String label = RUNWAY_END_PROPERTIES_LABEL_MAP.get(key);
    dbWriter.insertRunwayEndProperty(runwayEndId, label, value);
  }

  /**
   * Returns ICAO code correspdonding to {@code iata} is known, else {@code iata} itself.
   */
  private String getIcao(final String iata) {
    final String knownIcao = iataToIcao.get(iata);
    return knownIcao == null ? iata : knownIcao;
  }

  private static String getAirportNameToDisplay(String airportID, String airportName) {
    // TODO: Simplify name for display
    return StringUtils.capitalize(airportName);
  }

  private static void printHelp(final CommandLine line) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(100);
    formatter.printHelp("AviationMasterRecordParser", OPTIONS, true);
  }

  /**
   * Removes enclosing double quotes from all entries in {@code headers}.
   * <p>
   * <pre>[ "\"foo\"", "\"bar\"" ] -> [ "foo", "bar" ]</pre>
   *
   * @throws RuntimeException Entry in {@code headers} is not enclosed in quotes.
   */
  private static void removeEnclosingQuotes(final String[] headers) {
    for (int i = 0; i < headers.length; ++i) {
      String header = headers[i];
      if (!header.matches("\"\\w+\"")) {
        throw new RuntimeException("No enclosing quotes: " + header);
      }
      header = header.substring(1,header.length()-1);
      headers[i] = header;
    }
  }
}

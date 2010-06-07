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

package com.google.flightmap.parsing;

import com.google.flightmap.common.CustomGridUtil;
import com.google.flightmap.common.data.LatLng;

import org.apache.commons.lang.WordUtils;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

/**
 * Parses airports from FAA Airport Master Record file and adds them to a SQLite database
 *
 * http://www.faa.gov/airports/airport_safety/airportdata_5010/
 *
 */
public class AviationMasterRecordParser {
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
  // Airport operations headers
  private final static String[] AIRPORT_OPS_HEADERS = {
      "OperationsCommercial", "OperationsCommuter", "OperationsAirTaxi", "OperationsGALocal",
      "OperationsGAItin", "OperationsMilitary" };
  // Airport property headers
  private final static String AIRPORT_ELEVATION_HEADER = "ARPElevation";
  private final static String AIRPORT_BEACON_COLOR_HEADER = "BeaconColor";
  private final static String AIRPORT_FUEL_TYPES_HEADER = "FuelTypes";
  private final static String AIRPORT_LANDING_FEE_HEADER = "NonCommercialLandingFee";
  private final static String AIRPORT_SEGMENTED_CIRCLE_HEADER = "SegmentedCircle";
  private final static String AIRPORT_WIND_INDICATOR_HEADER = "WindIndicator";
  private final static String AIRPORT_EFFECTIVE_DATE_HEADER = "EffectiveDate";
  private final static Map<String, String> AIRPORT_PROPERTIES_LABEL_MAP;


  // Runway
  private final static String RUNWAY_SITE_NUMBER_HEADER = "SiteNumber";
  private final static String RUNWAY_LETTERS_HEADER = "RunwayID";
  private final static String RUNWAY_SURFACE_HEADER = "RunwaySurfaceTypeCondition";
  private final static String RUNWAY_LENGTH_HEADER = "RunwayLength";
  private final static String RUNWAY_WIDTH_HEADER = "RunwayWidth";
  private final static Map<String, String> RUNWAY_SURFACE_MAP;
  // Runway end
  private final static String BASE_RUNWAY_END_HEADER_PREFIX = "BaseEnd";
  private final static String RECIPROCAL_RUNWAY_END_HEADER_PREFIX = "ReciprocalEnd";
  private final static String RUNWAY_END_LETTERS_HEADER_SUFFIX = "ID";
  private final static String RUNWAY_END_TRUE_ALIGNMENT_HEADER_SUFFIX = "TrueAlignment";
  // Runway end properties
  private final static String RUNWAY_END_REIL_HEADER_SUFFIX = "REIL";
  private final static String RUNWAY_END_RIGHT_TRAFFIC_HEADER_SUFFIX = "RightTrafficPattern";
  private final static String RUNWAY_END_VASI_HEADER_SUFFIX = "VASI";
  private final static Map<String, String> RUNWAY_END_PROPERTIES_LABEL_MAP;

  static {
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

    // Runway type
    RUNWAY_SURFACE_MAP = new HashMap<String, String>();
    RUNWAY_SURFACE_MAP.put("CONC", "Portland cement concrete");
    RUNWAY_SURFACE_MAP.put("ASPH", "Asphalt or bituminous concrete");
    RUNWAY_SURFACE_MAP.put("SNOW", "Snow");
    RUNWAY_SURFACE_MAP.put("ICE", "Ice");
    RUNWAY_SURFACE_MAP.put("MATS", "Pierced steel planking, landing mats");
    RUNWAY_SURFACE_MAP.put("TREATED", "Oiled, soil cement or lime stabilized");
    RUNWAY_SURFACE_MAP.put("GRAVEL", "Gravel, cinders, crushed rock, ...");
    RUNWAY_SURFACE_MAP.put("TURF", "Grass, sod");
    RUNWAY_SURFACE_MAP.put("DIRT", "Natural soil");
    RUNWAY_SURFACE_MAP.put("WATER", "Water");
    RUNWAY_SURFACE_MAP.put("E", "Excellent condition");
    RUNWAY_SURFACE_MAP.put("G", "Good condition");
    RUNWAY_SURFACE_MAP.put("F", "Fair condition");
    RUNWAY_SURFACE_MAP.put("P", "Poor condition");
    RUNWAY_SURFACE_MAP.put("L", "Failed condition");

    // Runway end property labels
    RUNWAY_END_PROPERTIES_LABEL_MAP = new HashMap<String, String>();
    RUNWAY_END_PROPERTIES_LABEL_MAP.put(RUNWAY_END_REIL_HEADER_SUFFIX, "REIL");
    RUNWAY_END_PROPERTIES_LABEL_MAP.put(RUNWAY_END_RIGHT_TRAFFIC_HEADER_SUFFIX, "Traffic Pattern");
    RUNWAY_END_PROPERTIES_LABEL_MAP.put(RUNWAY_END_VASI_HEADER_SUFFIX, "VASI");
  }

  private Connection dbConn;
  private final String airportSourceFile;
  private final String runwaySourceFile;
  private final String targetFile;
  private final Map<String, String> airportIataToIcaoCodes = new HashMap<String, String>();
  private final Map<String, Integer> airportSiteNumberToDbId = new HashMap<String, Integer>();

  // Frequent SQL Prepared Statements
  private PreparedStatement insertAirportPropertyStatement;
  private PreparedStatement insertRunwayEndPropertyStatement;
  private PreparedStatement insertRunwayStatement;
  private PreparedStatement getRunwayIdStatement;
  private PreparedStatement insertRunwayEndStatement;
  private PreparedStatement getRunwayEndIdStatement;

  private Map<String, Integer> constantCache = new HashMap<String, Integer>();

  /**
   * @param airportSourceFile
   *          FAA Form 5010, Airport Master Record file
   * @param targetFile
   *          Target SQLite filename. Existing data is silently overwritten.
   */
  public AviationMasterRecordParser(final String airportSourceFile,
                                    final String runwaySourceFile,
                                    final String iataToIcaoFile,
                                    final String targetFile) {
    this.airportSourceFile = airportSourceFile;
    this.runwaySourceFile = runwaySourceFile;
    this.targetFile = targetFile;
    try {
      final BufferedReader in = new BufferedReader(new FileReader(iataToIcaoFile));
      String line;
      while ((line = in.readLine()) != null) {
        final String[] codes = line.split(" ");
        airportIataToIcaoCodes.put(codes[0], codes[1]);
      }
      in.close();
    } catch (IOException ioex) {
      System.err.println("Error while reading iataToIcao file");
      ioex.printStackTrace();
    }
  }

  /*
   * Parse latitude and longitude from fractional seconds.
   *
   * @param latitudeS   Latitude in fractional seconds ("136545.1250N")
   * @param longitudeS  Longitude in fractional seconds ("326633.3600W")

   * @return            Position corresponding to given strings
   */
  public static LatLng parseLatLng(final String latitudeS, final String longitudeS) {
    final double latitude = parseLatitudeS(latitudeS);
    final double longitude = parseLongitudeS(longitudeS);
    final LatLng position = LatLng.fromDouble(latitude, longitude);
    return position;
  }

  /**
   * Parse latitude seconds.
   *
   * @param latitudeS  Latitude in fractional seconds ("136545.1250N")
   * @return           Latitude in degrees (North positive, South negative)
   */
  private static double parseLatitudeS(final String latitudeS) {
    double latitudeSeconds = Double.parseDouble(latitudeS.substring(0, latitudeS.length() - 1));
    final char northSouthIndicatorChar = latitudeS.charAt(latitudeS.length() - 1);
    if (northSouthIndicatorChar == 'S') {
      latitudeSeconds *= -1;
    } else if (northSouthIndicatorChar != 'N') {
      throw new RuntimeException("Unknown north/south indicator: " + northSouthIndicatorChar);
    }
    return latitudeSeconds/3600.0;
  }

  /**
   * Parse longitude seconds.
   *
   * @param longitudeS  Longitude in fractional seconds ("326633.3600W")
   * @return            Longitude in degrees (East positive, West negative)
   */
  private static double parseLongitudeS(final String longitudeS) {
    double longitudeSeconds = Double.parseDouble(longitudeS.substring(0, longitudeS.length() - 1));
    final char westEastIndicatorChar = longitudeS.charAt(longitudeS.length() - 1);
    if (westEastIndicatorChar == 'W') {
      longitudeSeconds *= -1;
    } else if (westEastIndicatorChar != 'E') {
      throw new RuntimeException("Unknown west/east indicator: " + westEastIndicatorChar);
    }
    return longitudeSeconds/3600.0;
  }

  /**
   * Parse file and add airport data to the database.
   */
  private void addAirportDataToDb() throws SQLException, IOException {
    final BufferedReader in = new BufferedReader(new FileReader(airportSourceFile));
    String line = in.readLine();

    if (line == null) {
      throw new RuntimeException("Airport Master Record file is empty.");
    }

    // Parse first line to determine headers
    final String[] headers = line.split("\\t");
    final Map<String, Integer> headerPosition = new HashMap<String, Integer>();
    int position = 0;
    for (String header: headers) {
      if (! header.matches("\"\\w+\"")) {
        throw new RuntimeException("Unrecognized header format: " + header);
      }

      header = header.substring(1,header.length()-1);
      headerPosition.put(header, position++);
    }

    // Initialize db and prepare statements
    initAirportTables();

    PreparedStatement insertAirportStatement = dbConn.prepareStatement(
        "INSERT INTO airports " +
        "(icao, name, type, city, lat, lng, is_open, is_public, is_towered, " +
        "is_military, cell_id, rank) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
    PreparedStatement getAirportIdStatement = dbConn.prepareStatement(
        "SELECT _id FROM airports WHERE icao = ?");

    // Parse airport lines
    while ((line = in.readLine()) != null) {
      final String[] airportFields = line.split("\\t");

      // Extract fields
      int fieldCount = 0;
      //   icao
      final String airportIataCode = airportFields[headerPosition.get(AIRPORT_LOCATION_ID_HEADER)];
      final String airportIcaoCode = getAirportIcaoCode(airportIataCode);
      insertAirportStatement.setString(++fieldCount, airportIcaoCode);

      //   name
      final String airportName = airportFields[headerPosition.get(AIRPORT_FACILITY_NAME_HEADER)];
      insertAirportStatement.setString(
          ++fieldCount, getAirportNameToDisplay(airportIcaoCode, airportName));

      //   type
      final String airportType = airportFields[headerPosition.get(AIRPORT_TYPE_HEADER)];
      insertAirportStatement.setInt(
          ++fieldCount, getConstantId(capitalize(airportType.toLowerCase())));

      //   city
      final String airportCity = airportFields[headerPosition.get(AIRPORT_CITY_HEADER)];
      insertAirportStatement.setString(++fieldCount, airportCity);

      //   lat, lng
      final String airportLatitudeS = airportFields[headerPosition.get(AIRPORT_LATITUDE_HEADER)];
      final String airportLongitudeS = airportFields[headerPosition.get(AIRPORT_LONGITUDE_HEADER)];
      final LatLng airportPosition = parseLatLng(airportLatitudeS, airportLongitudeS);
      insertAirportStatement.setInt(++fieldCount, airportPosition.lat);
      insertAirportStatement.setInt(++fieldCount, airportPosition.lng);

      //   is_open
      final String airportStatus = airportFields[headerPosition.get(AIRPORT_STATUS_HEADER)];
      final boolean isOpen = "O".equals(airportStatus);
      insertAirportStatement.setBoolean(++fieldCount, isOpen);

      //   is_public
      final String airportUse = airportFields[headerPosition.get(AIRPORT_USE_HEADER)];
      final boolean isPublic = "PU".equals(airportUse);
      insertAirportStatement.setBoolean(++fieldCount, isPublic);

      //   is_towered
      final String airportControlTower =
          airportFields[headerPosition.get(AIRPORT_CONTROL_TOWER_HEADER)];
      final boolean isTowered = "Y".equals(airportControlTower);
      insertAirportStatement.setBoolean(++fieldCount, isTowered);

      //   is_military
      final String airportOwnership =
          airportFields[headerPosition.get(AIRPORT_OWNERSHIP_HEADER)];
      final boolean isMilitary =  airportOwnership.startsWith("M");
      insertAirportStatement.setBoolean(++fieldCount, isMilitary);

      //   cell_id
      int cellId = CustomGridUtil.GetCellId(airportPosition);
      insertAirportStatement.setInt(++fieldCount, cellId);

      //   rank
      //   TODO: Use ranking algorithm instead of daily ops.
      int totalOps = 0;
      for (String AIRPORT_OP_HEADER: AIRPORT_OPS_HEADERS) {
        final int currentOpPosition = headerPosition.get(AIRPORT_OP_HEADER);
        if (airportFields.length <= currentOpPosition) {
          continue;
        }

        final String currentOpsString = airportFields[currentOpPosition];
        if ("".equals(currentOpsString)) {
          continue;
        }

        totalOps += Integer.parseInt(currentOpsString);
      }

      int rank;
      if (totalOps < 1) {
        rank = 0;
      } else if (totalOps < 16300) {
        rank = 1;
      } else if (totalOps < 64200) {
        rank = 2;
      } else if (totalOps < 145000) {
        rank = 3;
      } else if (totalOps < 270000) {
        rank = 4;
      } else {
        rank = 5;
      }
      insertAirportStatement.setInt(++fieldCount, rank);

      // Insert in airport db
      insertAirportStatement.executeUpdate();

      // Retrieve db id of freshly added airport
      getAirportIdStatement.setString(1, airportIcaoCode);
      ResultSet airportIdResultSet = getAirportIdStatement.executeQuery();
      if (! airportIdResultSet.next()) {
        throw new RuntimeException("Could not determine db id of airport: " + airportIcaoCode);
      }
      int airportDbId = airportIdResultSet.getInt(1);

      // Map SiteNumber to icao code for future reference by runway parser
      final String airportSiteNumber =
          airportFields[headerPosition.get(AIRPORT_SITE_NUMBER_HEADER)];
      airportSiteNumberToDbId.put(airportSiteNumber, airportDbId);

      // Additional properties
      //   Elevation
      final String airportElevation = airportFields[headerPosition.get(AIRPORT_ELEVATION_HEADER)];
      addAirportProperty(airportDbId, AIRPORT_ELEVATION_HEADER, airportElevation);


      //   Beacon Color
      try {
        final String airportBeaconColor =
            airportFields[headerPosition.get(AIRPORT_BEACON_COLOR_HEADER)];
        final String airportBeaconColorProperty = parseAirportBeaconColor(airportBeaconColor);
        if (airportBeaconColorProperty != null) {
          addAirportProperty(airportDbId, AIRPORT_BEACON_COLOR_HEADER, airportBeaconColorProperty);
        }
      } catch (Exception ex) {}

      //   Fuel Types
      try {
        final String airportFuelTypes =
            airportFields[headerPosition.get(AIRPORT_FUEL_TYPES_HEADER)];
        final String airportFuelTypesProperty = parseAirportFuelTypes(airportFuelTypes);
        if (airportFuelTypesProperty != null) {
          addAirportProperty(airportDbId, AIRPORT_FUEL_TYPES_HEADER, airportFuelTypesProperty);
        }
      } catch (Exception ex) { }

      //   Non-Commercial Landing Fee
      try {
        final String airportLandingFee =
            airportFields[headerPosition.get(AIRPORT_LANDING_FEE_HEADER)];
        final String airportLandingFeeProperty = parseBooleanProperty(airportLandingFee);
        if (airportLandingFeeProperty != null) {
          addAirportProperty(airportDbId, AIRPORT_LANDING_FEE_HEADER, airportLandingFeeProperty);
        }
      } catch (Exception ex) { }

      //   Segmented circle
      try {
        final String airportSegmentedCircle =
            airportFields[headerPosition.get(AIRPORT_SEGMENTED_CIRCLE_HEADER)];
        final String airportSegmentedCircleProperty = parseBooleanProperty(airportSegmentedCircle);
        if (airportSegmentedCircleProperty != null) {
          addAirportProperty(airportDbId,
                             AIRPORT_SEGMENTED_CIRCLE_HEADER,
                             airportSegmentedCircleProperty);
        }
      } catch (Exception ex) { }

      //   Effective date
      final String airportEffectiveDate =
          airportFields[headerPosition.get(AIRPORT_EFFECTIVE_DATE_HEADER)];
      if (airportEffectiveDate.isEmpty()) {
        throw new RuntimeException("Cannot accept empty effective date!");
      }
      addAirportProperty(airportDbId, AIRPORT_EFFECTIVE_DATE_HEADER, airportEffectiveDate);

      // Wind indicator field can be "N", "Y", "Y-L" or non existent.
      try {
        final int windIndicatorHeaderPosition = headerPosition.get(AIRPORT_WIND_INDICATOR_HEADER);
        if (airportFields.length > windIndicatorHeaderPosition) {
          final String airportWindIndicator = airportFields[windIndicatorHeaderPosition];
          final String airportWindIndicatorProperty =
              parseAirportWindIndicatorProperty(airportWindIndicator);
          addAirportProperty(airportDbId,
                             AIRPORT_WIND_INDICATOR_HEADER,
                             airportWindIndicatorProperty);
        }
      } catch (Exception ex) { }
    }

    in.close();
    insertAirportStatement.close();
    getAirportIdStatement.close();
  }

  /**
   * Adds a property to an airport.
   *
   * Both key and value strings are converted to constants.
   * If the value string can be parsed as an integer, it is not converted.
   */
  private void addAirportProperty(final int airportDbId, final String key, final String value)
      throws SQLException {
    if (insertAirportPropertyStatement == null) {
      insertAirportPropertyStatement = dbConn.prepareStatement(
          "INSERT INTO airport_properties (airport_id, key, value) VALUES (?, ?,?)");
    }

    final String keyLabel = AIRPORT_PROPERTIES_LABEL_MAP.get(key);
    final int keyLabelConstantId = getConstantId(keyLabel);
    int valueConstantId;
    try {
      valueConstantId = Integer.parseInt(value);
    } catch (Exception ex) {
      valueConstantId = getConstantId(value);
    }

    insertAirportPropertyStatement.setInt(1, airportDbId);
    insertAirportPropertyStatement.setInt(2, keyLabelConstantId);
    insertAirportPropertyStatement.setInt(3, valueConstantId);

    insertAirportPropertyStatement.executeUpdate();
    return;
  }

  /**
   * Parse runway and add airport data to the database.
   */
  private void addRunwayDataToDb() throws SQLException, IOException {
    final BufferedReader in = new BufferedReader(new FileReader(runwaySourceFile));
    String line = in.readLine();

    if (line == null) {
      throw new RuntimeException("Runway Master Record file is empty.");
    }

    // Parse first line to determine headers
    final String[] headers = line.split("\\t");
    final Map<String, Integer> headerPosition = new HashMap<String, Integer>();
    int position = 0;
    for (String header: headers) {
      if (! header.matches("\"\\w+\"")) {
        throw new RuntimeException("Unrecognized header format: " + header);
      }

      header = header.substring(1,header.length()-1);
      headerPosition.put(header, position++);
    }

    // Initialize db and prepare statements
    initRunwayTables();

    if (insertRunwayStatement == null) {
      insertRunwayStatement = dbConn.prepareStatement(
          "INSERT INTO runways (airport_id, letters, length, width, surface) " +
          "VALUES (?, ?, ?, ?, ?);");
      getRunwayIdStatement = dbConn.prepareStatement(
          "SELECT _id FROM runways WHERE airport_id = ? AND letters = ?");
    }

    // Parse runway lines
    while ((line = in.readLine()) != null) {
      final String[] runwayFields = line.split("\\t");

      // Get corresponding airport
      final String runwaySiteNumber = runwayFields[headerPosition.get(RUNWAY_SITE_NUMBER_HEADER)];
      final Integer airportDbId = airportSiteNumberToDbId.get(runwaySiteNumber);
      if (airportDbId == null) {
        System.err.println("Could not find airport id for site number: " + runwaySiteNumber);
      }
      final String runwayLetters =
          runwayFields[headerPosition.get(RUNWAY_LETTERS_HEADER)].substring(1); // Remove first '
      final int runwayLength =
          Integer.parseInt(runwayFields[headerPosition.get(RUNWAY_LENGTH_HEADER)]);
      final int runwayWidth =
          Integer.parseInt(runwayFields[headerPosition.get(RUNWAY_WIDTH_HEADER)]);
      final String runwaySurface =
          parseRunwaySurface(runwayFields[headerPosition.get(RUNWAY_SURFACE_HEADER)]);

      // Fill prepared statement
      insertRunwayStatement.setInt(1, airportDbId);
      insertRunwayStatement.setString(2, runwayLetters);
      insertRunwayStatement.setInt(3, runwayLength);
      insertRunwayStatement.setInt(4, runwayWidth);
      insertRunwayStatement.setInt(5, getConstantId(runwaySurface));

      insertRunwayStatement.executeUpdate();

      // Retrieve db id of freshly added runway
      getRunwayIdStatement.setInt(1, airportDbId);
      getRunwayIdStatement.setString(2, runwayLetters);
      ResultSet runwayIdResultSet = getRunwayIdStatement.executeQuery();
      if (! runwayIdResultSet.next()) {
        throw new RuntimeException(
            String.format("Could not determine db id of runway %s (airport db id: %d)",
                          runwayLetters,
                          airportDbId));
      }
      int runwayDbId = runwayIdResultSet.getInt(1);

      // Add runway ends
      addRunwayEndDataToDb(runwayDbId, runwayFields, headerPosition);
    }

    in.close();
    insertRunwayStatement.close();
    getRunwayIdStatement.close();

    dbConn.close();
  }

  private void addRunwayEndDataToDb(final int runwayDbId,
                                    final String[] runwayFields,
                                    final Map<String, Integer> headerPosition) throws SQLException {
    addRunwayEndDataToDb(runwayDbId, runwayFields, headerPosition, BASE_RUNWAY_END_HEADER_PREFIX);
    addRunwayEndDataToDb(
        runwayDbId, runwayFields, headerPosition, RECIPROCAL_RUNWAY_END_HEADER_PREFIX);
  }

  private void addRunwayEndDataToDb(final int runwayDbId,
                                    final String[] runwayFields,
                                    final Map<String, Integer> headerPosition,
                                    final String runwayEndPrefix) throws SQLException {

    if (insertRunwayEndStatement == null) {
      insertRunwayEndStatement = dbConn.prepareStatement(
          "INSERT INTO runway_ends (runway_id, letters) " +
          "VALUES (?, ?);");
      getRunwayEndIdStatement = dbConn.prepareStatement(
          "SELECT _id FROM runway_ends WHERE runway_id = ? AND letters = ?");
    }

    final String RUNWAY_END_LETTERS_HEADER = runwayEndPrefix + RUNWAY_END_LETTERS_HEADER_SUFFIX;

    final String runwayEndLetters =
        runwayFields[headerPosition.get(RUNWAY_END_LETTERS_HEADER)].substring(1);

    if ("".equals(runwayEndLetters)) {
      // Skip runway ends with no letters (heliports)
      return;
    }


    // Fill prepared statement
    insertRunwayEndStatement.setInt(1, runwayDbId);
    insertRunwayEndStatement.setString(2, runwayEndLetters);

    insertRunwayEndStatement.executeUpdate();

    // Retrieve db id of freshly added runway
    getRunwayEndIdStatement.setInt(1, runwayDbId);
    getRunwayEndIdStatement.setString(2, runwayEndLetters);
    ResultSet runwayEndIdResultSet = getRunwayEndIdStatement.executeQuery();
    if (! runwayEndIdResultSet.next()) {
      throw new RuntimeException(
          String.format("Could not determine db id of runway end %s (runway db id: %d)",
                        runwayEndLetters,
                        runwayDbId));
    }
    int runwayEndDbId = runwayEndIdResultSet.getInt(1);

    // Add runway end properties:
    //   True Alignment
    final String RUNWAY_END_TRUE_ALIGNMENT_HEADER =
        runwayEndPrefix + RUNWAY_END_TRUE_ALIGNMENT_HEADER_SUFFIX;
    try {
      // If True Alignment property is there, use it to determine runway heading.
      final String runwayEndTrueAlignment =
        runwayFields[headerPosition.get(RUNWAY_END_TRUE_ALIGNMENT_HEADER)];
      if (!"".equals(runwayEndTrueAlignment)) {
        addRunwayEndProperty(
          runwayEndDbId, RUNWAY_END_TRUE_ALIGNMENT_HEADER_SUFFIX, runwayEndTrueAlignment);
      }
    } catch (Exception ex) {
      // Ignore exceptions: optional property.
    }

    //   Runway end identifier lights

    final String RUNWAY_END_REIL_HEADER = runwayEndPrefix + RUNWAY_END_REIL_HEADER_SUFFIX;
    try {
      final String runwayEndReil = runwayFields[headerPosition.get(RUNWAY_END_REIL_HEADER)];
      final String runwayEndReilProperty = parseBooleanProperty(runwayEndReil);
      if (runwayEndReilProperty != null) {
        addRunwayEndProperty(runwayEndDbId, RUNWAY_END_REIL_HEADER_SUFFIX, runwayEndReilProperty);
      }
    } catch (Exception ex) {
      // Ignore (optional property)
    }

    //    Traffic pattern
    final String RUNWAY_END_RIGHT_TRAFFIC_HEADER =
        runwayEndPrefix + RUNWAY_END_RIGHT_TRAFFIC_HEADER_SUFFIX;
    try {
      final String runwayEndRightTrafficPattern =
          runwayFields[headerPosition.get(RUNWAY_END_RIGHT_TRAFFIC_HEADER)];
      final String runwayEndTrafficPatternProperty =
          parseTrafficPatternProperty(runwayEndRightTrafficPattern);
      if (runwayEndTrafficPatternProperty != null) {
        addRunwayEndProperty(
            runwayEndDbId, RUNWAY_END_RIGHT_TRAFFIC_HEADER_SUFFIX, runwayEndTrafficPatternProperty);
      }
    } catch (Exception ex) {
      // Ignore (optional property)
    }

    //    Visual glide slope indicators
    final String RUNWAY_END_VASI_HEADER = runwayEndPrefix + RUNWAY_END_VASI_HEADER_SUFFIX;
    try {
      final String runwayEndVasi = runwayFields[headerPosition.get(RUNWAY_END_VASI_HEADER)];
      final String runwayEndVasiProperty = parseVasiProperty(runwayEndVasi);
      if (runwayEndVasiProperty != null) {
        addRunwayEndProperty(runwayEndDbId, RUNWAY_END_VASI_HEADER_SUFFIX, runwayEndVasiProperty);
    }
    } catch (Exception ex) {
      // Ignore (optional property)
    }
  }
  private void addRunwayEndProperty(final int runwayEndDbId, final String key, final String value)
      throws SQLException {
    if (insertRunwayEndPropertyStatement == null) {
      insertRunwayEndPropertyStatement = dbConn.prepareStatement(
          "INSERT INTO runway_end_properties (runway_end_id, key, value) VALUES (?, ?,?)");
    }

    final String keyLabel = RUNWAY_END_PROPERTIES_LABEL_MAP.get(key);
    final int keyLabelConstantId = getConstantId(keyLabel);
    int valueConstantId;
    try {
      valueConstantId = Integer.parseInt(value);
    } catch (Exception ex) {
      valueConstantId = getConstantId(value);
    }

    insertRunwayEndPropertyStatement.setInt(1, runwayEndDbId);
    insertRunwayEndPropertyStatement.setInt(2, keyLabelConstantId);
    insertRunwayEndPropertyStatement.setInt(3, valueConstantId);

    insertRunwayEndPropertyStatement.executeUpdate();
    return;
  }

  private String getAirportIcaoCode(final String airportIataCode) {
    final String knownAirportIcaoCode = airportIataToIcaoCodes.get(airportIataCode);
    return knownAirportIcaoCode == null ? airportIataCode : knownAirportIcaoCode;
  }

  /**
   * Return the id of a given constant text from the constants db table.
   * If such a constant doesn't exist, it is added and the freshly created id is returned.
   */
  private int getConstantId(String constant) throws SQLException {
    // Prepare statements if necessary: use previously created if possible
    if (constantCache.containsKey(constant)) {
      return constantCache.get(constant);
    }

    final PreparedStatement getConstantIdStatement =
        dbConn.prepareStatement("SELECT _id FROM constants WHERE constant = ?");
    final PreparedStatement insertConstantStatement =
        dbConn.prepareStatement("INSERT INTO constants (constant) VALUES (?)");

    insertConstantStatement.setString(1, constant);
    insertConstantStatement.executeUpdate();

    getConstantIdStatement.setString(1, constant);
    final ResultSet constantIdResultSet = getConstantIdStatement.executeQuery();
    if (! constantIdResultSet.next()) {
      throw new RuntimeException("Error while adding constant to db: " + constant);
    }
    final int constantId = constantIdResultSet.getInt(1);
    constantCache.put(constant, constantId);
    return constantId;
  }


  /**
   * Create airports db table.  Drop existing table if necessary.
   */
  private void initAirportTables() throws SQLException {
    Statement stat = null;
    try {
      stat = dbConn.createStatement();
      // airports
      stat.executeUpdate("DROP TABLE IF EXISTS airports;");
      stat.executeUpdate("DROP INDEX IF EXISTS airports_cell_id_index;");
      stat.executeUpdate("CREATE TABLE airports (" +
                         "_id INTEGER PRIMARY KEY ASC, " +
                         "icao TEXT UNIQUE NOT NULL, " +
                         "name TEXT NOT NULL, " +
                         "type INTEGER NOT NULL, " +
                         "city TEXT NOT NULL, " +
                         "lat INTEGER NOT NULL, " +
                         "lng INTEGER NOT NULL, " +
                         "is_open BOOLEAN NOT NULL, " +
                         "is_public BOOLEAN NOT NULL, " +
                         "is_towered BOOLEAN NOT NULL, " +
                         "is_military BOOLEAN NOT NULL, " +
                         "rank INTEGER NOT NULL, " +
                         "cell_id INTEGER NOT NULL);");
      stat.executeUpdate("CREATE INDEX airports_cell_id_index ON airports (cell_id)");

      // airport_properties
      stat.executeUpdate("DROP TABLE IF EXISTS airport_properties");
      stat.executeUpdate("DROP INDEX IF EXISTS airport_properties_airport_id_index;");
      stat.executeUpdate("CREATE TABLE airport_properties (" +
                         "_id INTEGER PRIMARY KEY ASC, " +
                         "airport_id INTEGER NOT NULL, " +
                         "key INTEGER NOT NULL, " +
                         "value INTEGER NOT NULL);");
      stat.executeUpdate("CREATE INDEX airport_properties_airport_id_index ON " +
                         "airport_properties (airport_id)");
    } finally {
      if (stat != null)
        stat.close();
    }
  }

  /**
   * Create metadata db table, needed by android.
   */
  private void initAndroidMetadataTable() throws SQLException {
    Statement stat = dbConn.createStatement();
    stat.executeUpdate("DROP TABLE IF EXISTS android_metadata;");
    stat.executeUpdate("CREATE TABLE android_metadata (locale TEXT);");
    stat.executeUpdate("INSERT INTO android_metadata VALUES ('en_US');");
    stat.close();
  }

  /**
   * Initialize constants db table.
   */
  private void initConstantsTable() throws SQLException {
    Statement stat = null;
    try {
      stat = dbConn.createStatement();
      stat.executeUpdate("DROP TABLE IF EXISTS constants;");
      stat.executeUpdate("DROP INDEX IF EXISTS constants_constant_index;");
      stat.executeUpdate("CREATE TABLE constants (" +
                         "_id INTEGER PRIMARY KEY ASC, " +
                         "constant TEXT UNIQUE NOT NULL);");
    } finally {
      if (stat != null)
        stat.close();
    }
  }

  /**
   * Gets a connection to the database.
   */
  private Connection initDB() throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    return DriverManager.getConnection("jdbc:sqlite:" + targetFile);
  }

  /**
   * Create airports db table.  Drop existing table if necessary.
   */
  private void initRunwayTables() throws SQLException {
    Statement stat = null;
    try {
      stat = dbConn.createStatement();
      // runways
      stat.executeUpdate("DROP TABLE IF EXISTS runways;");
      stat.executeUpdate("DROP INDEX IF EXISTS runways_airport_id_index;");
      stat.executeUpdate("CREATE TABLE runways (" +
                         "_id INTEGER PRIMARY KEY ASC, " +
                         "airport_id INTEGER NOT NULL, " +
                         "letters TEXT NOT NULL, " +
                         "length INTEGER NOT NULL, " +
                         "width INTEGER NOT NULL, " +
                         "surface INTEGER NOT NULL);");
      stat.executeUpdate("CREATE INDEX runways_airport_id_index ON runways (airport_id)");

      // runway_ends
      stat.executeUpdate("DROP TABLE IF EXISTS runway_ends;");
      stat.executeUpdate("DROP INDEX IF EXISTS runway_ends_runway_id_index;");
      stat.executeUpdate("CREATE TABLE runway_ends (" +
                         "_id INTEGER PRIMARY KEY ASC, " +
                         "runway_id INTEGER NOT NULL, " +
                         "letters TEXT NOT NULL);");
      stat.executeUpdate("CREATE INDEX runway_ends_runway_id_index ON runway_ends (runway_id)");

      // runway_end_properties
      stat.executeUpdate("DROP TABLE IF EXISTS runway_end_properties");
      stat.executeUpdate("DROP INDEX IF EXISTS runway_end_properties_runway_end_id_index;");
      stat.executeUpdate("CREATE TABLE runway_end_properties (" +
                         "_id INTEGER PRIMARY KEY ASC, " +
                         "runway_end_id INTEGER NOT NULL, " +
                         "key INTEGER NOT NULL, " +
                         "value INTEGER NOT NULL);");
      stat.executeUpdate("CREATE INDEX runway_end_properties_runway_end_id_index ON " +
                         "runway_end_properties (runway_end_id)");
    } finally {
      if (stat != null)
        stat.close();
    }
  }

  private void execute() {
    try {
      dbConn = initDB();
      initAndroidMetadataTable();
      initConstantsTable();
      addAirportDataToDb();
      addRunwayDataToDb();
      dbConn.close();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(2);
    }
  }

  private static String capitalize(String text) {
    return WordUtils.capitalize(text.toLowerCase());
  }

  private static String getAirportNameToDisplay(String airportID, String airportName) {
    // TODO: Simplify name for display
    return capitalize(airportName);
  }

  private static String parseAirportBeaconColor(String airportBeaconColor) {
    if ("".equals(airportBeaconColor)) {
      return null;
    }
    if ("CG".equals(airportBeaconColor) || "G".equals(airportBeaconColor)) {
      return "White-Green (Lighted land airport)";
    } else if ("CY".equals(airportBeaconColor) || "Y".equals(airportBeaconColor)) {
      return "White-Yellow (Lighted seaplane base)";
    } else if ("CGY".equals(airportBeaconColor)) {
      return "White-Green-Yellow (Heliport)";
    } else if ("SCG".equals(airportBeaconColor)) {
      return "Split White-Green (Lighted military airport)";
    } else if ("C".equals(airportBeaconColor)) {
      return "White (Unlighted land airport)";
    } else {
      throw new RuntimeException("Unknown Beacon Color: " + airportBeaconColor);
    }
  }

  private static String parseAirportFuelTypes(String airportFuelTypes) {
    final StringBuilder airportFuelTypesBuffer = new StringBuilder(airportFuelTypes);
    final StringBuilder airportFuelTypesPropertyBuffer =
        new StringBuilder(airportFuelTypes.length());
    final String separator = "\n";
    int length;
    while ( (length = airportFuelTypesBuffer.length()) > 0) {
      final int nextMark = Math.min(length, 5);
      String fuelType = airportFuelTypesBuffer.substring(0, nextMark).trim();
      airportFuelTypesBuffer.delete(0, nextMark);
      if ("".equals(fuelType)) {
        continue;
      } else if ("80".equals(fuelType)) {
        fuelType += ": Grade 80 gasoline (red)";
      } else if ("100".equals(fuelType)) {
        fuelType += ": Grade 100 gasoline (green)";
      } else if ("100LL".equals(fuelType)) {
        fuelType += ": Grade 100LL gasoline (blue)";
      } else if ("115".equals(fuelType)) {
        fuelType += ": Grade 115 gasoline";
      } else if ("A".equals(fuelType)) {
        fuelType += ": Jet A kerozene (freeze point -40C)";
      } else if ("A1".equals(fuelType)) {
        fuelType += ": Jet A-1 kerozene (freeze point -50C)";
      } else if ("A1+".equals(fuelType)) {
        fuelType += ": Jet A-1 kerozene with icing inhibitor (freeze point -50C)";
      } else if ("B".equals(fuelType)) {
        fuelType += ": Jet B wide-cut turbine fuel (freeze point -50C)";
      } else if ("B+".equals(fuelType)) {
        fuelType += ": Jet B wide-cut turbine fuel with icing inhibitor (freeze point -50C)";
      } else if ("MOGAS".equals(fuelType)) {
        fuelType = "Automotive gasoline";
      }  // Some fuel types are unknown (eg. A+).  Keep as is.
      airportFuelTypesPropertyBuffer.append(fuelType);
      airportFuelTypesPropertyBuffer.append(separator);
    }
    // Remove trailing ", "
    length = airportFuelTypesPropertyBuffer.length();

    if (length > 0) {
      airportFuelTypesPropertyBuffer.delete(length-separator.length(), length);
      return airportFuelTypesPropertyBuffer.toString();
    } else {
      return null;
    }
  }

  private String parseAirportWindIndicatorProperty(final String  airportWindIndicator) {
    if ("Y-L".equals(airportWindIndicator)) {
      return "Yes, Lighted";
    } else {
      try {
        return parseBooleanProperty(airportWindIndicator);
      } catch (RuntimeException rex) {
        throw new RuntimeException("Unknown wind indicator value: " + airportWindIndicator, rex);
      }
    }
  }

  private static String parseBooleanProperty(final String booleanText) {
    if (booleanText.isEmpty()) {
      return null;
    }
    if ("Y".equals(booleanText)) {
      return "Yes";
    } else if ("N".equals(booleanText)) {
      return "No";
    } else {
      throw new RuntimeException("Unknown boolean value: " + booleanText);
    }
  }

  private static String parseRunwaySurface(final String runwayTypeCondition) {
    final StringBuilder runwayTypeConditionBuffer = new StringBuilder(runwayTypeCondition);
    final StringBuilder runwaySurfaceBuffer = new StringBuilder();
    final String separator = "\n";

    int length;
    while ( (length = runwayTypeConditionBuffer.length()) > 0) {
      int nextMark = runwayTypeConditionBuffer.indexOf("-");
      if (nextMark == -1) {
        nextMark = length;
      }

      String runwayTypeOrCondition = runwayTypeConditionBuffer.substring(0, nextMark).trim();
      runwayTypeConditionBuffer.delete(0, nextMark+1);

      // Normalization
      if ("GRVL".equals(runwayTypeOrCondition)) {
        runwayTypeOrCondition = "GRAVEL";
      }

      if (RUNWAY_SURFACE_MAP.containsKey(runwayTypeOrCondition)) {
        runwayTypeOrCondition += ": " + RUNWAY_SURFACE_MAP.get(runwayTypeOrCondition);
      }
      runwaySurfaceBuffer.append(runwayTypeOrCondition);
      runwaySurfaceBuffer.append(separator);
    }
    // Remove trailing ", "
    length = runwaySurfaceBuffer.length();

    if (length > 0) {
      runwaySurfaceBuffer.delete(length-separator.length(), length);
      return runwaySurfaceBuffer.toString();
    } else {
      return "None";
    }
  }

  private static String parseTrafficPatternProperty(final String rightTrafficPattern) {
    if ("".equals(rightTrafficPattern)) {
      return null;
    }

    if ("Y".equals(rightTrafficPattern)) {
      return "Right";
    } else if ("N".equals(rightTrafficPattern)) {
      return "Left";
    } else {
      System.out.println("Unknown right traffic pattern value: " + rightTrafficPattern);
      return rightTrafficPattern;
    }
  }

  private static String parseVasiProperty(final String vasi) {
    if ("".equals(vasi)) {
      return null;
    }
    String description;
    if ("SAVASI".equals(vasi)) {
      return "Simplified abbreviated visual approach slope indicator";
    } else if ("VASI".equals(vasi)) {
      return "Visual approach slope indicator";
    } else if ("PAPI".equals(vasi)) {
      return "Precision approach path indicator";
    } else if ("tri".equals(vasi)) {
      return "Tri-color visual approach slope indicator)";
    } else if ("PSI".equals(vasi)) {
      return "Pulsating visual approach slope indicator";
    } else {
      return vasi;
    }
  }

  public static void main(String args[]) {
    //TODO: Use GetOpt
    if (args.length != 4) {
      System.err.println("Usage: java AviationMasterRecordParser <airport master record file>" +
                         " <runway master record file> <iata to icao> <DB file>");
      System.exit(1);
    }

    (new AviationMasterRecordParser(args[0], args[1], args[2], args[3])).execute();
  }

}

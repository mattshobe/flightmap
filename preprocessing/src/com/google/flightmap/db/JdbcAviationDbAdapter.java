/*
 * Copyright (C) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.flightmap.db;

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.data.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * JDBC adapter for aviation database
 *
 * This adapted is NOT meant to be used in production environment, as it is neither optimized nor
 * thread-safe.  It is rather intended to be used during in preprocessing, mainly to populate the
 * aviation database.
 */
public class JdbcAviationDbAdapter implements AviationDbAdapter {
  private final Connection dbConn;

  private PreparedStatement getAirportDataFromIdStmt;
  private PreparedStatement getAirportIdFromIcaoStmt;
  private PreparedStatement getAirportIdsInCellsStmt;
  private PreparedStatement getRunwayEndPropertiesStmt;
  private PreparedStatement getAirportPropertiesStmt;
  private PreparedStatement getConstantStmt;
  private PreparedStatement getRunwaysStmt;
  private PreparedStatement getRunwayEndsStmt;
  private PreparedStatement getAirportCommsStmt;

  // TODO(aristidis): Eliminate code duplication (see AndroidAviationDbAdapter)
  private static final HashSet<String> INTEGER_AIRPORT_PROPERTIES;
  private static final HashSet<String> INTEGER_RUNWAY_END_PROPERTIES;
  static {
    INTEGER_AIRPORT_PROPERTIES = new HashSet<String>();
    INTEGER_AIRPORT_PROPERTIES.add("Elevation");

    INTEGER_RUNWAY_END_PROPERTIES = new HashSet<String>();
    INTEGER_RUNWAY_END_PROPERTIES.add("True Alignment");
  }

  public JdbcAviationDbAdapter(final Connection dbConn) {
    this.dbConn = dbConn;
  }

  public synchronized void open() {
    throw new RuntimeException(
        "Invalid call: connection must be managed at caller.");
  }

  public synchronized void close() {
    throw new RuntimeException(
        "Invalid call: connection must be managed at caller.");
  }


  public Airport getAirport(final int airportId) {
    try {
      if (getAirportDataFromIdStmt == null) {
        getAirportDataFromIdStmt = dbConn.prepareStatement(
            "SELECT icao, name, type, city, lat, lng, is_open, is_public, is_towered, is_military" +
            ", rank FROM airports WHERE _id = ?");
      }

      getAirportDataFromIdStmt.setInt(1, airportId);
      ResultSet airportData = getAirportDataFromIdStmt.executeQuery();

      if (! airportData.next()) {
        return null;
      }

      final String icao = airportData.getString("icao");
      final String name = airportData.getString("name");
      final int typeConstantId = airportData.getInt("type");
      final String city = airportData.getString("city");
      final int rank = airportData.getInt("rank");

      final String typeString = getConstant(typeConstantId);
      final Airport.Type type = getAirportType(typeString);
      final int latE6 = airportData.getInt("lat");
      final int lngE6 = airportData.getInt("lng");
      final boolean isOpen = airportData.getInt("is_open") == 1;
      final boolean isPublic = airportData.getInt("is_public") == 1;
      final boolean isTowered = airportData.getInt("is_towered") == 1;
      final boolean isMilitary = airportData.getInt("is_military") == 1;

      final Airport airport =
          new Airport(airportId, icao, name, type, city, new LatLng(latE6, lngE6), isOpen, isPublic,
              isTowered, isMilitary, getRunways(airportId), rank);

      return airport;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  public int getAirportIdByIcao(final String airportICAO) {
    try {
      if (getAirportIdFromIcaoStmt == null) {
        getAirportIdFromIcaoStmt = dbConn.prepareStatement(
            "SELECT id FROM airports WHERE icao = ?");
        }
      getAirportIdFromIcaoStmt.setString(1, airportICAO);
      ResultSet airportId = getAirportIdFromIcaoStmt.executeQuery();

      if (!airportId.next()) {
        return -1;
        }
      final int id = airportId.getInt("type");

      return id;
      } catch (SQLException sqlEx) {
        throw new RuntimeException(sqlEx);
      }
    }

  public String getConstant(final int constantId) {
    try {
      if (getConstantStmt == null) {
        getConstantStmt = dbConn.prepareStatement("SELECT constant FROM constants WHERE _id = ?");
      }

      getConstantStmt.setInt(1, constantId);
      final ResultSet constant = getConstantStmt.executeQuery();
      if (!constant.next()) {
        return null;
      }
      return constant.getString("constant");
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  private SortedSet<Runway> getRunways(final int airportId) throws SQLException {
    if (getRunwaysStmt == null) {
      getRunwaysStmt = dbConn.prepareStatement(
          "SELECT _id, letters, length, width, surface FROM runways WHERE airport_id = ?");
    }

    getRunwaysStmt.setInt(1, airportId);
    final ResultSet runwayData = getRunwaysStmt.executeQuery();
    final TreeSet<Runway> runways = new TreeSet<Runway>(Collections.reverseOrder());
    while (runwayData.next()) {
      final int runwayId = runwayData.getInt("_id");
      final String runwayLetters = runwayData.getString("letters");
      final int runwayLength = runwayData.getInt("length");
      final int runwayWidth = runwayData.getInt("width");
      final String runwaySurface = getConstant(runwayData.getInt("surface"));

      final SortedSet<RunwayEnd> runwayEnds = getRunwayEnds(runwayId);

      runways.add(new Runway(airportId, runwayLetters, runwayLength, runwayWidth, runwaySurface,
          runwayEnds));
    }

    return runways;
  }

  private SortedSet<RunwayEnd> getRunwayEnds(final int runwayId) throws SQLException {
    if (getRunwayEndsStmt == null) {
      getRunwayEndsStmt = dbConn.prepareStatement(
          "SELECT _id, letters FROM runway_ends WHERE runway_id = ?");
    }

    getRunwayEndsStmt.setInt(1, runwayId);
    final ResultSet runwayEndData = getRunwayEndsStmt.executeQuery();

    final TreeSet<RunwayEnd> runwayEnds = new TreeSet<RunwayEnd>();

    while (runwayEndData.next()) {
      final int runwayEndId = runwayEndData.getInt("_id");
      final String runwayEndLetters = runwayEndData.getString("letters");
      runwayEnds.add(new RunwayEnd(runwayEndId, runwayEndLetters));
    }

    return runwayEnds;
  }

  /**
   * Returns a lits of airports in the given cells with rank >= {@code minRank}.
   */
  public LinkedList<Airport> getAirportsInCells(final int startCell,
                                                final int endCell,
                                                final int minRank) {
    try {
      if (getAirportIdsInCellsStmt == null) {
        getAirportIdsInCellsStmt = dbConn.prepareStatement(
          "SELECT _id FROM airports WHERE cell_id >= ? AND cell_id < ? AND rank >= ?");
      }

      getAirportIdsInCellsStmt.setInt(1, startCell);
      getAirportIdsInCellsStmt.setInt(2, endCell);
      getAirportIdsInCellsStmt.setInt(3, minRank);

      ResultSet airportIds = getAirportIdsInCellsStmt.executeQuery();
      LinkedList<Airport> airports = new LinkedList<Airport>();
      while (airportIds.next()) {
        final int id = airportIds.getInt("_id");
        airports.add(getAirport(id));
      }
      return airports;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }
  
  public HashMap<String, String> getAirportProperties(final int airportId) {
    try {
      if (getAirportPropertiesStmt == null) {
        getAirportPropertiesStmt = dbConn.prepareStatement(
            "SELECT key, value FROM airport_properties WHERE airport_id = ?");
      }

      getAirportPropertiesStmt.setInt(1, airportId);
      ResultSet airportPropertyData = getAirportPropertiesStmt.executeQuery();

      HashMap<String, String> airportProperties = new HashMap<String, String>();

      while (airportPropertyData.next()) {
        final int keyConstantId = airportPropertyData.getInt("key");
        final int valueConstantId = airportPropertyData.getInt("value");
        final String key = getConstant(keyConstantId);
        String value;
        if (INTEGER_AIRPORT_PROPERTIES.contains(key)) {
          value = Integer.toString(valueConstantId);
        } else {
          value = getConstant(valueConstantId);
        }
        airportProperties.put(key, value);
      }
      return airportProperties;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  public HashMap<String, String> getRunwayEndProperties(int runwayEndId) {
    try {
      if (getRunwayEndPropertiesStmt == null) {
        getRunwayEndPropertiesStmt = dbConn.prepareStatement(
            "SELECT key, value FROM runway_end_properties WHERE runway_end_id = ?");
      }

      getRunwayEndPropertiesStmt.setInt(1, runwayEndId);
      ResultSet runwayEndPropertyData = getRunwayEndPropertiesStmt.executeQuery();

      HashMap<String, String> runwayEndProperties = new HashMap<String, String>();

      while (runwayEndPropertyData.next()) {
        final int keyConstantId = runwayEndPropertyData.getInt("key");
        final int valueConstantId = runwayEndPropertyData.getInt("value");
        final String key = getConstant(keyConstantId);
        String value;
        if (INTEGER_RUNWAY_END_PROPERTIES.contains(key)) {
          value = Integer.toString(valueConstantId);
        } else {
          value = getConstant(valueConstantId);
        }
        runwayEndProperties.put(key, value);
      }
      return runwayEndProperties;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }

  }

  /**
   * Return Airport.Type enum value corresponding to the given string.
   * TODO(aristidis): Eliminate code duplication (see AndroidAviationDbAdapter)
   */
  private static Airport.Type getAirportType(final String typeString) {
    if ("Airport".equals(typeString)) {
      return Airport.Type.AIRPORT;
    } else if ("Seaplane Base".equals(typeString)) {
      return Airport.Type.SEAPLANE_BASE;
    } else if ("Heliport".equals(typeString)) {
      return Airport.Type.HELIPORT;
    } else if ("Ultralight".equals(typeString)) {
      return Airport.Type.ULTRALIGHT;
    } else if ("Gliderport".equals(typeString)) {
      return Airport.Type.GLIDERPORT;
    } else if ("Balloonport".equals(typeString)) {
      return Airport.Type.BALLOONPORT;
    } else {
      return Airport.Type.OTHER;
    }
  }

  public List<Comm> getAirportComms(int airportId) {
    try {
      if (getAirportCommsStmt == null) {
        getAirportCommsStmt = dbConn.prepareStatement(
            "SELECT identifier, frequency, remarks from airport_comm WHERE airport_id = ?");
      }

      getAirportCommsStmt.setInt(1, airportId);
      ResultSet airportComms = getAirportCommsStmt.executeQuery();
      final List<Comm> comms = new LinkedList<Comm>();
      while (airportComms.next()) {
        final String identifier = airportComms.getString("identifier");
        final String frequency = airportComms.getString("frequency");
        final String remarks = airportComms.getString("remarks");
        final Comm comm = new Comm(identifier, frequency, remarks);
        comms.add(comm);
      }
      return comms;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

}

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

import com.google.flightmap.common.data.*;
import com.google.flightmap.common.db.AviationDbAdapter;
import com.google.flightmap.parsing.db.AviationDbReader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;  // TODO: Remove (See doSearch())
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * JDBC adapter for aviation database
 *
 * This adapted is NOT meant to be used in production environment, as it is neither optimized nor
 * thread-safe.  It is rather intended to be used during preprocessing, mainly to populate the
 * aviation database.
 */
public class JdbcAviationDbAdapter implements AviationDbAdapter, AviationDbReader {
  public final static String AIRSPACE_ALPHA = "ALPHA";
  public final static String AIRSPACE_BRAVO = "BRAVO";
  public final static String AIRSPACE_CHARLIE = "CHARLIE";
  public final static String AIRSPACE_DELTA = "DELTA";
  public final static String AIRSPACE_ECHO = "ECHO";
  public final static String AIRSPACE_OTHER = "OTHER";

  /**
   * Approximate number of airports in db.  The accuracy of this value is not critical: it is only
   * Used for optimization reasons (for instance, to initialize collections such as ArrayLists).
   */
  private final static int APPROXIMATE_AIRPORT_COUNT = 19755; // Last update: 28 Oct 2010

  private final Connection dbConn;

  private PreparedStatement getAirportCommsStmt;
  private PreparedStatement getAirportDataFromIdStmt;
  private PreparedStatement getAirportIdFromIcaoStmt;
  private PreparedStatement getAirportIdsInCellsStmt;
  private PreparedStatement getAirportIdsWithCityLikeStmt;
  private PreparedStatement getAirportIdsWithNameLikeStmt;
  private PreparedStatement getAirportPropertiesStmt;
  private PreparedStatement getAirspaceIdsInRect;
  private PreparedStatement getAirspaceArcsStmt;
  private PreparedStatement getAirspacePointsStmt;
  private PreparedStatement getAirspaceStmt;
  private PreparedStatement getConstantStmt;
  private PreparedStatement getRunwayEndIdStatement;
  private PreparedStatement getRunwayIdStatement;
  private PreparedStatement getRunwaysStmt;
  private PreparedStatement getRunwayEndPropertiesStmt;
  private PreparedStatement getRunwayEndsStmt;
  private PreparedStatement getMetadataStmt;

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

  @Override
  public synchronized void open() {
    throw new RuntimeException(
        "Invalid call: connection must be managed at caller.");
  }

  @Override
  public synchronized void close() {
    throw new RuntimeException(
        "Invalid call: connection must be managed at caller.");
  }

  @Override
  public Airport getAirport(final int airportId) {
    try {
      if (getAirportDataFromIdStmt == null) {
        getAirportDataFromIdStmt = dbConn.prepareStatement(
            "SELECT icao, name, type, city, lat, lng, is_open, is_public, is_towered, is_military" +
            ", rank FROM airports WHERE _id = ?");
      }

      getAirportDataFromIdStmt.setInt(1, airportId);
      final ResultSet rs = getAirportDataFromIdStmt.executeQuery();

      if (!rs.next()) {
        rs.close();
        return null;
      }

      final String icao = rs.getString("icao");
      final String name = rs.getString("name");
      final int typeConstantId = rs.getInt("type");
      final String city = rs.getString("city");
      final int rank = rs.getInt("rank");
      final int latE6 = rs.getInt("lat");
      final int lngE6 = rs.getInt("lng");
      final boolean isOpen = rs.getInt("is_open") == 1;
      final boolean isPublic = rs.getInt("is_public") == 1;
      final boolean isTowered = rs.getInt("is_towered") == 1;
      final boolean isMilitary = rs.getInt("is_military") == 1;
      rs.close();
      final String typeString = getConstant(typeConstantId);
      final Airport.Type type = getAirportType(typeString);
      final Airport airport = new Airport(
          airportId, icao, name, type, city, new LatLng(latE6, lngE6), isOpen, isPublic, isTowered,
          isMilitary, getRunways(airportId), rank);
      return airport;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  @Override
  public int getAirportIdByIcao(final String airportICAO) {
    try {
      if (getAirportIdFromIcaoStmt == null) {
        getAirportIdFromIcaoStmt = dbConn.prepareStatement(
            "SELECT _id FROM airports WHERE icao = ?");
        }
      getAirportIdFromIcaoStmt.setString(1, airportICAO);
      final ResultSet rs = getAirportIdFromIcaoStmt.executeQuery();
      final int id = rs.next() ? rs.getInt("_id") : -1;
      rs.close();
      return id;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  @Override
  public List<Integer> getAllAirportIds() {
    try {
      final ResultSet rs = dbConn.createStatement().executeQuery("SELECT _id FROM airports");
      final List<Integer> ids = new ArrayList<Integer>(APPROXIMATE_AIRPORT_COUNT);
      while (rs.next()) {
        ids.add(rs.getInt("_id"));
      }
      rs.close();
      return ids;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  @Override
  public List<Integer> getAirportIdsWithCityLike(final String pattern) {
    try {
      if (getAirportIdsWithCityLikeStmt == null) {
        getAirportIdsWithCityLikeStmt = dbConn.prepareStatement(
            "SELECT _id FROM airports WHERE city LIKE ?");
      }
      getAirportIdsWithCityLikeStmt.setString(1, pattern);
      final ResultSet rs = getAirportIdsWithCityLikeStmt.executeQuery();
      final List<Integer> airportIds = new LinkedList<Integer>();
      while (rs.next()) {
        airportIds.add(rs.getInt("_id"));
      }
      rs.close();
      return airportIds;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  @Override
  public List<Integer> getAirportIdsWithNameLike(final String pattern) {
    try {
      if (getAirportIdsWithNameLikeStmt == null) {
        getAirportIdsWithNameLikeStmt = dbConn.prepareStatement(
            "SELECT _id FROM airports WHERE name LIKE ?");
      }
      getAirportIdsWithNameLikeStmt.setString(1, pattern);
      final ResultSet rs = getAirportIdsWithNameLikeStmt.executeQuery();
      final List<Integer> airportIds = new LinkedList<Integer>();
      while (rs.next()) {
        airportIds.add(rs.getInt("_id"));
      }
      rs.close();
      return airportIds;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  /**
   * Returns airspace with given {@code id}.
   */
  private Airspace getAirspace(final int id) {
    try {
      if (getAirspaceStmt == null) {
        getAirspaceStmt = dbConn.prepareStatement(
            "SELECT name, class, low_alt, high_alt FROM airspaces WHERE _id = ?");
      }
      getAirspaceStmt.setInt(1, id);
      final ResultSet rs = getAirspaceStmt.executeQuery();
      if (!rs.next()) {
        rs.close();
        return null;
      }
      final String name = rs.getString("name");
      final int classConstantId = rs.getInt("class");
      final int lowAlt = rs.getInt("low_alt");
      final int highAlt = rs.getInt("high_alt");
      rs.close();
      final String classString = getConstant(classConstantId);
      final Airspace.Class airspaceClass = Airspace.Class.valueOf(classString);
      final SortedMap<Integer, LatLng> points = getAirspacePoints(id);
      final SortedMap<Integer, AirspaceArc> arcs = getAirspaceArcs(id);
      return new Airspace(id, name, airspaceClass, lowAlt, highAlt, points, arcs);
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  /**
   * Returns polygon points for a given airspace.
   */
  private SortedMap<Integer, LatLng> getAirspacePoints(final int airspaceId) {
    try {
      if (getAirspacePointsStmt == null) {
        getAirspacePointsStmt = dbConn.prepareStatement(
            "SELECT num, lat, lng FROM airspace_points WHERE airspace_id = ?");
      }
      getAirspacePointsStmt.setInt(1, airspaceId);
      final ResultSet rs = getAirspacePointsStmt.executeQuery();
      final SortedMap<Integer, LatLng> points = new TreeMap<Integer, LatLng>();
      while (rs.next()) {
        final int num = rs.getInt("num");
        final int lat = rs.getInt("lat");
        final int lng = rs.getInt("lng");
        points.put(num, new LatLng(lat, lng));
      }
      rs.close();
      return points;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  /**
   * Returns arcs for a given airspace.
   */
  private SortedMap<Integer, AirspaceArc> getAirspaceArcs(final int airspaceId) {
    try {
      if (getAirspaceArcsStmt == null) {
        getAirspaceArcsStmt = dbConn.prepareStatement(
            "SELECT num, min_lat, max_lat, min_lng, max_lng, start_angle, sweep_angle " + 
            "FROM airspace_arcs WHERE airspace_id = ?");
      }
      getAirspaceArcsStmt.setInt(1, airspaceId);
      final ResultSet rs = getAirspaceArcsStmt.executeQuery();
      final SortedMap<Integer, AirspaceArc> arcs = new TreeMap<Integer, AirspaceArc>();
      while (rs.next()) {
        final int num = rs.getInt("num");
        final int minLat = rs.getInt("min_lat");
        final int maxLat = rs.getInt("max_lat");
        final int minLng = rs.getInt("min_lng");
        final int maxLng = rs.getInt("max_lng");
        final float startAngle = rs.getInt("start_angle") * 1E-6f;
        final float sweepAngle = rs.getInt("sweep_angle") * 1E-6f;
        final LatLng swCorner = new LatLng(minLat, minLng);
        final LatLng neCorner = new LatLng(maxLat, maxLng);
        final LatLngRect boundingBox = new LatLngRect(swCorner, neCorner);
        arcs.put(num, new AirspaceArc(boundingBox, startAngle, sweepAngle));
      }
      rs.close();
      return arcs;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  @Override
  public Collection<Airspace> getAirspacesInRectangle(final LatLngRect rect) {
    try {
      if (getAirspaceIdsInRect == null) {
        getAirspaceIdsInRect = dbConn.prepareStatement(
          "SELECT _id FROM airspaces WHERE " + 
          "(MAX(min_lat, ?) < MIN(max_lat, ?)) AND (MAX(min_lng, ?) < MIN(max_lng, ?))");
      }
      getAirspaceIdsInRect.setInt(1, rect.getSouth());
      getAirspaceIdsInRect.setInt(2, rect.getNorth());
      getAirspaceIdsInRect.setInt(3, rect.getWest());
      getAirspaceIdsInRect.setInt(4, rect.getEast());
      final ResultSet rs = getAirspaceIdsInRect.executeQuery();
      final Collection<Airspace> airspaces = new LinkedList<Airspace>();
      while (rs.next()) {
        final int id = rs.getInt(1);
        airspaces.add(getAirspace(id));
      }
      rs.close();
      return airspaces;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }
  
  @Override
  public String getConstant(final int constantId) {
    try {
      if (getConstantStmt == null) {
        getConstantStmt = dbConn.prepareStatement("SELECT constant FROM constants WHERE _id = ?");
      }
      getConstantStmt.setInt(1, constantId);
      final ResultSet rs = getConstantStmt.executeQuery();
      final String constant = rs.next() ? rs.getString("constant") : null;
      rs.close();
      return constant;
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
    final ResultSet rs = getRunwaysStmt.executeQuery();
    final TreeSet<Runway> runways = new TreeSet<Runway>(Collections.reverseOrder());
    while (rs.next()) {
      final int runwayId = rs.getInt("_id");
      final String runwayLetters = rs.getString("letters");
      final int runwayLength = rs.getInt("length");
      final int runwayWidth = rs.getInt("width");
      final String runwaySurface = getConstant(rs.getInt("surface"));
      final SortedSet<RunwayEnd> runwayEnds = getRunwayEnds(runwayId);
      runways.add(new Runway(
            airportId, runwayLetters, runwayLength, runwayWidth, runwaySurface, runwayEnds));
    }
    rs.close();

    return runways;
  }

  private SortedSet<RunwayEnd> getRunwayEnds(final int runwayId) throws SQLException {
    if (getRunwayEndsStmt == null) {
      getRunwayEndsStmt = dbConn.prepareStatement(
          "SELECT _id, letters FROM runway_ends WHERE runway_id = ?");
    }
    getRunwayEndsStmt.setInt(1, runwayId);
    final ResultSet rs = getRunwayEndsStmt.executeQuery();
    final TreeSet<RunwayEnd> runwayEnds = new TreeSet<RunwayEnd>();
    while (rs.next()) {
      final int runwayEndId = rs.getInt("_id");
      final String runwayEndLetters = rs.getString("letters");
      runwayEnds.add(new RunwayEnd(runwayEndId, runwayEndLetters));
    }
    rs.close();
    return runwayEnds;
  }

  /**
   * Returns a lits of airports in the given cells with rank >= {@code minRank}.
   */
  public LinkedList<Airport> getAirportsInCells(final int startCell, final int endCell,
      final int minRank) {
    try {
      if (getAirportIdsInCellsStmt == null) {
        getAirportIdsInCellsStmt = dbConn.prepareStatement(
          "SELECT _id FROM airports WHERE cell_id >= ? AND cell_id < ? AND rank >= ?");
      }
      getAirportIdsInCellsStmt.setInt(1, startCell);
      getAirportIdsInCellsStmt.setInt(2, endCell);
      getAirportIdsInCellsStmt.setInt(3, minRank);
      final ResultSet rs = getAirportIdsInCellsStmt.executeQuery();
      final LinkedList<Airport> airports = new LinkedList<Airport>();
      while (rs.next()) {
        final int id = rs.getInt("_id");
        airports.add(getAirport(id));
      }
      rs.close();
      return airports;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }
  
  public Map<String, String> getAirportProperties(final int airportId) {
    try {
      if (getAirportPropertiesStmt == null) {
        getAirportPropertiesStmt = dbConn.prepareStatement(
            "SELECT key, value FROM airport_properties WHERE airport_id = ?");
      }
      getAirportPropertiesStmt.setInt(1, airportId);
      final ResultSet rs = getAirportPropertiesStmt.executeQuery();
      final Map<String, String> airportProperties = new HashMap<String, String>();
      while (rs.next()) {
        final int keyConstantId = rs.getInt("key");
        final int valueConstantId = rs.getInt("value");
        final String key = getConstant(keyConstantId);
        String value;
        if (INTEGER_AIRPORT_PROPERTIES.contains(key)) {
          value = Integer.toString(valueConstantId);
        } else {
          value = getConstant(valueConstantId);
        }
        airportProperties.put(key, value);
      }
      rs.close();
      return airportProperties;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  public Map<String, String> getRunwayEndProperties(int runwayEndId) {
    try {
      if (getRunwayEndPropertiesStmt == null) {
        getRunwayEndPropertiesStmt = dbConn.prepareStatement(
            "SELECT key, value FROM runway_end_properties WHERE runway_end_id = ?");
      }

      getRunwayEndPropertiesStmt.setInt(1, runwayEndId);
      ResultSet runwayEndPropertyData = getRunwayEndPropertiesStmt.executeQuery();

      final Map<String, String> runwayEndProperties = new HashMap<String, String>();

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
            "SELECT identifier, frequency, remarks FROM airport_comm WHERE airport_id = ?");
      }
      getAirportCommsStmt.setInt(1, airportId);
      final ResultSet rs = getAirportCommsStmt.executeQuery();
      final List<Comm> comms = new LinkedList<Comm>();
      while (rs.next()) {
        final String identifier = rs.getString("identifier");
        final String frequency = rs.getString("frequency");
        final String remarks = rs.getString("remarks");
        final Comm comm = new Comm(identifier, frequency, remarks);
        comms.add(comm);
      }
      rs.close();
      return comms;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  @Override
  public String getMetadata(final String key) {
    try {
      if (getMetadataStmt == null) {
        getMetadataStmt = dbConn.prepareStatement(
            "SELECT value FROM metadata WHERE key = ?");
      }
      getMetadataStmt.setString(1, key);
      final ResultSet rs = getMetadataStmt.executeQuery();
      final String metadata = rs.next() ? rs.getString("value") : null;
      rs.close();
      return metadata;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  @Override
  public Map<Integer, Integer> doSearch(final String query) {
    throw new RuntimeException("This method should not be in AviationDbAdapter. TODO: REMOVE");
  }

  @Override
  public int getRunwayId(final int airportId, final String letters) {
    try {
      if (getRunwayIdStatement == null) {
        getRunwayIdStatement = dbConn.prepareStatement(
            "SELECT _id FROM runways WHERE airport_id = ? AND letters = ?");
      }
      getRunwayIdStatement.setInt(1, airportId);
      getRunwayIdStatement.setString(2, letters);
      final ResultSet rs = getRunwayIdStatement.executeQuery();
      final int id = rs.next() ? rs.getInt("_id") : -1;
      rs.close();
      return id;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }

  @Override
  public int getRunwayEndId(final int runwayId, final String letters) {
    try {
      if (getRunwayEndIdStatement == null) {
        getRunwayEndIdStatement = dbConn.prepareStatement(
            "SELECT _id FROM runway_ends WHERE runway_id = ? AND letters = ?");
      }
      getRunwayEndIdStatement.setInt(1, runwayId);
      getRunwayEndIdStatement.setString(2, letters);
      final ResultSet rs = getRunwayEndIdStatement.executeQuery();
      final int id = rs.next() ? rs.getInt("_id") : -1;
      rs.close();
      return id;
    } catch (SQLException sqlEx) {
      throw new RuntimeException(sqlEx);
    }
  }
}

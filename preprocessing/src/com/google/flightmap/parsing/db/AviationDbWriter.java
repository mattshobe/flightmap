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

package com.google.flightmap.parsing.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface AviationDbWriter {
  /**
   * Opens database connection.
   */
  public void open() throws SQLException;

  /**
   * Closes database connection..
   */
  public void close() throws SQLException;

  /**
   * Resets database connection.
   */
  public void reset() throws SQLException;

  /**
   * Returns current database connection.
   */
  public Connection getConnection();

  /**
   * Returns a new database connection.
   */
  public Connection getNewConnection() throws SQLException;

  /**
   * Begin a transaction on the database.  All future operations will not be auto committed.  The
   * {@link #commit} or {@link #rollback} methods must be called later.
   */
  public void beginTransaction() throws SQLException;

  /**
   * Commits pending changes to the database.
   */
  public void commit() throws SQLException;

  /**
   * Rollsback pending changes to the database.
   */
  public void rollback() throws SQLException;

  /**
   * Creates database tables that will hold airport data.  Deletes existing tables.
   */
  public void initAirportTables() throws SQLException;

  /**
   * Creates database table that will hold airport comm data.  Deletes existing table.
   */
  public void initAirportCommTable() throws SQLException;

  /**
   * Creates airspace tables.  Does NOT delete existing tables.
   */
  public void initAirspaceTables() throws SQLException;

  /**
   * Create metadata db table, needed by android.
   */
  public void initAndroidMetadataTable() throws SQLException;

  /**
   * Initialize constants db table.
   */
  public void initConstantsTable() throws SQLException;

  /**
   * Initializes metadata db table.
   */
  public void initMetadataTable() throws SQLException;

  /**
   * Create airports db table.  Drop existing table if necessary.
   */
  public void initRunwayTables() throws SQLException;

  /**
   * Inserts new airport in database.
   */
  public void insertAirport(String icao, String name, String type, String city, int latE6,
      int lngE6, boolean isOpen, boolean isPublic, boolean isTowered, boolean isMilitary,
      int cellId, int rank) throws SQLException;

  /**
   * Adds a property to an airport.
   * <p>
   * Both key and value strings are converted to constants.
   * If the value string can be parsed as an integer, it is not converted.
   *
   * @param id Airport id
   * @param key Property key
   * @param value Property value.
   *
   * @return {@code true} if {@code value} was converted to a constant, {@code false} otherwise.
   */
  public boolean insertAirportProperty(int id, String key, String value) throws SQLException;

  /**
   * Adds a comm entry for an airport.
   *
   * @param id Airport id
   * @param identifier Station identifier (eg. "TWR")
   * @param frequency Station frequency (eg. "122.5")
   * @param remarks Additional remarks (eg. "N-S")
   */
  public void insertAirportComm(int id, String identifier, String frequency, String remarks)
      throws SQLException;

  /**
   * Inserts new airspace in database.
   *
   * @return Database id of newly inserted airspace.
   */
  public int insertAirspace(int airportId, String name, String classString, int minLat, int maxLat,
      int minLng, int maxLng, int lowAlt, int highAlt) throws SQLException;

  /**
   * Adds a point to an airspace shape.
   *
   * @param id Airspace id.
   * @param num Sequence number of point.
   * @param lat Latitude, in E6 format.
   * @param lng Longitude, in E6 format.
   */
  public void insertAirspacePoint(int id, int num, int lat, int lng) throws SQLException;

  /**
   * Adds an arc to an airspace shape.
   *
   * @param id Airspace id.
   * @param num Sequence number of arc.
   * @param minLat Minimum latitude of the circle containing the arc, in E6 format
   * @param maxLat Maximum latitude of the circle containing the arc, in E6 format
   * @param minLng Minimum longitude of the circle containing the arc, in E6 format
   * @param maxLng Maximum longitude of the circle containing the arc, in E6 format
   * @param startAngle Starting angle (in degrees E6) clockwise from East
   * @param sweepAngle Sweep angle (in degrees E6) measured clockwise.
   */
  public void insertAirspaceArc(int id, int num, int minLat, int maxLat, int minLng, int maxLng,
      int startAngle, int sweepAngle) throws SQLException;

  /**
   * Updates rank of airport.
   */
  public void updateAirportRank(int id, int rank) throws SQLException;

  /**
   * Inserts new runway in database.
   */
  public void insertRunway(int airportId, String letters, int length, int width, String surface)
      throws SQLException;

  /**
   * Inserts new (base/reciprocal) runway end in database.
   */
  public void insertRunwayEnd(int runwayId, String letters) throws SQLException;

  /**
   * Inserts new runway end property in database.
   */
  public boolean insertRunwayEndProperty(int runwayEndId, String key, String value)
      throws SQLException;
}

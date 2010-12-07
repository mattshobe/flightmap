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

package com.google.flightmap.db;

import com.google.flightmap.parsing.db.AviationDbWriter;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

public class JdbcAviationDbWriter implements AviationDbWriter {
  private final static String SQL_LITE_DRIVER = "org.sqlite.JDBC";

  // Database metadata
  private final static int DB_SCHEMA_VERSION = 2;
  private final static long DB_EXPIRATION_TIMESTAMP = 1294909260000L; // 13 Jan 2011 09:01:00 GMT

  private final File file;
  private Connection dbConn;

  // Frequent SQL Prepared Statements
  private PreparedStatement getConstantIdStatement;
  private PreparedStatement insertAirportCommStatement;
  private PreparedStatement insertAirportPropertyStatement;
  private PreparedStatement insertAirportStatement;
  private PreparedStatement insertConstantStatement;
  private PreparedStatement insertRunwayEndStatement;
  private PreparedStatement insertRunwayEndPropertyStatement;
  private PreparedStatement insertRunwayStatement;
  private PreparedStatement updateAirportRankStatement;

  private Map<String, Integer> constantCache = new HashMap<String, Integer>();

  public JdbcAviationDbWriter(final File file) throws ClassNotFoundException {
    Class.forName(SQL_LITE_DRIVER);
    this.file = file;
  }

  @Override
  public synchronized Connection getConnection() {
    return dbConn;
  }

  @Override
  public Connection getNewConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
  }

  /**
   * Resets (cached) prepared SQL statements.
   */
  private  void resetPreparedStatements() {
    tryClose(getConstantIdStatement);
    getConstantIdStatement = null;
    tryClose(insertAirportCommStatement);
    insertAirportCommStatement = null;
    tryClose(insertAirportPropertyStatement);
    insertAirportPropertyStatement = null;
    tryClose(insertAirportStatement);
    insertAirportStatement = null;
    tryClose(insertConstantStatement);
    insertConstantStatement = null;
    tryClose(insertRunwayEndStatement);
    insertRunwayEndStatement = null;
    tryClose(insertRunwayEndPropertyStatement);
    insertRunwayEndPropertyStatement = null;
    tryClose(insertRunwayStatement);
    insertRunwayStatement = null;
    tryClose(updateAirportRankStatement);
    updateAirportRankStatement = null;
  }

  private static boolean tryClose(final Statement statement) {
    if (statement == null) {
      return false;
    }
    try { 
      statement.close();
      return true;
    } catch (SQLException sqlEx) {
      return false;
    } 
  }    

  @Override
  public synchronized void open() throws SQLException {
    dbConn = getNewConnection();
    resetPreparedStatements();
  }

  @Override
  public synchronized void close() throws SQLException {
    try {
      dbConn.close();
    } finally {
      dbConn = null;
    }
  }

  @Override
  public synchronized void reset() throws SQLException {
    close();
    open();
  }

  @Override
  public synchronized void beginTransaction() throws SQLException {
    dbConn.setAutoCommit(false);
  }
  
  @Override
  public synchronized void commit() throws SQLException {
    dbConn.commit();
  }

  @Override
  public synchronized void rollback() throws SQLException {
    dbConn.rollback();
  }

  @Override
  public synchronized void initAirportTables() throws SQLException {
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
      if (stat != null) {
        stat.close();
      }
    }
  }

  @Override
  public synchronized void initAirportCommTable() throws SQLException {
    Statement stat = null;
    try {
      stat = dbConn.createStatement();
      stat.executeUpdate("DROP TABLE IF EXISTS airport_comm");
      stat.executeUpdate("DROP INDEX IF EXISTS airport_comm_airport_id_index;");
      stat.executeUpdate("CREATE TABLE airport_comm (" +
                         "_id INTEGER PRIMARY KEY ASC, " +
                         "airport_id INTEGER NOT NULL, " +
                         "identifier TEXT NOT NULL, " +
                         "frequency TEXT NOT NULL, " +
                         "remarks TEXT);");
      stat.executeUpdate("CREATE INDEX airport_comm_airport_id_index ON " +
                         "airport_comm (airport_id)");
    } finally {
      if (stat != null) {
        stat.close();
      }
    }
  }

  @Override
  public synchronized void initAndroidMetadataTable() throws SQLException {
    Statement stat = null;
    try {
      stat = dbConn.createStatement();
      stat.executeUpdate("DROP TABLE IF EXISTS android_metadata;");
      stat.executeUpdate("CREATE TABLE android_metadata (locale TEXT);");
      stat.executeUpdate("INSERT INTO android_metadata VALUES ('en_US');");
    } finally {
      if (stat != null) {
        stat.close();
      }
    }
  }

  @Override
  public synchronized void initConstantsTable() throws SQLException {
    Statement stat = null;
    try {
      stat = dbConn.createStatement();
      stat.executeUpdate("DROP TABLE IF EXISTS constants;");
      stat.executeUpdate("DROP INDEX IF EXISTS constants_constant_index;");
      stat.executeUpdate("CREATE TABLE constants (" +
                         "_id INTEGER PRIMARY KEY ASC, " +
                         "constant TEXT UNIQUE NOT NULL);");
    } finally {
      if (stat != null) {
        stat.close();
      }
    }
  }

  @Override
  public synchronized void initMetadataTable() throws SQLException {
    Statement stat = null;
    try {
      stat = dbConn.createStatement();
      stat.executeUpdate("DROP TABLE IF EXISTS metadata;");
      stat.executeUpdate("CREATE TABLE metadata (" +
                         "key TEXT NOT NULL UNIQUE, " +
                         "value TEXT NOT NULL" +
                         ");");
      stat.executeUpdate("CREATE UNIQUE INDEX metadata_key_index ON metadata (key)");
      stat.executeUpdate("INSERT INTO metadata (key, value) VALUES ('schema version', '" +
                         DB_SCHEMA_VERSION + "');");
      stat.executeUpdate("INSERT INTO metadata (key, value) VALUES ('expires', '" +
                         DB_EXPIRATION_TIMESTAMP + "');");
    } finally {
      if (stat != null) {
        stat.close();
      }
    }
  }

  @Override
  public synchronized void initRunwayTables() throws SQLException {
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
      if (stat != null) {
        stat.close();
      }
    }
  }

  /**
   * Return the id of a given constant text from the constants db table.
   * If such a constant doesn't exist, it is added and the freshly created id is returned.
   */
  private synchronized int getConstantId(final String constant) throws SQLException {
    // Prepare statements if necessary: use previously created if possible
    if (constantCache.containsKey(constant)) {
      return constantCache.get(constant);
    }

    if (getConstantIdStatement == null) {
      getConstantIdStatement = dbConn.prepareStatement(
          "SELECT _id FROM constants WHERE constant = ?");
    }

    if (insertConstantStatement == null) {
      insertConstantStatement =
        dbConn.prepareStatement("INSERT INTO constants (constant) VALUES (?)");
    }

    insertConstantStatement.setString(1, constant);
    insertConstantStatement.executeUpdate();
    getConstantIdStatement.setString(1, constant);
    final ResultSet rs = getConstantIdStatement.executeQuery();
    try {
      if (!rs.next()) {
        throw new RuntimeException("Error while adding constant to db: " + constant);
      }
      final int id = rs.getInt(1);
      constantCache.put(constant, id);
      return id;
    } finally {
      rs.close();
    }
  }

  @Override
  public synchronized void insertAirport(final String icao, final String name, final String type,
      final String city, final int latE6, final int lngE6, final boolean isOpen,
      final boolean isPublic, final boolean isTowered, final boolean isMilitary, final int cellId,
      final int rank) throws SQLException {
    if (insertAirportStatement == null) {
      insertAirportStatement = dbConn.prepareStatement(
          "INSERT INTO airports " +
          "(icao, name, type, city, lat, lng, is_open, is_public, is_towered, " +
          "is_military, cell_id, rank) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
      dbConn.setAutoCommit(false);
    }
    int fieldCount = 0;
    insertAirportStatement.setString(++fieldCount, icao);
    insertAirportStatement.setString(++fieldCount, name);
    insertAirportStatement.setInt(++fieldCount, getConstantId(type));
    insertAirportStatement.setString(++fieldCount, city);
    insertAirportStatement.setInt(++fieldCount, latE6);
    insertAirportStatement.setInt(++fieldCount, lngE6);
    insertAirportStatement.setBoolean(++fieldCount, isOpen);
    insertAirportStatement.setBoolean(++fieldCount, isPublic);
    insertAirportStatement.setBoolean(++fieldCount, isTowered);
    insertAirportStatement.setBoolean(++fieldCount, isMilitary);
    insertAirportStatement.setInt(++fieldCount, cellId);
    insertAirportStatement.setInt(++fieldCount, rank);
    insertAirportStatement.executeUpdate();
  }

  @Override
  public synchronized boolean insertAirportProperty(final int id, final String key,
      final String value) throws SQLException {
    if (insertAirportPropertyStatement == null) {
      insertAirportPropertyStatement = dbConn.prepareStatement(
          "INSERT INTO airport_properties (key, value, airport_id) VALUES (?, ?,?)");
    }
    insertAirportPropertyStatement.setInt(3, id);
    return insertProperty(insertAirportPropertyStatement, key, value);
  }

  @Override
  public synchronized void insertAirportComm(final int id, final String identifier,
      final String frequency, final String remarks) throws SQLException {
    if (insertAirportCommStatement == null) {
      insertAirportCommStatement = dbConn.prepareStatement(
          "INSERT INTO airport_comm (airport_id, identifier, frequency, remarks) " +
          "VALUES (?, ?, ?, ?)");
    }
    insertAirportCommStatement.setInt(1, id);
    insertAirportCommStatement.setString(2, identifier);
    insertAirportCommStatement.setString(3, frequency);
    if (remarks != null) {
      insertAirportCommStatement.setString(4, remarks);
    } else {
      insertAirportCommStatement.setNull(4, Types.VARCHAR);
    }
    insertAirportCommStatement.executeUpdate();
  }

  @Override
  public synchronized void updateAirportRank(final int id, final int rank) throws SQLException {
    if (updateAirportRankStatement == null) {
      updateAirportRankStatement = 
          dbConn.prepareStatement("UPDATE airports SET rank = ? WHERE _id = ?");
    }
    updateAirportRankStatement.setInt(1, rank);
    updateAirportRankStatement.setInt(2, id);
    if (updateAirportRankStatement.executeUpdate() != 1) {
      throw new RuntimeException("Could not update rank for airport id: " + id);
    }
  }

  @Override
  public synchronized void insertRunway(final int airportId, final String letters, final int length,
      final int width, final String surface) throws SQLException {
    if (insertRunwayStatement == null) {
      insertRunwayStatement = dbConn.prepareStatement(
          "INSERT INTO runways (airport_id, letters, length, width, surface) " +
          "VALUES (?, ?, ?, ?, ?);");
    }
    int fieldCount = 0;
    insertRunwayStatement.setInt(++fieldCount, airportId);
    insertRunwayStatement.setString(++fieldCount, letters);
    insertRunwayStatement.setInt(++fieldCount, length);
    insertRunwayStatement.setInt(++fieldCount, width);
    insertRunwayStatement.setInt(++fieldCount, getConstantId(surface));
    insertRunwayStatement.executeUpdate();
  }

  @Override
  public synchronized void insertRunwayEnd(final int runwayId, final String letters)
      throws SQLException {
      if (insertRunwayEndStatement == null) {
        insertRunwayEndStatement = dbConn.prepareStatement(
            "INSERT INTO runway_ends (runway_id, letters) " +
            "VALUES (?, ?);");
      }
      insertRunwayEndStatement.setInt(1, runwayId);
      insertRunwayEndStatement.setString(2, letters);
      insertRunwayEndStatement.executeUpdate();
  }

  @Override
  public synchronized boolean insertRunwayEndProperty(final int runwayEndId, final String key,
      final String value) throws SQLException {
    if (insertRunwayEndPropertyStatement == null) {
      insertRunwayEndPropertyStatement = dbConn.prepareStatement(
          "INSERT INTO runway_end_properties (key, value, runway_end_id) VALUES (?, ?,?)");
    }
    insertRunwayEndPropertyStatement.setInt(3, runwayEndId);
    return insertProperty(insertRunwayEndPropertyStatement, key, value);
  }

  /**
   * Inserts new property in an arbitrary table.
   * <p>
   * Both {@code key} and {@code value} are converted to constants.
   * If {@code value} can be parsed as an integer, the latter is stored directly.
   * @param statement Prepared statement that accepts two {@code Int} values in the first and second
   * position, corresponding to {@code key} and {@code value} respectively.
   * @param key Property key
   * @param value Property value.
   * @return {@code true} if {@code value} was converted to a constant, {@code false} otherwise.
   * @see #addAirportProperty
   */
  private  boolean insertProperty(final PreparedStatement statement,
      final String key, final String value) throws SQLException {
    // Check if value can be parsed as integer
    int valueIdOrInt;
    boolean valueIsInteger;
    try {
      valueIdOrInt = Integer.parseInt(value);
      valueIsInteger = true;
    } catch (NumberFormatException ex) {
      valueIdOrInt = getConstantId(value);
      valueIsInteger = false;
    }
    // Get Key constant id
    final int keyId = getConstantId(key);
    // Insert in table
    statement.setInt(1, keyId);
    statement.setInt(2, valueIdOrInt);
    statement.executeUpdate();
    return !valueIsInteger;
  }


}

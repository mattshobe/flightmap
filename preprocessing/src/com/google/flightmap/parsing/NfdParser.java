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

package com.google.flightmap.parser;

import com.google.flightmap.common.CustomGridUtil;

import java.sql.*;
import java.util.regex.*;
import java.io.*;

/**
 * Parses airports from ARINC 424-18 file and adds them to a SQLite database
 * 
 */
public class NfdParser {
  private String sourceFile;
  private String targetFile;

  /**
   * @param sourceFile
   *          Source database in ARINC 424-18 format (eg NFD)
   * @param targetFile
   *          Target SQLite filename. Existing data is silently overwritten.
   */
  public NfdParser(String sourceFile, String targetFile) {
    this.sourceFile = sourceFile;
    this.targetFile = targetFile;
  }

  public static void main(String args[]) {
    if (args.length != 2) {
      System.err.println("Usage: java NfdParser <NFD file> <DB file>");
      System.exit(1);
    }

    (new NfdParser(args[0], args[1])).run();
  }

  private void run() {
    try {
//      Connection dbConn = initDB();
//      addAndroidMetadataToDb(dbConn);
      addAirportDataToDb();
//      dbConn.close();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(2);
    }
  }

  private void addAndroidMetadataToDb(Connection conn) throws SQLException {
    Statement stat = conn.createStatement();
    stat.executeUpdate("DROP TABLE IF EXISTS android_metadata;");
    stat.executeUpdate("CREATE TABLE android_metadata (locale TEXT);");
    stat.executeUpdate("INSERT INTO android_metadata VALUES ('en_US');");
    stat.close();

  }

  private void addAirportDataToDb() throws SQLException, IOException {
    BufferedReader in = new BufferedReader(new FileReader(this.sourceFile));

    /*
    initAirportsTable(conn);

    PreparedStatement airportStatement = conn
        .prepareStatement(
            "INSERT INTO airports (icao, name, lat, lng, cell_id) VALUES (?, ?, ?, ?, ?);");
*/
    Pattern airportArincPattern = Pattern
        .compile("S...P (.{4})..A(.{3})..   .{11}(.{9})(.{10}).{42}(.{30})\\d{9}");
    Matcher airportArincMatcher;

    // Airport data variables
    String icao, iata, name, latString, lngString;
    int latDeg, latMin, latSecHundredths;
    int lngDeg, lngMin, lngSecHundredths;
    double lat, lng;
    int latE6, lngE6;

    String line;
    while ((line = in.readLine()) != null) {
      airportArincMatcher = airportArincPattern.matcher(line);
      if (!airportArincMatcher.matches()) {
        // Not an airport entry
        continue;
      }

      icao = airportArincMatcher.group(1).trim();
      iata = airportArincMatcher.group(2).trim();
      if ( !(iata.isEmpty() || icao.isEmpty()) && !iata.equals(icao) )
        System.out.println(iata + " " + icao);
      continue;
      /*
      latString = airportArincMatcher.group(3);
      lngString = airportArincMatcher.group(4);
      name = airportArincMatcher.group(5).trim();

      latDeg = Integer.parseInt(latString.substring(1, 3));
      latMin = Integer.parseInt(latString.substring(3, 5));
      latSecHundredths = Integer.parseInt(latString.substring(5, 9));
      lat = 1e6 * latDeg + 1e6 * latMin / 60.0 + 1e6 * latSecHundredths / 360000.0;
      latE6 = (int) (lat + 0.5);
      if (latString.charAt(0) == 'S')
        latE6 *= -1;

      lngDeg = Integer.parseInt(lngString.substring(1, 4));
      lngMin = Integer.parseInt(lngString.substring(4, 6));
      lngSecHundredths = Integer.parseInt(lngString.substring(6, 10));
      lng = 1e6 * lngDeg + 1e6 * lngMin / 60.0 + 1e6 * lngSecHundredths / 360000.0;
      lngE6 = (int) (lng + 0.5);
      if (lngString.charAt(0) == 'W')
        lngE6 *= -1;

      int cellId = CustomGridUtil.GetCellId(latE6, lngE6);

      try {
        // Add current airport db statement to queue.
        airportStatement.setString(1, icao);
        airportStatement.setString(2, name);
        airportStatement.setInt(3, latE6);
        airportStatement.setInt(4, lngE6);
        airportStatement.setInt(5, cellId);
        airportStatement.addBatch();
      } catch (SQLException sqlEx) {
        System.err.println(icao + " " + name);
        sqlEx.printStackTrace();
      }
      */
    }

    in.close();
/*
    conn.setAutoCommit(false);
    airportStatement.executeBatch();
    conn.setAutoCommit(true);
    airportStatement.close();
    */
  }
/*
  private Connection initDB() throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    return DriverManager.getConnection("jdbc:sqlite:" + this.targetFile);
  }

  private void initAirportsTable(Connection conn) throws SQLException {
    Statement stat = null;
    try {
      stat = conn.createStatement();
      stat.executeUpdate("DROP TABLE IF EXISTS airports;");
      stat.executeUpdate("DROP INDEX IF EXISTS airports_cell_id_index;");
      stat.executeUpdate("CREATE TABLE airports (_id INTEGER PRIMARY KEY ASC, " +
                         "icao TEXT UNIQUE NOT NULL, name TEXT NOT NULL, " +
                         "lat INTEGER NOT NULL, lng INTEGER NOT NULL, cell_id INTEGER NOT NULL);");
      stat.executeUpdate("CREATE INDEX airports_cell_id_index ON airports (cell_id)");
    } finally {
      if (stat != null)
        stat.close();
    }
  }
  */
}

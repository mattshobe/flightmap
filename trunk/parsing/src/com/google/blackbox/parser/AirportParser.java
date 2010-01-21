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

package com.google.blackbox.parser;

import java.sql.*;
import java.util.regex.*;
import java.io.*;

/**
 * Parses airports from ARINC 424-18 file and adds them to a SQLite database
 * 
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class AirportParser {
  private String sourceFile;
  private String targetFile;

  /**
   * @param sourceFile
   *          Source database in ARINC 424-18 format (eg NFD)
   * @param targetFile
   *          Target SQLite filename. Existing data is silently overwritten.
   */
  public AirportParser(String sourceFile, String targetFile) {
    this.sourceFile = sourceFile;
    this.targetFile = targetFile;
  }

  public static void main(String args[]) {
    if (args.length != 2) {
      System.err.println("Usage: java AirportParser <NFD file> <DB file>");
      System.exit(1);
    }

    (new AirportParser(args[0], args[1])).run();
  }

  private void run() {
    try {
      Connection dbConn = initDB();
      addAndroidMetadataToDb(dbConn);
      addAirportDataToDb(dbConn);
      dbConn.close();
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

  private void addAirportDataToDb(Connection conn) throws SQLException,
      IOException {
    BufferedReader in = new BufferedReader(new FileReader(this.sourceFile));
    String line;

    Statement stat = conn.createStatement();
    stat.executeUpdate("DROP TABLE IF EXISTS airports;");
    stat.executeUpdate("CREATE TABLE airports (_id INTEGER PRIMARY KEY ASC, "
        + "icao TEXT UNIQUE NOT NULL, name TEXT NOT NULL, "
        + "lat INTEGER NOT NULL, lng INTEGER NOT NULL);");
    stat.close();

    PreparedStatement airportStatement = conn
        .prepareStatement("INSERT INTO airports (icao, name, lat, lng) VALUES (?, ?, ?, ?);");

    Pattern airportArincPattern = Pattern
        .compile("S...P (.{4})..A.{5}   .{11}(.{9})(.{10}).{42}(.{30})\\d{9}");
    Matcher airportArincMatcher;

    String icao, name, latString, lngString;
    int latDeg, latMin, latSecHundredths;
    int lngDeg, lngMin, lngSecHundredths;
    double lat, lng;
    int latE6, lngE6;

    while ((line = in.readLine()) != null) {
      airportArincMatcher = airportArincPattern.matcher(line);
      if (!airportArincMatcher.matches()) {
        continue;
      }

      icao = airportArincMatcher.group(1).trim();
      latString = airportArincMatcher.group(2);
      lngString = airportArincMatcher.group(3);
      name = airportArincMatcher.group(4).trim();

      try {
        latDeg = Integer.parseInt(latString.substring(1, 3));
        latMin = Integer.parseInt(latString.substring(3, 5));
        latSecHundredths = Integer.parseInt(latString.substring(5, 9));
        lat = 1e6 * latDeg + 1e6 * latMin / 60.0 + 1e6 * latSecHundredths
            / 360000.0;
        latE6 = (int) (lat + 0.5);
        if (latString.charAt(0) == 'S')
          latE6 *= -1;

        lngDeg = Integer.parseInt(lngString.substring(1, 4));
        lngMin = Integer.parseInt(lngString.substring(4, 6));
        lngSecHundredths = Integer.parseInt(lngString.substring(6, 10));
        lng = 1e6 * lngDeg + 1e6 * lngMin / 60.0 + 1e6 * lngSecHundredths
            / 360000.0;
        lngE6 = (int) (lng + 0.5);
        if (lngString.charAt(0) == 'W')
          lngE6 *= -1;

        airportStatement.setString(1, icao);
        airportStatement.setString(2, name);
        airportStatement.setInt(3, latE6);
        airportStatement.setInt(4, lngE6);
        airportStatement.addBatch();

      } catch (SQLException sqlEx) {
        System.err.println(icao + " " + name);
        sqlEx.printStackTrace();
      }
    }

    in.close();

    conn.setAutoCommit(false);
    airportStatement.executeBatch();
    conn.setAutoCommit(true);
    airportStatement.close();
  }

  private Connection initDB() throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    return DriverManager.getConnection("jdbc:sqlite:" + this.targetFile);
  }
}

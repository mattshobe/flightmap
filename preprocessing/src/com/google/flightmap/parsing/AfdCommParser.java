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

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

/**
 * Parses airports from compiled AFD file.
 *
 */
public class AfdCommParser {
  private Connection dbConn;
//  private final String runwaySourceFile;
  private final String dbFile;
  private final Map<String, String> iataToIcao = new HashMap<String, String>();
  private final Map<String, String> icaoToIata = new HashMap<String, String>();


  private final StringBuilder afd;
  /**
   * @param afdFile
   *          FAA Form 5010, Airport Master Record file
   * @param dbFile
   */
  public AfdCommParser(final String afdFile,
                       final String iataToIcaoFile,
                       final String dbFile) {
    this.dbFile = dbFile;
    try {
      final BufferedReader in = new BufferedReader(new FileReader(iataToIcaoFile));
      String line;
      while ((line = in.readLine()) != null) {
        final String[] codes = line.split(" ");
        iataToIcao.put(codes[0], codes[1]);
        icaoToIata.put(codes[1], codes[0]);
      }
      in.close();

      final BufferedReader afdIn = new BufferedReader(new FileReader(afdFile));
      afd = new StringBuilder(60000);
      while ((line = afdIn.readLine()) != null) {
        afd.append(line);
        afd.append(' ');
      }
    } catch (IOException ioex) {
      System.err.println("Error while reading iataToIcao file");
      throw new RuntimeException(ioex);
    }
  }


  /**
   * Gets a connection to the database.
   */
  private Connection initDB() throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    return DriverManager.getConnection("jdbc:sqlite:" + dbFile);
  }


  private void execute() {
    try {
      dbConn = initDB();
      initAirportCommTable();
      addCommData();
      dbConn.close();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(2);
    }
  }

  private void initAirportCommTable() throws SQLException {
    Statement stat = dbConn.createStatement();
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
    stat.close();

  }

  private void addCommData() throws SQLException {
    Pattern commSectionRegex = Pattern.compile(
        "\\((\\S+?)\\)\\s+?\\d+\\s*(?:N|E|W|S|NE|NW|SE|SW).+?UTC\\s+.+?COMMUNICATIONS\\:");
//        "\\((\\S+?)\\)\\s+?\\d+.+?UTC\\s+.+?COMMUNICATIONS\\:");
    Pattern freqRegex = Pattern.compile("([A-Z]+(?:[A-Z]| |/)+?)(\\d+\\.\\d+)\\s+(?:\\((.+?)\\))?");
    int start = 0;

    Matcher commSectionMatcher = commSectionRegex.matcher(afd);
    Matcher freqMatcher = freqRegex.matcher(afd);

    while (commSectionMatcher.find(start)) {
      start = commSectionMatcher.start();
      final String iata = commSectionMatcher.group(1);
      System.out.println(iata);

      // Determine potential end of this COMMUNICATIONS end
      int nextColon = afd.indexOf(":", commSectionMatcher.end());
      if (nextColon == -1) {
        nextColon = afd.length();
      }

      int nextMatch;
      if (commSectionMatcher.find(commSectionMatcher.end())) {
        nextMatch = commSectionMatcher.start();
      } else {
        nextMatch = afd.length();
      }

      final int stop = Math.min(nextColon, nextMatch);

      while (freqMatcher.find(start)) {
        if (freqMatcher.start() > stop) {
          start = stop;
          break;
        }
        System.out.println("  -> " + freqMatcher.group(0));
        final String identifier = freqMatcher.group(1).trim();
        final String frequency = freqMatcher.group(2).trim();
        String remarks = freqMatcher.group(3);
        if (remarks != null) {
          remarks = remarks.trim();
        }
        
        addAirportCommToDb(iata, identifier, frequency, remarks);
        start = freqMatcher.end();
      }
    }
  }


  private void addAirportCommToDb(final String iata, final String identifier,
      final String frequency, final String remarks) throws SQLException {
    final PreparedStatement addAirportCommStmt = dbConn.prepareStatement(
        "INSERT INTO airport_comm (airport_id, identifier, frequency, remarks) " + 
        "VALUES (?, ?, ?, ?)");
    final int airportId = getAirportId(iata);
    if (airportId != -1) {
      addAirportCommStmt.setInt(1, getAirportId(iata));
      addAirportCommStmt.setString(2, identifier);
      addAirportCommStmt.setString(3, frequency);
      if (remarks != null) {
        addAirportCommStmt.setString(4, remarks);
      } else {
        addAirportCommStmt.setNull(4, Types.VARCHAR);
      }
      addAirportCommStmt.executeUpdate();
    }
  }

  private int getAirportId(final String iata) {
    String icao = iataToIcao.get(iata);
    if (icao == null) {
      System.err.println("Could not find ICAO for: " + iata);
      icao = iata;
    }

    try {
      PreparedStatement getAirportIdStmt = dbConn.prepareStatement(
        "SELECT _id FROM airports WHERE icao = ?");

      getAirportIdStmt.setString(1, icao);
      ResultSet airportIdResult = getAirportIdStmt.executeQuery();
      if (!airportIdResult.next()) {
        System.err.println("Could not find airport for: " + icao);
        return -1;
      }

      return airportIdResult.getInt(1);

    } catch (Exception ex) {
      ex.printStackTrace();
      return -1;
    }
  }

  public static void main(String args[]) {
    //TODO: Use GetOpt
    if (args.length != 3) {
      System.err.println("Usage: java AfdCommParser <afd text file>" +
                         " <iata to icao> <DB file>");
      System.exit(1);
    }

    (new AfdCommParser(args[0], args[1], args[2])).execute();
  }

}

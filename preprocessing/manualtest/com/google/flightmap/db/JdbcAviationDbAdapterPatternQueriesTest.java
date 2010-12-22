// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.flightmap.db;

import java.io.*;
import java.sql.*;
import java.util.*;

import com.google.flightmap.common.db.AviationDbAdapter;

public class JdbcAviationDbAdapterPatternQueriesTest {
  private final AviationDbAdapter dbAdapter;

  private JdbcAviationDbAdapterPatternQueriesTest(final String dbPath) throws Exception {
    System.out.println("Initializing JdbcAviationDbAdapter...");
    dbAdapter = new JdbcAviationDbAdapter(initDb(dbPath));
  }

  private void runTest() throws Exception {
    final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
    String pattern;
    while (true) {
      System.out.print("Pattern: ");
      if ((pattern = in.readLine()) == null) {
        System.out.println("");
        break;
      }
      System.out.println("Airports with city LIKE: " + pattern);
      printAirports(dbAdapter.getAirportIdsWithCityLike(pattern));
      System.out.println("Airports with name LIKE: " + pattern);
      printAirports(dbAdapter.getAirportIdsWithNameLike(pattern));
      System.out.println("");
    }
  }

  /**
   * Gets a connection to the database.
   */
  private static Connection initDb(final String path) throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    return DriverManager.getConnection("jdbc:sqlite:" + path);
  }


  private void printAirports(List<Integer> airportIds) {
    for (Integer airportId: airportIds) {
      System.out.println("  " + dbAdapter.getAirport(airportId));
    }
  }

  public static void main(String[] args) {
    try {
      final String dbPath = args[0];
      new JdbcAviationDbAdapterPatternQueriesTest(dbPath).runTest();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


}

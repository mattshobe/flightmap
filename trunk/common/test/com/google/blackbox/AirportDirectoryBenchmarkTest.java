// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.blackbox;

import com.google.blackbox.data.*;

import junit.framework.TestCase;

import java.util.LinkedList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class AirportDirectoryBenchmarkTest extends TestCase {
  private final static String AIRPORT_DB_FILENAME = "aviation.db";
  private Connection dbConnection;

  private LinkedList<Airport> allAirports = new LinkedList<Airport>();

  private AirportDirectory airportDirectory1;
  private AirportDirectory airportDirectory2;



  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Class.forName("org.sqlite.JDBC");
    this.dbConnection = DriverManager.getConnection("jdbc:sqlite:" + AIRPORT_DB_FILENAME);
    this.loadAllAirports();
    this.airportDirectory1 = new NaiveAirportDirectory(this.dbConnection);
    this.airportDirectory2 = new CustomGridAirportDirectory(this.dbConnection);
  }

  private void loadAllAirports() throws SQLException {
    PreparedStatement getAllAirportsStmt = this.dbConnection.prepareStatement(
        "SELECT icao, lat, lng, name FROM airports");

    ResultSet allAirportsResultSet = getAllAirportsStmt.executeQuery();

    int count = 0;
    while (allAirportsResultSet.next()) {
      ++count;
      String icao = allAirportsResultSet.getString(1);
      int lat = allAirportsResultSet.getInt(2);
      int lng = allAirportsResultSet.getInt(3);
      String name = allAirportsResultSet.getString(4);
      LatLng location = new LatLng(lat, lng);

      this.allAirports.add(new Airport(icao, name, location));
    }
    getAllAirportsStmt.close();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    try {
      if (dbConnection != null)
        dbConnection.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void testGetAirportWithinRadius() throws Exception {
    final double RADIUS = 40.0;
    int count = 0;
    for (Airport airport: this.allAirports) {
      final String currentAirportIcao = airport.icao;
      final LatLng currentAirportLocation = airport.location;

      final long before2 = System.currentTimeMillis();
      final AirportDistance[] airports2 = this.airportDirectory2.getAirportsWithinRadius(
          currentAirportLocation, RADIUS);
      final long duration2 = System.currentTimeMillis() - before2;

      final long before1 = System.currentTimeMillis();
      final AirportDistance[] airports1 = this.airportDirectory1.getAirportsWithinRadius(
          currentAirportLocation, RADIUS);
      final long duration1 = System.currentTimeMillis() - before1;

      // For benchmarking, print duration
      System.err.println((count++) + " " + currentAirportIcao + " " + duration1 + " " + duration2);

      assertEquals(airports1.length, airports2.length);

      for (int i=0; i < airports1.length; ++i) {
        assertEquals(airports1[i].airport, airports2[i].airport);
        assertEquals(airports1[i].distance, airports2[i].distance, 1E-7);
      }
      //  Following assert removed: fails on some private airports, that are not even on the maps.
      //  It seems the latter have two different identifiers and names, but same location.
      //  assertEquals(currentAirportIcao, airports1[0].airport.icao);
    }
  }
}

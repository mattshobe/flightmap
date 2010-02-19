// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.blackbox;

import com.google.blackbox.data.*;

import junit.framework.TestCase;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class NaiveAirportDirectoryTest extends TestCase {
  private Connection dbConnection;
  private AirportDirectory airportDirectory;
  private final static String AIRPORT_DB_FILENAME = "aviation.db";


  @Override
  protected void setUp() throws Exception {
    super.setUp();
    Class.forName("org.sqlite.JDBC");
    this.dbConnection = DriverManager.getConnection("jdbc:sqlite:" + AIRPORT_DB_FILENAME);
    this.airportDirectory = new NaiveAirportDirectory(this.dbConnection);
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
    LatLng mtvPosition = new LatLng(37422133,-122083550);
    AirportDistance[] airports = this.airportDirectory.getAirportsWithinRadius(mtvPosition, 20.0);
    for (AirportDistance airport: airports) {
        System.err.println(airport);
    }
  }
}

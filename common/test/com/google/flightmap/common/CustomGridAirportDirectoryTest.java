// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.flightmap.common;

import java.util.TreeSet;

import junit.framework.TestCase;

import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;

/**
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class CustomGridAirportDirectoryTest extends TestCase {
  private AirportDirectory airportDirectory;
  private AviationDbAdapter aviationDbAdapter;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    aviationDbAdapter = new TestAviationDbAdapter();
    aviationDbAdapter.open();
    airportDirectory = new CustomGridAirportDirectory(aviationDbAdapter);
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    aviationDbAdapter.close();
  }

  public void testGetAirportWithinRadius() throws Exception {
    LatLng mtvPosition = new LatLng(37422133, -122083550);
    TreeSet<AirportDistance> airports = airportDirectory.getAirportsWithinRadius(mtvPosition, 20.0);
    for (AirportDistance airport : airports) {
      System.err.println(airport);
    }
  }
}

// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.blackbox;


import com.google.blackbox.data.Airport;
import com.google.blackbox.data.AirportDistance;
import com.google.blackbox.data.LatLng;

import java.util.LinkedList;
import java.util.TreeSet;

/**
 * 
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class CustomGridAirportDirectory implements AirportDirectory {
  private final AviationDbAdapter adapter;

  public CustomGridAirportDirectory(AviationDbAdapter adapter) {
    this.adapter = adapter;
  }

  @Override
  public void open() {
    adapter.open();
  }

  @Override
  public void close() {
    adapter.close();
  }

  public TreeSet<AirportDistance> getNearestAirports(final LatLng position, final int numAirports) {
    throw new RuntimeException("Not implemented yet");
  }

  public TreeSet<AirportDistance> getAirportsWithinRadius(final LatLng position, final double radius) {
    double earthRadiusAtLat =
        NavigationUtil.EARTH_RADIUS * Math.sin(Math.PI / 2 - position.latRad());
    double longRadius = radius / (2 * Math.PI * earthRadiusAtLat) * 360;
    double latRadius = radius / 60;
    int radiusE6 = (int) (Math.max(longRadius, latRadius) * 1E6);

    LinkedList<int[]> cellRanges = CustomGridUtil.GetCellsInRadius(position, radiusE6);
    TreeSet<AirportDistance> airportsInRange = new TreeSet<AirportDistance>();


    for (int[] range : cellRanges) {
      LinkedList<Airport> airportsInCells = adapter.getAirportsInCells(range[0], range[1]);
      for (Airport airport : airportsInCells) {
        double distance = NavigationUtil.computeDistance(position, airport.location);
        if (distance <= radius) {
          airportsInRange.add(new AirportDistance(airport, distance));
        }
      }
    }

    return airportsInRange;
  }
}

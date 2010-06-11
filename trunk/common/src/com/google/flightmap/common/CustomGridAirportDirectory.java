// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.flightmap.common;


import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

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


  public TreeSet<AirportDistance> getAirportsWithinRadius(final LatLng position,
      final double radius, final int minRank) {

    // Translate radius in degrees at that latitude (and all longitudes):
    final double earthRadiusAtLat =
        NavigationUtil.EARTH_RADIUS / NavigationUtil.METERS_PER_NM
            * Math.sin(Math.PI / 2 - position.latRad());
    final double lngRadius =
        radius / NavigationUtil.METERS_PER_NM / (2 * Math.PI * earthRadiusAtLat) * 360;
    final double latRadius = radius / NavigationUtil.METERS_PER_NM / 60;
    final int radiusE6 = (int) (Math.max(lngRadius, latRadius) * 1E6);


    // Retrieve airports in rectangular bounding box
    final LinkedList<Airport> airportsInBoundingBox =
       getAirportsInRectangle(LatLngRect.getBoundingBox(position, radiusE6), minRank);

    // Filter out airports outside of radius
    TreeSet<AirportDistance> airportsInRange = new TreeSet<AirportDistance>();
    for (Airport airport : airportsInBoundingBox) {
      double distance = NavigationUtil.computeDistance(position, airport.location);
      if (distance <= radius) {
        airportsInRange.add(new AirportDistance(airport, distance));
      } else {
        System.out.println("Outside of circle - " + airport);
      }
    }
    return airportsInRange;
  }


  public LinkedList<Airport> getAirportsInRectangle(final LatLngRect area, final int minRank) {
    LinkedList<int[]> cellRanges = CustomGridUtil.getCellsInRectangle(area);
    LinkedList<Airport> airportsInArea = new LinkedList<Airport>();
    for (int[] range : cellRanges) {
      LinkedList<Airport> airportsInCells = adapter.getAirportsInCells(range[0], range[1], minRank);
      for (Airport airport : airportsInCells) {
        if (area.contains(airport.location)) {
          airportsInArea.add(airport);
        } else {
          System.out.println("Outside of rectangle - " + airport);
        }
      }
    }
    return airportsInArea;
  }
}

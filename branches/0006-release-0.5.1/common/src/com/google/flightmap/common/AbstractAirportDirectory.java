// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.flightmap.common;


import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

import java.util.Collection;
import java.util.TreeSet;

public abstract class AbstractAirportDirectory implements AirportDirectory {

  @Override
  public TreeSet<AirportDistance> getAirportsWithinRadius(final LatLng position,
      final double radius, final int minRank) {

    // Translate radius in degrees at that latitude (and all longitudes):
    final double earthRadiusAtLat =
        NavigationUtil.EARTH_RADIUS / NavigationUtil.METERS_TO_NM
            * Math.sin(Math.PI / 2 - position.latRad());
    final double lngRadius =
        radius / NavigationUtil.METERS_TO_NM / (2 * Math.PI * earthRadiusAtLat) * 360;
    final double latRadius = radius / NavigationUtil.METERS_TO_NM / 60;
    final int radiusE6 = (int) (Math.max(lngRadius, latRadius) * 1E6);


    // Retrieve airports in rectangular bounding box
    final Collection<Airport> airportsInBoundingBox =
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
}

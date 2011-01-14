/*
 * Copyright (C) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.flightmap.common;


import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

import java.util.Collection;
import java.util.TreeSet;

/**
 * Abstract class provides a naive implementation of
 * {@link AirportDirectory#getAirportsWithinRadius getAirportsWithinRadius}.
 */
public abstract class AbstractAirportDirectory implements AirportDirectory {

  /**
   * Returns airports within {@code radius} meters of {@code position}.
   * <p>
   * Retrieves all airports in a rectangular region bouding the search area using
   * {@link AirportDirectory#getAirportsInRectangle}, and then filters out outliers.
   * <p>
   * Airport ranks are at least {@code minRank}. <br />
   * Results are sorted in order of increasing distance from {@code position}.
   * @param position  Center of radius search
   * @param radius    Radius of search [meters]
   * @param minRank   Minimum airport rank to return
   */
  @Override
  public TreeSet<AirportDistance> getAirportsWithinRadius(final LatLng position,
      final double radius, final int minRank) throws InterruptedException {

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

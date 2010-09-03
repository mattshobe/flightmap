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
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

import java.util.Collection;

public class CachedAirportDirectory extends AbstractAirportDirectory {
  private final AirportDirectory airportDirectory;

  private LatLngRect cachedArea;
  private int cachedMinRank;
  private Collection<Airport> cachedAirports;

  public CachedAirportDirectory(final AirportDirectory airportDirectory) {
    this.airportDirectory = airportDirectory;
  }

  @Override
  synchronized public Collection<Airport> getAirportsInRectangle(final LatLngRect area, 
      final int minRank) {
    if (cachedArea != null && cachedArea.contains(area) && cachedMinRank == minRank) {
      return cachedAirports;
    }

    // Cache miss: update cache
    final LatLng areaNeCorner = area.getNeCorner();
    final LatLng areaSwCorner = area.getSwCorner();
    final LatLng cachedAreaNeCorner =
        new LatLng(areaNeCorner.lat + 100000, areaNeCorner.lng + 100000);
    final LatLng cachedAreaSwCorner =
        new LatLng(areaSwCorner.lat - 100000, areaSwCorner.lng - 100000);
    final LatLngRect newCachedArea = new LatLngRect(cachedAreaNeCorner, cachedAreaSwCorner);
    final Collection<Airport> newCachedAirports = 
        airportDirectory.getAirportsInRectangle(newCachedArea, minRank);
    cachedArea = newCachedArea;
    cachedMinRank = minRank;
    cachedAirports = newCachedAirports;

    return cachedAirports;
  }

  @Override
  public void open() {
    airportDirectory.open();
  }

  @Override
  public void close() {
    airportDirectory.close();
  }
}


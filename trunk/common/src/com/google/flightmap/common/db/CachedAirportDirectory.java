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

package com.google.flightmap.common.db;

import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

import java.util.Collection;

/**
 * Caches access to {@link AirportDirectory} for improved performance.
 * <p>
 * Area searches are cached by retrieving all airports within a larger area than
 * requested, and keeping the results in memory. Subsequent requests are served
 * from memory, as long as the requested area is included in the cached area.
 * <p>
 * Requesting a search area that is not included in the cached area results in
 * clearing the cache and starting all over. Results needed to build the cache
 * are obtained by querying an underlying {@code AirportDirectory}.
 * <p>
 * The following figures illustrate this process.
 * <p align="center">
 * <a href="doc-files/CachedAirportDirectory-0.png" target="_blank"> <img
 * src="doc-files/CachedAirportDirectory-0.png" width="40%" /> </a>
 * <p align="center">
 * A first area search is performed: a larger area is cached.
 * <p align="center">
 * <a href="doc-files/CachedAirportDirectory-1.png" target="_blank"> <img
 * src="doc-files/CachedAirportDirectory-1.png" width="40%" /> </a>
 * <p align="center">
 * All subsequent area searches that fall within the cached area are served from
 * memory.
 * <p align="center">
 * <a href="doc-files/CachedAirportDirectory-2.png" target="_blank"> <img
 * src="doc-files/CachedAirportDirectory-2.png" width="40%" /> </a>
 * <p align="center">
 * When an area search falls outside of the cached area (cache miss), the latter
 * is cleared.
 * <p align="center">
 * <a href="doc-files/CachedAirportDirectory-3.png" target="_blank"> <img
 * src="doc-files/CachedAirportDirectory-3.png" width="40%" /> </a>
 * <p align="center">
 * A new cached area is then retrieved for the following requests.
 */
public class CachedAirportDirectory extends AbstractAirportDirectory {
  /**
   * Underlying AirportDirectory.
   */
  private final AirportDirectory airportDirectory;

  /**
   * Latest cached area.
   */
  private LatLngRect cachedArea;

  /**
   * Minimum rank of latest cached airports.
   */
  private int cachedMinRank;

  /**
   * Retrieved airports for latest cached area.
   */
  private Collection<Airport> cachedAirports;

  private LatLngRect inProgressArea;

  private int inProgressRank;

  /**
   * Creates decorator for underlying {@code airportDirectory}.
   * 
   * @see <a href="http://en.wikipedia.org/wiki/Decorator_pattern">Decorator
   *      pattern</a>
   */
  public CachedAirportDirectory(final AirportDirectory airportDirectory) {
    this.airportDirectory = airportDirectory;
  }

  @Override
  public Collection<Airport> getAirportsInRectangle(final LatLngRect area, final int minRank)
      throws InterruptedException {
    synchronized (this) {
      if (cachedArea != null && cachedArea.contains(area) && cachedMinRank == minRank) {
        return cachedAirports;
      }
    }
    // Cache miss: get new results.
    final LatLng areaNeCorner = area.getNeCorner();
    final LatLng areaSwCorner = area.getSwCorner();
    final int dLat = areaNeCorner.lat - areaSwCorner.lat;
    final int dLng = areaNeCorner.lng - areaSwCorner.lng;
    final LatLng cachedAreaNeCorner =
        new LatLng(areaNeCorner.lat + dLat / 2, areaNeCorner.lng + dLng / 2);
    final LatLng cachedAreaSwCorner =
        new LatLng(areaSwCorner.lat - dLat / 2, areaSwCorner.lng - dLng / 2);
    synchronized (this) {
      inProgressArea = new LatLngRect(cachedAreaNeCorner, cachedAreaSwCorner);
      inProgressRank = minRank;
    }

    // This call may be slow.
    System.out.println("Fetching airports for " + area + "    rank=" + minRank);
    final Collection<Airport> newCachedAirports =
        airportDirectory.getAirportsInRectangle(inProgressArea, minRank);
    System.out.println("Got airports in rectangle. Count=" + newCachedAirports.size());
    synchronized (this) {
      cachedArea = inProgressArea;
      cachedMinRank = minRank;
      cachedAirports = newCachedAirports;
      return cachedAirports;
    }
  }

  /**
   * Returns true if {@code area} and {@code minRank} will either 1) hit the
   * cache, or 2) give the same results as the query that's in progress by
   * {@link #getAirportsInRectangle(LatLngRect, int)}.
   */
  public synchronized boolean isCacheMatch(LatLngRect area, int minRank) {
    return inProgressArea != null && inProgressArea.contains(area) && inProgressRank == minRank;
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

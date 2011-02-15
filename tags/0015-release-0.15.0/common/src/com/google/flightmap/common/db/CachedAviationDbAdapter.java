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
import com.google.flightmap.common.data.Airspace;
import com.google.flightmap.common.data.Comm;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Low level interface to database entities.
 */
public class CachedAviationDbAdapter implements AviationDbAdapter {
  /** Underlying decorated (cached) instance. */
  final AviationDbAdapter cachedDbAdapter;

  // Caching
  private final Map<Integer, String> constants;

  /**
   * Latest cached area.
   */
  private LatLngRect cachedArea;

  /**
   * Retrieved airspaces for latest cached area.
   */
  private Collection<Airspace> cachedAirspaces;

  private LatLngRect inProgressArea;

  public CachedAviationDbAdapter(final AviationDbAdapter cachedDbAdapter) {
    this.cachedDbAdapter = cachedDbAdapter;
    constants = new HashMap<Integer, String>();
  }

  @Override
  public void open() {
    cachedDbAdapter.open();
  }

  @Override
  public void close() {
    cachedDbAdapter.close();
  }

  @Override
  public Airport getAirport(final int airportId) {
    return cachedDbAdapter.getAirport(airportId);
  }

  @Override
  public int getAirportIdByIcao(final String icao) {
    return cachedDbAdapter.getAirportIdByIcao(icao);
  }

  @Override
  public List<Integer> getAirportIdsWithCityLike(final String pattern) {
    return cachedDbAdapter.getAirportIdsWithCityLike(pattern);
  }

  @Override
  public List<Integer> getAirportIdsWithNameLike(final String pattern) {
    return cachedDbAdapter.getAirportIdsWithNameLike(pattern);
  }


  @Override
  public Map<String, String> getAirportProperties(final int airportId) {
    return cachedDbAdapter.getAirportProperties(airportId);
  }

  @Override
  public List<Comm> getAirportComms(final int airportId) {
    return cachedDbAdapter.getAirportComms(airportId);
  }

  @Override
  public Collection<Airspace> getAirspacesInRectangle(final LatLngRect area)
      throws InterruptedException {
    synchronized (this) {
      if (cachedArea != null && cachedArea.contains(area)) {
        return cachedAirspaces;
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
    }

    System.out.println("Fetching airspaces for " + area);
    final long start = System.currentTimeMillis();
    // This call may be slow.
    final Collection<Airspace> newCachedAirspaces =
        cachedDbAdapter.getAirspacesInRectangle(inProgressArea);
    final long stop = System.currentTimeMillis();
    System.out.println("Got airspaces in rectangle. Count: " + newCachedAirspaces.size() + " in " +
        (stop - start) + "ms.");
    synchronized (this) {
      cachedArea = inProgressArea;
      cachedAirspaces = newCachedAirspaces;
      return cachedAirspaces;
    }
  }

  /**
   * Returns true if {@code area} will either 1) hit the
   * cache, or 2) give the same results as the query that's in progress by
   * {@link #getAirspacesInRectangle}.
   */
  public synchronized boolean isCacheMatch(final LatLngRect area) {
    return inProgressArea != null && inProgressArea.contains(area);
  }


  @Override
  public Map<String, String> getRunwayEndProperties(final int runwayEndId) {
    return cachedDbAdapter.getRunwayEndProperties(runwayEndId);
  }

  @Override
  public String getConstant(final int constantId) {
    final Integer constantIdInteger = Integer.valueOf(constantId);
    final String cachedConstant = constants.get(constantIdInteger);
    if (cachedConstant != null) {
      return cachedConstant;
    }

    final String newConstant = cachedDbAdapter.getConstant(constantId);
    constants.put(constantIdInteger, newConstant);
    return newConstant;
  }

  @Override
  public Collection<Airport> getAirportsInCells(final int startCell, final int endCell,
      final int minRank) {
    return cachedDbAdapter.getAirportsInCells(startCell, endCell, minRank);
  }

  @Override
  public String getMetadata(final String key) {
    return cachedDbAdapter.getMetadata(key);
  }

  @Override
  public Map<Integer, Integer> doSearch(String query) {
    return cachedDbAdapter.doSearch(query);
  }

}

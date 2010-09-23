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
import com.google.flightmap.common.data.Comm;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Low level interface to database entities.
 */
public class CachedAviationDbAdapter implements AviationDbAdapter {
  /** Underlying decorated (cached) instance. */
  final AviationDbAdapter cachedDbAdapter;

  // Caching
  private final Map<Integer, String> constants;

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
  public Airport getAirportByICAO(final String icao) {
    return cachedDbAdapter.getAirportByICAO(icao);
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

}

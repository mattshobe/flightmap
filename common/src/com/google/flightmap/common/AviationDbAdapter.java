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
import java.util.Map;
import java.util.List;

/**
 * Low level interface to database entities.
 */
public interface AviationDbAdapter {
  /**
   * Prepares this object for future calls.
   * <p>
   * This method must be called prior to any other call
   */
  public void open();

  /**
   * Closes this object.
   * <p>
   * This method must be called after all other calls.  No other method should be called on this
   * without calling {@link AviationDbAdapter#open} first.
   */
  public void close();

  /**
   * Returns {@link Airport} with given id.
   */
  public Airport getAirport(int airportId);

  /**
   * Returns a list of airports in the given cells with rank >= {@code minRank}.
   */
  public Collection<Airport> getAirportsInCells(int startCell, int endCell, int minRank);

  /**
   * Returns non-essential properties for an airport.
   */
  public Map<String, String> getAirportProperties(int airportId);

  /**
   * Returns airport communication data for an airport.
   */
  public List<Comm> getAirportComms(int airportId);

  /**
   * Returns non-essential properties for a runway end.
   */
  public Map<String, String> getRunwayEndProperties(int runwayEndId);
  
  /**
   * Returns constant string.
   */
  public String getConstant(int constantId);
}

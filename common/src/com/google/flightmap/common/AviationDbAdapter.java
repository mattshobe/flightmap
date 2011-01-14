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
 * Low level interface to aviation database entities.
 */
public interface AviationDbAdapter extends DbAdapter {
  /**
   * Returns {@link Airport} with given id.
   */
  public Airport getAirport(int airportId);

  /**
   * Returns id of {@link Airport} with given icao.
   */
  public int getAirportIdByIcao(String icao);

  /**
   * Returns ids of {@link Airport}s whose {@code city} match {@code pattern}.
   * 
   * @param pattern A SQL "LIKE" query pattern. A percent symbol ("%") matches
   *        any sequence of zero or more characters, an underscore ("_") matches
   *        any single character. Any other character matches itself or its
   *        lower/upper case equivalent (i.e. case-insensitive matching).
   * 
   * @see <a href="http://www.sqlite.org/lang_expr.html#like">The LIKE and GLOB
   *      operators</a>
   */
  public List<Integer> getAirportIdsWithCityLike(String pattern);

  /**
   * Returns ids of {@link Airport}s whose {@code name} match {@code pattern}.
   * 
   * @see #getAirportIdsWithCityLike
   */
  public List<Integer> getAirportIdsWithNameLike(String pattern);

  /**
   * Returns a Map of {@link Airport}s and Rank to put in search results. An
   * exact match will return a single-element Map. The rank is incremented each
   * time the query matches search criteria, if the airport Rank is high
   * according to the db and if the airport is in close proximity.
   * 
   * @param query A string to try to match.
   * @return searchResults A Map of {@link Airport}Ids and ranks.
   */
  public Map<Integer, Integer> doSearch(final String query);

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

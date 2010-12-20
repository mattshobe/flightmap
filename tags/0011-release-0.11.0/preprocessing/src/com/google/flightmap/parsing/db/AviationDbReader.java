/* 
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.flightmap.parsing.db;

import com.google.flightmap.common.data.Airport;

import java.util.List;

public interface AviationDbReader {
  /**
   * Opens database connection.
   */
  public void open();

  /**
   * Closes database connection..
   */
  public void close();

  /**
   * Returns {@link Airport} with given id.
   */
  public Airport getAirport(int airportId);

  /**
   * Returns ids of all airports in database.
   */
  public List<Integer> getAllAirportIds();

  /**
   * Returns id of airport with given {@code icao}.
   */
  public int getAirportIdByIcao(String icao);

  /**
   * Returns id of runway.
   */
  public int getRunwayId(int airportId, String letters);

  /**
   * Returns id of (base/reciprocal) runway end.
   */
  public int getRunwayEndId(int runwayId, String letters);
}

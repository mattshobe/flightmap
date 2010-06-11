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

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.TreeSet;

public interface AirportDirectory {
  /**
   * Returns airports within {@code radius} meters of {@code position}.
   * The airport rank will be at least {@code minRank}. Results will be sorted
   * in order of increasing distance from {@code position}.
   * @param position  Center of radius search
   * @param radius    Radius of search [meters]
   * @param minRank   Minimum airport rank to return
   */
  public TreeSet<AirportDistance> getAirportsWithinRadius(final LatLng position,
      final double radius, final int minRank) throws SQLException;

  /**
   * Returns airports in {@code area}.
   * The returned collection is in fact a set: no airport can be included more than once.
   * @param area      Area of search
   * @param minRank   Minimum airport rank to return
   */
  public LinkedList<Airport> getAirportsInRectangle(final LatLngRect area, final int minRank)
    throws SQLException;

  public void open();

  public void close();
}

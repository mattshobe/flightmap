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

import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;

import java.sql.SQLException;
import java.util.TreeSet;

public interface AirportDirectory {
  public TreeSet<AirportDistance> getNearestAirports(final LatLng position, final int numAirports)
      throws SQLException;

  /**
   * Returns airports within {@code radius} meters of {@code position}.
   * The airport rank will be at least {@code minRank}. Results will be sorted
   * in order of increasing distance from {@code position}.
   */
  public TreeSet<AirportDistance> getAirportsWithinRadius(final LatLng position,
      final double radius, final int minRank) throws SQLException;

  public void open();

  public void close();
}

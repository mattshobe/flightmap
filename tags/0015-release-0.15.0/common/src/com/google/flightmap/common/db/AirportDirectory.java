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
import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

import java.util.Collection;
import java.util.SortedSet;

/**
 * High level interface to the airport database.
 */
public interface AirportDirectory {
  /**
   * Returns airports within {@code radius} meters of {@code position}.
   * <p>
   * The airport ranks are at least {@code minRank}.<br />
   * Results are sorted in order of increasing distance from {@code position}.
   * @param position  Center of radius search
   * @param radius    Radius of search [meters]
   * @param minRank   Minimum airport rank to return
   */
  public SortedSet<AirportDistance> getAirportsWithinRadius(final LatLng position,
      final double radius, final int minRank) throws InterruptedException;

  /**
   * Returns airports in {@code area}.
   * <p>
   * The returned collection is in fact a set: no airport can be included more than once.
   * The return value is kept as a {@link java.util.Collection Collection} for performance reasons:
   * membership tests in {@link java.util.Set Set} implementations can be unecessarily costly.
   *
   * @param area      Area of search
   * @param minRank   Minimum airport rank to return
   */
  public Collection<Airport> getAirportsInRectangle(final LatLngRect area, final int minRank)
      throws InterruptedException;

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
   * without calling {@link AirportDirectory#open} first.
   */
  public void close();
}

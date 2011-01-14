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

import java.util.Collection;
import java.util.LinkedList;

import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLngRect;

/**
 * High level interface to the aviation database based on the spatial indexing methods in
 * {@link CustomGridUtil}.
 */
public class CustomGridAirportDirectory extends AbstractAirportDirectory {
  /**
   * Low level interface to aviation database.
   */
  private final AviationDbAdapter adapter;

  /**
   * Creates an airport directory.
   *
   * @param adapter Low level interface to the aviation database.
   */
  public CustomGridAirportDirectory(AviationDbAdapter adapter) {
    this.adapter = adapter;
  }

  @Override
  public void open() {
    adapter.open();
  }

  @Override
  public void close() {
    adapter.close();
  }

  @Override
  public Collection<Airport> getAirportsInRectangle(final LatLngRect area, final int minRank)
      throws InterruptedException {
    final LinkedList<int[]> cellRanges = CustomGridUtil.getCellsInRectangle(area);
    final Collection<Airport> airportsInArea = new LinkedList<Airport>();
    for (int[] range : cellRanges) {
      ThreadUtils.checkIfInterrupted();
      final Collection<Airport> airportsInCells = adapter.getAirportsInCells(
          range[0], range[1], minRank);
      for (Airport airport : airportsInCells) {
        if (area.contains(airport.location)) {
          airportsInArea.add(airport);
        }
      }
    }
    return airportsInArea;
  }
}

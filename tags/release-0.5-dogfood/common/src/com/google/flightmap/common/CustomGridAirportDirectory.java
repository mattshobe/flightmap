// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.flightmap.common;


import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLngRect;

import java.util.Collection;
import java.util.LinkedList;

public class CustomGridAirportDirectory extends AbstractAirportDirectory {
  private final AviationDbAdapter adapter;

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
  public Collection<Airport> getAirportsInRectangle(final LatLngRect area, final int minRank) {
    final LinkedList<int[]> cellRanges = CustomGridUtil.getCellsInRectangle(area);
    final Collection<Airport> airportsInArea = new LinkedList<Airport>();
    for (int[] range : cellRanges) {
      final Collection<Airport> airportsInCells = adapter.getAirportsInCells(range[0], range[1], minRank);
      for (Airport airport : airportsInCells) {
        if (area.contains(airport.location)) {
          airportsInArea.add(airport);
        } else {
          System.out.println("Outside of rectangle - " + airport);
        }
      }
    }
    return airportsInArea;
  }
}

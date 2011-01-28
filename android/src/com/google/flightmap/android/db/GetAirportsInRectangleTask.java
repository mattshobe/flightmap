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

package com.google.flightmap.android.db;

import java.util.Collection;

import android.os.AsyncTask;

import com.google.flightmap.common.ProgressListener;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLngRect;
import com.google.flightmap.common.db.AirportDirectory;

/**
 * Background task to get airports in a rectangle (which is a fairly slow
 * query). Clients should call {@link #execute} to start the
 * background task.
 */
public class GetAirportsInRectangleTask extends
    QueryTask<GetAirportsInRectangleTask.QueryParams, Collection<Airport>> {
  private final AirportDirectory airportDirectory;

  /**
   * Initializes task to get airports in a {@link LatLngRect}.
   * 
   * @param airportDirectory directory to call on background thread.
   * @param listener listener to notify of completion. May be null.
   */
  public GetAirportsInRectangleTask(final AirportDirectory airportDirectory,
      final ProgressListener listener) {
    super(listener);
    this.airportDirectory = airportDirectory;
  }


  /**
   * {@inheritDoc}
   * <p>
   * Calls {@link AirportDirectory#getAirportsInRectangle} on a background
   * thread.
   */
  @Override
  protected Collection<Airport> doQuery(final QueryParams params) throws InterruptedException {
    final LatLngRect rectangle = params.rectangle;
    final int minRank = params.minRank;
    return airportDirectory.getAirportsInRectangle(rectangle, minRank);
  }

  /**
   * Specifies the rectangle to query and minimum airport rank when calling
   * {@link AirportDirectory#getAirportsInRectangle(LatLngRect, int)}.
   */
  public static class QueryParams {
    public final LatLngRect rectangle;
    public final int minRank;

    public QueryParams(LatLngRect rectangle, int minRank) {
      this.rectangle = rectangle;
      this.minRank = minRank;
    }
  }
}

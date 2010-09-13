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
package com.google.flightmap.android;

import java.util.Collection;

import android.os.AsyncTask;

import com.google.flightmap.common.AirportDirectory;
import com.google.flightmap.common.ProgressListener;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLngRect;

/**
 * Background task to get airports in a rectangle (which is a fairly slow
 * query). Clients should call {@link #execute(QueryParams...)} to start the
 * background task.
 */
public class GetAirportsInRectangleTask extends
    AsyncTask<GetAirportsInRectangleTask.QueryParams, Void, Collection<Airport>> {
  private final AirportDirectory airportDirectory;
  private final ProgressListener listener;
  private QueryParams queryParams;

  /**
   * Initializes task to get airports in a {@link LatLngRect}.
   * 
   * @param airportDirectory directory to call on background thread.
   * @param listener listener to notify of completion. May be null.
   */
  public GetAirportsInRectangleTask(AirportDirectory airportDirectory, ProgressListener listener) {
    this.airportDirectory = airportDirectory;
    this.listener = listener;
  }

  /**
   * Returns parameters passed to the query in progress. Returns null if there
   * is no query in progress.[
   */
  public synchronized QueryParams getInProgressQueryParams() {
    return queryParams;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Calls {@link AirportDirectory#getAirportsInRectangle} on a background
   * thread.
   */
  @Override
  protected Collection<Airport> doInBackground(QueryParams... queryData) {
    // AsyncTask specifies a Varargs parameter, but only the first element is
    // processed in this implementation.
    LatLngRect rectangle;
    int minRank;
    synchronized (this) {
      queryParams = queryData[0];
      rectangle = queryParams.rectangle;
      minRank = queryParams.minRank;
    }
    return airportDirectory.getAirportsInRectangle(rectangle, minRank);
  }

  @Override
  protected void onPostExecute(Collection<Airport> result) {
    synchronized (this) {
      queryParams = null;
    }
    if (listener != null) {
      listener.hasCompleted(true);
    }
  }

  /**
   * Specifies the rectangle to query and minimum airport rank when calling
   * {@link AirportDirectory#getAirportsInRectangle(LatLngRect, int)}.
   */
  public static class QueryParams {
    public final LatLngRect rectangle;
    public final int minRank;

    QueryParams(LatLngRect rectangle, int minRank) {
      this.rectangle = rectangle;
      this.minRank = minRank;
    }
  }
}

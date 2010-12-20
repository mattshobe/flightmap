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

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.ProgressListener;
import com.google.flightmap.common.data.Airspace;
import com.google.flightmap.common.data.LatLngRect;

/**
 * Background task to get airspaces in a rectangle (which is a fairly slow
 * query). Clients should call {@link #execute(QueryParams...)} to start the
 * background task.
 */
public class GetAirspacesInRectangleTask extends QueryTask<LatLngRect, Collection<Airspace>> {
  private final AviationDbAdapter dbAdapter;

  /**
   * Initializes task to get airspaces in a {@link LatLngRect}.
   * 
   * @param dbAdapter Interface to the aviation database.
   * @param listener listener to notify of completion. May be null.
   */
  public GetAirspacesInRectangleTask(final AviationDbAdapter dbAdapter,
      final ProgressListener listener) {
    super(listener);
    this.dbAdapter = dbAdapter;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Calls {@link AviationDbAdapter#getAirspacesInRectangle} on a background
   * thread.
   */
  @Override
  protected Collection<Airspace> doQuery(final LatLngRect params) throws InterruptedException {
    return dbAdapter.getAirspacesInRectangle(params);
  }
}

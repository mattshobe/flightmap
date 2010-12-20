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

import com.google.flightmap.common.ProgressListener;

/**
 * Background task to get airports in a rectangle (which is a fairly slow
 * query). Clients should call {@link #execute(QueryParams...)} to start the
 * background task.
 */
public abstract class QueryTask<A, B> extends AsyncTask<A, Void, B> {
  protected final ProgressListener listener;
  private A queryParams;

  /**
   * Initializes task to get airports in a {@link LatLngRect}.
   * 
   * @param airportDirectory directory to call on background thread.
   * @param listener listener to notify of completion. May be null.
   */
  public QueryTask(final ProgressListener listener) {
    this.listener = listener;
  }

  /**
   * Returns true if there is a query in progress.
   */
  public synchronized boolean isQueryInProgress() {
    return queryParams != null;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Executes query on a background thread.
   */
  @Override
  protected B doInBackground(A... queryData) {
    // AsyncTask specifies a Varargs parameter, but only the first element is
    // processed in this implementation.
    final A queryParamsCopy = queryData[0];
    synchronized (this) {
      queryParams = queryParamsCopy;
    }
    try {
      return doQuery(queryParamsCopy);
    } catch (InterruptedException iEx) {
      return null;
    }
  }

  protected abstract B doQuery(A params) throws InterruptedException ; 

  @Override
  protected void onPostExecute(B result) {
    synchronized (this) {
      queryParams = null;
    }
    if (listener != null) {
      listener.hasCompleted(true);
    }
  }
}

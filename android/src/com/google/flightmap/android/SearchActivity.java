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

import android.app.Activity;

import android.app.SearchManager;
import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.CachedAviationDbAdapter;
import com.google.flightmap.common.data.Airport;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SearchActivity extends Activity {
  private static final String TAG = SearchActivity.class.getSimpleName();

  private AviationDbAdapter aviationDbAdapter;
  private UserPrefs userPrefs;
  public Airport searchAirport;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Open database connection.
    userPrefs = new UserPrefs(getApplication());
    try {
      aviationDbAdapter = new CachedAviationDbAdapter(new AndroidAviationDbAdapter(userPrefs));
      aviationDbAdapter.open();
      } catch (Throwable t) {
        Log.w(TAG, "Unable to open database", t);
        finish();
        }
      Airport airport = handleIntent(getIntent());
      searchAirport = airport;
      if(searchAirport != null)
        Log.d("Airport", searchAirport.name);
      //TODO: Launch Tapcard with searchAirprt info. 
  }
  @Override
  protected void onNewIntent(Intent intent) {
    setIntent(intent);
    searchAirport = handleIntent(intent);
  }

  private Airport handleIntent(Intent intent) {
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      String query = intent.getStringExtra(SearchManager.USER_QUERY);
      Airport airport = doSearch(query);
      return airport;
      }
    else 
      return null;
  }
	
  private Airport doSearch(String query) {
     Airport airport = aviationDbAdapter.getAirportByICAO(query);
     return airport;	
  }
  
  public synchronized Airport getAirport() {
    return searchAirport;
  }
}

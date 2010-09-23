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

import android.app.ListActivity;

import android.app.SearchManager;
import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.CachedAviationDbAdapter;
import com.google.flightmap.common.data.Airport;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.SimpleCursorAdapter;

public class SearchActivity extends ListActivity {
  private static final String TAG = SearchActivity.class.getSimpleName();

  private AviationDbAdapter aviationDbAdapter;
  private UserPrefs userPrefs;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.search);
    // Open database connection.

    userPrefs = new UserPrefs(getApplication());
    try {
      aviationDbAdapter = new CachedAviationDbAdapter(new AndroidAviationDbAdapter(userPrefs));
      aviationDbAdapter.open();
      } catch (Throwable t) {
        Log.w(TAG, "Unable to open database", t);
        finish();
        }
      handleIntent(getIntent());

  }
  
  @Override
  protected void onNewIntent(Intent intent) {
    setIntent(intent);
    handleIntent(intent);
  }

  private void handleIntent(Intent intent) {
    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
      String query = intent.getStringExtra(SearchManager.USER_QUERY);
      doSearch(query);
    }
  }
	
  private void doSearch(String query) {
     int airportId = aviationDbAdapter.getAirportIdByICAO(query);
     if (airportId == -1 && query.length() == 3) {
       // Try again with 'K' prepended if it's only 3 letters.
       airportId = aviationDbAdapter.getAirportIdByICAO("K" + query);
     }
     if(airportId != -1) {
       showTapcard(airportId);
     }
     else {
       // Can return "like" results.
       String[] items = {query + " not found"};
       setListAdapter(new ArrayAdapter(this,android.R.layout.simple_list_item_1, items));
     }
  }
  
  private void showTapcard(int airportId) {
    Intent tapcardIntent = new Intent(this, TapcardActivity.class);
    tapcardIntent.putExtra(TapcardActivity.AIRPORT_ID, airportId);
    this.startActivity(tapcardIntent);
  }
}
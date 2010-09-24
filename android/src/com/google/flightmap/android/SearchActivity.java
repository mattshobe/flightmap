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
import android.app.SearchManager;
import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.CachedAviationDbAdapter;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;

/**
 * Activity for searching for airports by icao within MainActivity.
 */
public class SearchActivity extends ListActivity {
  private static final String TAG = SearchActivity.class.getSimpleName();

  private AviationDbAdapter aviationDbAdapter;
  private UserPrefs userPrefs;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Basic layout for showing search results.
    setContentView(R.layout.search);

    userPrefs = new UserPrefs(getApplication());
    try {
      aviationDbAdapter = new CachedAviationDbAdapter(new AndroidAviationDbAdapter(userPrefs));
      // Open database connection.
      aviationDbAdapter.open();
      } catch (Throwable t) {
        Log.w(TAG, "Unable to open database", t);
        finish();
        }
      // Handle the intent this way to avoid multiple tapcard instances.
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

  /**
   * Searches for an airport ID based on an icao as the input query.
   */
  private void doSearch(String query) {
    // 
    int airportId = aviationDbAdapter.getAirportIdByICAO(query);
    // -1 returned if the icao was not recognized. Since many pilots use only
    // the last 3 letters if an icao, try prepending a 'K' and retrying.
    if (airportId == -1 && query.length() == 3) {
      airportId = aviationDbAdapter.getAirportIdByICAO("K" + query);
    }
    if(airportId != -1) {
      showTapcard(airportId);
    }
  }
  
  /**
   * Starts the Tapcard Activity using the ID of the airport from doSearch.
   */
  private void showTapcard(int airportId) {
    Intent tapcardIntent = new Intent(this, TapcardActivity.class);
    tapcardIntent.putExtra(TapcardActivity.AIRPORT_ID, airportId);
    this.startActivity(tapcardIntent);
  }
}
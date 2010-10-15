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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.google.flightmap.common.data.Airport;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/**
 * Activity for search within MainActivity.
 */
public class SearchActivity extends ListActivity {
  private static final String TAG = SearchActivity.class.getSimpleName();

  private AviationDbAdapter aviationDbAdapter;
  private UserPrefs userPrefs;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
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
    handleIntent(getIntent());

  }

  /**
   * This method will be called when an item in the list is selected and show
   * the tapcard for the selected item.
   */
  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    super.onListItemClick(l, v, position, id);
    String content = (String) getListView().getItemAtPosition(position);
    String icao = content.split(" ")[0];
    int airportId = aviationDbAdapter.getAirportIdByIcao(icao);
    if (airportId != -1) {
      showTapcard(airportId);
    }
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
   * Shows a Tapcard for direct ICAO or single result matches, otherwise
   * displays a list of results.
   * 
   * @param query
   */

  private void doSearch(String query) {
    // Maps each airport using the airport.id as the key with the value
    // being the rank of the result.
    Map<Integer, Integer> searchResults = aviationDbAdapter.doSearch(query);
    if (searchResults.isEmpty()) {
      String[] items = {query + " not found"};
      setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
    }
    // If there is only one valid result, show it.
    if (searchResults.size() == 1) {
      Set<Integer> keys = searchResults.keySet();
      for (Iterator<Integer> iter = keys.iterator(); iter.hasNext();) {
        int id = iter.next();
        if (id == -1) {
          String[] items = {query + " not found"};
          setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items));
        } else {
          showTapcard(id);
        }
      }
    } else {
      // Sort results by descending rank.
      List<Integer> airportIds = new ArrayList<Integer>(searchResults.keySet());
      final Map<Integer, Integer> keysForComp = searchResults;
      Collections.sort(airportIds, new Comparator<Object>() {
        public int compare(Object left, Object right) {
          Integer leftKey = (Integer) left;
          Integer rightKey = (Integer) right;

          Integer leftValue = keysForComp.get(leftKey);
          Integer rightValue = keysForComp.get(rightKey);

          return rightValue.compareTo(leftValue);
        }
      });
      int airportCount = 0;
      String[] airportList = new String[airportIds.size()];
      for (Iterator<Integer> iter = airportIds.iterator(); iter.hasNext();) {
        int id = iter.next();
        Airport airport = aviationDbAdapter.getAirport(id);
        String showName = airport.icao + " " + airport.name;
        airportList[airportCount] = showName;
        airportCount++;
      }
      setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
          airportList));
    }
  }

  private void showTapcard(int airportId) {
    Intent tapcardIntent = new Intent(this, TapcardActivity.class);
    tapcardIntent.putExtra(TapcardActivity.AIRPORT_ID, airportId);
    this.startActivity(tapcardIntent);
  }
}

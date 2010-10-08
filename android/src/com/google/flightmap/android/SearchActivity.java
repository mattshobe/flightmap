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

import java.util.Iterator;
import java.util.List;

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
			aviationDbAdapter = new CachedAviationDbAdapter(
					new AndroidAviationDbAdapter(userPrefs));
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
		String icao = content.substring(0, 3);
		int airportId = aviationDbAdapter.getAirportIdByIcao(icao);
		if (airportId == -1) {
			// Try again with 'K' prepended if it's only 3 letters.
			// TODO: Move this down to the list results.
			airportId = aviationDbAdapter.getAirportIdByIcao(content.substring(
					0, 4));
		}
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

	private void doSearch(String query) {
		int airportId = aviationDbAdapter.getAirportIdByIcao(query);
		if (airportId == -1 && query.length() == 3) {
			// Try again with 'K' prepended if it's only 3 letters.
			// TODO: Move this down to the list results.
			airportId = aviationDbAdapter.getAirportIdByIcao("K" + query);
		}
		if (airportId != -1) {
			showTapcard(airportId);
		} else {
			// Replaces spaces with % for the %LIKE% db search.
			String query_like = query.replace(' ', '%');
			query_like = '%' + query_like + '%';
			List<Integer> airportsName = aviationDbAdapter
					.getAirportIdsWithNameLike(query_like);
			List<Integer> airports = aviationDbAdapter
					.getAirportIdsWithCityLike(query_like);
			Iterator<Integer> nameIterator = airportsName.iterator();
			// Merge nameMatches into the airports list. That list will have all
			// matches by name or city.
			while (nameIterator.hasNext()) {
				int id = nameIterator.next();
				if (!airports.contains(id)) {
					airports.add(id);
				}
			}
			if (airports.isEmpty()) {
				String[] items = { query + " not found" };
				setListAdapter(new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, items));
			} else {
				Iterator<Integer> airportIterator = airports.iterator();
				int i = 0;
				String[] airportList = new String[airports.size()];
				while (airportIterator.hasNext()) {
					int id = airportIterator.next();
					String showName = aviationDbAdapter.getAirport(id).icao
							+ " " + aviationDbAdapter.getAirport(id).name;
					airportList[i] = showName;
					i++;
				}
				setListAdapter(new ArrayAdapter<String>(this,
						android.R.layout.simple_list_item_1, airportList));
			}
		}
	}

	private void showTapcard(int airportId) {
		Intent tapcardIntent = new Intent(this, TapcardActivity.class);
		tapcardIntent.putExtra(TapcardActivity.AIRPORT_ID, airportId);
		this.startActivity(tapcardIntent);
	}
}
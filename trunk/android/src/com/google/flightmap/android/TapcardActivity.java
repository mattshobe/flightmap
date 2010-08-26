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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.CachedAviationDbAdapter;
import com.google.flightmap.common.data.Airport;

/**
 * Shows details about an airport.
 */
public class TapcardActivity extends Activity {
  private static final String TAG = TapcardActivity.class.getSimpleName();

  // Keys used in the bundle passed to this activity.
  private static final String PACKAGE_NAME = TapcardActivity.class.getPackage().getName();
  public static final String AIRPORT_ID = PACKAGE_NAME + "AirportId";

  private LinearLayout tapcardLayout;
  private AviationDbAdapter aviationDbAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    tapcardLayout = new LinearLayout(this);
    tapcardLayout.setOrientation(LinearLayout.VERTICAL);

    UserPrefs userPrefs = new UserPrefs(getApplication());
    aviationDbAdapter = new CachedAviationDbAdapter(new AndroidAviationDbAdapter(userPrefs));
    // TODO: handle the case of this throwing when there's no database.
    aviationDbAdapter.open();

    final Intent startingIntent = getIntent();
    int airportId = startingIntent.getIntExtra(AIRPORT_ID, -1);
    Airport airport = aviationDbAdapter.getAirport(airportId);
    if (null == airport) {
      Log.w(TAG, "Unable to get airport for id " + airportId);
      finish();
    }

    TextView icaoText = new TextView(this);
    icaoText.setText(airport.icao);
    tapcardLayout.addView(icaoText);

    TextView airportName = new TextView(this);
    airportName.setText(airport.name);
    tapcardLayout.addView(airportName);
    setContentView(tapcardLayout);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (aviationDbAdapter != null) {
      aviationDbAdapter.close();
    }
  }
}

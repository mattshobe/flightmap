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
package com.google.blackbox;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.blackbox.AirportDbAdapter.AirportDistance;
import com.google.blackbox.data.LatLng;

public class BlackBox extends Activity {
  private static final String TAG = BlackBox.class.getSimpleName();
  private AirportDbAdapter airportReader;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    airportReader = new AirportDbAdapter();
    airportReader.open();
  }

  @Override
  protected void onResume() {
    super.onResume();
    AirportDistance[] airportDistances = airportReader.getNearestAirports(
        new LatLng(20, 30), 5);
    if (null == airportDistances) {
      Log.w(TAG, "No airports returned");
      return;
    }
    TableLayout table = new TableLayout(this);
    table.setLayoutParams(new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.FILL_PARENT,
        LinearLayout.LayoutParams.FILL_PARENT));
    table.setColumnStretchable(1, true);
    for (int i = 0; i < airportDistances.length; i++) {
      AirportDistance airportDistance = airportDistances[i];
      TableRow row = new TableRow(this);
      row.addView(createTextView(airportDistance.airport.icao));
      row.addView(createTextView(airportDistance.airport.name));
      row.addView(createTextView(String
          .format("%.1f", airportDistance.distance)));
      table.addView(row);
    }
    setContentView(table);
  }

  private View createTextView(String text) {
    TextView result = new TextView(this);
    result.setText(text);
    result.setPadding(3, 3, 3, 3);
    return result;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    airportReader.close();
  }
}

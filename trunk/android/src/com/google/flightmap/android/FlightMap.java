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

import java.util.SortedSet;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.flightmap.common.CustomGridAirportDirectory;
import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;

public class FlightMap extends Activity {
  private static final String TAG = FlightMap.class.getSimpleName();
  /** Milliseconds beteween screen updates. */
  private static final int UPDATE_RATE = 100;
  private boolean isRunning;
  private long lastUpdateTime;
  private UpdateHandler updater = new UpdateHandler();
  private LocationHandler locationHandler;
  private Location previousLocation;
  private CustomGridAirportDirectory airportDirectory;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    locationHandler =
        new LocationHandler((LocationManager) getSystemService(Context.LOCATION_SERVICE));
    airportDirectory = new CustomGridAirportDirectory(new AndroidAviationDbAdapter());
    airportDirectory.open();
  }

  @Override
  protected void onResume() {
    super.onResume();
    locationHandler.startListening();
    isRunning = true;
    update();
  }

  @Override
  protected void onPause() {
    super.onPause();
    isRunning = false;
    locationHandler.stopListening();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    airportDirectory.close();
  }

  /**
   * Updates the view every {@link #UPDATE_RATE} milliseconds using
   * {@link UpdateHandler}.
   */
  private void update() {
    if (!isRunning) {
      return;
    }
    long now = System.currentTimeMillis();
    if (now - lastUpdateTime > UPDATE_RATE) {
      drawUi();
      lastUpdateTime = now;
    }
    updater.scheduleUpdate(UPDATE_RATE);
  }

  private void drawUi() {
    TableLayout table = new TableLayout(this);
    table.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
        LinearLayout.LayoutParams.FILL_PARENT));
    table.setColumnStretchable(1, true);
    Location location = locationHandler.getLocation();
    if (null == location) {
      table.addView(createLabelValueRow("Location", "Unknown"));
      setContentView(table);
      return;
    }
    if (location.equals(previousLocation)) {
      return;
    }
    previousLocation = location;
    
    LatLng position = LatLng.fromDouble(location.getLatitude(), location.getLongitude());
    Log.i(TAG, "About to fetch airports...");
    SortedSet<AirportDistance> airports = airportDirectory.getAirportsWithinRadius(position, 50);
    Log.i(TAG, String.format("Got %d airports", airports.size()));

    table.addView(createLabelValueRow("Airport", "Dist"));
    for (AirportDistance airportDistance : airports) {
      table.addView(createLabelValueRow(airportDistance.airport.name, String.format("%.1f",
          airportDistance.distance)));
    }
    setContentView(table);
  }

  private TableRow createLabelValueRow(String label, String value) {
    TableRow result = new TableRow(this);
    result.addView(createTextView(label));
    result.addView(createTextView(value));
    return result;
  }

  private View createTextView(String text) {
    TextView result = new TextView(this);
    result.setText(text);
    result.setPadding(3, 3, 3, 3);
    return result;
  }

  /**
   * Updates the UI using a delayed message.
   */
  private class UpdateHandler extends Handler {
    private static final int UPDATE_MESSAGE = 1;

    @Override
    public void handleMessage(Message msg) {
      update();
    }

    /**
     * Call {@link #update} after {@code delay} milliseconds.
     */
    public void scheduleUpdate(long delay) {
      removeMessages(UPDATE_MESSAGE);
      sendMessageDelayed(obtainMessage(UPDATE_MESSAGE), delay);
    }
  }
}

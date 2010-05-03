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
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.content.SharedPreferences;

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.CustomGridAirportDirectory;

public class FlightMap extends Activity {
  private static final String TAG = FlightMap.class.getSimpleName();
  /**
   * Milliseconds between screen updates. Note that the fastest I've seen GPS
   * updates arrive is once per second.
   */
  private static long UPDATE_RATE = 100;

  private static final int MENU_SETTINGS = 0;

  // Saved instance state constants
  private static final String DISCLAIMER_ACCEPTED = "disclaimer-accepted";

  private boolean disclaimerAccepted;
  private boolean isRunning;
  private UpdateHandler updater = new UpdateHandler();
  private LocationHandler locationHandler;
  private MapView mapView;
  AviationDbAdapter aviationDbAdapter;
  CustomGridAirportDirectory airportDirectory;
  
  public static long updateInterval;
  public static boolean isNorthUp;
  public static String units;
  public static boolean showSeaplane;
  public static boolean showMilitary;
  public static boolean showSoft;
  public static boolean showPrivate;
  public static boolean showHeli;
  public static int runwayLength;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    locationHandler =
        new LocationHandler((LocationManager) getSystemService(Context.LOCATION_SERVICE));
    
    aviationDbAdapter = new AndroidAviationDbAdapter();
    airportDirectory = new CustomGridAirportDirectory(aviationDbAdapter);
    airportDirectory.open();

    getPreferences(this);
    
    if (null == getMapView()) {
      setMapView(new MapView(FlightMap.this));
    }

    // Show the disclaimer screen if there's no previous state, or the user
    // didn't accept the disclaimer.
    if (null == savedInstanceState || !savedInstanceState.getBoolean(DISCLAIMER_ACCEPTED)) {
      setDisclaimerAccepted(false);
      showDisclaimerView();
    } else { // disclaimer accepted.
      setDisclaimerAccepted(true);
      showMapView();
    }
  }

  
  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(DISCLAIMER_ACCEPTED, isDisclaimerAccepted());
    MapView map = getMapView();
    if (null != map) {
      map.saveInstanceState(outState);
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    MapView map = getMapView();
    if (null != map) {
      map.restoreInstanceState(savedInstanceState);
    }
  }

  
  private void showDisclaimerView() {
    setContentView(R.layout.disclaimer);

    // Set disclaimer "agree" button to switch to map view.
    Button agreeButton = (Button) findViewById(R.id.agree);
    agreeButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        setDisclaimerAccepted(true);
        showMapView();
      }
    });
  }

  private void showMapView() {
    setContentView(getMapView());
  }

  private void getPreferences(Context context) {
  	SharedPreferences sharedPreferences = this.getPreferences(0);
	isNorthUp = sharedPreferences.getBoolean(UserPrefs.NORTH_UP, false);
	updateInterval = sharedPreferences.getLong(UserPrefs.UPDATE_INTERVAL, 100);
	units = sharedPreferences.getString(UserPrefs.DISTANCE_UNITS, "Nautical Miles");
	showSeaplane = sharedPreferences.getBoolean(UserPrefs.SHOW_SEAPLANE, false);
	showMilitary = sharedPreferences.getBoolean(UserPrefs.SHOW_MILITARY, true);
	showSoft = sharedPreferences.getBoolean(UserPrefs.SHOW_SOFT, true);
	showPrivate = sharedPreferences.getBoolean(UserPrefs.SHOW_PRIVATE, true);
	showHeli = sharedPreferences.getBoolean(UserPrefs.SHOW_HELI, false);
	runwayLength = sharedPreferences.getInt(UserPrefs.RUNWAY_LENGTH, 2000);
  }
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_SETTINGS, 0, "Settings");
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case MENU_SETTINGS:
    	  Intent startIntent = new Intent(this, UserPrefs.class);
    	  startActivity(startIntent);
    	  return true;
    }
    return false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    locationHandler.startListening();
    setRunning(true);
    update();
  }

  @Override
  protected void onPause() {
    super.onPause();
    setRunning(false);
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
    if (!isRunning()) {
      return;
    }
    drawUi();
    
    updater.scheduleUpdate(UPDATE_RATE);
  }

  private void drawUi() {
    MapView map = getMapView();
    if (null == map) {
      return; // Disclaimer not accepted yet.
    }
    Location location = locationHandler.getLocation();
    map.drawMap(location);
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

  private synchronized boolean isRunning() {
    return isRunning;
  }

  private synchronized void setRunning(boolean isRunning) {
    this.isRunning = isRunning;
  }

  private synchronized MapView getMapView() {
    return mapView;
  }

  private synchronized void setMapView(MapView mapView) {
    this.mapView = mapView;
  }

  public synchronized void setDisclaimerAccepted(boolean disclaimerAccepted) {
    this.disclaimerAccepted = disclaimerAccepted;
  }

  public synchronized boolean isDisclaimerAccepted() {
    return disclaimerAccepted;
  }
}

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

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.CachedAviationDbAdapter;
import com.google.flightmap.common.CachedMagneticVariation;
import com.google.flightmap.common.NavigationUtil;
import com.google.flightmap.common.NavigationUtil.DistanceUnits;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.Comm;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.Runway;
import com.google.flightmap.common.data.RunwayEnd;

/**
 * Shows details about an airport.
 */
public class TapcardActivity extends Activity {
  private static final String TAG = TapcardActivity.class.getSimpleName();

  // Colors.
  private static final int AIRPORT_NAME_COLOR = Color.WHITE;
  private static final int DROP_SHADOW_COLOR = Color.argb(0x80, 0x33, 0x33, 0x33);
  private static final int DEFAULT_BACKGROUND_COLOR = Color.WHITE;
  private static final int DEFAULT_FOREGROUND_COLOR = Color.BLACK;

  /**
   * Milliseconds between screen updates. Note that the fastest I've seen GPS
   * updates arrive is once per second.
   */
  private static long UPDATE_RATE = 100;
  private boolean isRunning;
  private UpdateHandler updater = new UpdateHandler();

  // Keys used in the bundle passed to this activity.
  private static final String PACKAGE_NAME = TapcardActivity.class.getPackage().getName();
  public static final String AIRPORT_ID = PACKAGE_NAME + "AirportId";

  private LinearLayout tapcardLayout;
  private AviationDbAdapter aviationDbAdapter;
  private LocationHandler locationHandler;
  private UserPrefs userPrefs;
  // Magnetic variation w/ caching.
  private final CachedMagneticVariation magneticVariation = new CachedMagneticVariation();

  // Items for the navigation display.
  private LatLng airportLatLng;
  private TextView distanceText;
  private TextView bearingText;
  private TextView eteText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
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

    // Get location updates
    locationHandler =
        new LocationHandler((LocationManager) getSystemService(Context.LOCATION_SERVICE));

    // No title bar.
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // Find which airport to show.
    final Intent startingIntent = getIntent();
    int airportId = startingIntent.getIntExtra(AIRPORT_ID, -1);
    Airport airport = aviationDbAdapter.getAirport(airportId);
    if (null == airport) {
      Log.w(TAG, "Unable to get airport for id " + airportId);
      finish();
    }
    displayTapcardUi(airport);
  }

  private void displayTapcardUi(Airport airport) {
    tapcardLayout = new LinearLayout(this);
    tapcardLayout.setOrientation(LinearLayout.VERTICAL);
    tapcardLayout.setBackgroundColor(DEFAULT_BACKGROUND_COLOR);

    // ICAO id and airport name.
    addIcaoAndName(airport);

    // Navigation info to airport
    addNavigationInfo(airport);

    // Communication info
    addCommInfo(aviationDbAdapter.getAirportComms(airport.id));

    // Runway details
    addRunways(airport.runways);

    // General properties
    // TODO: Don't show all of these. Just extract elevation.
    final Map<String, String> airportProperties =
        aviationDbAdapter.getAirportProperties(airport.id);
    if (airportProperties != null) {
      tapcardLayout.addView(getPropertiesTextView(airportProperties));
    }

    ScrollView scroller = new ScrollView(this);
    scroller.addView(tapcardLayout);
    setContentView(scroller);
  }

  /**
   * Adds the ICAO and airport name to the tapcard. These are in a one-row
   * table.
   */
  private void addIcaoAndName(Airport airport) {
    TableLayout nameTable = new TableLayout(this);
    nameTable.setColumnStretchable(1, true);
    TableRow nameRow = new TableRow(this);
    int nameBackground =
        airport.isTowered ? UiConstants.TOWERED_PAINT.getColor() : UiConstants.NON_TOWERED_PAINT
            .getColor();
    nameRow.setBackgroundColor(nameBackground);
    nameRow.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);

    // ICAO
    TextView icaoText = new TextView(this);
    icaoText.setText(airport.icao);
    icaoText.setTypeface(Typeface.DEFAULT_BOLD);
    icaoText.setShadowLayer(1.2f, 3, 3, DROP_SHADOW_COLOR);
    icaoText.setTextSize(32);
    icaoText.setTextColor(AIRPORT_NAME_COLOR);
    icaoText.setPadding(5, 2, 5, 2);
    nameRow.addView(icaoText);

    // Name
    TextView airportName = new TextView(this);
    airportName.setText(airport.name);
    airportName.setTypeface(Typeface.DEFAULT);
    airportName.setTextSize(16);
    airportName.setTextColor(AIRPORT_NAME_COLOR);
    airportName.setPadding(5, 2, 5, 2);
    nameRow.addView(airportName);

    nameTable.addView(nameRow);
    tapcardLayout.addView(nameTable);
  }

  private void addNavigationInfo(Airport airport) {
    airportLatLng = airport.location;

    LinearLayout navLayout = new LinearLayout(this);
    navLayout.setOrientation(LinearLayout.HORIZONTAL);
    navLayout.setBackgroundColor(Color.BLACK);
    navLayout.setPadding(5, 5, 5, 5);

    // TODO: Replace with graphic airplane pointer
    TextView airplanePlaceholder = new TextView(this);
    airplanePlaceholder.setText("]-/-"); // That's my ASCII airplane :-)
    setNavigationTextAttributes(airplanePlaceholder);
    navLayout.addView(airplanePlaceholder);

    // These will be updated by #updateNavigationDisplay.
    distanceText = new TextView(this);
    bearingText = new TextView(this);
    eteText = new TextView(this);
    setNavigationTextAttributes(distanceText);
    setNavigationTextAttributes(bearingText);
    setNavigationTextAttributes(eteText);
    navLayout.addView(distanceText);
    navLayout.addView(bearingText);
    navLayout.addView(eteText);
    tapcardLayout.addView(navLayout);

    updateNavigationDisplay();
  }

  private void setNavigationTextAttributes(TextView text) {
    text.setTextColor(Color.WHITE);
    text.setTypeface(Typeface.DEFAULT_BOLD);
    text.setTextSize(14);
  }


  /**
   * Adds communication frequencies to the tapcard.
   */
  private void addCommInfo(final List<Comm> comms) {
    if (comms != null) {
      for (Comm comm : comms) {
        final StringBuilder sb = new StringBuilder();
        sb.append(comm.identifier);
        sb.append(" ");
        sb.append(comm.frequency);
        if (comm.remarks != null) {
          sb.append(" (");
          sb.append(comm.remarks);
          sb.append(")");
        }
        final TextView commText = new TextView(this);
        commText.setText(sb.toString());
        tapcardLayout.addView(commText);
      }
    }
  }

  /**
   * Adds runway details to the tapcard.
   */
  private void addRunways(SortedSet<Runway> runways) {
    for (Runway runway : runways) {
      final StringBuilder sb = new StringBuilder();
      sb.append("\n");
      sb.append("Runway ");
      sb.append(runway.letters);
      sb.append(": ");
      sb.append(runway.length);
      sb.append("x");
      sb.append(runway.width);
      sb.append(" (");
      sb.append(runway.surface);
      sb.append(")");
      final TextView runwayText = new TextView(this);
      runwayText.setText(sb.toString());
      tapcardLayout.addView(runwayText);

      for (RunwayEnd runwayEnd : runway.runwayEnds) {
        final TextView runwayEndText = new TextView(this);
        runwayEndText.setText("Runway " + runwayEnd.letters + ":");
        tapcardLayout.addView(runwayEndText);
        final Map<String, String> runwayEndsProperties =
            aviationDbAdapter.getRunwayEndProperties(runwayEnd.id);
        if (runwayEndsProperties != null) {
          tapcardLayout.addView(getPropertiesTextView(runwayEndsProperties));
        }
      }
    }
  }

  /**
   * Returns text corresponding to {@code properties}.
   */
  private TextView getPropertiesTextView(final Map<String, String> properties) {
    final StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> property : properties.entrySet()) {
      sb.append(property.getKey());
      sb.append(":");
      sb.append(property.getValue());
      sb.append("\n");
    }
    final TextView propertiesView = new TextView(this);
    propertiesView.setText(sb.toString());
    return propertiesView;
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
    if (aviationDbAdapter != null) {
      aviationDbAdapter.close();
    }
  }

  /**
   * Updates the view every {@link #UPDATE_RATE} milliseconds using
   * {@link UpdateHandler}.
   */
  private void update() {
    updater.scheduleUpdate(UPDATE_RATE);
    if (!isRunning()) {
      return;
    }
    updateNavigationDisplay();
  }

  /**
   * Updates the pointer, distance, bearing and ETE to the selected airport.
   */
  private void updateNavigationDisplay() {
    final Location location = locationHandler.getLocation();
    if (null == location) {
      distanceText.setText("Location unavailable");
      bearingText.setText("");
      eteText.setText("");
      return;
    }

    // Calculate distance and bearing to airport.
    final double locationLat = location.getLatitude();
    final double locationLng = location.getLongitude();
    final LatLng locationLatLng = LatLng.fromDouble(locationLat, locationLng);
    // results[0]===distance, results[1]==bearing
    float[] results = new float[2];
    Location.distanceBetween(locationLat, locationLng, airportLatLng.latDeg(), airportLatLng
        .lngDeg(), results);
    final float distanceMeters = results[0];
    final float bearingTo =
        (float) NavigationUtil.normalizeBearing(results[1])
            + magneticVariation
                .getMagneticVariation(locationLatLng, (float) location.getAltitude());

    DistanceUnits distanceUnits = userPrefs.getDistanceUnits();
    String distance =
        String.format("      %.1f%s", distanceMeters * distanceUnits.distanceMultiplier,
            distanceUnits.distanceAbbreviation);
    distanceText.setText(distance);
    bearingText.setText(String.format(" - %03.0f%s", bearingTo, UiConstants.DEGREES_SYMBOL));

    final DistanceUnits nauticalUnits = DistanceUnits.NAUTICAL_MILES;
    final double speedInKnots = nauticalUnits.getSpeed(location.getSpeed());
    if (speedInKnots > 3) {
      final float metersPerSecond = location.getSpeed();
      float timeInSeconds = distanceMeters / metersPerSecond;
      int hours = (int) (timeInSeconds / 60 / 60);
      int minutes = (int) (timeInSeconds / 60) - (hours * 60);
      int seconds = (int) (timeInSeconds - (hours * 60 * 60) - (minutes * 60));

      // Normally hours will be 0, so omit if possible.
      if (hours == 0) {
        eteText.setText(String.format(" - %d:%02d", minutes, seconds));
      } else {
        eteText.setText(String.format(" - %d:%02d:%02d", hours, minutes, seconds));
      }
    } else {
      eteText.setText("");
    }
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
}

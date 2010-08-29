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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
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
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.Comm;
import com.google.flightmap.common.data.Runway;
import com.google.flightmap.common.data.RunwayEnd;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Shows details about an airport.
 */
public class TapcardActivity extends Activity {
  private static final String TAG = TapcardActivity.class.getSimpleName();

  private static final int AIRPORT_NAME_COLOR = Color.WHITE;
  private static final int DROP_SHADOW_COLOR = Color.argb(0x80, 0x33, 0x33, 0x33);
  private static final Paint ETE_TEXT_PAINT = new Paint();
  private static final Paint NORMAL_TEXT_PAINT = new Paint();
  private static final Paint BOLD_TEXT_PAINT = new Paint();
  private static final Paint COMM_SMALL_TEXT_PAINT = new Paint();
  private static final Paint RUNWAYS_TEXT_PAINT = new Paint();
  private static boolean textSizesSet;

  // Keys used in the bundle passed to this activity.
  private static final String PACKAGE_NAME = TapcardActivity.class.getPackage().getName();
  public static final String AIRPORT_ID = PACKAGE_NAME + "AirportId";

  private LinearLayout tapcardLayout;
  private AviationDbAdapter aviationDbAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Open database connection.
    UserPrefs userPrefs = new UserPrefs(getApplication());
    try {
      aviationDbAdapter = new CachedAviationDbAdapter(new AndroidAviationDbAdapter(userPrefs));
      aviationDbAdapter.open();
    } catch (Throwable t) {
      Log.w(TAG, "Unable to open database", t);
      finish();
    }

    // No title bar.
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    // Set text sizes based on display density.
    setTextSizes(getApplication().getResources().getDisplayMetrics().density);

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

    // ICAO id and airport name.
    addIcaoAndName(airport);

    // TODO: Add airport pointer, dist, brg, ete here.

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
    int nameBackground = airport.isTowered
        ? UiConstants.TOWERED_PAINT.getColor() : UiConstants.NON_TOWERED_PAINT.getColor();
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
  protected void onDestroy() {
    super.onDestroy();
    if (aviationDbAdapter != null) {
      aviationDbAdapter.close();
    }
  }

  /**
   * Scales text sizes based on screen density. See
   * http://developer.android.com/guide/practices/screens_support.html#dips-pels
   */
  private synchronized static void setTextSizes(float density) {
    if (textSizesSet) {
      return;
    }
    textSizesSet = true;
    ETE_TEXT_PAINT.setTextSize(14 * density);
    NORMAL_TEXT_PAINT.setTextSize(18 * density);
    BOLD_TEXT_PAINT.setTextSize(18 * density);
    COMM_SMALL_TEXT_PAINT.setTextSize(15 * density);
    RUNWAYS_TEXT_PAINT.setTextSize(22 * density);
  }
}

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
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.TypedValue;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.LinearLayout;
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

import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * Shows details about an airport.
 */
public class TapcardActivity extends Activity implements SurfaceHolder.Callback {
  private static final String TAG = TapcardActivity.class.getSimpleName();

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

  // Dimensions of the "to" pointer in pixels
  private static final float POINTER_LENGTH = 12;
  private static final float POINTER_WIDTH = 10;

  private AviationDbAdapter aviationDbAdapter;
  private LocationHandler locationHandler;
  private UserPrefs userPrefs;

  // Screen density
  private float density;

  // Magnetic variation w/ caching.
  private final CachedMagneticVariation magneticVariation = new CachedMagneticVariation();

  // Last known bearing
  private float lastBearing;

  // Items for the navigation display.
  private LatLng airportLatLng;
  private Drawable airplaneImage;
  private Path pointerPath = new Path();
  private SurfaceView miniMap;
  private SurfaceHolder holder;
  private TextView distanceText;
  private TextView bearingText;
  private TextView eteText;
  private Paint airplanePaint = new Paint();
  private float[] distanceBearingResult = new float[2];

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // No title bar. This must be done before setContentView.
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.tapcard);

    // Open database connection.
    userPrefs = new UserPrefs(getApplication());
    try {
      aviationDbAdapter = new CachedAviationDbAdapter(new AndroidAviationDbAdapter(userPrefs));
      aviationDbAdapter.open();
    } catch (Throwable t) {
      Log.w(TAG, "Unable to open database", t);
      finish();
    }

    // Screen density. Scale pixel sizes up based on this.
    density = getResources().getDisplayMetrics().density;

    // Get location updates
    locationHandler =
        new LocationHandler((LocationManager) getSystemService(Context.LOCATION_SERVICE));

    // Find which airport to show.
    final Intent startingIntent = getIntent();
    int airportId = startingIntent.getIntExtra(AIRPORT_ID, -1);
    Airport airport = aviationDbAdapter.getAirport(airportId);
    if (null == airport) {
      Log.w(TAG, "Unable to get airport for id " + airportId);
      finish();
    }
    initializeTapcardUi(airport, getResources());
  }

  /**
   * Initializes the tapcard UI. Should only be called once from onCreate().
   */
  private void initializeTapcardUi(Airport airport, Resources res) {
    // ICAO id and airport name.
    setIcaoAndName(airport, res);

    // Navigation info to airport
    setNavigationInfo(airport, res);

    // Communication info
    addCommInfo(aviationDbAdapter.getAirportComms(airport.id), res);

    // Runway details
    addRunways(airport.runways, res);

    // Elevation
    addElevation(airport);
  }

  /**
   * Sets color for ICAO and name item at the top.
   */
  private void setIcaoAndName(Airport airport, Resources res) {
    TableRow nameRow = (TableRow) findViewById(R.id.tapcard_icao_and_name_row);
    int nameBackground = airport.isTowered ? res.getColor(R.color.ToweredAirport) : res.getColor(
        R.color.NonToweredAirport);
    nameRow.setBackgroundColor(nameBackground);

    // ICAO
    TextView icaoText = (TextView) findViewById(R.id.tapcard_icao);
    icaoText.setText(airport.icao);

    // Name
    TextView airportName = (TextView) findViewById(R.id.tapcard_airport_name);
    airportName.setText(airport.name);
  }

  /**
   * Sets up the static parts of the navigation display.
   */
  private void setNavigationInfo(Airport airport, Resources res) {
    airportLatLng = airport.location;
    airplaneImage = MapView.centerImage(res.getDrawable(R.drawable.aircraft));
    airplaneImage.setFilterBitmap(true); // Improves rendering quality.
    airplanePaint.setColor(res.getColor(R.color.AircraftPaint));
    airplanePaint.setStrokeWidth(1.5f);
    airplanePaint.setAntiAlias(true);

    // Create path for the pointer.
    pointerPath.lineTo(POINTER_WIDTH / 2f, 0);
    pointerPath.lineTo(0, -POINTER_LENGTH);
    pointerPath.lineTo(-POINTER_WIDTH / 2f, 0);
    pointerPath.close();

    miniMap = (SurfaceView) findViewById(R.id.tapcard_minimap);
    holder = miniMap.getHolder();
    holder.addCallback(this);
    distanceText = (TextView) findViewById(R.id.tapcard_distance);
    bearingText = (TextView) findViewById(R.id.tapcard_bearing);
    eteText = (TextView) findViewById(R.id.tapcard_ete);
  }

  /**
   * Adds communication frequencies to the tapcard.
   */
  private void addCommInfo(final List<Comm> comms, Resources res) {
    if (comms == null) {
      return;
    }
    final TableLayout commTable = (TableLayout) findViewById(R.id.tapcard_comm_table);
    final LayoutParams rowLayout =
        new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
    final int textColor = res.getColor(R.color.TapcardForeground);
    for (Comm comm : comms) {
      TableRow commRow = new TableRow(this);
      commRow.setLayoutParams(rowLayout);

      // Identifier
      TextView ident = new TextView(this);
      ident.setText(comm.identifier);
      ident.setTypeface(Typeface.SANS_SERIF);
      ident.setTextColor(textColor);
      ident.setTextSize(TypedValue.DENSITY_DEFAULT, 18 * density);
      ident.setPadding(5, 5, 25, 5);
      commRow.addView(ident);

      // Frequency
      TextView frequency = new TextView(this);
      frequency.setText(comm.frequency);
      frequency.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
      frequency.setTextColor(textColor);
      frequency.setTextSize(TypedValue.DENSITY_DEFAULT, 22 * density);
      frequency.setPadding(25, 5, 5, 5);
      commRow.addView(frequency);

      commTable.addView(commRow);

      if (comm.remarks != null) {
        commRow = new TableRow(this);
        commRow.setLayoutParams(rowLayout);
        TextView remarks = new TextView(this);
        remarks.setText(comm.remarks);
        remarks.setTypeface(Typeface.SANS_SERIF);
        remarks.setTextColor(textColor);
        remarks.setTextSize(TypedValue.DENSITY_DEFAULT, 15 * density);
        remarks.setPadding(5, 0, 0, 5);

        commRow.addView(remarks);
        commTable.addView(commRow);
      }
    }
  }

  /**
   * Adds runway details to the tapcard.
   *
   * @param res
   */
  private void addRunways(SortedSet<Runway> runways, Resources res) {
    final LinearLayout runwayLayout = (LinearLayout) findViewById(R.id.tapcard_runway_layout);
    final int textColor = res.getColor(R.color.TapcardForeground);

    for (Runway runway : runways) {
      TextView letters = new TextView(this);
      letters.setText(runway.letters);
      letters.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
      letters.setTextColor(textColor);
      letters.setTextSize(TypedValue.DENSITY_DEFAULT, 22 * density);
      letters.setPadding(5, 5, 5, 0);
      runwayLayout.addView(letters);

      TextView size = new TextView(this);
      size.setText(runway.length + "x" + runway.width + " " + runway.surface);
      size.setTypeface(Typeface.SANS_SERIF);
      size.setTextColor(textColor);
      size.setTextSize(TypedValue.DENSITY_DEFAULT, 18 * density);
      size.setPadding(5, 0, 5, 10);
      runwayLayout.addView(size);
    }
  }

  /**
   * Adds airport elevation to the tapcard
   */
  private void addElevation(Airport airport) {
    final Map<String, String> airportProperties =
        aviationDbAdapter.getAirportProperties(airport.id);
    if (airportProperties == null) {
      return;
    }
    String elevation = airportProperties.get("Elevation");
    if (elevation == null) {
      return;
    }
    TextView elevationText = (TextView) findViewById(R.id.tapcard_elevation);
    elevationText.setText("ELEV " + elevation + "' MSL");
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
   * Surface dimensions changed.
   */
  @Override
  public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
  }

  @Override
  public synchronized void surfaceCreated(SurfaceHolder holder) {
    this.holder = holder;
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    this.holder = null;
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
   * This is called repeatedly by {@link #update()}.
   */
  private void updateNavigationDisplay() {
    final Location location = locationHandler.getLocation();
    float magneticConversion = 0;
    if (location != null) {
      final LatLng locationLatLng =
          LatLng.fromDouble(location.getLatitude(), location.getLongitude());
      magneticConversion =
          magneticVariation.getMagneticVariation(locationLatLng, (float) location.getAltitude());
    }

    setDistanceBearingResult(location, magneticConversion);
    updateNavigationMiniMap(location, magneticConversion);
    updateNavigationTextItems(location, magneticConversion);
  }

  /**
   * Stores the distance and bearing from {@code location} to {@code
   * airportLatLng} in distanceBearingResult[]. Index 0 will be the distance in
   * meters, index 1 will be the bearing in magnetic degrees.
   */
  private synchronized void setDistanceBearingResult(Location location, float magneticConversion) {
    // Calculate distance and bearing to airport.
    final double locationLat = location.getLatitude();
    final double locationLng = location.getLongitude();
    // results[0]===distance, results[1]==bearing
    Location.distanceBetween(locationLat, locationLng, airportLatLng.latDeg(),
        airportLatLng.lngDeg(), distanceBearingResult);
    distanceBearingResult[1] =
        (float) NavigationUtil.normalizeBearing(distanceBearingResult[1] + magneticConversion);
  }

  /**
   * Updates the mini map pointing to the airport.
   */
  private synchronized void updateNavigationMiniMap(Location location, float magneticConversion) {
    Canvas c = null;
    try {
      if (null == holder) {
        return;
      }
      c = holder.lockCanvas();
      synchronized (holder) {
        if (c != null) {
          c.drawColor(Color.BLACK);
          if (location == null) {
            return;
          }

          // Update bearing (if possible)
          if (location.hasBearing()) {
            lastBearing = location.getBearing() + magneticConversion;
          }

          // Center everythng on the canvas.
          c.translate(c.getWidth() / 2, c.getHeight() / 2);
          // Scale the image down based on the size of the canvas.
          final int width = c.getWidth();
          final float imageScale = width / 150f;
          c.save();
          c.scale(imageScale, imageScale);

          // Rotate the airplane, then draw it.
          // For north-up, rotate the airplane to its current track.
          // Do nothing for track-up (the airplane pointing up is right).
          if (userPrefs.isNorthUp()) {
            c.rotate(lastBearing);
          }
          airplaneImage.draw(c);

          // Undo the downscaling and rotation for the airplane.
          c.restore();

          // Draw a circle around the airplane.
          airplanePaint.setStyle(Style.STROKE);
          float radius = (width / 2f) - POINTER_LENGTH;
          c.drawCircle(0, 0, radius, airplanePaint);

          // Calculate pointer direction. If in north-up mode, the it's just
          // bearingTo, otherwise it's the relative bearing to.
          float bearingTo = distanceBearingResult[1];
          if (!userPrefs.isNorthUp()) {
            bearingTo = (float) NavigationUtil.normalizeBearing(bearingTo - lastBearing);
          }

          // Rotate the pointer and draw it.
          c.rotate(bearingTo);
          c.translate(0, -radius);
          airplanePaint.setStyle(Style.FILL);
          c.drawPath(pointerPath, airplanePaint);
        }
      }
    } finally {
      if (c != null) {
        holder.unlockCanvasAndPost(c);
      }
    }
  }

  /**
   * Updates the distance, bearing and ete text items.
   */
  private synchronized void updateNavigationTextItems(
      final Location location, float magneticConversion) {
    if (null == location) {
      distanceText.setText("Location unavailable");
      bearingText.setText("");
      eteText.setText("");
      return;
    }

    final float distanceMeters = distanceBearingResult[0];
    final float bearingTo = distanceBearingResult[1];

    DistanceUnits distanceUnits = userPrefs.getDistanceUnits();
    String distance =
        String.format("      %.1f%s", distanceUnits.getDistance(distanceMeters),
            distanceUnits.distanceAbbreviation);
    distanceText.setText(distance);
    bearingText.setText(String.format(" - %03.0f%s BRG", bearingTo, MapView.DEGREES_SYMBOL));

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

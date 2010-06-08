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

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ZoomButtonsController;

import com.google.flightmap.common.CachedMagneticVariation;
import com.google.flightmap.common.NavigationUtil;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;

import java.util.SortedSet;

/**
 * View for the moving map.
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback {
  private static final String TAG = MapView.class.getSimpleName();

  // Saved instance state constants.
  private static final String ZOOM_LEVEL = "zoom-level";

  /** Position is considered "old" after this many milliseconds. */
  private static final long MAX_LOCATION_AGE = 300000; // 5 minutes.

  // Paints.
  private static final Paint ERROR_TEXT_PAINT = new Paint();
  private static final Paint TOWERED_PAINT = new Paint();
  private static final Paint NON_TOWERED_PAINT = new Paint();
  private static final Paint AIRPORT_TEXT_PAINT = new Paint();
  private static final Paint AIRCRAFT_PAINT = new Paint();
  private static final Paint PANEL_BACKGROUND_PAINT = new Paint();
  private static final Paint PANEL_DIGITS_PAINT = new Paint();
  private static final Paint PANEL_UNITS_PAINT = new Paint();

  // Zoom items.
  private static final int MIN_ZOOM = 4;
  private static final int MAX_ZOOM = 15;
  private static final float ZOOM_STEP = 0.5f;
  private ZoomButtonsController zoomController;
  private float zoom = 10;

  // Top panel items.
  static final float PANEL_HEIGHT = 60;
  private static final float PANEL_TEXT_MARGIN = 10;
  private static final float PANEL_NOTCH_HEIGHT = 15;
  private static final float PANEL_NOTCH_WIDTH = 10;
  private static final float PANEL_TEXT_BASELINE =
      PANEL_HEIGHT - PANEL_NOTCH_HEIGHT - PANEL_TEXT_MARGIN;
  private static final String DEGREES_SYMBOL = "\u00b0";
  private Path topPanel;

  // Main class.
  private final FlightMap flightMap;

  // Coordinates to draw the aircraft on the map.
  private int aircraftX;
  private int aircraftY;

  // Underlying surface for this view.
  private volatile SurfaceHolder holder;

  // Rect used to get text width. Create so we don't new one for each frame.
  private final Rect textBounds = new Rect();

  // Airplane image and location where to draw the left, top so it's centered.
  private final Drawable airplaneImage;

  // Screen density.
  private final float density;

  // Graphical zoom scale.
  private final ZoomScale zoomScale;

  // Caching. Values from the last time the map was drawn.
  private Location previousLocation;
  private float previousZoom;

  // Magnetic variation w/ caching.
  private final CachedMagneticVariation magneticVariation = new CachedMagneticVariation();

  // Static initialization.
  static {
    // Do not put any calls to setTextSize here. Put them in #setTextSizes().
    ERROR_TEXT_PAINT.setAntiAlias(true);
    ERROR_TEXT_PAINT.setColor(Color.WHITE);
    ERROR_TEXT_PAINT.setTextAlign(Align.CENTER);
    TOWERED_PAINT.setAntiAlias(true);
    TOWERED_PAINT.setARGB(0xff, 0x0, 0xcc, 0xff);
    TOWERED_PAINT.setTextAlign(Align.CENTER);
    NON_TOWERED_PAINT.setAntiAlias(true);
    NON_TOWERED_PAINT.setARGB(0xff, 0xcc, 0x33, 0xcc);
    NON_TOWERED_PAINT.setTextAlign(Align.CENTER);
    AIRPORT_TEXT_PAINT.setAntiAlias(true);
    AIRPORT_TEXT_PAINT.setARGB(0xff, 0xff, 0xff, 0xff);
    AIRPORT_TEXT_PAINT.setTypeface(Typeface.SANS_SERIF);
    AIRPORT_TEXT_PAINT.setTextAlign(Align.CENTER);
    AIRCRAFT_PAINT.setColor(Color.GREEN);
    AIRCRAFT_PAINT.setStrokeWidth(3);
    PANEL_BACKGROUND_PAINT.setARGB(0xee, 0x22, 0x22, 0x22); // 0.8 alpha, #333
    PANEL_DIGITS_PAINT.setAntiAlias(true);
    PANEL_DIGITS_PAINT.setColor(Color.WHITE);
    PANEL_DIGITS_PAINT.setTypeface(Typeface.SANS_SERIF);
    PANEL_DIGITS_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    PANEL_UNITS_PAINT.setAntiAlias(true);
    PANEL_UNITS_PAINT.setARGB(0xff, 0x99, 0x99, 0x99);
    PANEL_UNITS_PAINT.setTypeface(Typeface.SANS_SERIF);
  }

  public MapView(FlightMap flightMap) {
    super(flightMap);
    this.flightMap = flightMap;
    this.density = flightMap.getResources().getDisplayMetrics().density;
    this.zoomScale = new ZoomScale(density);
    getHolder().addCallback(this);
    setFocusable(true); // make sure we get key events
    setKeepScreenOn(true);
    createZoomController();
    setTextSizes();

    // Set up airplane image.
    Resources res = flightMap.getResources();
    airplaneImage = res.getDrawable(R.drawable.aircraft);
    int airplaneImageWidth = airplaneImage.getIntrinsicWidth();
    int airplaneImageHeight = airplaneImage.getIntrinsicHeight();
    // Set bounds so the airplane is centered when drawn.
    int left = -airplaneImageWidth / 2;
    int top = -airplaneImageHeight / 2;
    airplaneImage.setBounds(left, top, left + airplaneImageWidth, top + airplaneImageHeight);
  }

  /**
   * Scales text sizes based on screen density. See
   * http://developer.android.com/guide/practices/screens_support.html#dips-pels
   */
  private synchronized void setTextSizes() {
    ERROR_TEXT_PAINT.setTextSize(15 * density);
    AIRPORT_TEXT_PAINT.setTextSize(16 * density);
    PANEL_DIGITS_PAINT.setTextSize(26 * density);
    PANEL_UNITS_PAINT.setTextSize(18 * density);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    showZoomController();
    return true;
  }

  @Override
  public boolean onTrackballEvent(MotionEvent event) {
    showZoomController();
    return true;
  }

  private synchronized void showZoomController() {
    if (null != zoomController) {
      zoomController.setVisible(true);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (null != zoomController) {
      zoomController.setVisible(false);
    }
  }

  /**
   * Surface dimensions changed.
   */
  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    if (!FlightMap.isNorthUp) {
      // Center the aircraft horizontally, and 3/4 of the way down vertically.
      aircraftX = width / 2;
      aircraftY = height - (height / 4);
    } else {
      // Center the aircraft on the screen.
      aircraftX = width / 2;
      aircraftY = height / 2;
    }
    createTopPanelPath(width);
  }

  private void createTopPanelPath(int width) {
    final float center = width / 2.0f;
    topPanel = new Path();
    topPanel.moveTo(0, 0);
    topPanel.lineTo(width, 0);
    topPanel.lineTo(width, PANEL_HEIGHT);
    topPanel.lineTo(center + PANEL_NOTCH_WIDTH, PANEL_HEIGHT);
    topPanel.lineTo(center, PANEL_HEIGHT - PANEL_NOTCH_HEIGHT);
    topPanel.lineTo(center - PANEL_NOTCH_WIDTH, PANEL_HEIGHT);
    topPanel.lineTo(0, PANEL_HEIGHT);
    topPanel.close();
  }

  @Override
  public synchronized void surfaceCreated(SurfaceHolder holder) {
    this.holder = holder;
    previousLocation = null;
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    this.holder = null;
  }

  private void createZoomController() {
    zoomController = new ZoomButtonsController(this);
    zoomController.setOnZoomListener(new ZoomButtonsController.OnZoomListener() {
      @Override
      public void onZoom(boolean zoomIn) {
        final float zoomCopy = getZoom(); // copy for thread safety.
        if (zoomIn) {
          setZoom(zoomCopy + ZOOM_STEP);
        } else {
          setZoom(zoomCopy - ZOOM_STEP);
        }
        zoomController.setZoomInEnabled(getZoom() < MAX_ZOOM);
        zoomController.setZoomOutEnabled(getZoom() > MIN_ZOOM);
      }

      @Override
      public void onVisibilityChanged(boolean visible) {
        // Ignored.
      }
    });
  }

  public void drawMap(Location location) {
    Canvas c = null;
    try {
      if (null == holder) {
        return;
      }
      synchronized (this) {
        // Do nothing if position and zoom are unchanged.
        // TODO: Also check for display preferences change here.
        if (!hasMoved(location) && zoom == previousZoom) {
          return;
        }
        previousLocation = location;
        previousZoom = zoom;
      }
      c = holder.lockCanvas(null);
      synchronized (holder) {
        drawMapOnCanvas(c, location);
      }
    } finally {
      if (c != null) {
        holder.unlockCanvasAndPost(c);
      }
    }
  }

  private synchronized void drawMapOnCanvas(Canvas c, Location location) {
    if (null == c) {
      Log.w(TAG, "null canvas");
      return;
    }

    // Clear the map.
    c.drawColor(Color.BLACK);

    // Display message about no current location and return.
    if (null == location || System.currentTimeMillis() - location.getTime() > MAX_LOCATION_AGE) {
      c.drawText(flightMap.getText(R.string.old_location).toString(), c.getWidth() / 2, //
          c.getHeight() / 2, ERROR_TEXT_PAINT);
      return;
    }

    LatLng locationLatLng = LatLng.fromDouble(location.getLatitude(), location.getLongitude());

    // Convert bearing from true to magnetic.
    location = convertToMagneticBearing(location, locationLatLng);

    final boolean isTrackUp = !FlightMap.isNorthUp; // copy for thread safety.

    // Draw everything relative to the aircraft.
    c.translate(aircraftX, aircraftY);
    if (isTrackUp) {
      // Rotate to make track up (no rotation = north up).
      c.rotate(360 - location.getBearing());
    }

    // Get location pixel coordinates. Then set translation so everything is
    // drawn relative to the current location.
    final float zoomCopy = getZoom(); // copy for thread safety.
    Point locationPoint = MercatorProjection.toPoint(zoomCopy, locationLatLng);
    c.translate(-locationPoint.x, -locationPoint.y);

    // Draw airports.
    double screenRadius = getScreenRadius(zoomCopy, locationLatLng, locationPoint);
    SortedSet<AirportDistance> nearbyAirports =
        flightMap.airportDirectory.getAirportsWithinRadius(locationLatLng, screenRadius,
            getMinimumAirportRank(zoomCopy));
    for (AirportDistance airportDistance : nearbyAirports) {
      final Paint airportPaint = getAirportPaint(airportDistance.airport);
      Point airportPoint = MercatorProjection.toPoint(zoomCopy, airportDistance.airport.location);
      c.drawCircle(airportPoint.x, airportPoint.y, 15, airportPaint);
      // Undo, then redo the track-up rotation so the labels are always at the
      // top for track up.
      if (isTrackUp) {
        c.rotate(location.getBearing(), airportPoint.x, airportPoint.y);
      }
      c.drawText(airportDistance.airport.icao, airportPoint.x, airportPoint.y - 20,
          AIRPORT_TEXT_PAINT);
      if (isTrackUp) {
        c.rotate(360 - location.getBearing(), airportPoint.x, airportPoint.y);
      }
    }


    // Draw airplane.
    c.translate(locationPoint.x, locationPoint.y);
    // Rotate no matter what. If track up, this will make the airplane point to
    // the top of the screen. If north up, this will point the airplane at the
    // current track.
    c.rotate(location.getBearing());
    airplaneImage.draw(c);

    // Undo to-track rotation for north up.
    if (!isTrackUp) {
      c.rotate(360 - location.getBearing());
    }

    // Draw items that are in fixed locations. Set origin to top-left corner.
    c.translate(-aircraftX, -aircraftY);

    zoomScale.drawScale(c, location, zoomCopy);

    // Polygon for top params display.
    if (null != topPanel) {
      c.drawPath(topPanel, PANEL_BACKGROUND_PAINT);
      String knots = "-";
      String track = "-" + DEGREES_SYMBOL;
      String altitude = "-";

      if (location.hasSpeed()) {
        knots = String.format("%.0f", location.getSpeed() * NavigationUtil.METERS_PER_SEC_TO_KNOTS);
      }
      if (location.hasBearing()) {
        track = String.format(" %03.0f%s", location.getBearing(), DEGREES_SYMBOL);
      }
      if (location.hasAltitude()) {
        // Round altitude to nearest 10 foot increment to avoid jitter.
        int altitudeNearestTen =
            (int) (Math.round(location.getAltitude() * NavigationUtil.METERS_PER_FOOT / 10.0) * 10);
        altitude = String.format("%,5d", altitudeNearestTen);
      }

      // Draw speed.
      PANEL_DIGITS_PAINT.setTextAlign(Align.LEFT);
      c.drawText(knots, PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE, PANEL_DIGITS_PAINT);
      int textWidth = getTextWidth(knots, PANEL_DIGITS_PAINT);
      PANEL_UNITS_PAINT.setTextAlign(Align.LEFT);
      c.drawText(" kts", textWidth + PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE, PANEL_UNITS_PAINT);

      // Draw track.
      final int width = c.getWidth();
      final float center = width / 2.0f;
      PANEL_DIGITS_PAINT.setTextAlign(Align.CENTER);
      c.drawText(track, center, PANEL_TEXT_BASELINE, PANEL_DIGITS_PAINT);

      // Draw altitude. Draw the units first, since it's right-aligned.
      PANEL_UNITS_PAINT.setTextAlign(Align.RIGHT);
      c.drawText(" ft", width - PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE, PANEL_UNITS_PAINT);
      textWidth = getTextWidth(" ft", PANEL_UNITS_PAINT);
      PANEL_DIGITS_PAINT.setTextAlign(Align.RIGHT);
      c.drawText(altitude, width - textWidth - PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE,
          PANEL_DIGITS_PAINT);
    }
  }

  /**
   * Returns {@code location} with the bearing converted from true to magnetic.
   * Does not modify {@code location} if location.hasBearing() is false.
   * 
   * @param locationLatLng
   */
  private Location convertToMagneticBearing(Location location, LatLng locationLatLng) {
    if (!location.hasBearing()) {
      return location;
    }

    float magneticBearing = location.getBearing() // relative to true north.
        + magneticVariation.getMagneticVariation(locationLatLng, (float) location.getAltitude());
    location.setBearing(magneticBearing);
    return location;
  }

  /**
   * Returns true if location is different than {@link #previousLocation}
   * (ignoring fields that don't affect the rendering).
   */
  private synchronized boolean hasMoved(Location location) {
    if (null == previousLocation || location.getBearing() != previousLocation.getBearing()
        || location.getAltitude() != previousLocation.getAltitude()
        || location.getSpeed() != previousLocation.getSpeed()) {
      return true;
    }
    return false;
  }

  /**
   * Returns the minimum airport rank for the given zoom level. Ranks are in the
   * range 0-5 (5 being most important).
   */
  private int getMinimumAirportRank(float zoom) {
    if (zoom <= 6) {
      return 5;
    }
    if (zoom < 8) {
      return 4;
    }
    if (zoom < 9) {
      return 3;
    }
    if (zoom < 11) {
      return 1;
    }
    return 0;
  }

  /**
   * Return the appropriate paint based on whether the airport is towered or
   * not.
   */
  private Paint getAirportPaint(Airport airport) {
    return airport.isTowered ? TOWERED_PAINT : NON_TOWERED_PAINT;
  }

  /**
   * Returns width in pixels of {@code text} drawn with {@code paint}.
   */
  private synchronized int getTextWidth(String text, Paint paint) {
    paint.getTextBounds(text, 0, text.length(), textBounds);
    return textBounds.right; // origin is always (0, 0).
  }

  /**
   * On return MIN_ZOOM <= zoom <= MAX_ZOOM.
   */
  public synchronized void setZoom(float zoom) {
    zoom = Math.max(zoom, MIN_ZOOM);
    zoom = Math.min(zoom, MAX_ZOOM);
    this.zoom = zoom;
  }

  /**
   * Returns the distance in meters from the aircraft screen location to the
   * furthest screen edge.
   */
  private synchronized double getScreenRadius(float zoom, LatLng location, Point locationPoint) {
    // Pixel coordinates of the top-left screen corner.
    Point topLeftCorner = new Point(locationPoint.x - aircraftX, locationPoint.y - aircraftY);
    LatLng topLeftLocation = MercatorProjection.fromPoint(zoom, topLeftCorner);
    return NavigationUtil.computeDistance(location, topLeftLocation);
  }

  public synchronized float getZoom() {
    return zoom;
  }

  /**
   * Saves map-specific state info to {@code outState}.
   */
  public void saveInstanceState(Bundle outState) {
    outState.putFloat(ZOOM_LEVEL, getZoom());
  }

  /**
   * Restores map-specific state info from {@code savedInstanceState}
   */
  public void restoreInstanceState(Bundle savedInstanceState) {
    if (null == savedInstanceState || !savedInstanceState.containsKey(ZOOM_LEVEL)) {
      return;
    }
    setZoom(savedInstanceState.getFloat(ZOOM_LEVEL));
  }
}

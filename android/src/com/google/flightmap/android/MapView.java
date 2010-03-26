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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ZoomButtonsController;

import com.google.flightmap.common.NavigationUtil;
import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;

/**
 * View for the moving map.
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback {
  private static final String TAG = MapView.class.getSimpleName();
  // Saved instance state constants
  private static final String ZOOM_LEVEL = "zoom-level";

  // Paints.
  private static final Paint MAGENTA_PAINT = new Paint();
  private static final Paint AIRCRAFT_PAINT = new Paint();
  private static final Paint AIRPORT_LABEL_PAINT = new Paint();
  private static final Paint PANEL_PAINT = new Paint();
  private static final Paint PANEL_TEXT_PAINT = new Paint();

  // Zoom items.
  private static final int MIN_ZOOM = 0;
  private static final int MAX_ZOOM = 30;
  private static final float ZOOM_STEP = 0.5f;
  private ZoomButtonsController zoomController;
  private float zoom = 12;

  // Top panel items.
  private static final float PANEL_HEIGHT = 50;
  private static final float PANEL_TEXT_MARGIN = 10;
  private static final float PANEL_NOTCH_HEIGHT = 10;
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

  // Static initialization.
  static {
    MAGENTA_PAINT.setAntiAlias(true);
    MAGENTA_PAINT.setColor(Color.MAGENTA);
    AIRPORT_LABEL_PAINT.setColor(Color.WHITE);
    AIRPORT_LABEL_PAINT.setTypeface(Typeface.SANS_SERIF);
    AIRPORT_LABEL_PAINT.setTextSize(20);
    AIRPORT_LABEL_PAINT.setAntiAlias(true);
    AIRPORT_LABEL_PAINT.setTextAlign(Align.CENTER);
    AIRCRAFT_PAINT.setColor(Color.WHITE);
    AIRCRAFT_PAINT.setStrokeWidth(3);
    PANEL_PAINT.setARGB(200, 200, 200, 200);
    PANEL_TEXT_PAINT.setColor(Color.WHITE);
    PANEL_TEXT_PAINT.setTypeface(Typeface.SANS_SERIF);
    PANEL_TEXT_PAINT.setTextSize(30);
    PANEL_TEXT_PAINT.setAntiAlias(true);
  }

  public MapView(FlightMap flightMap) {
    super(flightMap);
    this.flightMap = flightMap;
    getHolder().addCallback(this);
    setFocusable(true); // make sure we get key events
    setKeepScreenOn(true);
    createZoomController();
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
    Log.i(TAG, String.format("format=%d w=%d h=%d", format, width, height));
    aircraftX = width / 2;
    aircraftY = height - (height / 4);
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
  public void surfaceCreated(SurfaceHolder holder) {
    this.holder = holder;
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    this.holder = null;
  }

  /**
   * 
   */
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
        Log.w(TAG, "Null holder");
        return;
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

  private void drawMapOnCanvas(Canvas c, Location location) {
    if (null == c) {
      Log.w(TAG, "null canvas");
      return;
    }
    c.drawColor(Color.BLACK);
    if (null == location) {
      c.drawText(flightMap.getText(R.string.old_location).toString(), c.getWidth() / 2, //
          c.getHeight() / 2, AIRPORT_LABEL_PAINT);
      return;
    }

    // Rotate to make the track up, center on where the aircraft is drawn.
    c.translate(aircraftX, aircraftY);
    c.rotate(360 - location.getBearing());

    // Get aircraft pixel coordinates. Then set translation so everything is
    // drawn relative to the aircraft location.
    LatLng locationLatLng = LatLng.fromDouble(location.getLatitude(), location.getLongitude());
    final float zoomCopy = getZoom(); // copy for thread safety.
    Point aircraftPoint = MercatorProjection.toPoint(zoomCopy, locationLatLng);
    c.translate(-aircraftPoint.x, -aircraftPoint.y);

    // Draw airports.
    SortedSet<AirportDistance> nearbyAirports =
        flightMap.airportDirectory.getAirportsWithinRadius(locationLatLng, 20);
    // TODO: 20nm hardcoded, should get airports within bounding box of screen.
    // Need a new aviation db interface that takes a lat/lng bounding box.
    for (AirportDistance airportDistance : nearbyAirports) {
      Point airportPoint = MercatorProjection.toPoint(zoomCopy, airportDistance.airport.location);
      c.drawCircle(airportPoint.x, airportPoint.y, 15, MAGENTA_PAINT);
      // Undo, then redo the track-up rotation so the labels are always at the
      // bottom.
      c.rotate(location.getBearing(), airportPoint.x, airportPoint.y);
      c.drawText(airportDistance.airport.icao, airportPoint.x, airportPoint.y + 40,
          AIRPORT_LABEL_PAINT);
      c.rotate(360 - location.getBearing(), airportPoint.x, airportPoint.y);
    }

    // Draw airplane
    c.translate(aircraftPoint.x, aircraftPoint.y);
    c.rotate(location.getBearing()); // Undo track-up rotation.
    // TODO: Use a png here.
    c.drawLine(0, -10, 0, 15, AIRCRAFT_PAINT);
    c.drawLine(-10, 0, 10, 0, AIRCRAFT_PAINT);
    c.drawLine(-6, 15, 6, 15, AIRCRAFT_PAINT);

    // Draw items that are in fixed locations. Set origin to top-left corner.
    c.translate(-aircraftX, -aircraftY);

    // Polygon for top params display.
    if (null != topPanel) {
      c.drawPath(topPanel, PANEL_PAINT);
      String knots = "--- KTS";
      String track = "---" + DEGREES_SYMBOL;
      String altitude = "--- FT";
      if (location.hasSpeed()) {
        knots =
            String.format("%.0f KTS", location.getSpeed() * NavigationUtil.METERS_PER_SEC_TO_KNOTS);
      }
      if (location.hasBearing()) {
        track = String.format(" %03.0f%s", location.getBearing(), DEGREES_SYMBOL);
      }
      if (location.hasAltitude()) {
        // Round altitude to nearest 10 feet increment to avoid jitter.
        int altitudeNearestTen =
            (int) (Math.round(location.getAltitude() * NavigationUtil.METERS_PER_FOOT / 10.0) * 10);
        altitude = String.format("%,5d FT", altitudeNearestTen);
      }

      PANEL_TEXT_PAINT.setTextAlign(Align.LEFT);
      c.drawText(knots, PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE, PANEL_TEXT_PAINT);
      final int width = c.getWidth();
      final float center = width / 2.0f;
      PANEL_TEXT_PAINT.setTextAlign(Align.CENTER);
      c.drawText(track, center, PANEL_TEXT_BASELINE, PANEL_TEXT_PAINT);
      PANEL_TEXT_PAINT.setTextAlign(Align.RIGHT);
      c.drawText(altitude, width - PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE, PANEL_TEXT_PAINT);
    }
  }

  /**
   * On return MIN_ZOOM <= zoom <= MAX_ZOOM
   */
  public synchronized void setZoom(float zoom) {
    zoom = Math.max(zoom, MIN_ZOOM);
    zoom = Math.min(zoom, MAX_ZOOM);
    this.zoom = zoom;
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

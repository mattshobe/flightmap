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
import android.graphics.Point;
import android.location.Location;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ZoomButtonsController;

import com.google.flightmap.common.data.AirportDistance;
import com.google.flightmap.common.data.LatLng;

/**
 * View for the moving map.
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback {
  private static final String TAG = MapView.class.getSimpleName();
  private static final Paint MAGENTA_PAINT = new Paint();
  private static final Paint WHITE_PAINT = new Paint();

  // Zoom constants.
  private static final int MIN_ZOOM = 0;
  private static final int MAX_ZOOM = 30;
  private static final double ZOOM_STEP = 0.5;

  // Main class.
  private final FlightMap flightMap;

  // Coordinates to draw the aircraft on the map.
  private int aircraftX;
  private int aircraftY;

  // Zoom variables.
  private ZoomButtonsController zoomController;
  private double zoom = 12;

  // Static initialization.
  static {
    MAGENTA_PAINT.setAntiAlias(true);
    MAGENTA_PAINT.setColor(Color.MAGENTA);
    WHITE_PAINT.setColor(Color.WHITE);
    WHITE_PAINT.setStrokeWidth(3);
  }

  public MapView(FlightMap flightMap) {
    super(flightMap);
    this.flightMap = flightMap;
    getHolder().addCallback(this);
    setFocusable(true); // make sure we get key events
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

  /**
   * Surface dimensions changed.
   */
  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width,
      int height) {
    Log.i(TAG, String.format("format=%d w=%d h=%d", format, width, height));
    aircraftX = width / 2;
    aircraftY = height - (height / 4);
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    zoomController = new ZoomButtonsController(this);
    zoomController
        .setOnZoomListener(new ZoomButtonsController.OnZoomListener() {
          @Override
          public void onZoom(boolean zoomIn) {
            final double zoomCopy = getZoom(); // copy for thread safety.
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

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
  }

  public void drawMap(Location location) {
    SurfaceHolder holder = null;
    Canvas c = null;
    try {
      holder = getHolder();
      if (null == holder) {
        Log.w(TAG, "Null holder");
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
      Log.i(TAG, "null canvas");
      return;
    }
    c.drawColor(Color.BLACK);
    if (null == location) {
      c.drawText("No location", 20, 600, WHITE_PAINT);
      return;
    }

    Log.i(TAG, "Map update " + location);

    // Rotate to make the track up, center on where the aircraft is drawn.
    c.translate(aircraftX, aircraftY);
    c.rotate(360 - location.getBearing());

    // Get aircraft pixel coordinates. Then set translation so everything is
    // drawn relative to the aircraft location.
    LatLng locationLatLng = LatLng.fromDouble(location.getLatitude(), location
        .getLongitude());
    final double zoomCopy = getZoom(); // copy for thread safety.
    Point aircraftPoint = MercatorProjection.toPoint(zoomCopy, locationLatLng);
    c.translate(-aircraftPoint.x, -aircraftPoint.y);

    // Draw airport
    SortedSet<AirportDistance> nearbyAirports = flightMap.airportDirectory
        .getAirportsWithinRadius(locationLatLng, 20);
    for (AirportDistance airportDistance : nearbyAirports) {
      Point airportPoint = MercatorProjection.toPoint(zoomCopy,
          airportDistance.airport.location);
      c.drawCircle(airportPoint.x, airportPoint.y, 5, MAGENTA_PAINT);
      c.drawText(airportDistance.airport.icao, airportPoint.x,
          airportPoint.y + 10, WHITE_PAINT);
    }

    // Draw airplane
    c.translate(aircraftPoint.x, aircraftPoint.y);
    c.rotate(location.getBearing()); // Undo track-up rotation.
    c.drawLine(0, -5, 0, 10, WHITE_PAINT);
    c.drawLine(-7, 0, 7, 0, WHITE_PAINT);

  }

  /**
   * On return MIN_ZOOM <= zoom <= MAX_ZOOM
   */
  public synchronized void setZoom(double zoom) {
    zoom = Math.max(zoom, MIN_ZOOM);
    zoom = Math.min(zoom, MAX_ZOOM);
    this.zoom = zoom;
  }

  public synchronized double getZoom() {
    return zoom;
  }
}

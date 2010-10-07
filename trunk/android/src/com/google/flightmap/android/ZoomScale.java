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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.location.Location;
import android.util.Log;

import com.google.flightmap.common.NavigationUtil.DistanceUnits;
import com.google.flightmap.common.data.LatLng;

/**
 * Draws the graphical zoom scale. The size of the scale bar changes slightly to
 * make it match the scale shown. The distances shown will be integers.
 */
public class ZoomScale {
  private static final String TAG = ZoomScale.class.getSimpleName();

  /** Ideally how wide the scale bar should be in absolute pixels. */
  private static final int PREFERRED_WIDTH = 60;
  /** Height of the lines on the ends */
  private static final int HEIGHT = 20;

  /** Number of pixels up from the bottom of the screen to draw the scale. */
  private static final int BOTTOM_OFFSET = 20 + HEIGHT;

  /** Number of pixels left from the right of the screen to draw the right edge. */
  private static final int RIGHT_OFFSET = 15;
  /** Meters to travel between updating the zoom scale. */
  private static final int ZOOM_UPDATE_DISTANCE = 20000;

  // Paints.
  private static final Paint LINE_PAINT = new Paint();
  private static final Paint TEXT_PAINT = new Paint();

  // Screen density.
  private final float density;

  // PREFERRED_WIDTH, adjusted to make an integer zoom scale.
  private float actualWidth = Float.NaN;

  private final UserPrefs userPrefs;

  // Used to cache scale text.
  private Location previousLocation;
  private double previousZoom;
  private String previousScaleText;
  private DistanceUnits previousDistanceUnits;

  static {
    LINE_PAINT.setAntiAlias(true);
    LINE_PAINT.setColor(Color.rgb(0xee, 0xee, 0xee));
    LINE_PAINT.setStyle(Style.STROKE);
    TEXT_PAINT.setAntiAlias(true);
    TEXT_PAINT.setColor(LINE_PAINT.getColor());
    TEXT_PAINT.setTextAlign(Align.CENTER);
  }

  /**
   * Creates a ZoomScale.
   * 
   * @param density screen density.
   */
  public ZoomScale(float density, UserPrefs userPrefs) {
    this.density = density;
    this.userPrefs = userPrefs;
    TEXT_PAINT.setTextSize(15.5f * density);
    LINE_PAINT.setStrokeWidth(1.5f * density);
  }

  /**
   * Draws zoom scale.
   * 
   * @param c canvas to draw on.
   * @param l current location.
   * @param zoom zoom level.
   */
  public synchronized void drawScale(Canvas c, Location l, double zoom) {
    final String scaleText = getScaleText(l, zoom); // also sets actualWidth.
    final int canvasWidth = c.getWidth();
    final int canvasHeight = c.getHeight();
    final float rightX = canvasWidth - RIGHT_OFFSET * density;
    final float leftX = rightX - actualWidth * density;
    final float topY = canvasHeight - BOTTOM_OFFSET;
    final float bottomY = topY + HEIGHT;
    final float centerX = (leftX + rightX) / 2.0f;
    final float centerY = (topY + bottomY) / 2.0f;

    c.drawLine(leftX, topY, leftX, bottomY, LINE_PAINT);
    c.drawLine(leftX, centerY, rightX, centerY, LINE_PAINT);
    c.drawLine(rightX, topY, rightX, bottomY, LINE_PAINT);
    c.drawText(scaleText, centerX, centerY - 5, TEXT_PAINT);
  }

  /**
   * Returns the text to display above the zoom scale bar. Also updates {@code
   * actualWidth}.
   * 
   * @param location current location.
   * @param zoom zoom level.
   */
  private synchronized String getScaleText(Location location, double zoom) {
    if (null != previousLocation && null != previousScaleText && null != previousDistanceUnits
        && zoom == previousZoom) {
      // Return cached result if the location hasn't changed much and units are
      // unchanged.
      if (previousDistanceUnits == userPrefs.getDistanceUnits()
          && previousLocation.distanceTo(location) < ZOOM_UPDATE_DISTANCE) {
        return previousScaleText;
      }
    }
    float mpp =
        (float) AndroidMercatorProjection.getMetersPerPixel(zoom, LatLng.fromDouble(location
            .getLatitude(), location.getLongitude()));
    float scaleInMeters = mpp * PREFERRED_WIDTH * density;

    // Use user prefs to determine units to show.
    DistanceUnits distanceUnits = userPrefs.getDistanceUnits();
    DistanceUnits finalDistanceUnits = distanceUnits;

    // Round to nearest whole unit.
    // If scaleInMeters is less than 1 convert to short units.
    int scaleInUnits = (int) distanceUnits.getDistance(scaleInMeters);
    scaleInUnits = (int) (scaleInUnits + 0.5);
    if (scaleInUnits < 1) {
      finalDistanceUnits = distanceUnits.getShortDistance();
      scaleInUnits = (int) finalDistanceUnits.getDistance(scaleInMeters);
    }
    actualWidth = (float) (scaleInUnits / (finalDistanceUnits.distanceMultiplier * mpp * density));

    String units = finalDistanceUnits.distanceAbbreviation;
    String result = null;
    result = String.format("%d %s", scaleInUnits, units);
    previousLocation = location;
    previousZoom = zoom;
    previousScaleText = result;
    previousDistanceUnits = distanceUnits;
    Log.i(TAG, String.format("Zoom: %.1f New scale: %s", zoom, result));
    return result;
  }
}

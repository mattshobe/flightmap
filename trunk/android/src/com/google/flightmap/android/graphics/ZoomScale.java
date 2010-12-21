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

import com.google.flightmap.android.geo.AndroidMercatorProjection;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.geo.NavigationUtil.DistanceUnits;

/**
 * Draws the graphical zoom scale. The size of the scale bar changes slightly to
 * make it match the scale shown. The distances shown will be integers.
 */
public class ZoomScale {
  private static final String TAG = ZoomScale.class.getSimpleName();

  /** Ideally how wide the scale bar should be in absolute pixels. */
  private static final int PREFERRED_WIDTH = 70;

  /** Maximum width of the scale bar, in absolute pixels. */
  private static final int MAX_WIDTH = 90;

  /** Minimum width of the scale bar, in absolute pixels. */
  private static final int MIN_WIDTH = 65;

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
  private synchronized String getScaleText(final Location location, final double zoom) {
    if (scaleTextIsCached(location, zoom)) {
      return previousScaleText;
    }

    // meters per pixel at given location and zoom level.
    final float mpp =
        (float) AndroidMercatorProjection.getMetersPerPixel(zoom, LatLng.fromDouble(location
            .getLatitude(), location.getLongitude()));
    final DistanceUnits distanceUnits = userPrefs.getDistanceUnits();
    final float preferredScaleInMeters = mpp * PREFERRED_WIDTH * density;
    final float preferredScaleInUnits = (float) (distanceUnits.getDistance(preferredScaleInMeters));

    // Determine whether smaller units are needed (ft instead of nm/mi, ...)
    String units;
    int scaleInUnits;
    float oneMeterPixelWidth;
    if (preferredScaleInUnits < 1) {
      // Use short units.
      units = distanceUnits.shortDistanceAbbreviation;
      scaleInUnits = (int) (distanceUnits.getShortDistance(preferredScaleInMeters) + 0.5);
      oneMeterPixelWidth = (float) (distanceUnits.getShortDistance(1) * mpp * density);
    } else {
      units = distanceUnits.distanceAbbreviation;
      scaleInUnits = (int) (preferredScaleInUnits + 0.5);
      oneMeterPixelWidth = (float) (distanceUnits.getDistance(1) * mpp * density);
    }

    // Try to obtain a rounded distance with as many trailing zeroes as possible
    int trailingZeroes = (int) Math.log10(scaleInUnits);
    while (trailingZeroes > 0) {
      int leadingDigits = (int) (scaleInUnits / Math.pow(10, trailingZeroes));
      int roundedDistance;
      float width;
      do {
        roundedDistance = (int) (leadingDigits * Math.pow(10, trailingZeroes));
        width = (float) (roundedDistance / oneMeterPixelWidth);
        ++leadingDigits;
      } while (width < MIN_WIDTH);

      if (width <= MAX_WIDTH) {
        scaleInUnits = roundedDistance;
        break;
      }
      --trailingZeroes;
    }

    // Prepare final scale text
    actualWidth = (float) (scaleInUnits / oneMeterPixelWidth);
    final String scaleText = String.format("%d %s", scaleInUnits, units);

    // Update cached values
    previousLocation = location;
    previousZoom = zoom;
    previousScaleText = scaleText;
    previousDistanceUnits = distanceUnits;
    return scaleText;
  }

  /**
   * Checks if cached scale text is still valid. Cached result is valid if the
   * location has changed less than {@code ZOOM_UPDATE_DISTANCE} and units are
   * unchanged.
   */
  private synchronized boolean scaleTextIsCached(final Location location, final double zoom) {
    return (null != previousLocation && null != previousScaleText && null != previousDistanceUnits
        && zoom == previousZoom && previousDistanceUnits == userPrefs.getDistanceUnits() && previousLocation
        .distanceTo(location) < ZOOM_UPDATE_DISTANCE);
  }
}

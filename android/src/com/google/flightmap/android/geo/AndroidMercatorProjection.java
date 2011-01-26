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

package com.google.flightmap.android.geo;

import android.graphics.Point;
import android.graphics.RectF;

import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;
import com.google.flightmap.common.geo.MercatorProjection;

/**
 * Adapter for {@link MercatorProjection}.
 */
public class AndroidMercatorProjection {
  /** Reuse point storage for better performance. */
  private static int[] pointArray = new int[4];

  private AndroidMercatorProjection() {
    // Utility class.
  }

  /**
   * Returns point (in Mercator pixel space) corresponding to {@code location}.
   * 
   * @param zoom zoom level.
   * @param location location to convert to Mercator pixel.
   * @see {@link MercatorProjection#toPoint}.
   */
  public static synchronized Point toPoint(double zoom, LatLng location) {
    MercatorProjection.toPoint(zoom, location, pointArray);
    return new Point(pointArray[0], pointArray[1]);
  }

  /**
   * Returns a LatLng corresponding to {@code point} (in Mercator pixel space).
   * 
   * @param zoom zoom level.
   * @param point point to convert to a LatLng.
   * @see {@link MercatorProjection#fromPoint}
   */
  public static synchronized LatLng fromPoint(double zoom, Point point) {
    pointArray[0] = point.x;
    pointArray[1] = point.y;
    return MercatorProjection.fromPoint(zoom, pointArray);
  }

  /**
   * Returns rectangle (in Mercator pixel space) corresponding to {@code rect}.
   * 
   * @param zoom zoom level.
   * @param rect area to convert to Mercator rectangle.
   * @see {@link MercatorProjection#toRect}.
   */
  public static synchronized RectF toRectF(double zoom, LatLngRect rect) {
    MercatorProjection.toRect(zoom, rect, pointArray);
    final float west = pointArray[0];
    final float south = pointArray[1];
    final float east = pointArray[2];
    final float north  = pointArray[3];
    return new RectF(west, north, east, south);
  }

  /**
   * Returns the number of meters per pixel at {@code location}.
   * <p>
   * <b>Important:</b> the screen density is not accounted for by this method,
   * so you may need to multiply the result by the screen density.
   */
  public static double getMetersPerPixel(double zoom, LatLng location) {
    return MercatorProjection.getMetersPerPixel(zoom, location);
  }
}

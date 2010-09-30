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

import android.graphics.Point;

import com.google.flightmap.common.MercatorProjection;
import com.google.flightmap.common.data.LatLng;

/**
 * Adapter for {@link MercatorProjection}.
 */
public class AndroidMercatorProjection {
  /** Reuse point storage for better performance. */
  private static int[] pointArray = new int[2];

  private AndroidMercatorProjection() {
    // Utility class.
  }

  /**
   * Returns point (in Mercator pixel space) corresponding to {@code location}.
   * 
   * @param zoom zoom level.
   * @param location location to conver to Mercator pixel.
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
   * Returns the number of meters per pixel at {@code location}.
   * <p>
   * <b>Important:</b> the screen density is not accounted for by this method,
   * so you may need to multiply the result by the screen density.
   */
  public static double getMetersPerPixel(double zoom, LatLng location) {
    return MercatorProjection.getMetersPerPixel(zoom, location);
  }
}

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

import com.google.flightmap.common.data.LatLng;

import android.graphics.Point;

/**
 * Converts between latitude,longitude and x,y locations. See
 * http://en.wikipedia.org/wiki/Mercator_projection for the math.
 */
public class MercatorProjection {
  private static final int ZOOM_0_EQUATOR_PIXELS = 512;

  private MercatorProjection() {
    // Utility class.
  }

  /**
   * Returns point corresponding to {@code location}
   * 
   * @param zoom At zoom 0, the equator is 256 pixels wide. Each integer
   *        increment zooms in by 2x, so zoom 1 has a 512-pixel wide equator and
   *        zoom 2 has 1024 pixels at the equator. Fractional zooms are
   *        supported.
   * @param location location to convert to a point.
   */
  public static Point toPoint(double zoom, LatLng location) {
    double lng = location.lngDeg();
    double equatorPixels = ZOOM_0_EQUATOR_PIXELS * Math.pow(2, zoom);
    double centerPixel = equatorPixels / 2;
    double x = centerPixel + (equatorPixels * (lng / 360));
    double sinLat = Math.sin(location.latRad());
    double y = centerPixel - Math.log((1 + sinLat) / (1 - sinLat)) / 4 / Math.PI * equatorPixels;
    return new Point((int) Math.round(x), (int) Math.round(y));
  }

  /**
   * Returns a LatLng corresponding to point.
   * 
   * @param zoom zoom level. See {@link #toPoint} for details.
   * @param point point to convert.
   */
  public static LatLng fromPoint(double zoom, Point point) {
    double equatorPixels = ZOOM_0_EQUATOR_PIXELS * Math.pow(2, zoom);
    double centerPixel = equatorPixels / 2;
    double lng = 360 * (point.x - centerPixel) / equatorPixels;
    double lat =
        2 * Math.atan(Math.exp(2 * Math.PI * (centerPixel - point.y) / equatorPixels))
            - (Math.PI / 2);
    return LatLng.fromDouble(lat, lng);
  }
}

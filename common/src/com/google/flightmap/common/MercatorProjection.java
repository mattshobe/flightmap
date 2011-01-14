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
package com.google.flightmap.common;

import com.google.flightmap.common.NavigationUtil;
import com.google.flightmap.common.data.LatLng;

/**
 * Converts between latitude,longitude and pixels in Mercator space. Mercator
 * pixel space is a square with the size determined by the zoom level. Zoom 0 is
 * the more coarse, and each integer increment of the zoom level doubles the
 * length of a side of the square. Fractional zoom levels are supported.
 * <p>
 * The center of the square is always at latitude 0, longitude 0.
 * <p>
 * See http://en.wikipedia.org/wiki/Mercator_projection for the math.
 */
public class MercatorProjection {
  /** Length of a side of the Mercator pixel space at zoom 0. */
  private static final int ZOOM_0_PIXELS = 512;

  private MercatorProjection() {
    // Utility class.
  }

  /**
   * Converts {@code location} to a point in Mercator pixel space.
   * 
   * @param zoom zoom level.
   * @param location location to convert to a point.
   * @param point an array with at least 2 elements. On return point[0] = x,
   *        point[1] = y.
   */
  public static void toPoint(double zoom, LatLng location, int[] point) {
    double lng = location.lngDeg();
    double equatorPixels = ZOOM_0_PIXELS * Math.pow(2, zoom);
    double centerPixel = equatorPixels / 2;
    double x = centerPixel + (equatorPixels * (lng / 360));
    double sinLat = Math.sin(location.latRad());
    double y = centerPixel - Math.log((1 + sinLat) / (1 - sinLat)) / 4 / Math.PI * equatorPixels;
    point[0] = (int) (x + 0.5);
    point[1] = (int) (y + 0.5);
  }

  /**
   * Returns a LatLng corresponding to {@code point} (in Mercator pixel space).
   * 
   * @param zoom zoom level. See {@link #toPoint} for details.
   * @param point an array with point[0] = x, point[1] = y.
   */
  public static LatLng fromPoint(double zoom, int[] point) {
    double equatorPixels = ZOOM_0_PIXELS * Math.pow(2, zoom);
    double centerPixel = equatorPixels / 2;
    double lng = 360 * (point[0] - centerPixel) / equatorPixels;
    double lat = Math.toDegrees(2 * Math.atan( //
        Math.exp(2 * Math.PI * (centerPixel - point[1]) / equatorPixels)) - (Math.PI / 2));
    return LatLng.fromDouble(lat, lng);
  }

  /**
   * Returns the number of meters per pixel at {@code location}.
   * <p>
   * <b>Important:</b> the screen density is not accounted for by this method,
   * so you may need to multiply the result by the screen density.
   */
  public static double getMetersPerPixel(double zoom, LatLng location) {
    // Get the pixel coordinates of location, then make a pixel that's offset to
    // the east.
    int[] pixel = new int[2];
    toPoint(zoom, location, pixel);
    final int pixelOffset = 100;
    pixel[0] += pixelOffset;

    // Get the LatLng for eastPixel, then measure the distance.
    LatLng eastLocation = fromPoint(zoom, pixel);
    double distance = NavigationUtil.computeDistance(location, eastLocation);
    return distance / pixelOffset;
  }
}

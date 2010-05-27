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

import com.google.flightmap.common.data.LatLng;

/**
 * Utility class for navigation calculations such as distance, bearing, etc.
 */
public class NavigationUtil {
  public static final double METERS_PER_NM = 1852;
  public static final double METERS_PER_FOOT = 3.2808399;
  public static final double METERS_PER_SEC_TO_KNOTS = 1.94384449;
  public static final double EARTH_RADIUS = 6371009;

  /**
   * Returns the distance in meters between point1 and point2.
   * Calculation is done by the Haversine Formula.
   */
  public static double computeDistance(LatLng point1, LatLng point2) {
    final double lat1 = point1.latRad();
    final double lng1 = point1.lngRad();
    final double lat2 = point2.latRad();
    final double lng2 = point2.lngRad();

    return computeDistance(lat1, lng1, lat2, lng2);
  }

  public static double computeDistance(int lat1E6, int lng1E6, int lat2E6, int lng2E6) {
    final double lat1 = Math.toRadians(lat1E6 * 1e-6);
    final double lng1 = Math.toRadians(lng1E6 * 1e-6);
    final double lat2 = Math.toRadians(lat2E6 * 1e-6);
    final double lng2 = Math.toRadians(lng2E6 * 1e-6);

    return computeDistance(lat1, lng1, lat2, lng2);
  }

  public static double computeDistance(LatLng point1, int lat2E6, int lng2E6) {
    return computeDistance(point1.lat, point1.lng, lat2E6, lng2E6);
  }

  public static double computeDistance(double lat1, double lng1, double lat2, double lng2) {
    final double dLat = lat2 - lat1;
    final double dLng = lng2 - lng1;
    final double a =
        Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2)
            * Math.pow(Math.sin(dLng / 2), 2);
    return EARTH_RADIUS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }
}

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

package com.google.blackbox;

import com.google.blackbox.data.LatLng;

/**
 * Utility class for navigation calculations such as distance, bearing, etc.
 * 
 * @author Phil Verghese
 */
public class NavigationUtil {
  private static final double EARTH_RADIUS = 6371.009 / 1.852; // KM to NM

  /**
   * Returns the distance in nautical miles between point1 and point2.
   * Calculation is done by the Haversine Formula.
   */
  public static double computeDistance(LatLng point1, LatLng point2) {
    final double lat1 = point1.latRad();
    final double lng1 = point1.lngRad();
    final double lat2 = point2.latRad();
    final double lng2 = point2.lngRad();

    final double dLat = lat2 - lat1;
    final double dLng = lng2 - lng1;
    final double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1)
        * Math.cos(lat2) * Math.pow(Math.sin(dLng / 2), 2);
    return EARTH_RADIUS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }
}

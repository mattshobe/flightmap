/* 
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.flightmap.parsing.faa.amr;

import com.google.flightmap.common.data.LatLng;

/**
 * Utility class provides methods to parse latitude and longitude data.
 */
class LatLngParsingUtils {
  /**
   *  Utility class: default and only constructor is private.
   */
  private LatLngParsingUtils() { }

   /**
   * Parses latitude and longitude from fractional seconds.
   *
   * @param latitudeS   Latitude in fractional seconds ("136545.1250N")
   * @param longitudeS  Longitude in fractional seconds ("326633.3600W")
  
   * @return            Position corresponding to given strings
   */ 
  public static LatLng parseLatLng(final String latitudeS, final String longitudeS) {
    final double latitude = parseLatitudeS(latitudeS);
    final double longitude = parseLongitudeS(longitudeS);
    final LatLng position = LatLng.fromDouble(latitude, longitude);
    return position;
  }

  /**
   * Parses latitude seconds.
   *
   * @param latitudeS  Latitude in fractional seconds ("136545.1250N")
   * @return           Latitude in degrees (North positive, South negative)
   */
  private static double parseLatitudeS(final String latitudeS) {
    double latitudeSeconds = Double.parseDouble(latitudeS.substring(0, latitudeS.length() - 1));
    final char northSouthIndicatorChar = latitudeS.charAt(latitudeS.length() - 1);
    if (northSouthIndicatorChar == 'S') {
      latitudeSeconds *= -1;
    } else if (northSouthIndicatorChar != 'N') {
      throw new RuntimeException("Unknown north/south indicator: " + northSouthIndicatorChar);
    }
    return latitudeSeconds/3600.0;
  }

  /**
   * Parses longitude seconds.
   *
   * @param longitudeS  Longitude in fractional seconds ("326633.3600W")
   * @return            Longitude in degrees (East positive, West negative)
   */
  private static double parseLongitudeS(final String longitudeS) {
    double longitudeSeconds = Double.parseDouble(longitudeS.substring(0, longitudeS.length() - 1));
    final char westEastIndicatorChar = longitudeS.charAt(longitudeS.length() - 1);
    if (westEastIndicatorChar == 'W') {
      longitudeSeconds *= -1;
    } else if (westEastIndicatorChar != 'E') {
      throw new RuntimeException("Unknown west/east indicator: " + westEastIndicatorChar);
    }
    return longitudeSeconds/3600.0;
  }
}

/* 
 * Copyright (C) 2011 Google Inc.
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

package com.google.flightmap.parsing.faa.nfd;

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
   * Parses latitude and longitude.
   *
   * @param latitudeS   Latitude as specified in ARINC SPEC 424, section 5.36 ("NDDMMSSss")
   * @param longitudeS  Longitude as specified in ARINC SPEC 424, section 5.36 ("WDDDMMSSss")
  
   * @return            Position corresponding to given strings
   */ 
  public static LatLng parseLatLng(final String latitudeS, final String longitudeS) {
    final double latitude = parseLatitude(latitudeS);
    final double longitude = parseLongitude(longitudeS);
    final LatLng position = LatLng.fromDouble(latitude, longitude);
    return position;
  }

  /**
   * Parses latitude.
   *
   * @param latitudeS  Latitude as specified in ARINC SPEC 424, section 5.36 ("NDDMMSSss")
   * @return           Latitude in degrees (North positive, South negative)
   */
  private static double parseLatitude(final String latitudeS) {
    final char northSouthIndicatorChar = latitudeS.charAt(0);
    final int latDeg = Integer.parseInt(latitudeS.substring(1, 3));
    final int latMin = Integer.parseInt(latitudeS.substring(3, 5));
    final int latSecHundredths = Integer.parseInt(latitudeS.substring(5,9));
    double lat = latDeg;
    lat += latMin / 60.0;
    lat += latSecHundredths / 360000.0;
    if (northSouthIndicatorChar == 'S') {
      lat *= -1;
    } else if (northSouthIndicatorChar != 'N') {
      throw new RuntimeException("Unknown north/south indicator: " + northSouthIndicatorChar);
    }
    return lat;
  }

  /**
   * Parses longitude.
   *
   * @param longitudeS  Longitude as specified in ARINC SPEC 424, section 5.36 ("WDDDMMSSss")
   * @return            Longitude in degrees (East positive, West negative)
   */
  private static double parseLongitude(final String longitudeS) {
    final char westEastIndicatorChar = longitudeS.charAt(0);
    final int lngDeg = Integer.parseInt(longitudeS.substring(1, 4));
    final int lngMin = Integer.parseInt(longitudeS.substring(4, 6));
    final int lngSecHundredths = Integer.parseInt(longitudeS.substring(6,10));
    double lng = lngDeg;
    lng += lngMin / 60.0;
    lng += lngSecHundredths / 360000.0;
    if (westEastIndicatorChar == 'W') {
      lng *= -1;
    } else if (westEastIndicatorChar != 'E') {
      throw new RuntimeException("Unknown west/east indicator: " + westEastIndicatorChar);
    }
    return lng;
  }
}

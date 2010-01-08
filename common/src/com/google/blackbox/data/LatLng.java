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

package com.google.blackbox.data;

/**
 * Latitude and Longitude data structure.
 *<p>
 * Internal representation is in E6 format (degrees times 1E6, rounded to the 
 * closest integer).
 * </p>
 * <p>
 * <b>Beware of integer under/overflows</b> when working with those values 
 * (using a {@code long} buffer might be necessary)
 * </p>
 *
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class LatLng {
  /** 
   * Latitude,  in E6 format 
   */
  public final int lat;

  /**
   * Longitude, in E6 format
   */
  public final int lng;

  public LatLng(int lat, int lng) {
    this.lat = lat;
    this.lng = lng;
  }

  /**
   * @return Latitude in Radians
   * @see java.lang.Math#toRadians(double)
   */
  public double latRad() {
    return Math.toRadians(this.lat*1e-6);
  }

  /**
   * @return Latitude in Radians
   * @see java.lang.Math#toRadians(double)
   */
  public double lngRad() {
    return Math.toRadians(this.lng*1e-6);
  }
}

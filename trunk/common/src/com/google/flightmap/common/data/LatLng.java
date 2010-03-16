/*
 * Copyright (C) 2009 Google Inc.
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
package com.google.flightmap.common.data;

/**
 * Latitude and Longitude data structure.
 *<p>
 * Internal representation is in E6 format (degrees times 1E6, rounded to the
 * closest integer).
 * <p>
 * <b>Beware of integer under/overflows</b> when working with those values
 * (using a {@code long} buffer might be necessary)
 * 
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class LatLng {
  /**
   * Latitude, in E6 format
   */
  public final int lat;

  /**
   * Longitude, in E6 format
   */
  public final int lng;

  /**
   * Create LatLng from E6 coordinates.
   */
  public LatLng(int lat, int lng) {
    this.lat = lat;
    this.lng = lng;
  }

  /**
   * Factory method to avoid confusion with primary constructor that takes E6
   * format.
   */
  public static LatLng fromDouble(double lat, double lng) {
    return new LatLng((int) Math.round(lat * 1E6), (int) Math.round(lng * 1E6));
  }

  /**
   * @return Latitude in Radians
   * @see java.lang.Math#toRadians(double)
   */
  public double latRad() {
    return Math.toRadians(this.lat * 1e-6);
  }

  /**
   * @return Latitude in Radians
   * @see java.lang.Math#toRadians(double)
   */
  public double lngRad() {
    return Math.toRadians(this.lng * 1e-6);
  }
}

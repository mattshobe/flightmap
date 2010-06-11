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
 * <p>
 * Internal representation is in E6 format (degrees times 1E6, rounded to the
 * closest integer).
 * <p>
 * <b>Beware of integer under/overflows</b> when working with those values
 * (using a {@code long} buffer might be necessary)
 */
public class LatLng implements Comparable<LatLng> {
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
  public LatLng(final int lat, final int lng) {
    this.lat = lat;
    this.lng = lng;
  }

  /**
   * Factory method to avoid confusion with primary constructor that takes E6
   * format.
   */
  public static LatLng fromDouble(final double lat, final double lng) {
    return new LatLng((int) Math.round(lat * 1E6), (int) Math.round(lng * 1E6));
  }

  /**
   * @return Latitude in Radians
   * @see java.lang.Math#toRadians(double)
   */
  public double latRad() {
    return Math.toRadians(this.lat * 1E-6);
  }

  /**
   * @return Longitude in Radians
   * @see java.lang.Math#toRadians(double)
   */
  public double lngRad() {
    return Math.toRadians(this.lng * 1E-6);
  }

  /**
   * @return Latitude in degrees.
   */
  public double latDeg() {
    return lat * 1E-6;
  }

  /**
   * @return Longitude in degrees.
   */
  public double lngDeg() {
    return lng * 1E-6;
  }

  /*
   * LatLng are ordered by increasing latitude, then longitude
   */
  @Override
  public int compareTo(final LatLng o) {
    final int latDiff = lat - o.lat;
    if (latDiff != 0) {
      return latDiff;
    }

    return lng - o.lng;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) return true;
    if ( !(obj instanceof LatLng)) return false;

    final LatLng other = (LatLng)obj;
    return this.compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    return (lat * 127)  ^ lng;
  }
  
  @Override
  public String toString() {
    return String.format("(%.5f, %.5f)", latDeg(), lngDeg());
  }
}

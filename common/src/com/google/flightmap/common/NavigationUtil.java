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
  /**
   * Multiply meters by this constant to obtain nautical miles.
   */
  public static final double METERS_TO_NM = 0.000539956803;

  /**
   * Multiply meters by this constant to obtain statute miles.
   */
  public static final double METERS_TO_MILE = 0.000621371192;

  /**
   * Multiply meters by this constant to obtain kilometers.
   */
  public static final double METERS_TO_KM = .001;

  /**
   * Multiply meters by this constant to obtain feet.
   */
  public static final double METERS_TO_FEET = 3.2808399;

  /**
   * Multiply meters-per-second by this constant to obtain knots.
   */
  public static final double METERS_PER_SEC_TO_KNOTS = 1.94384449;

  /**
   * Multiply meters-per-second by this constant to obtain (statute) miles per hour.
   */
  public static final double METERS_PER_SEC_TO_MPH = 2.23693629;

  /**
   * Multiply meters-per-second by this constant to obtain kilometers per hour.
   */
  public static final double METERS_PER_SEC_TO_KPH = 3.6;

  /** Earth radius in meters. */
  public static final double EARTH_RADIUS = 6371009;

  /**
   * Session timestamp for caching purposes.
   */
  private static final long SESSION_TIME_MILLIS = System.currentTimeMillis();

  /**
   * Utility class: default and only constructor is private.
   */
  private NavigationUtil() {
  }

  public enum DistanceUnits {
    MILES("mi", METERS_TO_MILE, "mph", METERS_PER_SEC_TO_MPH), NAUTICAL_MILES("nm", METERS_TO_NM,
        "kts", METERS_PER_SEC_TO_KNOTS), KILOMETERS("km", METERS_TO_KM, "kph",
        METERS_PER_SEC_TO_KPH);
    public final String distanceAbbreviation;
    public final String speedAbbreviation;
    public final double distanceMultiplier;
    public final double speedMultiplier;

    /**
     * @param distanceAbbreviation abbreviation for distance units.
     * @param distanceMultiplier multiply meters by this to convert.
     * @param speedAbbreviation abbreviation for speed units.
     * @param speedMultiplier multiply meters-per-second by this to convert.
     */
    private DistanceUnits(String distanceAbbreviation, double distanceMultiplier,
        String speedAbbreviation, double speedMultiplier) {
      this.distanceAbbreviation = distanceAbbreviation;
      this.distanceMultiplier = distanceMultiplier;
      this.speedAbbreviation = speedAbbreviation;
      this.speedMultiplier = speedMultiplier;
    }

    /**
     * Returns {@code metersPerSecond} converted to this unit's speed.
     */
    public double getSpeed(double metersPerSecond) {
      return metersPerSecond * speedMultiplier;
    }
  }

  /**
   * Normalizes {@code bearing} to be in the range [0-360). Some Android SDK
   * methods return negative bearings for what's normally 180-359 degrees.
   *
   * @param bearing bearing in degrees.
   */
  public static double normalizeBearing(double bearing) {
    while (bearing > 360) {
      bearing -= 360;
    }
    while (bearing < 0) {
      bearing += 360;
    }
    return bearing;
  }

  /**
   * Returns the distance in meters between {@code point1} and {@code point2}.
   *
   * @see NavigationUtil#computeDistance(double, double, double, double) 
   */
  public static double computeDistance(LatLng point1, LatLng point2) {
    final double lat1 = point1.latRad();
    final double lng1 = point1.lngRad();
    final double lat2 = point2.latRad();
    final double lng2 = point2.lngRad();

    return computeDistance(lat1, lng1, lat2, lng2);
  }

  /**
   * Returns the distance in meters between ({@code lat1E6}, {@code lng1E6})  and
   * ({@code lat2E6}, {@code lng2E6}).
   *
   * @see NavigationUtil#computeDistance(double, double, double, double) 
   */
  public static double computeDistance(int lat1E6, int lng1E6, int lat2E6, int lng2E6) {
    final double lat1 = Math.toRadians(lat1E6 * 1e-6);
    final double lng1 = Math.toRadians(lng1E6 * 1e-6);
    final double lat2 = Math.toRadians(lat2E6 * 1e-6);
    final double lng2 = Math.toRadians(lng2E6 * 1e-6);

    return computeDistance(lat1, lng1, lat2, lng2);
  }

  /**
   * Returns the distance in meters between {@code point1}  and ({@code lat2E6}, {@code lng2E6}).
   *
   * @see NavigationUtil#computeDistance(double, double, double, double) 
   */
  public static double computeDistance(LatLng point1, int lat2E6, int lng2E6) {
    return computeDistance(point1.lat, point1.lng, lat2E6, lng2E6);
  }

  /**
   * Returns the distance in meters between ({@code lat1}, {@code lng1})  and
   * ({@code lat2}, {@code lng2}).
   * <p>
   * Calculation is done by the Haversine Formula.
   *
   * @param lat1  Latitude of first point, in radians
   * @param lng1  Longitude of first point, in radians
   * @param lat2  Latitude of second point, in radians
   * @param lng2  Longitude of second point, in radians
   *
   * @see <a href="http://en.wikipedia.org/wiki/Haversine_formula" target="_parent">
   * Haversine formula</a>
   */
  public static double computeDistance(double lat1, double lng1, double lat2, double lng2) {
    final double dLat = lat2 - lat1;
    final double dLng = lng2 - lng1;
    final double a =
        Math.pow(Math.sin(dLat / 2), 2) + Math.cos(lat1) * Math.cos(lat2)
            * Math.pow(Math.sin(dLng / 2), 2);
    return EARTH_RADIUS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  /**
   * Computes the magnetic variation at a given position.
   * 
   * @param position Latitude and longitude
   * @param height Height (meters)
   * 
   * @return Magnetic variation (degrees). West positive, East negative.
   */
  public static double getMagneticVariation(final LatLng position, final double height) {
    final double latRad = position.latRad();
    final double lngRad = position.lngRad();
    final double heightKm = height / 1000.0;

    final double varRad = MagField.GetMagVar(latRad, lngRad, heightKm, SESSION_TIME_MILLIS, null);
    return Math.toDegrees(varRad);
  }
}

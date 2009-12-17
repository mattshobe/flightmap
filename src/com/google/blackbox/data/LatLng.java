// Copyright 2009 Google Inc. All Rights Reserved.

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

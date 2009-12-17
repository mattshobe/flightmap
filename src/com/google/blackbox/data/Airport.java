// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.blackbox.data;

/**
 * Airport data structure.
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class Airport {
  /** ICAO identifier.  Unique.
   */
  public final String icao;

  /** Airport common name.  Not necessarily unique.
   */
  public final String name;

  /** Airport location
   */
  public final LatLng location;

  private String _toString;

  public Airport(String icao, String name, LatLng location) {
    this.icao = icao;
    this.name = name;
    this.location = location;
  }


  /** Convenience constructor.  Creates {@link LatLng} object and calls 
   * {@link #Airport(String, String, LatLng)}.
   */
  public Airport(String icao, String name, int lat, int lng) {
    this(icao, name, new LatLng(lat,lng));
  }

  public String toString() {
    if (this._toString == null) {
      StringBuilder sb = new StringBuilder(37);
      sb.append(this.icao);
      sb.append(" (");
      sb.append(this.name);
      sb.append(')');
      this._toString = sb.toString();
    }

    return this._toString;
  }

}

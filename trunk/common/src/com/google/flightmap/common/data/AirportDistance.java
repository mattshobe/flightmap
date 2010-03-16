// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.flightmap.common.data;

/**
 *
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class AirportDistance implements Comparable<AirportDistance> {
  public final Airport airport;
  /** Distance to {@link #airport} in nautical miles. */
  public final double distance;

  private Integer hashCode = null;

  public AirportDistance(Airport airport, double distance) {
    this.airport = airport;
    this.distance = distance;
  }

  @Override
  public int compareTo(AirportDistance o) {
//System.err.println("Comparing: " + this + " and " + o);
    int sign = (int) (Math.signum(this.distance - o.distance));
//    System.err.println("sign: " + sign + ": " + (this.distance - o.distance));
    return sign != 0 ? sign : this.airport.compareTo(o.airport);
  }

  @Override
  public String toString() {
    return String.format("%s - %.1f", airport.toString(), distance);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if ( !(obj instanceof AirportDistance)) return false;

    AirportDistance other = (AirportDistance)obj;
    return this.compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    if (this.hashCode == null) {
      int hashCode = this.airport.hashCode();
      hashCode ^= new Double(this.distance).hashCode();
      this.hashCode = hashCode;
    }

    return this.hashCode;
  }
}

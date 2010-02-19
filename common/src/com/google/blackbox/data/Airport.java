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

package com.google.blackbox.data;

/**
 * Airport data structure.
 * 
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class Airport implements Comparable<Airport> {
  /**
   * ICAO identifier. A 3-4 character string that must be unique in the
   * database.
   */
  public final String icao;

  /**
   * Airport common name. Not necessarily unique.
   */
  public final String name;

  /**
   * Airport location
   */
  public final LatLng location;

  public Airport(String icao, String name, LatLng location) {
    this.icao = icao;
    this.name = name;
    this.location = location;
  }

  public Airport(String icao, String name, int lat, int lng) {
    this(icao, name, new LatLng(lat, lng));
  }

  @Override
  public String toString() {
    return String.format("%s (%s)", icao, name);
  }

  @Override
  public int compareTo(Airport o) {
    return this.icao.compareTo(o.icao);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if ( !(obj instanceof Airport)) return false;

    Airport other = (Airport)obj;
//System.err.println("Comparing: " + this.toString() + " and " + other.toString());
    return this.compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    return this.icao.hashCode();
  }
}

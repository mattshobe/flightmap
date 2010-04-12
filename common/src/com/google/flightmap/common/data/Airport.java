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

import java.util.SortedSet;

/**
 * Airport data structure.
 */
public class Airport implements Comparable<Airport> {
  /**
   * Application-specific identifier.  An integer uniquely identifying the
   * airport for the application.
   */
  public final int id;

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
   * Airport type (land airport, seaplane base, heliport)
   */
  public final String type;

  /**
   * Airport city.  Not unique.
   */
  public final String city;

  /**
   * Airport location
   */
  public final LatLng location;

  /**
   * Airport status: true if open, false if closed (permanently or temporarily).
   */
  public final boolean isOpen;

  /**
   * Airport use: true if public, false if private (prior permission required)
   */
  public final boolean isPublic;

  /**
   * Control Tower: true if airport is towered, false otherwise.
   */
  public final boolean isTowered;

  /**
   * Airport runways.  In decreasing order of length, then width.
   */
  public final SortedSet<Runway> runways;

  public Airport(final int id,
                 final String icao,
                 final String name,
                 final String type,
                 final String city,
                 final LatLng location,
                 final boolean isOpen,
                 final boolean isPublic,
                 final boolean isTowered,
                 final SortedSet<Runway> runways) {
    this.id = id;
    this.icao = icao;
    this.name = name;
    this.type = type;
    this.city = city;
    this.location = location;
    this.isOpen = isOpen;
    this.isPublic = isPublic;
    this.isTowered = isTowered;
    this.runways = runways;
  }

  @Override
  public String toString() {
    return String.format("%s (%s)", icao, name);
  }

  @Override
  public int compareTo(Airport o) {
    return this.id - o.id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if ( !(obj instanceof Airport)) return false;

    Airport other = (Airport)obj;
    return this.compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    return this.icao.hashCode();
  }
}

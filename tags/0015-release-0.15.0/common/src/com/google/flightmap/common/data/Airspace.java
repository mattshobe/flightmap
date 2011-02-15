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

package com.google.flightmap.common.data;

import java.util.SortedMap;

/**
 * Airspace data structure.
 */
public class Airspace implements Comparable<Airspace> {
  /**
   * Special value used to represent a airspace starting from the surface.
   */
  public static final int SFC = Integer.MIN_VALUE;

  /**
   * Facility types.
  */
  public enum Class {
    ALPHA, BRAVO, CHARLIE, DELTA, ECHO, OTHER;

    public static Class valueOf(final char abbr) {
      switch (abbr) {
        case 'A':
          return ALPHA;
        case 'B':
          return BRAVO;
        case 'C':
          return CHARLIE;
        case 'D':
          return DELTA;
        case 'E':
          return ECHO;
        default:
          return OTHER;
      }
    }
  } 

  /**
   * Application-specific identifier. An integer uniquely identifying the
   * airspace for the application.
   */
  public final int id;

  /**
   * Airspace name. Should be unique.
   */
  public final String name;

  /**
   * Airspace class.
   */
  public final Class airspaceClass;
  
  /**
   * Lowest altitude of airspace, in feet MSL.  If equal to {@link #SFC}, the lowest altitude is the
   * surface (0 AGL).
   */
  public final int bottom;

  /**
   * Highest altitude of airspace, in feet MSL.
   */
  public final int top;
  
  /**
   * Points forming airspace, sorted by sequence number.
   */
  public final SortedMap<Integer, LatLng> points;

  /**
   * Arcs forming airspace, sorted by sequence number.
   */
  public final SortedMap<Integer, AirspaceArc> arcs;

  public Airspace(final int id,
                  final String name,
                  final Class airspaceClass,
                  final int bottom,
                  final int top,
                  final SortedMap<Integer, LatLng> points,
                  final SortedMap<Integer, AirspaceArc> arcs) {
    this.id = id;
    this.name = name;
    this.airspaceClass = airspaceClass;
    this.bottom = bottom;
    this.top = top;
    this.points = points;
    this.arcs = arcs;
  }

  @Override
  public String toString() {
    return String.format("%s (%s - %d)", name, bottom == SFC ? "SFC" : bottom, top);
  }

  @Override
  public int compareTo(final Airspace o) {
    return this.id - o.id;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof Airspace)) return false;

    final Airspace other = (Airspace) obj;
    return this.compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    return this.id;
  }
}

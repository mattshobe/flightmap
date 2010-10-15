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
 * Caches results from {@link NavigationUtil#getMagneticVariation} so that the
 * calculation isn't done too frequently.
 */
public class CachedMagneticVariation {
  /**
   * Number of degrees of location difference for recalculating variation. This
   * is in E6 format, to have units compatible with LatLng's E6 format.
   */
  private static final int MOVED_DELTA = (int) (1 * 1E6); // 1 degree

  /**
   * Location of last computed magnetic variation.
   */
  private LatLng previousLocation;

  /**
   * Last computed magnetic variation
   */
  private float previousVariation;

  /**
   * Returns magnetic variation for {@code location}. May return cached result
   * if location hasn't changed significantly since last call.
   * <p>
   * Note: typically the magnetic variation is only applied to the numeric
   * display of a bearing or track. It <b>should not</b> be applied to drawing
   * the map, vectors, etc.
   * 
   * @param location location
   * @param altitude altitude in meters
   * @return magnetic variation (negative for east, positive for west).
   */
  public synchronized float getMagneticVariation(LatLng location, float altitude) {
    if (null != previousLocation && Math.abs(location.lat - previousLocation.lat) < MOVED_DELTA
        && Math.abs(location.lng - previousLocation.lng) < MOVED_DELTA) {
      return previousVariation;
    }
    previousLocation = location;
    previousVariation = (float) NavigationUtil.getMagneticVariation(location, altitude);
    return previousVariation;
  }
}

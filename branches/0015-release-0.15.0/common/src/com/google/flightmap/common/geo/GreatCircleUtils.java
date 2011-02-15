/* 
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.flightmap.common.geo;

import com.google.flightmap.common.data.LatLng;

import java.util.LinkedList;
import java.util.List;

/**
 * Utility methods related to Great Circles (Earth geodesics).
 */
public class GreatCircleUtils {
  /**
   *  Utility class: default and only constructor is private.
   */
  private GreatCircleUtils() { }

  /**
   * Samples great circle between {@code point1} and {@code point2}.  The great circle and the 
   * resulting points connected by Rhumb lines should differ by no more than {@code maxError}
   * meters.
   */
  public static List<LatLng> sampleGreatCircle(final LatLng point1, final LatLng point2,
      final double maxError) {
    final double distance = NavigationUtil.computeDistance(point1, point2); 
    final double distanceRad = distance / NavigationUtil.EARTH_RADIUS;
    List<LatLng> samples = new LinkedList<LatLng>();
    samples.add(point1);
    LatLng last = point1;
    while (last != point2) {
      LatLng candidate = point2;
      double nextFrac = 1.0;
      while (! pathsBelowError(last, candidate, maxError)) {
        nextFrac /= 2;
        candidate = getIntermediateGreatCirclePoint(last, point2, nextFrac);
      }
      samples.add(candidate);
      last = candidate;
    }
    return samples;
  }

  /**
   * Returns {@code true} if the rhumb line and great circle paths between {@code point1} and {@code
   * point2} are (probably) always closer than {@code maxError} meters.
   * <p>
   * This is done by measuring the distance between points at the same intermediate (relative)
   * distance between {code point1} and {@code point2} on the Great Circle and Rhumb line.
   * The distance between successive points used for verification is in the order of magnitude of
   * {@code maxError} (currently, it is divided by two to avoid aliasing issues.)
   * <p>
   * Note that this algorithm is both computationally expensive and not geometrically correct:
   * points at the same relative distance on the two lines are not necessarily the closest ones.
   * Finding the closest Rhumb line point to a given Great Circle point (or vice-versa) is complex
   * and far beyond the scope of this method.  In practice, the current heuristic approach yields
   * satisfactory results.
   */
  private static boolean pathsBelowError(final LatLng point1, final LatLng point2,
      final double maxError) {
    final double distance = NavigationUtil.computeDistance(point1, point2);
    if (distance <= maxError) {
      return true;
    }
    final double distanceRad = distance / NavigationUtil.EARTH_RADIUS;
    final double step = maxError / distance / 2;
    for (double frac = step; frac < 1; frac += step) {
      final LatLng greatCirclePoint =
        getIntermediateGreatCirclePoint(point1, point2, frac, distanceRad);
      final LatLng rhumbLinePoint = getIntermediateRhumbLinePoint(point1, point2, frac);
      if (NavigationUtil.computeDistance(greatCirclePoint, rhumbLinePoint) > maxError) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns an intermediate point on the great circle between {@code point1} and {@code point2}.
   *
   * @see #getIntermediateGreatCirclePoint(LatLng, LatLng, double, double)
   */
  private static LatLng getIntermediateGreatCirclePoint(final LatLng point1, final LatLng point2,
      final double fraction) {
    final double distance = NavigationUtil.computeDistance(point1, point2);
    final double distanceRad = distance / NavigationUtil.EARTH_RADIUS;
    return getIntermediateGreatCirclePoint(point1, point2, fraction, distanceRad);
  }

  /**
   * Returns an intermediate point on the great circle between {@code point1} and {@code point2}.
   *
   * @param point1 Starting point
   * @param point2 End point
   * @param fraction Relative position of intermediate point (going from {@code point1} to {@code
   * point2}
   * @param distanceRad Great circle distance between {@code point1} and {@code point2}, in radians.
   * @return Intermediate point on the great circle between {@code point1} and {@code point2} at the
   * relative position {@code fraction} from {@code point1}.
   *
   * @see <a href="http://williams.best.vwh.net/avform.htm#Intermediate" target="_parent">
   * Aviation formulary</a>
   */
  private static LatLng getIntermediateGreatCirclePoint(final LatLng point1, final LatLng point2,
      final double fraction, final double distanceRad) {
    // Precomputations
    final double fracDistanceRad = fraction * distanceRad;
    final double remainingDistanceRad = distanceRad - fracDistanceRad;
    final double distanceRadSin = Math.sin(distanceRad);

    // Trigonometric computations
    final double lat1 = point1.latRad();
    final double lng1 = point1.lngRad();
    final double lat2 = point2.latRad();
    final double lng2 = point2.lngRad();
    final double lat1Cos = Math.cos(lat1);
    final double lng1Cos = Math.cos(lng1);
    final double lat1Sin = Math.sin(lat1);
    final double lng1Sin = Math.sin(lng1);
    final double lat2Cos = Math.cos(lat2);
    final double lng2Cos = Math.cos(lng2);
    final double lat2Sin = Math.sin(lat2);
    final double lng2Sin = Math.sin(lng2);

    final double A = Math.sin(remainingDistanceRad) / distanceRadSin;
    final double B = Math.sin(fracDistanceRad) / distanceRadSin;
    final double x = A * lat1Cos * lng1Cos + B * lat2Cos * lng2Cos;
    final double y = A * lat1Cos * lng1Sin + B * lat2Cos * lng2Sin;
    final double z = A * lat1Sin + B * lat2Sin;
    final double lat = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
    final double lng = Math.atan2(y, x);
    return LatLng.fromRadians(lat, lng);
  }

  /**
   * Returns an intermediate point on the Rhumb line between {@code point1} and {@code point2}.
   */
  private static LatLng getIntermediateRhumbLinePoint(final LatLng point1, final LatLng point2,
      final double f) {
    final int lat = (int) ((1 - f) * point1.lat + f * point2.lat + 0.5);
    final int lng = (int) ((1 - f) * point1.lng + f * point2.lng + 0.5);
    return new LatLng(lat, lng);
  }
}

/**
 * Module to calculate magnetic variation and field given position,
 * altitude, and date
 *
 * Copyright (C) 2000  Edward A Williams <Ed_Williams@compuserve.com>
 *
 *   The routine uses a spherical harmonic expansion of the magnetic
 * potential up to thirteenth order, together with its time variation, as
 * described in Chapter 4 of "Geomagnetism, Vol 1, Ed. J.A.Jacobs,
 * Academic Press (London 1987)". The program first converts geodetic
 * coordinates (lat/long on elliptic earth and altitude) to spherical
 * geocentric (spherical lat/long and radius) coordinates. Using this,
 * the spherical (B_r, B_theta, B_phi) magnetic field components are
 * computed from the model. These are finally referred to surface (X, Y,
 * Z) coordinates.
 *
 *   Fields are accurate to better than 200nT, variation and dip to
 * better than 0.5 degrees, with the exception of the declination near
 * the magnetic poles (where it is ill-defined) where the error may reach
 * 4 degrees or more.
 *
 *   Variation is undefined at both the geographic and
 * magnetic poles, even though the field itself is well-behaved. To
 * avoid the routine blowing up, latitude entries corresponding to
 * the geographic poles are slightly offset. At the magnetic poles,
 * the routine returns zero variation.
 *
 * HISTORY
 * Adapted from EAW Excel 3.0 version 3/27/94 EAW
 * Recoded in C++ by Starry Chan
 * WMM95 added and rearranged in ANSI-C EAW 7/9/95
 * Put shell around program and made Borland & GCC compatible EAW 11/22/95
 * IGRF95 added 2/96 EAW
 * WMM2000 IGR2000 added 2/00 EAW
 * Released under GPL  3/26/00 EAW
 * Adaptions and modifications for the SimGear project  3/27/2000 CLO
 * Removed all pow() calls and made static roots[][] arrays to
 * save many sqrt() calls on subsequent invocations
 * 3/28/2000  Norman Vine -- nhv@yahoo.com
 * Put in some bullet-proofing to handle magnetic and geographic poles.
 * 3/28/2000 EAW
 * Converted to Java class
 * 12/6/2000 Reece Robinson
 * 06/05/2010 Updated coefficients, integrated in Flight Map.
 * 10/07/2010 Redistributed under Apache License v2, with permission of EAW.
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
package com.google.flightmap.common.geo;

import java.lang.Math;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MagField {
  /** Major radius (km) IAU66 ellipsoid */
  private static final double a = 6378.16;
  private static final double a2 = a*a;
  private static final double a4 = a2*a2;

  /** Minor radius (km) IAU66 ellipsoid
   * b=a*(1-f) */
  private static final double b = 6378.16 * (1.0 - 1.0 / 298.25);
  private static final double b2 = b*b;
  private static final double b4 = b2*b2;

  /** "Mean" radius (km)  for spherical harmonic expansion */
  private static final double r_0 = 6371.2;

  // IGRF 2010

  private static final long DATA_TIME =
      new GregorianCalendar(2010, Calendar.JANUARY, 1).getTimeInMillis();

  private static final double MILLIS_PER_YEAR = 365.25 * 24 * 60 * 60 * 1000;

  private static final double[][] gnm_igrf2010 = {
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {-29496.5,-1585.9,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {-2396.6,3026.0,1668.6,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {1339.7,-2326.3,1231.7,634.2,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {912.6,809.0,166.6,-357.1, 89.7,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {-231.1,357.2,200.3,-141.2,-163.1, -7.7,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      { 72.8, 68.6, 76.0,-141.4,-22.9, 13.1,-77.9,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      { 80.4,-75.0, -4.7, 45.3, 14.0, 10.4,  1.6,  4.9,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      { 24.3,  8.2,-14.5, -5.7,-19.3, 11.6, 10.9,-14.1, -3.7,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  5.4,  9.4,  3.4, -5.3,  3.1,-12.4, -0.8,  8.4, -8.4,-10.1,  0.0,  0.0,  0.0,  0.0},
      { -2.0, -6.3,  0.9, -1.1, -0.2,  2.5, -0.3,  2.2,  3.1, -1.0, -2.8,  0.0,  0.0,  0.0},
      {  3.0, -1.5, -2.1,  1.6, -0.5,  0.5, -0.8,  0.4,  1.8,  0.2,  0.8,  3.8,  0.0,  0.0},
      { -2.1, -0.2,  0.3,  1.0, -0.7,  0.9, -0.1,  0.5, -0.4, -0.4,  0.2, -0.8,  0.0,  0.0},
      { -0.2, -0.9,  0.3,  0.4, -0.4,  1.1, -0.3,  0.8, -0.2,  0.4,  0.0,  0.4, -0.3, -0.3}};

  private static final double[][] gtnm_igrf2010 = {
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      { 11.4, 16.7,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {-11.3, -3.9,  2.7,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  1.3, -3.9, -2.9, -8.1,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      { -1.4,  2.0, -8.9,  4.4, -2.3,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      { -0.5,  0.5, -1.5, -0.7,  1.3,  1.4,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      { -0.3, -0.3, -0.3,  1.9, -1.6, -0.2,  1.8,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.2, -0.1, -0.6,  1.4,  0.3,  0.1, -0.8,  0.4,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      { -0.1,  0.1, -0.5,  0.3, -0.3,  0.3,  0.2, -0.5,  0.2,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0}};

  private static final double[][] hnm_igrf2010 = {
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,4945.1,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,-2707.7,-575.4,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,-160.5,251.7,-536.8,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,286.4,-211.2,164.4,-309.2,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0, 44.7,188.9,-118.1,  0.1,100.9,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,-20.8, 44.2, 61.5,-66.3,  3.1, 54.9,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,-57.8,-21.2,  6.6, 24.9,  7.0,-27.7, -3.4,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0, 10.9,-20.0, 11.9,-17.4, 16.7,  7.1,-10.8,  1.7,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,-20.5, 11.6, 12.8, -7.2, -7.4,  8.0,  2.2, -6.1,  7.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  2.8, -0.1,  4.7,  4.4, -7.2, -1.0, -4.0, -2.0, -2.0, -8.3,  0.0,  0.0,  0.0},
      {  0.0,  0.1,  1.7, -0.6, -1.8,  0.9, -0.4, -2.5, -1.3, -2.1, -1.9, -1.8,  0.0,  0.0},
      {  0.0, -0.8,  0.3,  2.2, -2.5,  0.5,  0.6,  0.0,  0.1,  0.3, -0.9, -0.2,  0.8,  0.0},
      {  0.0, -0.8,  0.3,  1.7, -0.6, -1.2, -0.1,  0.5,  0.1,  0.5,  0.4, -0.2, -0.5, -0.8}};

  private static final double[][] htnm_igrf2010 = {
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,-28.8,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,-23.0,-12.9,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  8.6, -2.9, -2.1,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.4,  3.2,  3.6, -0.8,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.5,  1.5,  0.9,  3.7, -0.6,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0, -0.1, -2.1, -0.4, -0.5,  0.8,  0.5,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.6,  0.3, -0.2, -0.1, -0.8, -0.3,  0.2,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.2,  0.5,  0.4,  0.1, -0.1,  0.4,  0.4,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0},
      {  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0,  0.0}};

  private static final int nmax = htnm_igrf2010.length - 1;

  private static double[][] P = new double[nmax + 1][nmax + 1];
  private static double[][] DP = new double[nmax + 1][nmax + 1];
  private static double[][] gnm = new double[nmax + 1][nmax + 1];
  private static double[][] hnm = new double[nmax + 1][nmax + 1];
  private static double[] sm = new double[nmax + 1];
  private static double[] cm = new double[nmax + 1];

  private static final double[] root;
  private static final double[][][] roots;

  static {
    root = new double[nmax + 1];
    for (int n = 2; n <= nmax; ++n) {
      root[n] = Math.sqrt((2.0 * n - 1) / (2.0 * n));
    }

    roots = new double[nmax + 1][nmax + 1][2];
    for (int m = 0; m <= nmax; ++m) {
      final double m2 = m*m;
      for (int n = Math.max(m + 1, 2); n <= nmax; ++n) {
        roots[m][n][0] = Math.sqrt((n - 1) * (n - 1) - m2);
        roots[m][n][1] = 1.0 / Math.sqrt(n*n - m2);
      }
    }
  }

  /**
   * Time of latest retrieved magnetic variation (milliseconds since Epoch)
   */
  private static Long previousTime = null;

  /**
   * Utility class: default and only constructor is private.
   */
  private MagField() { }

  /**
   * Computes magnetic variation.
   *
   * @param lat     geodetic latitude (radians)
   * @param lng     geodetic longitude (radians)
   * @param h       height (km)
   * @param time    time (milliseconds since Epoch)
   * @param field   output fields (or null): B_r, B_th, B_phi, B_x, B_y, B_z
   *
   * @return        magnetic variation (radians), West is positive
   *
   * N and E latitude and longitudes are positive, S and W negative.
   */
  public static double GetMagVar(final double lat,
                                 final double lng,
                                 final double h,
                                 final long time,
                                 final double[] field) {
    final double sinlat = Math.sin(lat);
    final double sinlat2 = sinlat * sinlat;
    final double coslat = Math.cos(lat);
    final double coslat2 = coslat * coslat;

    // convert to geocentric:

    // sr is effective radius
    final double sr = Math.sqrt(a2 * coslat2 + b2 * sinlat2);

    // theta is geocentric co-latitude
    final double theta = Math.atan2(coslat * (h * sr + a2), sinlat * (h * sr + b2));

    // r is geocentric radial distance
    final double r =
        Math.sqrt(h * h + 2.0 * h * sr + (a4 - (a4 - b4) * sinlat2) / (a2 - (a2 - b2) * sinlat2));

    final double c = Math.cos(theta);
    final double s = Math.sin(theta);

    // protect against zero divide at geographic poles
    final double inv_s =  1.0 / (s + ((s == 0.) ? 1.0e-8 : 0));

    // reset arrays
    for (int n = 0; n <= nmax; ++n) {
      for (int m = 0; m <= n; ++m) {
        P[n][m] = 0;
        DP[n][m] = 0;
      }
    }

    // diagonal elements
    P[0][0] = 1;
    P[1][1] = s;
    DP[0][0] = 0;
    DP[1][1] = c;
    P[1][0] = c ;
    DP[1][0] = -s;

    for (int n = 2; n <= nmax; ++n) {
      P[n][n] = P[n-1][n-1] * s * root[n];
      DP[n][n] = (DP[n - 1][n - 1] * s + P[n - 1][n - 1] * c) * root[n];
    }

    // lower triangle
    for (int m = 0; m <= nmax; ++m) {
      for (int n = Math.max(m + 1, 2); n <= nmax; ++n) {
        P[n][m] = (P[n - 1][m] * c * (2.0 * n - 1) - P[n - 2][m] * roots[m][n][0]) * roots[m][n][1];
        DP[n][m] = ((DP[n - 1][m] * c - P[n - 1][m] * s) *
                    (2.0 * n - 1) - DP[n - 2][m] * roots[m][n][0]) * roots[m][n][1];
      }
    }

    // On first call or if time has changed, compute Gauss coefficients (gnm, hnm).
    if (previousTime == null || !previousTime.equals(time)) {
      final double yearfrac = (time - DATA_TIME) / MILLIS_PER_YEAR;
      for (int n = 1; n <= nmax ; ++n) {
        for (int m = 0 ; m <= nmax ; ++m) {
          gnm[n][m] = gnm_igrf2010[n][m] + yearfrac * gtnm_igrf2010[n][m];
          hnm[n][m] = hnm_igrf2010[n][m] + yearfrac * htnm_igrf2010[n][m];
        }
      }
      previousTime = time;
    }

    // compute sm (sin(m lng) and cm (cos(m lng))
    for (int m = 0 ; m <= nmax ; ++m) {
      sm[m] = Math.sin(m * lng);
      cm[m] = Math.cos(m * lng);
    }

    // compute B fields
    double B_r = 0.0;
    double B_theta = 0.0;
    double B_phi = 0.0;

    final double fn_0 = r_0 / r;
    double fn = fn_0 * fn_0;

    for (int n = 1; n <= nmax; ++n) {
      double c1_n = 0.0;
      double c2_n = 0.0;
      double c3_n = 0.0;

      for (int m = 0; m <= n; ++m) {
        final double tmp = (gnm[n][m] * cm[m] + hnm[n][m] * sm[m]);
        c1_n += tmp * P[n][m];
        c2_n += tmp * DP[n][m];
        c3_n +=  m * (gnm[n][m] * sm[m] - hnm[n][m] * cm[m]) * P[n][m];
      }

      // fn = pow(r_0 / r, n + 2.0);
      fn *= fn_0;
      B_r += (n + 1) * c1_n * fn;
      B_theta -= c2_n * fn;
      B_phi += c3_n * fn * inv_s;
    }

    // Find geodetic field components:
    final double psi = theta - (Math.PI / 2.0 - lat);
    final double sinpsi = Math.sin(psi);
    final double cospsi = Math.cos(psi);
    final double X = -B_theta * cospsi - B_r * sinpsi;
    final double Y = B_phi;
    final double Z = B_theta * sinpsi - B_r * cospsi;

    if (field != null) {
      // Output fields
      field[0] = B_r;
      field[1] = B_theta;
      field[2] = B_phi;
      field[3] = X;
      field[4] = Y;
      field[5] = Z;
    }

    // Return 0 at the poles, else change sign (WEST is positive)
    return (X != 0.0 || Y != 0.0) ? (-1 * Math.atan2(Y, X)) : 0.0;
  }
}

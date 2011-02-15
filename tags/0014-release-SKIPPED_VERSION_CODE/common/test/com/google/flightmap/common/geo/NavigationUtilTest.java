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

package com.google.flightmap.common.geo;

import com.google.flightmap.common.data.LatLng;

import junit.framework.TestCase;

public class NavigationUtilTest extends TestCase {

  private static final double MAX_ERROR = 1E-5;
  private static final LatLng WEST = LatLng.fromDouble(0, -10);
  private static final LatLng EAST = LatLng.fromDouble(0, 10);
  private static final LatLng NORTH = LatLng.fromDouble(10,0);
  private static final LatLng SOUTH = LatLng.fromDouble(-10,0);
  private static final LatLng KSFO = LatLng.fromDouble(37.6189722,  -122.3748889);
  private static final LatLng KOAK = LatLng.fromDouble(37.7212778, -122.2207222);
  private static final LatLng KLAX = LatLng.fromDouble(33.9425222, -118.4071611);
  private static final LatLng KJFK = LatLng.fromDouble(40.6397511, -73.7789256);

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  public void testNormalizeBearing() {
    assertEquals(0, NavigationUtil.normalizeBearing(0), MAX_ERROR);
    assertEquals(0, NavigationUtil.normalizeBearing(1080), MAX_ERROR);
    assertEquals(0, NavigationUtil.normalizeBearing(-720), MAX_ERROR);
    assertEquals(10, NavigationUtil.normalizeBearing(370), MAX_ERROR);
    assertEquals(180.01, NavigationUtil.normalizeBearing(-179.99), MAX_ERROR);
    assertEquals(350, NavigationUtil.normalizeBearing(-10), MAX_ERROR);
    assertEquals(350, NavigationUtil.normalizeBearing(-370), MAX_ERROR);
  }

  public void testGetInitialCourse() {
    assertEquals(270, NavigationUtil.getInitialCourse(EAST, WEST), MAX_ERROR);
    assertEquals(90, NavigationUtil.getInitialCourse(WEST, EAST), MAX_ERROR);
    assertEquals(0, NavigationUtil.getInitialCourse(SOUTH, NORTH), MAX_ERROR);
    assertEquals(180, NavigationUtil.getInitialCourse(NORTH, SOUTH), MAX_ERROR);

    assertEquals(45, NavigationUtil.getInitialCourse(WEST, NORTH), 1);
    assertEquals(135, NavigationUtil.getInitialCourse(NORTH, EAST), 1);
    assertEquals(225, NavigationUtil.getInitialCourse(EAST, SOUTH), 1);
    assertEquals(315, NavigationUtil.getInitialCourse(SOUTH, WEST), 1);

    assertEquals(138, NavigationUtil.getInitialCourse(KSFO, KLAX), 0.5);
    assertEquals(320, NavigationUtil.getInitialCourse(KLAX, KSFO), 0.5);
    assertEquals(70, NavigationUtil.getInitialCourse(KSFO, KJFK), 0.5);
    assertEquals(282, NavigationUtil.getInitialCourse(KJFK, KSFO), 0.5);
  }

  public void testGetPointAlongRadial() {
    final double latDistance = NavigationUtil.computeDistance(NORTH, SOUTH);
    final LatLng south = NavigationUtil.getPointAlongRadial(NORTH, 180, latDistance);
    assertEquals(SOUTH, south);
    final LatLng north = NavigationUtil.getPointAlongRadial(SOUTH, 0, latDistance);
    assertEquals(NORTH, north);

    final double lngDistance = NavigationUtil.computeDistance(EAST, WEST);
    final LatLng west = NavigationUtil.getPointAlongRadial(EAST, 270, lngDistance);
    assertEquals(WEST, west);
    final LatLng east = NavigationUtil.getPointAlongRadial(WEST, 90, lngDistance);
    assertEquals(EAST, east);

    final LatLng ksfo =
        NavigationUtil.getPointAlongRadial(KOAK, 230, 9.6 / NavigationUtil.METERS_TO_NM);
    assertEquals(0, NavigationUtil.computeDistance(KSFO, ksfo), 100);

    final LatLng koak =
        NavigationUtil.getPointAlongRadial(KSFO, 50, 9.6 / NavigationUtil.METERS_TO_NM);
    assertEquals(0, NavigationUtil.computeDistance(KOAK, koak), 100);
  }

}

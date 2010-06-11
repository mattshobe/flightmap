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

/**
 * Rectangle with sides that run north-south and east-west.
 */
public class LatLngRect {
  /** Southwest corner of rectangle. */
  private LatLng swCorner;
  /** Northeast corner of rectangle. */
  private LatLng neCorner;

  /**
   * Creates an empty rectangle.
   */
  public LatLngRect() {
  }

  /**
   * Creates a rectangle with the given corners. The {@code swCorner} and
   * {@code neCorner} params will be set correctly, even if the parameters are
   * not those particular corners.
   */
  public LatLngRect(final LatLng corner1, final LatLng corner2) {
    add(corner1);
    add(corner2);
  }

  /**
   * @param center  Center of circular area
   * @param radius  Radius of circular area, in degrees * 1E6
   * @return        Rectangular area bounding the given circular area
   */
  public static LatLngRect getBoundingBox(final LatLng center, final int radius) {
    final LatLng swBoundingBoxCorner = new LatLng(center.lat - radius, center.lng - radius);
    final LatLng neBoundingBoxCorner = new LatLng(center.lat + radius, center.lat + radius);
    return new LatLngRect(swBoundingBoxCorner, neBoundingBoxCorner);
  }

  /**
   * Grows rectangle to include this point.
   */
  public synchronized void add(final LatLng newPoint) {
    // Special case for empty.
    if (isEmpty()) {
      swCorner = newPoint;
      neCorner = newPoint;
      return;
    }
    int northLat = Math.max(neCorner.lat, newPoint.lat);
    int southLat = Math.min(swCorner.lat, newPoint.lat);
    int eastLng = Math.max(neCorner.lng, newPoint.lng);
    int westLng = Math.min(swCorner.lng, newPoint.lng);
    neCorner = new LatLng(northLat, eastLng);
    swCorner = new LatLng(southLat, westLng);
  }

  /**
   * Grow rectangle to include this area.
   */
  public synchronized void add(final LatLngRect area) {
    add(area.neCorner);
    add(area.swCorner);
  }

  /**
   * @return true if point is in this area, false otherwise.
   */
  public synchronized boolean contains(final LatLng point) {
    return point.lat >= swCorner.lat && point.lat <= neCorner.lat &&
           point.lng >= swCorner.lng && point.lng <= neCorner.lng;
  }

  /**
   * @return true if given area is included in this area, false otherwise.
   */
  public synchronized boolean contains(final LatLngRect area) {
    return contains(area.neCorner) && contains(area.swCorner);
  }

  /**
   * Returns true if the rectangle is empty (such as when created with the
   * default constructor).
   */
  public synchronized boolean isEmpty() {
    return swCorner == null || neCorner == null;
  }

  @Override
  public synchronized String toString() {
    if (isEmpty()) {
      return "Empty";
    }
    return swCorner + " - " + neCorner;
  }

  public synchronized LatLng getSwCorner() {
    return swCorner;
  }

  public synchronized LatLng getNeCorner() {
    return neCorner;
  }

  /**
   * @return North latitude, in E6 format.
   */
  public synchronized int getNorth() {
    return neCorner.lat;
  }

  /**
   * @return South latitude, in E6 format.
   */
  public synchronized int getSouth() {
    return swCorner.lat;
  }

  /**
   * @return East longitude, in E6 format.
   */
  public synchronized int getEast() {
    return neCorner.lng;
  }

  /**
   * @return West longitude, in E6 format.
   */
  public synchronized int getWest() {
    return swCorner.lng;
  }

}

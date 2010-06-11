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
  public LatLngRect(LatLng corner1, LatLng corner2) {
    add(corner1);
    add(corner2);
  }

  /**
   * Grows rectangle to include this point.
   */
  public synchronized void add(LatLng newPoint) {
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
}

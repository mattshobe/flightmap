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

import java.util.Iterator;
import java.util.LinkedList;

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
    final LatLng neBoundingBoxCorner = new LatLng(center.lat + radius, center.lng + radius);
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
    final int northLat = Math.max(neCorner.lat, newPoint.lat);
    final int southLat = Math.min(swCorner.lat, newPoint.lat);
    final int eastLng = Math.max(neCorner.lng, newPoint.lng);
    final int westLng = Math.min(swCorner.lng, newPoint.lng);
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
    return contains(point.lat, point.lng);
  }

  protected synchronized boolean contains(final int latE6, final int lngE6) {
    if (isEmpty()) {
      return false;
    }
    return latE6 >= swCorner.lat && latE6 <= neCorner.lat &&
           lngE6 >= swCorner.lng && lngE6 <= neCorner.lng;
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
  public synchronized boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }

    if (this == obj) {
      return true;
    }

    if (!(obj instanceof LatLngRect)) {
      return false;
    }

    final LatLngRect other = (LatLngRect)obj;
    return (isEmpty() && other.isEmpty()) || 
           (neCorner.equals(other.neCorner) && swCorner.equals(other.swCorner));
  }

  @Override
  public int hashCode() {
    return neCorner.hashCode() ^ swCorner.hashCode();
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

  public synchronized LatLng getCenter() {
    final int lat = (int) Math.round((swCorner.lat + neCorner.lat)/2.0);
    final int lng = (int) Math.round((swCorner.lng + neCorner.lng)/2.0);
    return new LatLng(lat, lng);
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

  /**
   * Returns intersection of {@code this} and {@code that}.
   */
  public synchronized LatLngRect intersect(final LatLngRect that) {
    if (isEmpty() || that.isEmpty()) {
      return new LatLngRect();
    }

    final int thisWest = this.getWest();
    final int thisEast = this.getEast();
    final int thisNorth = this.getNorth();
    final int thisSouth = this.getSouth();
    final int thatWest = that.getWest();
    final int thatEast = that.getEast();
    final int thatNorth = that.getNorth();
    final int thatSouth = that.getSouth();

    if (thisWest > thatEast || thisEast < thatWest ||
        thisSouth > thatNorth || thisNorth < thatSouth) {
      return new LatLngRect();
    }

    final int interWest = Math.max(thisWest, thatWest);
    final int interEast = Math.min(thisEast, thatEast);
    final int interSouth = Math.max(thisSouth, thatSouth);
    final int interNorth = Math.min(thisNorth, thatNorth);
    final LatLng interNE = new LatLng(interNorth, interEast);
    final LatLng interSW = new LatLng(interSouth, interWest);
    return new LatLngRect(interNE, interSW);
  }

  private static LatLngRect getRectOrNullIfEmpty(final int lat1, final int lng1,
                                                 final int lat2, final int lng2) {
    if (lat1 == lat2 || lng1 == lng2) {
      return null;
    }

    final LatLng corner1 = new LatLng(lat1, lng1);
    final LatLng corner2 = new LatLng(lat2, lng2);
    return new LatLngRect(corner1, corner2);
  }

  /**
   * Returns relative complement of {@code that} in {@code this}.
   * In set notation: this \ that (or this-that)
   */
  public synchronized LinkedList<LatLngRect> remove(final LatLngRect that) {
    final LinkedList<LatLngRect> complementRects = new LinkedList<LatLngRect>();
    final LatLngRect intersection = this.intersect(that);
    if (intersection.isEmpty()) {
      complementRects.add(new LatLngRect(neCorner, swCorner));
    } else {
      final int thisNorth = getNorth();
      final int thisSouth = getSouth();
      final int thisWest = getWest();
      final int thisEast = getEast();
      final int interNorth = intersection.getNorth();
      final int interSouth = intersection.getSouth();
      final int interWest = intersection.getWest();
      final int interEast = intersection.getEast();

      complementRects.add(getRectOrNullIfEmpty(thisNorth, thisWest, interNorth, thisEast));
      complementRects.add(getRectOrNullIfEmpty(interNorth, thisWest, interSouth, interWest));
      complementRects.add(getRectOrNullIfEmpty(interNorth, interEast, interSouth, thisEast));
      complementRects.add(getRectOrNullIfEmpty(interSouth, thisWest, thisSouth, thisEast));

      for (Iterator<LatLngRect> i = complementRects.iterator(); i.hasNext(); ) {
        final LatLngRect next = i.next();
        if (next == null) {
          i.remove();
        }
      }
    }
    return complementRects;
  }
}

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
package com.google.flightmap.android.map;

import android.graphics.Point;
import android.os.Bundle;

import com.google.flightmap.common.data.LatLng;


/**
 * Model for the map (as in Model-View-Presenter).
 */
class MapModel {
  // Saved instance state constants.
  private static final String ZOOM_LEVEL = "zoom-level";
  private static final String MAP_ANCHOR_LAT = "map-anchor-lat";
  private static final String MAP_ANCHOR_LNG = "map-anchor-lng";
  private static final String IS_PANNING = "is-panning";

  private boolean redrawNeeded;
  // Size of the canvas in pixels
  private int canvasWidth;
  private int canvasHeight;

  // Zoom items.
  static final int MIN_ZOOM = 4;
  static final int MAX_ZOOM = 12;
  private static final float ZOOM_STEP = 0.5f;
  private float zoom = 10;

  /**
   * Origin of the map. All scaling centers on this point. This is typically the
   * location where the aircraft icon is drawn, however it may move if the user
   * uses the scale gesture (pinch-to-zoom).
   */
  Point mapOrigin = new Point();

  /** Panning changes the map anchor. May be null when not panning. */
  private LatLng mapAnchorLatLng;

  /**
   * True when the user has panned at all. Stays true across multiple touch
   * events until panning is cancelled.
   */
  private volatile boolean isPanning;

  /**
   * Resets the map origin.
   * 
   * @param isNorthUp true if the user preference is to put north at the top
   *        (false for track-up).
   */
  public synchronized void resetMapOrigin(boolean isNorthUp) {
    if (isNorthUp) {
      // Center the origin on the screen.
      setMapOrigin(canvasWidth / 2, canvasHeight / 2);
    } else {
      // Center the origin horizontally, and 3/4 of the way down vertically.
      setMapOrigin(canvasWidth / 2, canvasHeight - (canvasHeight / 4));
    }
  }

  /**
   * Saves map-specific state info to {@code outState}.
   */
  synchronized void saveInstanceState(Bundle outState) {
    outState.putFloat(ZOOM_LEVEL, getZoom());
    if (mapAnchorLatLng != null) {
      outState.putInt(MAP_ANCHOR_LAT, mapAnchorLatLng.lat);
      outState.putInt(MAP_ANCHOR_LNG, mapAnchorLatLng.lng);
    }
    outState.putBoolean(IS_PANNING, isPanning);
  }

  /**
   * Restores map-specific state info from {@code savedInstanceState}
   */
  synchronized void restoreInstanceState(Bundle savedInstanceState) {
    if (null == savedInstanceState) {
      return;
    }
    if (savedInstanceState.containsKey(ZOOM_LEVEL)) {
      setZoom(savedInstanceState.getFloat(ZOOM_LEVEL));
    }
    if (savedInstanceState.containsKey(MAP_ANCHOR_LAT)
        && savedInstanceState.containsKey(MAP_ANCHOR_LNG)) {
      int lat = savedInstanceState.getInt(MAP_ANCHOR_LAT);
      int lng = savedInstanceState.getInt(MAP_ANCHOR_LNG);
      mapAnchorLatLng = new LatLng(lat, lng);
    }
    isPanning = savedInstanceState.getBoolean(IS_PANNING);
  }

  public synchronized boolean isRedrawNeeded() {
    return redrawNeeded;
  }

  public synchronized void setRedrawNeeded(boolean redrawNeeded) {
    this.redrawNeeded = redrawNeeded;
  }

  /**
   * On return MIN_ZOOM <= zoom <= MAX_ZOOM.
   */
  public synchronized void setZoom(float zoom) {
    zoom = Math.max(zoom, MIN_ZOOM);
    zoom = Math.min(zoom, MAX_ZOOM);
    this.zoom = zoom;
  }

  public synchronized float getZoom() {
    return zoom;
  }

  public boolean canvasSizeEquals(int width, int height) {
    return width == this.canvasWidth && height == this.canvasHeight;
  }

  public void setCanvasSize(int width, int height) {
    this.canvasWidth = width;
    this.canvasHeight = height;
  }

  /**
   * Zooms in or out by {@link #ZOOM_STEP}
   * 
   * @param zoomIn true to zoom in, false to zoom out.
   */
  public synchronized void zoomStep(boolean zoomIn) {
    if (zoomIn) {
      setZoom(zoom + ZOOM_STEP);
    } else {
      setZoom(zoom - ZOOM_STEP);
    }
  }

  /**
   * Sets the map anchor point. Pass null to reset the anchor to the current
   * location.
   */
  public synchronized void setMapAnchorLatLng(LatLng mapAnchorLatLng) {
    this.mapAnchorLatLng = mapAnchorLatLng;
  }

  public synchronized LatLng getMapAnchorLatLng() {
    return mapAnchorLatLng;
  }

  /**
   * Sets the map origin to the given screen pixel coordinates.
   */
  public synchronized void setMapOrigin(int mapOriginX, int mapOriginY) {
    mapOrigin.set(mapOriginX, mapOriginY);
  }

  public synchronized int getMapOriginX() {
    return mapOrigin.x;
  }

  public synchronized int getMapOriginY() {
    return mapOrigin.y;
  }

  public synchronized void setPanning(boolean isPanning) {
    this.isPanning = isPanning;
  }

  public synchronized boolean isPanning() {
    return isPanning;
  }
}

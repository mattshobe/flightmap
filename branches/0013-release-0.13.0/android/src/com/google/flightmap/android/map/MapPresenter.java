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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.flightmap.android.MainActivity;
import com.google.flightmap.android.TapcardActivity;
import com.google.flightmap.android.UserPrefs;
import com.google.flightmap.android.db.GetAirportsInRectangleTask;
import com.google.flightmap.android.db.GetAirspacesInRectangleTask;
import com.google.flightmap.android.geo.AndroidMercatorProjection;
import com.google.flightmap.common.ProgressListener;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.Airspace;
import com.google.flightmap.common.data.AirspaceArc;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;
import com.google.flightmap.common.db.CachedAirportDirectory;
import com.google.flightmap.common.db.CachedAviationDbAdapter;
import com.google.flightmap.common.geo.CachedMagneticVariation;
import com.google.flightmap.common.geo.NavigationUtil;
import com.google.flightmap.common.geo.NavigationUtil.DistanceUnits;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.location.Location;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Presenter part of Model-View-Presenter for the map display.
 * <p>
 * All the non-trivial UI work is done here.
 *
 * @see MapModel
 * @see MapView
 */
public class MapPresenter implements OnSharedPreferenceChangeListener {
  private static final String TAG = MapPresenter.class.getSimpleName();

  private static final double LOG_OF_2 = Math.log(2);
  /** Position is considered "old" after this many milliseconds. */
  private static final long MAX_LOCATION_AGE = 300000; // 5 minutes.
  
  // Fields relating to touch events and panning.
  private static final int PAN_CROSSHAIR_SIZE = 12;
  private static final int PAN_INFO_MARGIN = 30;
  /** Minimum number of screen pixels to drag to indicate panning. */
  private static final int PAN_TOUCH_THRESHOLD = 15;
  /** Radius in screen pixels to search around touch location. */
  static final int TOUCH_PIXEL_RADIUS = 30;
  // Screen coordinates where the user touched.
  private float touchX;
  private float touchY;

  private final MainActivity mainActivity;
  private final MapView view;
  private final MapModel model;
  /** True if the last touch event was a move. */
  private volatile boolean previousTouchWasMove;
  /** Reused Point object in Screen pixel space. */
  private final Point tempPoint = new Point();

  // Performance optimization. Create these objects only once since they are
  // used in rendering.
  private final Matrix screenMatrix = new Matrix();
  /** Used by {@link #getLocationForPoint} and {@link #getPointForLocation} */
  private float[] screenPoints = new float[2];
  /**
   * Coordinates of the 4 corners of the screen in screen pixel space. Numbered
   * clockwise starting with the top-left corner screenCorners[3] is the
   * bottom-left corner. These are set in {@link #surfaceChanged}
   */
  private final Point[] screenCorners = new Point[4];
  /**
   * Reused Point object in Mercator pixel space.
   */
  private final Point mercatorPoint = new Point();
  /** Used for calls to Location.distanceBetween */
  private float[] distanceBearingResult = new float[2];

  // Last known bearing.
  private float lastBearing;

  // Magnetic variation w/ caching.
  private final CachedMagneticVariation magneticVariation = new CachedMagneticVariation();

  // Airports currently on the screen. This collection is updated by a
  // background task.
  private Collection<Airport> airportsOnScreen;

  // Populates airportsOnScreen in a background thread.
  private GetAirportsInRectangleTask getAirportsTask;

  private ProgressListener getAirportsListener;

  private Collection<Airspace> airspacesOnScreen;

  private GetAirspacesInRectangleTask getAirspacesTask;

  private ProgressListener getAirspacesListener;

  private UserPrefs userPrefs;

  MapPresenter(MapView view, MainActivity mainActivity) {
    this.view = view;
    this.mainActivity = mainActivity;
    this.userPrefs = mainActivity.getUserPrefs();
    model = new MapModel();
    for (int i = 0; i < 4; i++) {
      screenCorners[i] = new Point();
    }
    userPrefs.registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public synchronized void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    model.setRedrawNeeded(true);
    if (UserPrefs.NORTH_UP.equals(key)) {
      model.resetMapOrigin(mainActivity.getUserPrefs().isNorthUp());
    }
  }

  /**
   * Stops panning.
   * 
   * @return true if was panning prior to this call.
   */
  private synchronized boolean stopPanning() {
    if (!model.isPanning()) {
      return false;
    }
    model.setPanning(false);
    previousTouchWasMove = false;
    model.setMapAnchorLatLng(null);
    model.setRedrawNeeded(true);
    model.resetMapOrigin(mainActivity.getUserPrefs().isNorthUp());
    return true;
  }

  /**
   * Handles the user performing the scale gesture.
   */
  synchronized void onScale(float scaleFactor) {
    double zoomDelta = Math.log(scaleFactor) / LOG_OF_2;
    model.setZoom((float) (model.getZoom() + zoomDelta));
    model.setRedrawNeeded(true);
    previousTouchWasMove = true;
  }


  /**
   * Handles a touch pointer getting cancelled.
   * 
   * @returns true if the event was handled, false otherwise.
   */
  synchronized boolean actionCancel() {
    previousTouchWasMove = false;
    return true;
  }

  /**
   * Handles the user touching the screen.
   * 
   * @returns true if the event was handled, false otherwise.
   */
  synchronized boolean actionDown(float touchX, float touchY) {
    this.touchX = touchX;
    this.touchY = touchY;
    previousTouchWasMove = false;
    return true;
  }

  /**
   * Handles the user releasing one of multiple pointers.
   * 
   * @returns true if the event was handled, false otherwise.
   */
  synchronized boolean actionPointerUp(float touchX, float touchY) {
    this.touchX = touchX;
    this.touchY = touchY;
    previousTouchWasMove = false;
    return true;
  }


  /**
   * Handles the user releasing a touch.
   * 
   * @returns true if the event was handled, false otherwise.
   */
  synchronized boolean actionUp(int x, int y) {
    if (previousTouchWasMove) {
      // Don't do tapcard action right after a move.
      previousTouchWasMove = false;
      return true;
    }
    // See if the Pan Reset button was hit.
    if (model.isPanning()) {
      if (view.isPanResetButtonHit(x, y)) {
        stopPanning();
        return true;
      }
    }

    // See if an airport was tapped.
    Collection<Airport> airportsNearTap;
    airportsNearTap = getAirportsNearScreenPoint(new Point(x, y));
    if (!airportsNearTap.isEmpty()) {
      Airport airport = chooseSingleAirport(airportsNearTap);
      if (airport != null) {
        showTapcard(airport);
      }
      return true;
    }

    // Only get here if the user tapped in a blank area of the map.
    view.showZoomController();
    return true;
  }

  /**
   * Handles the user moving a single finger (to pan).
   * 
   * @returns true if the event was handled, false otherwise.
   */
  synchronized boolean actionMove(int x, int y) {
    return actionMoveCommon(x, y);
  }

  /**
   * Handles the user moving two fingers (to scale and pan). The coordinates
   * should be the center of the bounding rectangle of the two fingers.
   * 
   * @returns true if the event was handled, false otherwise.
   */
  synchronized boolean actionMoveWhileScaling(int x, int y) {
    tempPoint.x = x;
    tempPoint.y = y;
    // Move the map anchor to the center of the pan gesture.
    model.setMapAnchorLatLng(getLocationForPoint(tempPoint));
    // Change the map pixel origin to the touch point.
    model.setMapOrigin(tempPoint.x, tempPoint.y);
    return actionMoveCommon(x, y);
  }

  /**
   * Does common steps for {@link #actionMove} and {@link #actionMoveWithScale}.
   * 
   * @returns true if the event was handled, false otherwise.
   */
  private synchronized boolean actionMoveCommon(int x, int y) {
    synchronized (view) {
      int deltaX = Math.round(touchX - x);
      int deltaY = Math.round(touchY - y);
      if (!previousTouchWasMove
          && Math.max(Math.abs(deltaX), Math.abs(deltaY)) < PAN_TOUCH_THRESHOLD
              * view.density) {
        // Ignore very small moves, since they may actually be taps.
        // Round to int to match what Point uses.
        return true;
      }

      model.setRedrawNeeded(true);
      previousTouchWasMove = true;
      model.setPanning(true);
      touchX = x;
      touchY = y;
      panByPixelAmount(deltaX, deltaY);
      return true;
    }
  }

  /**
   * Returns ground position corresponding to {@code screenPoint}.
   * 
   * @param zoom zoom level.
   * @param orientation bearing in degrees from {@code locationPoint} to the top
   *        center of the screen. Will be 0 when north-up, and the current track
   *        when track-up.
   * @param anchorPoint pixel coordinates of anchor location (in Mercator pixel
   *        space as returned by {@link AndroidMercatorProjection#toPoint}
   * @param screenPoint coordinates in screen pixel space (such as from a touch
   *        event).
   */
  private synchronized LatLng getLocationForPoint(final float zoom, final float orientation,
      final Point anchorPoint, final Point screenPoint) {
    // The operations on screenMatrix are the opposite of the ones applied to
    // the canvas in drawMapOnCanvas().
    //
    // drawMapOnCanvas() does the following:
    // c.translate(mapOriginX, mapOriginY);
    // if (isTrackUp) c.rotate(360 - bearing);
    // c.translate(-mapAnchorPoint.x, -mapAnchorPoint.y);
    screenMatrix.reset();
    screenMatrix.postTranslate(-model.getMapOriginX(), -model.getMapOriginY());
    if (!mainActivity.getUserPrefs().isNorthUp()) {
      screenMatrix.postRotate(orientation);
    }
    screenMatrix.postTranslate(anchorPoint.x, anchorPoint.y);
    screenPoints[0] = screenPoint.x;
    screenPoints[1] = screenPoint.y;
    screenMatrix.mapPoints(screenPoints);
    mercatorPoint.x = Math.round(screenPoints[0]);
    mercatorPoint.y = Math.round(screenPoints[1]);

    LatLng result = AndroidMercatorProjection.fromPoint(zoom, mercatorPoint);
    return result;
  }

  /**
   * Returns ground position corresponding to {@code screenPoint}. Uses {@code
   * mapAnchorLatLng} for location and {@code lastBearing} for orientation and
   * calls {@link #getLocationForPoint(float, float, Point, Point)}.
   * 
   * @param screenPoint coordinates in screen pixel space (such as from a touch
   *        event).
   * @return ground position, or null if {@code mapAnchorLatLng} is null.
   */
  private synchronized LatLng getLocationForPoint(Point screenPoint) {
    LatLng mapAnchorLatLng = model.getMapAnchorLatLng();
    if (null == mapAnchorLatLng) {
      return null;
    }
    float zoom = model.getZoom();
    final Point anchorPoint = AndroidMercatorProjection.toPoint(zoom, mapAnchorLatLng);
    final float orientation = mainActivity.getUserPrefs().isNorthUp() ? 0 : lastBearing;
    return getLocationForPoint(zoom, orientation, anchorPoint, screenPoint);
  }

  /**
   * Returns screen pixel coordinates corresponding to {@code location}.
   * 
   * @param zoom zoom level.
   * @param anchorPoint pixel coordinates of anchor location (in Mercator pixel
   *        space as returned by {@link AndroidMercatorProjection#toPoint}
   * @param location location to convert to a screen point.
   */
  private synchronized Point getPointForLocation(final float zoom, final Point anchorPoint,
      LatLng location) {
    // The operations on screenMatrix are the same as the ones applied to
    // the canvas in drawMapOnCanvas(), EXCEPT for the rotate operation.
    //
    // TODO - figure out why including screenMatrix.rotate in this method
    // does not work, and excluding it gives correct results.
    //
    // drawMapOnCanvas() does the following:
    // c.translate(mapOriginX, mapOriginY);
    // if (isTrackUp) c.rotate(360 - bearing);
    // c.translate(-mapAnchorPoint.x, -mapAnchorPoint.y);
    screenMatrix.reset();
    screenMatrix.postTranslate(model.getMapOriginX(), model.getMapOriginY());
    screenMatrix.postTranslate(-anchorPoint.x, -anchorPoint.y);
    Point mercator = AndroidMercatorProjection.toPoint(zoom, location);
    screenPoints[0] = mercator.x;
    screenPoints[1] = mercator.y;
    screenMatrix.mapPoints(screenPoints);
    Point result = new Point(Math.round(screenPoints[0]), Math.round(screenPoints[1]));
    return result;
  }

  /**
   * Returns screen pixel coordinates corresponding to {@code location}. Uses
   * {@code lastBearing} for orientation.
   * 
   * @param location ground location to map to screen pixel coordinates.
   * @return screen pixel coordinates, or null if {@code mapAnchorLatLng} is
   *         null.
   */
  private synchronized Point getPointForLocation(LatLng location) {
    final LatLng mapAnchorLatLng = model.getMapAnchorLatLng();
    if (null == mapAnchorLatLng) {
      return null;
    }
    float zoom = model.getZoom();
    Point anchorPoint = AndroidMercatorProjection.toPoint(zoom, mapAnchorLatLng);
    return getPointForLocation(zoom, anchorPoint, location);
  }

  /**
   * Moves the mapAnchorLatLng by panning by the given x and y delta amounts.
   * 
   * @param deltaX x change in screen pixels.
   * @param deltaY y change in screen pixels.
   */
  private void panByPixelAmount(final int deltaX, final int deltaY) {
    // Move the map anchor location by the given delta values. First get
    // mapAnchorLatLng in screen pixel space, then apply the deltas, then
    // pan to the resulting screen location.
    Point mapAnchorPoint = getPointForLocation(model.getMapAnchorLatLng());
    if (mapAnchorPoint == null) {
      return;
    }
    panToScreenPoint(mapAnchorPoint.x + deltaX, mapAnchorPoint.y + deltaY);
  }

  /**
   * Sets the mapAnchorLatLng to correspond to the given screen pixel
   * coordinates.
   */
  private synchronized void panToScreenPoint(final float x, final float y) {
    tempPoint.x = (int) (x + 0.5);
    tempPoint.y = (int) (y + 0.5);
    model.setMapAnchorLatLng(getLocationForPoint(tempPoint));
  }

  /**
   * Returns a rectangle that is enclosed by a circle of {@code radius} pixels
   * centered on {@code point}.
   */
  private LatLngRect createRectangleAroundPoint(final Point point, final int radius) {
    LatLngRect result = new LatLngRect();
    // Set cornerPoint to each corner of a square with sides 2 * radius.
    Point cornerPoint = new Point();
    cornerPoint.x = point.x - radius;
    cornerPoint.y = point.y - radius;
    result.add(getLocationForPoint(cornerPoint));
    cornerPoint.x = point.x + radius;
    result.add(getLocationForPoint(cornerPoint));
    cornerPoint.y = point.y + radius;
    result.add(getLocationForPoint(cornerPoint));
    cornerPoint.x = point.x - radius;
    result.add(getLocationForPoint(cornerPoint));
    return result;
  }

  /**
   * Returns a list of airports near {@code screenPoint}. Result may be empty,
   * but will never be null.
   */
  private synchronized Collection<Airport> getAirportsNearScreenPoint(Point screenPoint) {
    Collection<Airport> result = new LinkedList<Airport>();
    if (airportsOnScreen == null || airportsOnScreen.isEmpty()) {
      return result;
    }

    // Make a rectangle enclosed by a TOUCH_PIXEL_RADIUS circle around {@code
    // screenPoint}. Test if any airports are contained by that rectangle.
    LatLngRect touchRect = createRectangleAroundPoint(screenPoint, TOUCH_PIXEL_RADIUS);
    for (Airport airport : airportsOnScreen) {
      if (touchRect.contains(airport.location)) {
        result.add(airport);
      }
    }
    return result;
  }

  /**
   * Returns a rectangle enclosing the current view.
   * 
   * @param zoom zoomlevel
   * @param orientation bearing in degrees from {@code locationPoint} to the top
   *        center of the screen. Will be 0 when north-up, and the current track
   *        when track-up.
   * @param anchorPoint pixel coordinates of current anchor point (as returned
   *        by {@link AndroidMercatorProjection#toPoint}
   */
  private synchronized LatLngRect getScreenRectangle(final float zoom, final float orientation,
      final Point anchorPoint) {
    // Make rectangle that encloses the 4 screen corners.
    LatLngRect result = new LatLngRect();
    for (int i = 0; i < 4; i++) {
      result.add(getLocationForPoint(zoom, orientation, anchorPoint, screenCorners[i]));
    }
    return result;
  }

  /**
   * Returns single airport from {@code airports}. If the collection has exactly
   * one item, that Airport will be returned. Otherwise a dialog box will be
   * shown so the user can choose an airport.
   * 
   * @param airports airports to choose from. May not be null or empty.
   */
  private Airport chooseSingleAirport(Collection<Airport> airports) {
    // TODO: Show a dialog to let the user choose an airport when
    // airports.size() > 1.
    return airports.iterator().next();
  }

  /**
   * Shows tapcard for an airport.
   */
  private void showTapcard(Airport airport) {
    Intent tapcardIntent = new Intent(mainActivity, TapcardActivity.class);
    tapcardIntent.putExtra(TapcardActivity.AIRPORT_ID, airport.id);
    mainActivity.startActivity(tapcardIntent);
  }

  void setCanvasSize(int width, int height) {
    model.setCanvasSize(width, height);
    model.resetMapOrigin(mainActivity.getUserPrefs().isNorthUp());
    // Update pixel coordinates of screen corners.
    // 
    // screenCorners[0] x & y are always 0.
    // screenCorners[1].y is always 0.
    // screenCorners[3].x is always 0.
    screenCorners[1].x = width - 1;
    screenCorners[2].x = width - 1;
    screenCorners[2].y = height - 1;
    screenCorners[3].y = height - 1;
  }

  float getMagneticTrack(Location location) {
    return (float) NavigationUtil.normalizeBearing(location.getBearing()
        + magneticVariation.getMagneticVariation(model.getMapAnchorLatLng(), (float) location
            .getAltitude()));
  }

  synchronized void drawMapOnCanvas(Canvas c, Location location) {
    if (null == c) {
      return;
    }

    // Restore point: Canvas origin set to top-left corner.
    final int restoreToCanvasOrigin = c.save();

    // Clear the map.
    c.drawColor(Color.BLACK);

    // Display message about no current location and return.
    if (null == location || System.currentTimeMillis() - location.getTime() > MAX_LOCATION_AGE) {
      view.showNoGpsLocationMessage(c);
      return;
    }

    // Show or hide the simulator warning. Blink off every 2 seconds.
    boolean blinkOff = ((location.getTime() / 1000) % 4) < 2;
    if (mainActivity.getFlightMap().getLocationHandler().isLocationSimulated() && !blinkOff) {
      view.showSimulatorWarning(true);
    } else {
      view.showSimulatorWarning(false);
    }

    // Copy for thread safety.
    final boolean isTrackUp = !mainActivity.getUserPrefs().isNorthUp();

    // Update bearing (if possible). Do not change bearing while panning when
    // track-up.
    if (location.hasBearing()) {
      if (!isTrackUp || !model.isPanning()) {
        lastBearing = location.getBearing();
      }
    }

    //
    // Initialize transform to draw airports.
    //

    // Rotate everything relative to the origin.
    c.translate(model.getMapOriginX(), model.getMapOriginY());
    if (isTrackUp) {
      // Rotate to make track up (no rotation = north up).
      c.rotate(360 - lastBearing);
    }

    // Get map anchor pixel coordinates. Then set translation so everything is
    // drawn relative to the map anchor. When not panning, the map anchor is
    // the aircraft location. When panning, it's the panned to location.
    final float zoom = model.getZoom(); // copy for thread safety.
    final LatLng locationLatLng =
        LatLng.fromDouble(location.getLatitude(), location.getLongitude());
    if (model.getMapAnchorLatLng() == null || !model.isPanning()) {
      model.setMapAnchorLatLng(locationLatLng);
    }
    Point mapAnchorPoint = AndroidMercatorProjection.toPoint(zoom, model.getMapAnchorLatLng());
    c.translate(-mapAnchorPoint.x, -mapAnchorPoint.y);

    // Set orientation to North or last known bearing
    final float orientation = isTrackUp ? lastBearing : 0;

    final LatLngRect screenArea = getScreenRectangle(zoom, orientation, mapAnchorPoint);
    drawMapItems(c, location, screenArea, zoom, isTrackUp);

    //
    // Draw airplane.
    //
    c.translate(mapAnchorPoint.x, mapAnchorPoint.y);
    final Point locationPoint = AndroidMercatorProjection.toPoint(zoom, locationLatLng);
    if (model.isPanning()) {
      //
      // Draw a crosshair at the map anchor point.
      //

      // Rotate if in track-up mode to have the crosshair aligned with the
      // track.
      if (isTrackUp) {
        c.save();
        c.rotate(lastBearing);
      }
      drawPanCrosshairAndInfo(c, location);
      if (isTrackUp) {
        c.restore();
      }

      // Dotted line from anchor point to aircraft location.
      c.drawLine(0, 0, locationPoint.x - mapAnchorPoint.x, locationPoint.y - mapAnchorPoint.y,
          MapView.PAN_DASH_PAINT);

      // Translate to the spot where the airplane should be drawn.
      c.translate(locationPoint.x - mapAnchorPoint.x, locationPoint.y - mapAnchorPoint.y);
    }

    // Rotate no matter what. If track up, this will make the airplane point to
    // the top of the screen. If north up, this will point the airplane at the
    // current track.
    //
    // Special case if track-up and panning. lastBearing is frozen in that case,
    // but we should draw the airplane rotated to the most recent bearing.
    if (!model.isPanning() || !isTrackUp) {
      c.rotate(lastBearing);
    } else if (isTrackUp) {
      // In panning mode while track-up.
      if (location.hasBearing()) {
        c.rotate(location.getBearing());
      }
    }
    view.airplaneImage.draw(c);

    // Draw items that are in fixed locations. Restore canvas transform to the
    // original canvas (no rotations, orgin at top-left corner).
    c.restoreToCount(restoreToCanvasOrigin);

    view.drawFixedLocationItems(c, location, zoom);
  }
  
  private void drawMapItems(Canvas c, Location location, LatLngRect screenArea, float zoom,
      boolean isTrackUp) {
    final int minAirportRank = getMinimumAirportRank(zoom);
    updateAirportsOnScreen(screenArea, minAirportRank);
    updateAirspacesOnScreen(screenArea);
    drawAirspacesOnMap(c, zoom, location);
    drawAirportsOnMap(c, minAirportRank, zoom, isTrackUp);
  }

  private synchronized void drawAirportsOnMap(Canvas c, int minRank, float zoom, boolean isTrackUp) {
    // airportsOnScreen could be null if the background task hasn't finished
    // yet.
    if (airportsOnScreen == null) {
      return;
    }
    final Iterator<Airport> i = airportsOnScreen.iterator();
    while (i.hasNext()) {
      final Airport airport = i.next();
      if (!mainActivity.getUserPrefs().shouldInclude(airport) || airport.rank < minRank) {
        i.remove();
        continue;
      }

      final Paint airportPaint = view.getAirportPaint(airport);
      Point airportPoint = AndroidMercatorProjection.toPoint(zoom, airport.location);
      c.drawCircle(airportPoint.x, airportPoint.y, 24, airportPaint);

      // Undo, then redo the track-up rotation so the labels are always at the
      // top for track up.
      if (isTrackUp) {
        c.save();
        c.rotate(lastBearing, airportPoint.x, airportPoint.y);
      }
      c.drawText(airport.icao, airportPoint.x, airportPoint.y - 31, MapView.AIRPORT_TEXT_PAINT);
      if (isTrackUp) {
        c.restore();
      }
    }
  }

  /**
   * Draws the pan crosshair and information on the bearing and distance to the
   * map anchor point.
   * 
   * Before calling this method, the canvas transform should be set so the
   * origin is at the point to draw the crosshair.
   * 
   * @param c canvas to draw on.
   * @param location current location.
   */
  private synchronized void drawPanCrosshairAndInfo(Canvas c, Location location) {
    final float density = view.density;
    final float crosshairSize = PAN_CROSSHAIR_SIZE * density;
    c.drawLine(0, -crosshairSize, 0, crosshairSize, MapView.PAN_SOLID_PAINT);
    c.drawLine(-crosshairSize, 0, crosshairSize, 0, MapView.PAN_SOLID_PAINT);
    final LatLng mapAnchorLatLng = model.getMapAnchorLatLng();
    if (mapAnchorLatLng == null) {
      return;
    }
    Location.distanceBetween(location.getLatitude(), location.getLongitude(), //
        mapAnchorLatLng.latDeg(), mapAnchorLatLng.lngDeg(), distanceBearingResult);
    float distanceMeters = distanceBearingResult[0];
    float bearingTo =
        (float) NavigationUtil.normalizeBearing(distanceBearingResult[1]
            + magneticVariation.getMagneticVariation(mapAnchorLatLng, 0));
    DistanceUnits distanceUnits = mainActivity.getUserPrefs().getDistanceUnits();
    String navigationText =
        String.format("%.1f%s - BRG %03.0f%s", distanceUnits.getDistance(distanceMeters),
            distanceUnits.distanceAbbreviation, bearingTo, MapView.DEGREES_SYMBOL);
    c.drawText(navigationText, 0, PAN_INFO_MARGIN * density, MapView.PAN_INFO_PAINT);
  }

  private synchronized void drawAirspacesOnMap(final Canvas c, final float zoom, final Location l) {
    // Draw airspaces
    if (airportsOnScreen == null || airspacesOnScreen == null) {
      return;
    }
    for (Airspace airspace : airspacesOnScreen) {
      final Path path = new Path();
      boolean first = true;

      final Iterator<Map.Entry<Integer, LatLng>> pointIter = airspace.points.entrySet().iterator();
      final Iterator<Map.Entry<Integer, AirspaceArc>> arcIter = airspace.arcs.entrySet().iterator();

      Map.Entry<Integer, LatLng> pointEntry = pointIter.hasNext() ? pointIter.next() : null;
      Map.Entry<Integer, AirspaceArc> arcEntry = arcIter.hasNext() ? arcIter.next() : null;
      while (pointEntry != null || arcEntry != null) {
        final int pointSeqNr = pointEntry != null ? pointEntry.getKey() : Integer.MAX_VALUE;
        final int arcSeqNr = arcEntry != null ? arcEntry.getKey() : Integer.MAX_VALUE;
        assert pointSeqNr != arcSeqNr;
        if (pointSeqNr < arcSeqNr) {
          final LatLng latLng = pointEntry.getValue();
          final Point point = AndroidMercatorProjection.toPoint(zoom, latLng);
          if (first) {
            path.moveTo(point.x, point.y);
          } else {
            path.lineTo(point.x, point.y);
          }
          pointEntry = pointIter.hasNext() ? pointIter.next() : null;
        } else {
          assert arcSeqNr < pointSeqNr;
          final AirspaceArc arc = arcEntry.getValue();
          final RectF boundingRect = AndroidMercatorProjection.toRectF(zoom, arc.boundingBox);
          path.arcTo(boundingRect, arc.startAngle, arc.sweepAngle);
          arcEntry = arcIter.hasNext() ? arcIter.next() : null;
        }
        first = false;
      }
      path.close();

      final Paint airspacePaint = view.getAirspacePaint(airspace, l);
      c.drawPath(path, airspacePaint);
    }
  }

  /**
   * Returns the minimum airport rank for the given zoom level. Ranks are in the
   * range 0-5 (5 being most important).
   */
  private static int getMinimumAirportRank(float zoom) {
    if (zoom <= 6) {
      return 5;
    }
    if (zoom < 8) {
      return 4;
    }
    if (zoom < 9) {
      return 3;
    }
    if (zoom < 11) {
      return 1;
    }
    return 0;
  }

  /**
   * Helper method for the activity's onDestroy().
   */
  synchronized void destroy() {
    cancelQueriesInProgress();
    mainActivity.getUserPrefs().unregisterOnSharedPreferenceChangeListener(this);
  }

  MapModel getModel() {
    return model;
  }

  /**
   * Cancels all pending queries (airports, airspaces).
   */
  private synchronized void cancelQueriesInProgress() {
    if (getAirportsTask != null && getAirportsTask.isQueryInProgress()) {
      cancelTask(getAirportsTask);
    }
    if (getAirspacesTask != null && getAirspacesTask.isQueryInProgress()) {
      cancelTask(getAirspacesTask);
    }
  }

  private synchronized void updateAirportsOnScreen(LatLngRect screenArea, int minimumAirportRank) {
    final CachedAirportDirectory airportDirectory = mainActivity.getAirportDirectory();
    // Is there a query in progress?
    if (getAirportsTask != null && getAirportsTask.isQueryInProgress()) {
      if (airportDirectory.isCacheMatch(screenArea, minimumAirportRank)) {
        // The query in progress will give the same results as passing
        // (screenArea, minRank) to a new task.
        Log.i(TAG, "updateAirportsOnScreen: Still waiting on query");
        return;
      }
      cancelTask(getAirportsTask);
    }
    // Create listener only once
    if (getAirportsListener == null) {
      getAirportsListener = new AirportsQueryListener();
    }
    // Have to make a new task here. Can't call execute again on an active task.
    getAirportsTask =
        new GetAirportsInRectangleTask(airportDirectory, getAirportsListener);
    getAirportsTask.execute(new GetAirportsInRectangleTask.QueryParams(screenArea,
        minimumAirportRank));
  }

  private synchronized void updateAirspacesOnScreen(LatLngRect screenArea) {
    final CachedAviationDbAdapter aviationDbAdapter = mainActivity.getAviationDbAdapter();
    // Is there a query in progress?
    if (getAirspacesTask != null && getAirspacesTask.isQueryInProgress()) {
      if (aviationDbAdapter.isCacheMatch(screenArea)) {
        Log.i(TAG, "updateAirspacesOnScreen: Still waiting on query");
        return;
      }
      cancelTask(getAirspacesTask);
    }
    // Create listener only once
    if (getAirspacesListener == null) {
      getAirspacesListener = new AirspacesQueryListener();
    }
    // Have to make a new task here. Can't call execute again on an active task.
    getAirspacesTask =
        new GetAirspacesInRectangleTask(aviationDbAdapter, getAirspacesListener);
    getAirspacesTask.execute(screenArea);
  }

  /**
   * Cancels task. Logs a warning message on failure.
   */
  private static void cancelTask(AsyncTask<?, ?, ?> task) {
    Log.d(TAG, "Cancelling task: " + task);
    boolean cancelled = task.cancel(true);
    if (!cancelled) {
      Log.w(TAG, "FAILED to cancel task: " + task);
    }
  }

  private class AirportsQueryListener implements ProgressListener {
    /**
     * {@inheritDoc} Called when {@link GetAirportsInRectanglTask} completes.
     */
    @Override
    public void hasCompleted(boolean success) {
      synchronized (MapPresenter.this) {
        if (success && getAirportsTask != null) {
          try {
            airportsOnScreen = getAirportsTask.get();
            model.setRedrawNeeded(true);
          } catch (InterruptedException e) {
            Log.i(TAG, "Interrupted while getting airports on screen", e);
          } catch (ExecutionException e) {
            Log.w(TAG, "Execution failed getting airports on screen", e);
          }
        }
      }
    }

    @Override
    public void hasProgressed(int unused) {
      // GetAirportsInRectangleTask does not give intermediate progress.
    }
  }

  private class AirspacesQueryListener implements ProgressListener {
    /**
     * {@inheritDoc} Called when {@link GetAirspacesInRectangleTask} completes.
     */
    @Override
    public void hasCompleted(boolean success) {
      synchronized (MapPresenter.this) {
        if (success && getAirspacesTask != null) {
          try {
            airspacesOnScreen = getAirspacesTask.get();
            model.setRedrawNeeded(true);
          } catch (InterruptedException e) {
            Log.i(TAG, "Interrupted while getting airspaces on screen", e);
          } catch (ExecutionException e) {
            Log.w(TAG, "Execution failed getting airspacess on screen", e);
          }
        }
      }
    }

    @Override
    public void hasProgressed(int unused) {
      // GetAirspacesInRectangleTask does not give intermediate progress.
    }
  }
}

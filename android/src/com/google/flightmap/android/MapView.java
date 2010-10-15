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
package com.google.flightmap.android;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ZoomButtonsController;

import com.google.flightmap.android.location.LocationHandler;
import com.google.flightmap.android.location.LocationHandler.Source;
import com.google.flightmap.common.CachedMagneticVariation;
import com.google.flightmap.common.NavigationUtil;
import com.google.flightmap.common.ProgressListener;
import com.google.flightmap.common.NavigationUtil.DistanceUnits;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

/**
 * View for the moving map.
 * <p>
 * Magnetic variation does not affect how the map is drawn. It only matters when
 * displaying the numeric track value on the top panel.
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback,
    OnSharedPreferenceChangeListener, ProgressListener {
  private static final String TAG = MapView.class.getSimpleName();
  public static final String DEGREES_SYMBOL = "\u00b0";

  // Saved instance state constants.
  private static final String ZOOM_LEVEL = "zoom-level";

  /** Position is considered "old" after this many milliseconds. */
  private static final long MAX_LOCATION_AGE = 300000; // 5 minutes.

  /** Radius in screen pixels to search around touch location. */
  private static final int TOUCH_PIXEL_RADIUS = 30;

  // Paints.
  private static final Paint ERROR_TEXT_PAINT = new Paint();
  private static final Paint AIRPORT_TEXT_PAINT = new Paint();
  private static final Paint PANEL_BACKGROUND_PAINT = new Paint();
  private static final Paint PANEL_DIGITS_PAINT = new Paint();
  private static final Paint PANEL_UNITS_PAINT = new Paint();
  private static boolean textSizesSet;
  private Paint toweredPaint = new Paint();
  private Paint nonToweredPaint = new Paint();

  // Zoom items.
  private static final int MIN_ZOOM = 4;
  private static final int MAX_ZOOM = 12;
  private static final float ZOOM_STEP = 0.5f;

  private ZoomButtonsController zoomController;
  private float zoom = 10;

  // Top panel items.
  static final float PANEL_HEIGHT = 60;
  private static final float PANEL_TEXT_MARGIN = 10;
  private static final float PANEL_NOTCH_HEIGHT = 15;
  private static final float PANEL_NOTCH_WIDTH = 10;
  private static final float PANEL_TEXT_BASELINE =
      PANEL_HEIGHT - PANEL_NOTCH_HEIGHT - PANEL_TEXT_MARGIN;

  // Rectangle with a notch that's the background for the top panel area.
  private Path topPanel;

  // Main activity.
  private final MainActivity mainActivity;

  // Last known bearing.
  private float lastBearing;

  // Coordinates to draw the aircraft on the map.
  private int aircraftX;
  private int aircraftY;

  // Underlying surface for this view.
  private volatile SurfaceHolder holder;

  // Rect used to get text width. Create so we don't new one for each frame.
  private final Rect textBounds = new Rect();

  // Airplane image and location where to draw the left, top so it's centered.
  private final Drawable airplaneImage;

  // Layout holding the simulator message.
  private LinearLayout simulatorMessage;

  // Screen density.
  private final float density;

  // Graphical zoom scale.
  private final ZoomScale zoomScale;

  // Caching. Values from the last time the map was drawn.
  private Location previousLocation;
  private float previousZoom;
  private boolean redrawNeeded;

  // Performance optimization. Create these objects only once since they are
  // used in rendering.
  private final Matrix screenMatrix = new Matrix();
  /** Used by {@link #getLocationForPoint} */
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

  // Magnetic variation w/ caching.
  private final CachedMagneticVariation magneticVariation = new CachedMagneticVariation();

  // Airports currently on the screen. This collection is updated by a
  // background task.
  private Collection<Airport> airportsOnScreen;

  // Populates airportsOnScreen in a background thread.
  private GetAirportsInRectangleTask getAirportsTask;

  // Static initialization.
  static {
    // Do not put any calls to setTextSize here. Put them in #setTextSizes().
    ERROR_TEXT_PAINT.setAntiAlias(true);
    ERROR_TEXT_PAINT.setColor(Color.WHITE);
    ERROR_TEXT_PAINT.setTextAlign(Paint.Align.CENTER);
    AIRPORT_TEXT_PAINT.setAntiAlias(true);
    AIRPORT_TEXT_PAINT.setARGB(0xff, 0xff, 0xff, 0xff);
    AIRPORT_TEXT_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    AIRPORT_TEXT_PAINT.setTextAlign(Paint.Align.CENTER);
    PANEL_BACKGROUND_PAINT.setARGB(0xee, 0x22, 0x22, 0x22);
    PANEL_DIGITS_PAINT.setAntiAlias(true);
    PANEL_DIGITS_PAINT.setColor(Color.WHITE);
    PANEL_DIGITS_PAINT.setTypeface(Typeface.SANS_SERIF);
    PANEL_DIGITS_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    PANEL_UNITS_PAINT.setAntiAlias(true);
    PANEL_UNITS_PAINT.setARGB(0xff, 0x99, 0x99, 0x99);
    PANEL_UNITS_PAINT.setTypeface(Typeface.SANS_SERIF);
  }

  public MapView(MainActivity mainActivity) {
    super(mainActivity);
    this.mainActivity = mainActivity;
    this.density = mainActivity.getResources().getDisplayMetrics().density;
    this.zoomScale = new ZoomScale(density, mainActivity.userPrefs);
    for (int i = 0; i < 4; i++) {
      screenCorners[i] = new Point();
    }
    getHolder().addCallback(this);
    setFocusable(true); // make sure we get key events
    setKeepScreenOn(true);
    createZoomController();
    setTextSizes(density);

    simulatorMessage = (LinearLayout) mainActivity.findViewById(R.id.simulator_message);

    Resources res = mainActivity.getResources();
    // Set up paints from resource colors.
    toweredPaint.setColor(res.getColor(R.color.ToweredAirport));
    nonToweredPaint.setColor(res.getColor(R.color.NonToweredAirport));
    // Set up airplane image.
    airplaneImage = centerImage(res.getDrawable(R.drawable.aircraft));
  }

  /**
   * Returns {@code image} with its bounds set so that when drawn the center of
   * the image will be at the drawing coordinates.
   * 
   * @param image the image to center (will be modified by this call).
   */
  public static Drawable centerImage(Drawable image) {
    // TODO: Move this method to generic utility class.
    int imageWidth = image.getIntrinsicWidth();
    int imageHeight = image.getIntrinsicHeight();
    // Set bounds so the airplane is centered when drawn.
    int left = -imageWidth / 2;
    int top = -imageHeight / 2;
    image.setBounds(left, top, left + imageWidth, top + imageHeight);
    return image;
  }

  /**
   * Scales text sizes based on screen density. See
   * http://developer.android.com/guide/practices/screens_support.html#dips-pels
   */
  private static synchronized void setTextSizes(float density) {
    if (textSizesSet) {
      return;
    }
    textSizesSet = true;
    ERROR_TEXT_PAINT.setTextSize(15 * density);
    AIRPORT_TEXT_PAINT.setTextSize(19 * density);
    PANEL_DIGITS_PAINT.setTextSize(26 * density);
    PANEL_UNITS_PAINT.setTextSize(18 * density);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (event.getAction() != MotionEvent.ACTION_DOWN) {
      return false;
    }
    // See if an airport was tapped.
    Collection<Airport> airportsNearTap;
    airportsNearTap =
        getAirportsNearScreenPoint(new Point(Math.round(event.getX()), Math.round(event.getY())));
    if (!airportsNearTap.isEmpty()) {
      for (Airport airport : airportsNearTap) {
        Log.i(TAG, "Airport near tap: " + airport);
      }

      Airport airport = chooseSingleAirport(airportsNearTap);
      if (airport != null) {
        showTapcard(airport);
      }
      return true;
    }

    // Only get here if the user tapped in a blank area of the map.
    showZoomController();
    return true;
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

  private synchronized void showZoomController() {
    if (null != zoomController) {
      zoomController.setVisible(true);
    }
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

  @Override
  protected void onDetachedFromWindow() {
    if (null != zoomController) {
      zoomController.setVisible(false);
    }
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    setRedrawNeeded(true);
  }

  private synchronized boolean isRedrawNeeded() {
    return redrawNeeded;
  }

  private synchronized void setRedrawNeeded(boolean redrawNeeded) {
    this.redrawNeeded = redrawNeeded;
  }

  private synchronized void createTopPanelPath(int width) {
    final float center = width / 2.0f;
    topPanel = new Path();
    topPanel.moveTo(0, 0);
    topPanel.lineTo(width, 0);
    topPanel.lineTo(width, PANEL_HEIGHT);
    topPanel.lineTo(center + PANEL_NOTCH_WIDTH, PANEL_HEIGHT);
    topPanel.lineTo(center, PANEL_HEIGHT - PANEL_NOTCH_HEIGHT);
    topPanel.lineTo(center - PANEL_NOTCH_WIDTH, PANEL_HEIGHT);
    topPanel.lineTo(0, PANEL_HEIGHT);
    topPanel.close();
  }

  /**
   * Surface dimensions changed.
   */
  @Override
  public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    if (mainActivity.userPrefs.isNorthUp()) {
      // Center the aircraft on the screen.
      aircraftX = width / 2;
      aircraftY = height / 2;
    } else {
      // Center the aircraft horizontally, and 3/4 of the way down vertically.
      aircraftX = width / 2;
      aircraftY = height - (height / 4);
    }

    // Update pixel coordinates of screen corners.
    // 
    // screenCorners[0] x & y are always 0.
    // screenCorners[1].y is always 0.
    // screenCorners[3].x is always 0.
    screenCorners[1].x = width - 1;
    screenCorners[2].x = width - 1;
    screenCorners[2].y = height - 1;
    screenCorners[3].y = height - 1;
    createTopPanelPath(width);
  }

  @Override
  public synchronized void surfaceCreated(SurfaceHolder holder) {
    this.holder = holder;
    setRedrawNeeded(true);
    // Set up listener to changes to SharedPreferences.
    mainActivity.userPrefs.registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    this.holder = null;
    mainActivity.userPrefs.unregisterOnSharedPreferenceChangeListener(this);
  }

  private void createZoomController() {
    zoomController = new ZoomButtonsController(this);

    // Set the gravity on the the zoom controls to the bottom left of this view.
    FrameLayout.LayoutParams zoomParams =
        new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT);
    zoomParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
    zoomController.getZoomControls().setLayoutParams(zoomParams);

    zoomController.setOnZoomListener(new ZoomButtonsController.OnZoomListener() {
      @Override
      public void onZoom(boolean zoomIn) {
        final float zoomCopy = getZoom(); // copy for thread safety.
        if (zoomIn) {
          setZoom(zoomCopy + ZOOM_STEP);
        } else {
          setZoom(zoomCopy - ZOOM_STEP);
        }
        zoomController.setZoomInEnabled(getZoom() < MAX_ZOOM);
        zoomController.setZoomOutEnabled(getZoom() > MIN_ZOOM);
      }

      @Override
      public void onVisibilityChanged(boolean visible) {
        // Ignored.
      }
    });
  }

  public void drawMap(Location location) {
    Canvas c = null;
    try {
      if (null == holder) {
        return;
      }
      synchronized (this) {
        // Stop the simulator if the user preference was changed.
        final LocationHandler locationHandler = mainActivity.flightMap.getLocationHandler();
        if (locationHandler.isLocationSimulated() && !mainActivity.userPrefs.enableSimulator()) {
          Log.i(TAG, "Stopping simulator due to user preference.");
          locationHandler.setLocationSource(Source.REAL);
        }

        if (!hasMoved(location) && !isRedrawNeeded() && zoom == previousZoom) {
          return;
        }
        if (isRedrawNeeded()) {
          setRedrawNeeded(false);
        }
        previousLocation = location;
        previousZoom = zoom;
      }
      c = holder.lockCanvas(null);
      synchronized (holder) {
        drawMapOnCanvas(c, location);
      }
    } finally {
      if (c != null) {
        holder.unlockCanvasAndPost(c);
      }
    }
  }

  private synchronized void drawMapOnCanvas(Canvas c, Location location) {
    if (null == c) {
      Log.w(TAG, "null canvas");
      return;
    }

    // Clear the map.
    c.drawColor(Color.BLACK);

    // Display message about no current location and return.
    if (null == location || System.currentTimeMillis() - location.getTime() > MAX_LOCATION_AGE) {
      c.drawText(mainActivity.getText(R.string.old_location).toString(), c.getWidth() / 2, //
          c.getHeight() / 2, ERROR_TEXT_PAINT);
      simulatorMessage.setVisibility(GONE);
      return;
    }

    // Show or hide the simulator warning. Blink off every 2 seconds.
    boolean blinkOff = ((location.getTime() / 1000) % 4) < 2;
    if (mainActivity.flightMap.getLocationHandler().isLocationSimulated() && !blinkOff) {
      simulatorMessage.setVisibility(VISIBLE);
    } else {
      simulatorMessage.setVisibility(GONE);
    }

    LatLng locationLatLng = LatLng.fromDouble(location.getLatitude(), location.getLongitude());

    // Copy for thread safety.
    final boolean isTrackUp = !mainActivity.userPrefs.isNorthUp();

    // Update bearing (if possible)
    if (location.hasBearing()) {
      lastBearing = location.getBearing();
    }

    // Draw everything relative to the aircraft.
    c.translate(aircraftX, aircraftY);
    if (isTrackUp) {
      // Rotate to make track up (no rotation = north up).
      c.rotate(360 - lastBearing);
    }

    // Get location pixel coordinates. Then set translation so everything is
    // drawn relative to the current location.
    final float zoomCopy = getZoom(); // copy for thread safety.
    Point locationPoint = AndroidMercatorProjection.toPoint(zoomCopy, locationLatLng);
    c.translate(-locationPoint.x, -locationPoint.y);

    // Draw airports.
    final int width = c.getWidth();

    // Set orientation to North or last known bearing
    final float orientation = isTrackUp ? lastBearing : 0;

    final LatLngRect screenArea = getScreenRectangle(zoomCopy, orientation, locationPoint);
    final int minAirportRank = getMinimumAirportRank(zoomCopy);
    updateAirportsOnScreen(screenArea, minAirportRank);

    // airportsOnScreen could be null if the background task hasn't finished
    // yet.
    if (airportsOnScreen != null) {
      final Iterator<Airport> i = airportsOnScreen.iterator();

      while (i.hasNext()) {
        final Airport airport = i.next();
        if (!mainActivity.userPrefs.shouldInclude(airport) || airport.rank < minAirportRank) {
          i.remove();
          continue;
        }

        final Paint airportPaint = getAirportPaint(airport);
        Point airportPoint = AndroidMercatorProjection.toPoint(zoomCopy, airport.location);
        c.drawCircle(airportPoint.x, airportPoint.y, 24, airportPaint);

        // Undo, then redo the track-up rotation so the labels are always at the
        // top for track up.
        if (isTrackUp) {
          c.rotate(lastBearing, airportPoint.x, airportPoint.y);
        }
        c.drawText(airport.icao, airportPoint.x, airportPoint.y - 31, AIRPORT_TEXT_PAINT);
        if (isTrackUp) {
          c.rotate(360 - lastBearing, airportPoint.x, airportPoint.y);
        }
      }
    }
    // Draw airplane.
    c.translate(locationPoint.x, locationPoint.y);
    // Rotate no matter what. If track up, this will make the airplane point to
    // the top of the screen. If north up, this will point the airplane at the
    // current track.
    c.rotate(lastBearing);
    airplaneImage.draw(c);

    // Undo to-track rotation for north up.
    if (!isTrackUp) {
      c.rotate(360 - lastBearing);
    }

    // Draw items that are in fixed locations. Set origin to top-left corner.
    c.translate(-aircraftX, -aircraftY);

    zoomScale.drawScale(c, location, zoomCopy);

    // Polygon for top params display.
    if (null != topPanel) {
      c.drawPath(topPanel, PANEL_BACKGROUND_PAINT);
      String speed = "-";
      String track = "-" + DEGREES_SYMBOL;
      String altitude = "-";
      DistanceUnits distanceUnits = mainActivity.userPrefs.getDistanceUnits();
      String speedUnits = " " + distanceUnits.speedAbbreviation;

      if (location.hasSpeed()) {
        speed = String.format("%.0f", location.getSpeed() * distanceUnits.speedMultiplier);
      }
      if (location.hasBearing()) {
        // Show numeric display relative to magnetic north.
        float magneticTrack =
            location.getBearing()
                + magneticVariation.getMagneticVariation(locationLatLng, (float) location
                    .getAltitude());
        magneticTrack = (float) NavigationUtil.normalizeBearing(magneticTrack);
        track = String.format(" %03.0f%s", magneticTrack, DEGREES_SYMBOL);
      }
      if (location.hasAltitude()) {
        // Round altitude to nearest 10 foot increment to avoid jitter.
        int altitudeNearestTen =
            (int) (Math.round(location.getAltitude() * NavigationUtil.METERS_TO_FEET / 10.0) * 10);
        altitude = String.format("%,5d", altitudeNearestTen);
      }

      // Draw speed.
      PANEL_DIGITS_PAINT.setTextAlign(Paint.Align.LEFT);
      c.drawText(speed, PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE, PANEL_DIGITS_PAINT);
      int textWidth = getTextWidth(speed, PANEL_DIGITS_PAINT);
      PANEL_UNITS_PAINT.setTextAlign(Paint.Align.LEFT);
      c.drawText(speedUnits, textWidth + PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE, PANEL_UNITS_PAINT);

      // Draw track.
      final float center = width / 2.0f;
      PANEL_DIGITS_PAINT.setTextAlign(Paint.Align.CENTER);
      c.drawText(track, center, PANEL_TEXT_BASELINE, PANEL_DIGITS_PAINT);

      // Draw altitude. Draw the units first, since it's right-aligned.
      PANEL_UNITS_PAINT.setTextAlign(Paint.Align.RIGHT);
      c.drawText(" ft", width - PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE, PANEL_UNITS_PAINT);
      textWidth = getTextWidth(" ft", PANEL_UNITS_PAINT);
      PANEL_DIGITS_PAINT.setTextAlign(Paint.Align.RIGHT);
      c.drawText(altitude, width - textWidth - PANEL_TEXT_MARGIN, PANEL_TEXT_BASELINE,
          PANEL_DIGITS_PAINT);
    }
  }

  /**
   * Returns true if location is different than {@link #previousLocation}
   * (ignoring fields that don't affect the rendering).
   */
  private synchronized boolean hasMoved(Location location) {
    if (null == location || null == previousLocation
        || location.getBearing() != previousLocation.getBearing()
        || location.getAltitude() != previousLocation.getAltitude()
        || location.getSpeed() != previousLocation.getSpeed()
        || location.distanceTo(previousLocation) > 5) {
      return true;
    }
    return false;
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
   * Return the appropriate paint based on whether the airport is towered or
   * not.
   */
  private Paint getAirportPaint(Airport airport) {
    return airport.isTowered ? toweredPaint : nonToweredPaint;
  }

  /**
   * Returns width in pixels of {@code text} drawn with {@code paint}.
   */
  private synchronized int getTextWidth(String text, Paint paint) {
    paint.getTextBounds(text, 0, text.length(), textBounds);
    return textBounds.right; // origin is always (0, 0).
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

  /**
   * Returns a rectangle enclosing the current view.
   * 
   * @param zoom zoomlevel
   * @param orientation bearing in degrees from {@code locationPoint} to the top
   *        center of the screen. Will be 0 when north-up, and the current track
   *        when track-up.
   * @param locationPoint pixel coordinates of current location (as returned by
   *        {@link AndroidMercatorProjection#toPoint}
   */
  private synchronized LatLngRect getScreenRectangle(final float zoom, final float orientation,
      final Point locationPoint) {
    // Make rectangle that encloses the 4 screen corners.
    LatLngRect result = new LatLngRect();
    for (int i = 0; i < 4; i++) {
      result.add(getLocationForPoint(zoom, orientation, locationPoint, screenCorners[i]));
    }
    return result;
  }

  /**
   * Returns ground position corresponding to {@code screenPoint}. Uses {@code
   * previousLocation} to set location and orientation and calls
   * {@link #getLocationForPoint(float, float, Point, Point)}.
   * 
   * @param screenPoint coordinates in screen pixel space (such as from a touch
   *        event).
   * @return ground position, or null if {@code previousLocation} is null.
   */
  private synchronized LatLng getLocationForPoint(Point screenPoint) {
    if (null == previousLocation) {
      return null;
    }
    final LatLng location =
        LatLng.fromDouble(previousLocation.getLatitude(), previousLocation.getLongitude());
    final Point locationPoint = AndroidMercatorProjection.toPoint(getZoom(), location);
    final float orientation =
        mainActivity.userPrefs.isNorthUp() ? 0 : previousLocation.getBearing();
    return getLocationForPoint(getZoom(), orientation, locationPoint, screenPoint);
  }

  /**
   * Returns ground position corresponding to {@code screenPoint}.
   * 
   * @param zoom zoom level.
   * @param orientation bearing in degrees from {@code locationPoint} to the top
   *        center of the screen. Will be 0 when north-up, and the current track
   *        when track-up.
   * @param locationPoint pixel coordinates of current location (in Mercator
   *        pixel space as returned by {@link AndroidMercatorProjection#toPoint}
   * @param screenPoint coordinates in screen pixel space (such as from a touch
   *        event).
   */
  private synchronized LatLng getLocationForPoint(final float zoom, final float orientation,
      final Point locationPoint, final Point screenPoint) {
    // The operations on screenMatrix are the opposite of the ones applied to
    // the canvas in drawMapOnCanvas().
    //
    // drawMapOnCanvas() does the following:
    // c.translate(aircraftX, aircraftY);
    // if (isTrackUp) c.rotate(360 - bearing);
    // c.translate(-locationPoint.x, -locationPoint.y);
    screenMatrix.reset();
    screenMatrix.postTranslate(-aircraftX, -aircraftY);
    if (!mainActivity.userPrefs.isNorthUp()) {
      screenMatrix.postRotate(orientation);
    }
    screenMatrix.postTranslate(locationPoint.x, locationPoint.y);
    screenPoints[0] = screenPoint.x;
    screenPoints[1] = screenPoint.y;
    screenMatrix.mapPoints(screenPoints);
    mercatorPoint.x = Math.round(screenPoints[0]);
    mercatorPoint.y = Math.round(screenPoints[1]);
    LatLng result = AndroidMercatorProjection.fromPoint(zoom, mercatorPoint);
    return result;
  }

  /**
   * Saves map-specific state info to {@code outState}.
   */
  public void saveInstanceState(Bundle outState) {
    outState.putFloat(ZOOM_LEVEL, getZoom());
  }

  /**
   * Restores map-specific state info from {@code savedInstanceState}
   */
  public void restoreInstanceState(Bundle savedInstanceState) {
    if (null == savedInstanceState || !savedInstanceState.containsKey(ZOOM_LEVEL)) {
      return;
    }
    setZoom(savedInstanceState.getFloat(ZOOM_LEVEL));
  }

  private synchronized void updateAirportsOnScreen(LatLngRect screenArea, int minimumAirportRank) {
    // Is there a query in progress?
    if (getAirportsTask != null && getAirportsTask.isQueryInProgress()) {
      if (mainActivity.airportDirectory.isCacheMatch(screenArea, minimumAirportRank)) {
        // The query in progress will give the same results as passing
        // (screenArea, minRank) to a new task.
        Log.i(TAG, "updateAirportsOnScreen: Still waiting on query");
        return;
      }
      // Cancel the in progress task. It's working on an answer we no longer
      // need.
      Log.i(TAG, "updateAirportsOnScreen: Cancel query in progress. " + getAirportsTask);
      boolean cancelled = getAirportsTask.cancel(true);
      if (!cancelled) {
        Log.w(TAG, "updateAirportsOnScreen: FAILED to cancel query.");
      }
    }
    // Have to make a new task here. Can't call execute again on an active task.
    getAirportsTask = new GetAirportsInRectangleTask(mainActivity.airportDirectory, this);
    getAirportsTask.execute(new GetAirportsInRectangleTask.QueryParams(screenArea,
        minimumAirportRank));
  }

  /**
   * {@inheritDoc} Called when {@link GetAirportsInRectangleTask} completes.
   */
  @Override
  public synchronized void hasCompleted(boolean success) {
    Log.d(TAG, "hasCompleted: started, success: " + success);
    if (success && getAirportsTask != null) {
      try {
        airportsOnScreen = getAirportsTask.get();
        setRedrawNeeded(true); 
      } catch (InterruptedException e) {
        Log.i(TAG, "Interrupted while getting airports on screen", e);
      } catch (ExecutionException e) {
        Log.w(TAG, "Execution failed getting airports on screen", e);
      }
    }
  }

  @Override
  public void hasProgressed(int unused) {
    // GetAirportsInRectangleTask does not give intermediate progress.
  }
}

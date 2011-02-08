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

import modified.android.view.ScaleGestureDetector;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
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

import com.google.flightmap.android.MainActivity;
import com.google.flightmap.android.R;
import com.google.flightmap.android.graphics.AirportPalette;
import com.google.flightmap.android.graphics.AirspacePalette;
import com.google.flightmap.android.graphics.ZoomScale;
import com.google.flightmap.android.location.LocationHandler;
import com.google.flightmap.android.location.LocationHandler.Source;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.Airspace;
import com.google.flightmap.common.geo.NavigationUtil;
import com.google.flightmap.common.geo.NavigationUtil.DistanceUnits;

/**
 * View for the moving map (as in Model-View-Presenter). Only basic UI actions
 * are done here. Anything requiring non-trival logic should go in MapPresenter.
 * <p>
 * Magnetic variation does not affect how the map is drawn. It only matters when
 * displaying the numeric track value on the top panel.
 * 
 * @see MapModel
 * @see MapPresenter
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback {
  private static final String TAG = MapView.class.getSimpleName();
  public static final String DEGREES_SYMBOL = "\u00b0";

  // Paints.
  private static final Paint ERROR_TEXT_PAINT = new Paint();
  static final Paint AIRPORT_TEXT_PAINT = new Paint();
  private static final Paint PANEL_BACKGROUND_PAINT = new Paint();
  private static final Paint PANEL_DIGITS_PAINT = new Paint();
  private static final Paint PANEL_UNITS_PAINT = new Paint();
  private static final Paint LAST_POSITION_PAINT = new Paint();
  public static final Paint LOST_GPS_PAINT = new Paint();
  public static final Paint AIRPLANE_SOLID_PAINT = new Paint();
  public static final Paint AIRPLANE_OUTLINE_STROKE_PAINT = new Paint();
  public static final Paint AIRPLANE_OUTLINE_FILL_PAINT = new Paint();
  static final Paint PAN_SOLID_PAINT = new Paint();
  static final Paint PAN_DASH_PAINT = new Paint();
  static final Paint PAN_INFO_PAINT = new Paint();
  private static final Paint PAN_RESET_PAINT = new Paint();
  public static final Paint RED_SLASH_PAINT = new Paint();
  private static boolean textSizesSet;
  private final AirportPalette airportPalette;
  private final AirspacePalette airspacePalette = new AirspacePalette();

  // Zoom items.
  private ZoomButtonsController zoomController;

  // Top panel items.
  private static final float PANEL_HEIGHT = 60;
  private static final float PANEL_TEXT_MARGIN = 10;
  private static final float PANEL_NOTCH_HEIGHT = 15;
  private static final float PANEL_NOTCH_WIDTH = 10;
  private static final float PANEL_TEXT_BASELINE =
      PANEL_HEIGHT - PANEL_NOTCH_HEIGHT - PANEL_TEXT_MARGIN;

  // Rectangle with a notch that's the background for the top panel area.
  private Path topPanel;

  // Main activity.
  private final MainActivity mainActivity;

  // Fields relating to touch events and panning.
  private static final int INVALID_POINTER_ID = -1;
  private int activePointerId = INVALID_POINTER_ID;
  private final ScaleGestureDetector scaleDetector;
  private final PanResetButton panResetButton;

  // Underlying surface for this view.
  private volatile SurfaceHolder holder;

  // Rect used to get text width. Create so we don't new one for each frame.
  private final Rect textBounds = new Rect();

  // Airplane image.
  final Path airplanePath;
  final private RectF airplanePathBounds;

  // Layout holding the simulator message.
  private LinearLayout simulatorMessage;

  // Screen density.
  final float density;

  // Graphical zoom scale.
  private final ZoomScale zoomScale;

  // Caching. Values from the last time the map was drawn.
  private Location previousLocation;
  private float previousZoom;

  private final MapPresenter presenter;
  private final MapModel model;

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
    LAST_POSITION_PAINT.setAntiAlias(true);
    LAST_POSITION_PAINT.setColor(Color.GREEN);
    LAST_POSITION_PAINT.setTypeface(Typeface.SANS_SERIF);
    LAST_POSITION_PAINT.setTextAlign(Paint.Align.CENTER);
    LOST_GPS_PAINT.setAntiAlias(true);
    LOST_GPS_PAINT.setColor(Color.GREEN);
    LOST_GPS_PAINT.setTypeface(Typeface.SANS_SERIF);
    LOST_GPS_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    LOST_GPS_PAINT.setTextAlign(Paint.Align.CENTER);
    PANEL_BACKGROUND_PAINT.setARGB(0xee, 0x22, 0x22, 0x22);
    PANEL_DIGITS_PAINT.setAntiAlias(true);
    PANEL_DIGITS_PAINT.setColor(Color.WHITE);
    PANEL_DIGITS_PAINT.setTypeface(Typeface.SANS_SERIF);
    PANEL_DIGITS_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    PANEL_UNITS_PAINT.setAntiAlias(true);
    PANEL_UNITS_PAINT.setARGB(0xff, 0x99, 0x99, 0x99);
    PANEL_UNITS_PAINT.setTypeface(Typeface.SANS_SERIF);
    PAN_RESET_PAINT.setAntiAlias(true);
    PAN_RESET_PAINT.setColor(Color.BLACK);
    PAN_RESET_PAINT.setTypeface(Typeface.DEFAULT_BOLD);
    PAN_RESET_PAINT.setTextAlign(Paint.Align.CENTER);
    PAN_INFO_PAINT.setAntiAlias(true);
    PAN_INFO_PAINT.setColor(Color.GREEN);
    PAN_INFO_PAINT.setTypeface(Typeface.SANS_SERIF);
    PAN_INFO_PAINT.setTextAlign(Paint.Align.CENTER);
    RED_SLASH_PAINT.setColor(Color.RED);
    RED_SLASH_PAINT.setStrokeWidth(3);
    RED_SLASH_PAINT.setAntiAlias(true);
  }

  public MapView(MainActivity mainActivity) {
    super(mainActivity);
    this.mainActivity = mainActivity;
    this.presenter = new MapPresenter(this, mainActivity);
    this.model = presenter.getModel();
    this.density = mainActivity.getResources().getDisplayMetrics().density;
    this.zoomScale = new ZoomScale(density, mainActivity.getUserPrefs());
    getHolder().addCallback(this);
    setFocusable(true); // make sure we get key events
    setKeepScreenOn(true);
    createZoomController();
    setTextSizes(density);

    simulatorMessage = (LinearLayout) mainActivity.findViewById(R.id.simulator_message);

    Resources res = mainActivity.getResources();
    airportPalette = new AirportPalette(res);
    // Set up paints from resource colors.
    AIRPLANE_SOLID_PAINT.setColor(res.getColor(R.color.AircraftPaint));
    AIRPLANE_SOLID_PAINT.setAntiAlias(true);
    AIRPLANE_SOLID_PAINT.setStyle(Paint.Style.FILL);
    AIRPLANE_OUTLINE_FILL_PAINT.setColor(res.getColor(R.color.MapBackground));
    AIRPLANE_OUTLINE_FILL_PAINT.setAntiAlias(true);
    AIRPLANE_OUTLINE_FILL_PAINT.setStyle(Paint.Style.FILL);
    AIRPLANE_OUTLINE_STROKE_PAINT.setColor(res.getColor(R.color.AircraftPaint));
    AIRPLANE_OUTLINE_STROKE_PAINT.setAntiAlias(true);
    AIRPLANE_OUTLINE_STROKE_PAINT.setStrokeWidth(1.5f);
    AIRPLANE_OUTLINE_STROKE_PAINT.setStyle(Paint.Style.STROKE);
    PAN_SOLID_PAINT.setColor(res.getColor(R.color.PanItems));
    PAN_SOLID_PAINT.setAntiAlias(true);
    PAN_SOLID_PAINT.setStrokeWidth(5);
    PAN_DASH_PAINT.setColor(res.getColor(R.color.PanItems));
    PAN_DASH_PAINT.setAntiAlias(true);
    PAN_DASH_PAINT.setStrokeWidth(2);
    float[] intervals = {30, 10};
    PAN_DASH_PAINT.setPathEffect(new DashPathEffect(intervals, 0));
    panResetButton = new PanResetButton();

    // Set up airplane image.
    airplanePath = createAirplanePath();
    airplanePathBounds = new RectF();
    airplanePath.computeBounds(airplanePathBounds, false);

    // Set up scale gesture detector.
    scaleDetector = new ScaleGestureDetector(mainActivity, new ScaleListener());
  }

  public static Path createAirplanePath() {
    Path result = new Path();
    float points[][] =
        { {22, 0}, {25, 0}, {27, 9}, {46, 11}, {46, 16}, {27, 19}, {26, 31}, {31, 33}, {31, 35},
            {24, 37}, {23, 37}, {16, 35}, {16, 33}, {21, 31}, {20, 19}, {0, 16}, {0, 11}, {20, 9}};
    boolean isFirstPoint = true;
    for (float[] point : points) {
      float x = point[0];
      float y = point[1];
      if (isFirstPoint) {
        isFirstPoint = false;
        result.moveTo(x, y);
      } else {
        result.lineTo(x, y);
      }
    }
    result.close();
    // Set the origin to the center of the bounding rectangle.
    RectF bounds = new RectF();
    result.computeBounds(bounds, false);
    result.offset(-bounds.centerX(), -bounds.centerY());
    return result;
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
    AIRPORT_TEXT_PAINT.setTextSize(19 * density);
    ERROR_TEXT_PAINT.setTextSize(15 * density);
    LAST_POSITION_PAINT.setTextSize(16 * density);
    LOST_GPS_PAINT.setTextSize(26 * density);
    PANEL_DIGITS_PAINT.setTextSize(26 * density);
    PANEL_UNITS_PAINT.setTextSize(18 * density);
    PAN_RESET_PAINT.setTextSize(18 * density);
    PAN_INFO_PAINT.setTextSize(18 * density);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    // Let the ScaleGestureDetector inspect all events.
    scaleDetector.onTouchEvent(event);

    final int action = event.getAction();
    switch (action & MotionEvent.ACTION_MASK) {
      case MotionEvent.ACTION_DOWN: {
        activePointerId = event.getPointerId(0);
        return presenter.actionDown(event.getX(), event.getY());
      }

      case MotionEvent.ACTION_UP: {
        return presenter.actionUp((int) (event.getX() + 0.5), (int) (event.getY() + 0.5));
      }

      case MotionEvent.ACTION_MOVE: {
        float x;
        float y;
        if (scaleDetector.isInProgress()) {
          return presenter.actionMoveWhileScaling(scaleDetector.getFocusX(), scaleDetector
              .getFocusY());

        } else {
          int pointerIndex = event.findPointerIndex(activePointerId);
          x = event.getX(pointerIndex);
          y = event.getY(pointerIndex);
          return presenter.actionMove(x, y);
        }
      }

      case MotionEvent.ACTION_CANCEL: {
        activePointerId = INVALID_POINTER_ID;
        return presenter.actionCancel();
      }

      case MotionEvent.ACTION_POINTER_UP: {
        // There were multiple pointers down, and one of them went up.
        final int pointerId = (event.getAction() & MotionEvent.ACTION_POINTER_ID_MASK) //
            >> MotionEvent.ACTION_POINTER_ID_SHIFT;
        if (pointerId == activePointerId) {
          // The pointer we were tracking went up. Pick a new one.
          int pointerIndex = event.findPointerIndex(pointerId);
          final int newIndex = pointerIndex == 0 ? 1 : 0;
          activePointerId = event.getPointerId(newIndex);
          return presenter.actionPointerUp(event.getX(newIndex), event.getY(newIndex));
        }
      }
    }
    return false;
  }

  synchronized void showZoomController() {
    if (null != zoomController) {
      zoomController.setVisible(true);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    if (null != zoomController) {
      zoomController.setVisible(false);
    }
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
    if (model.canvasSizeEquals(width, height)) {
      return;
    }
    presenter.setCanvasSize(width, height);
    panResetButton.canvasSizeChanged(width, height);
    createTopPanelPath(width);
  }

  @Override
  public synchronized void surfaceCreated(SurfaceHolder holder) {
    this.holder = holder;
    model.setRedrawNeeded(true);
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    this.holder = null;
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
        model.zoomStep(zoomIn);
        zoomController.setZoomInEnabled(model.getZoom() < MapModel.MAX_ZOOM);
        zoomController.setZoomOutEnabled(model.getZoom() > MapModel.MIN_ZOOM);
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
        final LocationHandler locationHandler = mainActivity.getFlightMap().getLocationHandler();
        // Special case actions for the simulator.
        if (locationHandler.isLocationSimulated()) {
          if (!mainActivity.getUserPrefs().enableSimulator()) {
            Log.i(TAG, "Stopping simulator due to user preference.");
            locationHandler.setLocationSource(Source.REAL);
            location = locationHandler.getLocation();
          } else if (location == null) {
            // Could happen if the simulator enabled when there was no real
            // location.
            location = locationHandler.getLocation();
          }
        }

        if (!hasMoved(location) && !model.isRedrawNeeded() && model.getZoom() == previousZoom) {
          return;
        }
        if (model.isRedrawNeeded()) {
          model.setRedrawNeeded(false);
        }
        previousLocation = location;
        previousZoom = model.getZoom();
      }
      c = holder.lockCanvas(null);
      synchronized (holder) {
        presenter.drawMapOnCanvas(c, location);
      }
    } finally {
      if (c != null) {
        holder.unlockCanvasAndPost(c);
      }
    }
  }

  void drawFixedLocationItems(Canvas c, Location location, float zoom) {
    // Draw reset panning button.
    panResetButton.draw(c);

    zoomScale.drawScale(c, location, zoom);

    // Polygon for top params display.
    drawTopPanel(c, location);
  }

  void drawAirplaneImage(Canvas c, Paint paint) {
    c.drawPath(airplanePath, paint);
  }

  void drawLastKnownPosition(Canvas c) {
    float slashLength = Math.min(airplanePathBounds.width(), airplanePathBounds.height()) * 0.7f;
    c.drawLine(-slashLength, -slashLength, slashLength, slashLength, MapView.RED_SLASH_PAINT);
    int offset = drawLastKnowTextLabel(c, "LAST KNOWN", 0);
    drawLastKnowTextLabel(c, "POSITION", offset);
  }

  private int drawLastKnowTextLabel(Canvas c, String text, int offset) {
    synchronized (textBounds) {
      LAST_POSITION_PAINT.getTextBounds(text, 0, text.length(), textBounds);
      // Add a bit of padding to the bounds.
      int padding = 5;
      textBounds.set(textBounds.left - padding, textBounds.top - padding, textBounds.right
          + padding, textBounds.bottom + padding);
      // Offset textBounds down to the baseline and left to center the text.
      textBounds.offset(-textBounds.width() / 2,
          (int) (airplanePathBounds.height() + padding + 15 + offset));
      // Erase background of text.
      c.drawRect(textBounds, AIRPLANE_OUTLINE_FILL_PAINT);
      c.drawText(text, 0, textBounds.bottom - padding, LAST_POSITION_PAINT);
      return textBounds.height() + offset;
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
   * Returns width in pixels of {@code text} drawn with {@code paint}.
   */
  private synchronized int getTextWidth(String text, Paint paint) {
    paint.getTextBounds(text, 0, text.length(), textBounds);
    return textBounds.right; // origin is always (0, 0).
  }

  /**
   * Returns true if the coordinates hit the pan reset button.
   */
  boolean isPanResetButtonHit(int x, int y) {
    return panResetButton.isInsideButton(x, y);
  }

  /**
   * Custom button to reset from panning.
   */
  private class PanResetButton {
    private static final String RESET_LABEL = "Reset";
    private static final int BOTTOM_MARGIN = 10;
    private static final float TEXT_MARGIN = 10;
    private int top;
    private int bottom;
    private int left;
    private int right;
    private int center;
    private Rect rect = new Rect();

    /**
     * Draws the button on the given canvas. Does nothing if not panning.
     */
    private synchronized void draw(Canvas c) {
      if (!model.isPanning()) {
        return;
      }
      c.drawRect(rect, PAN_SOLID_PAINT);
      c.drawText(RESET_LABEL, center, bottom - TEXT_MARGIN * density, PAN_RESET_PAINT);
    }

    /**
     * Returns true if the given screen coordinates intersect the button.
     * {@link MapView#TOUCH_PIXEL_RADIUS} is applied.
     */
    private synchronized boolean isInsideButton(int x, int y) {
      final int radius = MapPresenter.TOUCH_PIXEL_RADIUS;
      return rect.intersects(x - radius, y - radius, x + radius, y + radius);
    }

    private synchronized void canvasSizeChanged(int width, int height) {
      Rect textBounds = new Rect();
      PAN_RESET_PAINT.getTextBounds(RESET_LABEL, 0, RESET_LABEL.length(), textBounds);

      bottom = (int) (height - (BOTTOM_MARGIN * density) + 0.5);
      top = (int) (bottom - (textBounds.height() + TEXT_MARGIN) * density - 0.5);
      center = (int) ((width / 2.0f) + 0.5);
      float halfWidth = textBounds.width() / 2.0f + TEXT_MARGIN * density;
      left = (int) ((center - halfWidth) - 0.5);
      right = (int) ((center + halfWidth) + 0.5);
      rect.set(left, top, right, bottom);
    }
  }

  /**
   * Listens for scale gesture events.
   */
  private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      presenter.onScale(detector.getScaleFactor());
      return true;
    }
  }

  void showNoGpsLocationMessage(Canvas c) {
    c.drawText(mainActivity.getText(R.string.old_location).toString(), c.getWidth() / 2, //
        c.getHeight() / 2, ERROR_TEXT_PAINT);
    simulatorMessage.setVisibility(GONE);
  }

  void showSimulatorWarning(boolean showMessage) {
    simulatorMessage.setVisibility(showMessage ? VISIBLE : GONE);
  }

  void drawTopPanel(Canvas c, Location location) {
    if (null == topPanel) {
      return;
    }
    c.drawPath(topPanel, PANEL_BACKGROUND_PAINT);
    // Show the lost GPS signal message if needed.
    final LocationHandler locationHandler = mainActivity.getFlightMap().getLocationHandler();
    if (!locationHandler.isLocationCurrent()) {
      drawNoGpsMessage(c, location);
      return;
    }

    String speed = "-";
    String track = "-" + DEGREES_SYMBOL;
    String altitude = "-";
    DistanceUnits distanceUnits = mainActivity.getUserPrefs().getDistanceUnits();
    String speedUnits = " " + distanceUnits.speedAbbreviation;

    if (location.hasSpeed()) {
      speed = String.format("%.0f", distanceUnits.getSpeed(location.getSpeed()));
    }
    if (location.hasBearing()) {
      // Show numeric display relative to magnetic north.
      float magneticTrack = presenter.getMagneticTrack(location);
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
    final int width = c.getWidth();
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

  private void drawNoGpsMessage(Canvas c, Location location) {
    long locationAgeSeconds = (System.currentTimeMillis() - location.getTime()) / 1000;
    String age = NavigationUtil.getHoursMinutesSeconds(locationAgeSeconds);
    final float center = c.getWidth() / 2.0f;
    c.drawText("No GPS signal \u2022 " + age, center, PANEL_TEXT_BASELINE, LOST_GPS_PAINT);
  }


  Paint getAirportPaint(Airport airport) {
    return airportPalette.getPaint(airport);
  }

  Paint getAirspacePaint(Airspace airspace, Location l) {
    return airspacePalette.getPaint(airspace, l);
  }

  /**
   * Helper method for {@link MainActivity#onSaveInstanceState(Bundle)}.
   */
  public void saveInstanceState(Bundle outState) {
    model.saveInstanceState(outState);
  }

  /**
   * Restores information stored by {@link #saveInstanceState(Bundle)}.
   * 
   * @param savedInstanceState
   */
  public void restoreInstanceState(Bundle savedInstanceState) {
    model.restoreInstanceState(savedInstanceState);
  }

  /**
   * Helper method for {@link MainActivity#onDestroy}.
   */
  public void destroy() {
    presenter.destroy();
  }
}

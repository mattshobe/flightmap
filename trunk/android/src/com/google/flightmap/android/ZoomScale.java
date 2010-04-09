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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.location.Location;

/**
 * Draws the graphical zoom scale. The size of the scale bar changes slightly to
 * make it match the scale shown. The distances shown will start with 1, 2, or 5
 * followed by all zeros.
 */
public class ZoomScale {
  /** Ideally how wide the scale bar should be in absolute pixels. */
  private static final int PREFERRED_WIDTH = 50;
  /** Height of the lines on the ends */
  private static final int HEIGHT = 15;
  /** Number of pixels down from the top of the screen to draw the scale. */
  private static final int TOP_OFFSET = (int) (50 + MapView.PANEL_HEIGHT);
  /** Number of pixels left from the right of the screen to draw the right edge. */
  private static final int RIGHT_OFFSET = 50;

  // Used to draw scale graphic.
  private static final Path SCALE_PATH = new Path();

  // Paints.
  private static final Paint LINE_PAINT = new Paint();
  private static final Paint TEXT_PAINT = new Paint();

  static {
    LINE_PAINT.setAntiAlias(true);
    LINE_PAINT.setColor(Color.WHITE);
    LINE_PAINT.setStyle(Style.STROKE);
    TEXT_PAINT.setAntiAlias(true);
    TEXT_PAINT.setColor(Color.WHITE);
    TEXT_PAINT.setTextAlign(Align.CENTER);
  }

  private ZoomScale() {
    // Utility class.
  }

  /**
   * Draws zoom scale.
   * 
   * @param c canvas to draw on.
   * @param l current location.
   * @param density screen density.
   */
  public static void drawScale(Canvas c, Location l, float density) {
    TEXT_PAINT.setTextSize(15 * density);
    LINE_PAINT.setStrokeWidth(2.5f * density);
    final int width = c.getWidth();
    final float rightX = width - RIGHT_OFFSET;
    final float leftX = width - RIGHT_OFFSET - PREFERRED_WIDTH * density;
    final float centerX = (leftX + rightX) / 2.0f;
    final float topY = TOP_OFFSET;
    final float bottomY = topY + HEIGHT;

    SCALE_PATH.rewind();
    SCALE_PATH.moveTo(leftX, bottomY);
    SCALE_PATH.lineTo(leftX, topY);
    SCALE_PATH.lineTo(rightX, topY);
    SCALE_PATH.lineTo(rightX, bottomY);
    c.drawPath(SCALE_PATH, LINE_PAINT);
    c.drawText("42 nm", centerX, topY - 5, TEXT_PAINT);
  }
}

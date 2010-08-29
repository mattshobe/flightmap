// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.flightmap.android;

import android.graphics.Color;
import android.graphics.Paint;

/**
 * Constants such as paints and fonts shared across multiple UI screens.
 */
class UiConstants {
  public static final String DEGREES_SYMBOL = "\u00b0";
  static final Paint AIRCRAFT_PAINT = new Paint();
  static final Paint NON_TOWERED_PAINT = new Paint();
  static final Paint TOWERED_PAINT = new Paint();

  // Static initialization.
  static {
    // Do not put any calls to setTextSize here. Put them in #setTextSizes().
    TOWERED_PAINT.setAntiAlias(true);
    TOWERED_PAINT.setARGB(0xff, 0x0, 0xcc, 0xff);
    TOWERED_PAINT.setTextAlign(Paint.Align.CENTER);
    NON_TOWERED_PAINT.setAntiAlias(true);
    NON_TOWERED_PAINT.setARGB(0xff, 0xcc, 0x33, 0xcc);
    NON_TOWERED_PAINT.setTextAlign(Paint.Align.CENTER);
    AIRCRAFT_PAINT.setColor(Color.GREEN);
    AIRCRAFT_PAINT.setStrokeWidth(3);
  }

  private UiConstants() {
  }
}

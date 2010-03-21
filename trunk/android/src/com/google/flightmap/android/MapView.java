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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * View for the moving map.
 */
public class MapView extends SurfaceView implements SurfaceHolder.Callback {
  private static final String TAG = MapView.class.getSimpleName();
  private static final Paint YELLOW_PAINT = new Paint();

  private boolean active;
  private int width;
  private int height;
  
  // temp variables to bounce the circle
  int x = 200;
  int y = 300;
  int dx = 5;
  int dy = -3;
  
  static {
    YELLOW_PAINT.setAntiAlias(true);
    YELLOW_PAINT.setARGB(180, 255, 255, 0);
  }

  public MapView(Context context) {
    super(context);
    getHolder().addCallback(this);
    setFocusable(true); // make sure we get key events
  }

  /**
   * Surface dimensions changed.
   */
  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    Log.i(TAG, String.format("format=%d w=%d h=%d", format, width, height));
    this.width = width;
    this.height = height;
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    Log.i(TAG, "surfaceCreated");
    setActive(true);
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {
    Log.i(TAG, "surfaceDestroyed");
    setActive(false);
  }

  public void drawMap(Location location) {
    if (!isActive()) {
      Log.i(TAG, "Not active");
      return;
    }
    SurfaceHolder holder = null;
    Canvas c = null;
    try {
      holder = getHolder();
      if (null == holder) {
        Log.w(TAG, "Null holder");
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

  private void drawMapOnCanvas(Canvas c, Location location) {
    if (null == c) {
      Log.i(TAG, "null canvas");
      return;
    }
    Log.i(TAG, "Map update");
    c.drawColor(Color.GREEN);
    c.drawCircle(x, y, 50, YELLOW_PAINT);
    x += dx;
    y += dy;
    if (x <= 0 || x >= width) {
      dx *= -1;
    }
    if (y <= 0 || y >= height) {
      dy *= -1;
    }
//    if (null == location) {
//      c.drawText("No location", 20, 600, WHITE_PAINT);
//    }
  }

  private synchronized boolean isActive() {
    return active;
  }

  private synchronized void setActive(boolean active) {
    this.active = active;
  }
}

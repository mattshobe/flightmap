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
package com.google.flightmap.android.graphics;

import com.google.flightmap.common.data.Airspace;

import android.graphics.Color;
import android.graphics.Paint;

public class AirspacePalette implements Palette<Airspace> {
  private final static Paint BRAVO_AIRSPACE_PAINT;
  private final static Paint CHARLIE_AIRSPACE_PAINT;
  private final static Paint DELTA_AIRSPACE_PAINT;
  private final static Paint OTHER_AIRSPACE_PAINT;
  static {
    BRAVO_AIRSPACE_PAINT = new Paint();
    BRAVO_AIRSPACE_PAINT.setColor(Color.BLUE);
    BRAVO_AIRSPACE_PAINT.setAntiAlias(true);
    BRAVO_AIRSPACE_PAINT.setStyle(Paint.Style.STROKE);
    BRAVO_AIRSPACE_PAINT.setStrokeWidth(6);
    BRAVO_AIRSPACE_PAINT.setAlpha(140);

    CHARLIE_AIRSPACE_PAINT = new Paint(BRAVO_AIRSPACE_PAINT);
    CHARLIE_AIRSPACE_PAINT.setColor(Color.RED);

    DELTA_AIRSPACE_PAINT = new Paint(BRAVO_AIRSPACE_PAINT);
    DELTA_AIRSPACE_PAINT.setColor(Color.CYAN);

    OTHER_AIRSPACE_PAINT = new Paint(BRAVO_AIRSPACE_PAINT);
    OTHER_AIRSPACE_PAINT.setColor(Color.WHITE);
  }

  @Override
  public Paint getPaint(final Airspace item) {
    switch (item.airspaceClass) {
      case BRAVO:
        return BRAVO_AIRSPACE_PAINT;
      case CHARLIE:
        return CHARLIE_AIRSPACE_PAINT;
      case DELTA:
        return DELTA_AIRSPACE_PAINT;
      default:
        return OTHER_AIRSPACE_PAINT;
    }
  }
}

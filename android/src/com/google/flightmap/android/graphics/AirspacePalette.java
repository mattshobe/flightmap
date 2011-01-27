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
import com.google.flightmap.common.geo.NavigationUtil;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.location.Location;

public class AirspacePalette implements Palette<Airspace> {
  private final static Paint BRAVO_AIRSPACE_PAINT;
  private final static Paint CHARLIE_AIRSPACE_PAINT;
  private final static Paint DELTA_AIRSPACE_PAINT;
  private final static Paint OTHER_AIRSPACE_PAINT;
  private final static Paint BRAVO_FAR_AIRSPACE_PAINT;
  private final static Paint CHARLIE_FAR_AIRSPACE_PAINT;
  private final static Paint DELTA_FAR_AIRSPACE_PAINT;
  private final static Paint OTHER_FAR_AIRSPACE_PAINT;
  static {
    BRAVO_AIRSPACE_PAINT = new Paint();
    BRAVO_AIRSPACE_PAINT.setARGB(255, 00, 204, 255);
    BRAVO_AIRSPACE_PAINT.setAntiAlias(true);
    BRAVO_AIRSPACE_PAINT.setStyle(Paint.Style.STROKE);
    BRAVO_AIRSPACE_PAINT.setStrokeWidth(2);
    BRAVO_FAR_AIRSPACE_PAINT = new Paint(BRAVO_AIRSPACE_PAINT);
    BRAVO_FAR_AIRSPACE_PAINT.setAlpha(127);

    CHARLIE_AIRSPACE_PAINT = new Paint(BRAVO_AIRSPACE_PAINT);
    CHARLIE_AIRSPACE_PAINT.setARGB(255, 204, 51, 102);
    CHARLIE_FAR_AIRSPACE_PAINT = new Paint(CHARLIE_AIRSPACE_PAINT);
    CHARLIE_FAR_AIRSPACE_PAINT.setAlpha(127);

    DELTA_AIRSPACE_PAINT = new Paint(BRAVO_AIRSPACE_PAINT);
    DELTA_AIRSPACE_PAINT.setPathEffect(new DashPathEffect(new float[]{15,5}, 0));
    DELTA_FAR_AIRSPACE_PAINT = new Paint(DELTA_AIRSPACE_PAINT);
    DELTA_FAR_AIRSPACE_PAINT.setAlpha(127);

    OTHER_AIRSPACE_PAINT = new Paint(BRAVO_AIRSPACE_PAINT);
    OTHER_AIRSPACE_PAINT.setColor(Color.WHITE);
    OTHER_FAR_AIRSPACE_PAINT = new Paint(OTHER_AIRSPACE_PAINT);
    OTHER_FAR_AIRSPACE_PAINT.setAlpha(127);
  }

  @Override
  public Paint getPaint(final Airspace airspace) {
    return getPaint(airspace, null);
  }

  public Paint getPaint(final Airspace airspace, final Location l) {
    boolean airspaceIsFar = false;
    if (l != null && l.hasAltitude()) {
      final int alt = (int) Math.round(l.getAltitude() * NavigationUtil.METERS_TO_FEET);
      airspaceIsFar = alt + 500 < airspace.bottom || alt - 500 > airspace.top;
    }

    switch (airspace.airspaceClass) {
      case BRAVO:
        return airspaceIsFar ? BRAVO_FAR_AIRSPACE_PAINT : BRAVO_AIRSPACE_PAINT;
      case CHARLIE:
        return airspaceIsFar ? CHARLIE_FAR_AIRSPACE_PAINT : CHARLIE_AIRSPACE_PAINT;
      case DELTA:
        return airspaceIsFar ? DELTA_FAR_AIRSPACE_PAINT : DELTA_AIRSPACE_PAINT;
      default:
        return airspaceIsFar ? OTHER_FAR_AIRSPACE_PAINT : OTHER_AIRSPACE_PAINT;
    }
  }
}

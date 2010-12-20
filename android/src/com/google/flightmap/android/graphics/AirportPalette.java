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

import com.google.flightmap.android.R;
import com.google.flightmap.common.data.Airport;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;

public class AirportPalette implements Palette<Airport> {
  private final Resources res;
  private final Paint toweredPaint;
  private final Paint nonToweredPaint;

  /**
   * Initializes airport paints using values specified in resources.
   */
  public AirportPalette(final Resources res) {
    this.res = res;

    toweredPaint = new Paint();
    toweredPaint.setColor(res.getColor(R.color.ToweredAirport));
    toweredPaint.setAntiAlias(true);

    nonToweredPaint = new Paint(toweredPaint);
    nonToweredPaint.setColor(res.getColor(R.color.NonToweredAirport));
  }

  /**
   * @InheritDoc
   *
   * Return the appropriate paint based on whether the airport is towered or
   * not.
   */
  public Paint getPaint(final Airport item) {
    return item.isTowered ? toweredPaint : nonToweredPaint;
  }
}

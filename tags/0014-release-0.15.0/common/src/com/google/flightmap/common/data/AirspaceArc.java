/* 
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.flightmap.common.data;


/**
 * Defines an arc for a segment of airspace.
 */
public class AirspaceArc {
  /**
   * Bounding box of circle from which this arc is drawn.
   */
  public final LatLngRect boundingBox;

  /**
   * Start angle of arc, measured in degrees clockwise from East.
   */
  public final float startAngle;

  /**
   * Sweep angle, in degrees (positive for clockwise, negative for counterclockwise).
   */
  public final float sweepAngle;

  public AirspaceArc(final LatLngRect boundingBox, final float startAngle, final float sweepAngle) {
    this.boundingBox = boundingBox;
    this.startAngle = startAngle;
    this.sweepAngle = sweepAngle;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\"");
    sb.append(boundingBox);
    sb.append("\",\"");
    sb.append(startAngle);
    sb.append("\",\"");
    sb.append(sweepAngle);
    sb.append("\"");
    return sb.toString();
  }
}

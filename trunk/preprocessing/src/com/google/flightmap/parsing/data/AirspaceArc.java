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

package com.google.flightmap.parsing.data;

import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

public class AirspaceArc {
  public final int seqNr;
  public final LatLng origin;
  public final double startAngle;
  public final double sweepAngle;
  public final LatLngRect boundingBox;

  public AirspaceArc(final int seqNr, final LatLng origin, final double startAngle,
      final double sweepAngle, final LatLngRect boundingBox) {
    this.seqNr = seqNr;
    this.origin = origin;
    this.startAngle = startAngle;
    this.sweepAngle = sweepAngle;
    this.boundingBox = boundingBox;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\"");
    sb.append(seqNr);
    sb.append("\",\"");
    sb.append(origin);
    sb.append("\",\"");
    sb.append(startAngle);
    sb.append("\",\"");
    sb.append(sweepAngle);
    sb.append("\",\"");
    sb.append(boundingBox);
    sb.append("\"");
    return sb.toString();
  }

}

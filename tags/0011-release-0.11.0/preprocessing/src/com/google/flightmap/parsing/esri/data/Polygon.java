/* 
 * Copyright (C) 2010 Google Inc.
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

package com.google.flightmap.parsing.esri.data;

public class Polygon {

  public static class Part {
    public final Point[] points;

    public Part(final Point[] points) {
      this.points = points;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("Part -");
      for (Point point: points) {
        sb.append("\n  ");
        sb.append(point);
      }
      return sb.toString();
    }
  }

  public final int recordNumber;
  public final double xMin;
  public final double yMin;
  public final double xMax;
  public final double yMax;
  public final Part[] parts;

  public Polygon(final int recordNumber, final double xMin, final double yMin, final double xMax,
      final double yMax, final Part[] parts) {
    this.recordNumber = recordNumber;
    this.xMin = xMin;
    this.yMin = yMin;
    this.xMax = xMax;
    this.yMax = yMax;
    this.parts = parts;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Polygon -");
    sb.append(" Record Number: ");
    sb.append(recordNumber);
    sb.append(" x: [");
    sb.append(xMin);
    sb.append(";");
    sb.append(xMax);
    sb.append("] y: [");
    sb.append(yMin);
    sb.append(";");
    sb.append(yMax);
    sb.append("]");
    sb.append(" Num parts: ");
    sb.append(parts.length);
    sb.append(" Parts:");
    for (Part part: parts) {
      sb.append("\n");
      sb.append(part);
    }
    return sb.toString();
  }
}

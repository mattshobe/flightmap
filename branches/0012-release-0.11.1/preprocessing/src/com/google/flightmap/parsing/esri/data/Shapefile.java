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

import java.util.Collection;
import java.util.LinkedList;

public class Shapefile {
  public final int fileCode;
  public final int length;
  public final int version;
  public final int shapeType;
  public final double xMin;
  public final double yMin;
  public final double xMax;
  public final double yMax;
  public final double zMin;
  public final double zMax;
  public final double mMin;
  public final double mMax;
  private final Collection<Polygon> polygons;

  public Shapefile(final int fileCode, final int length, final int version, final int shapeType,
      final double xMin, final double yMin, final double xMax, final double yMax,
      final double zMin, final double zMax, final double mMin, final double mMax) {
    this.fileCode = fileCode;
    this.length = length;
    this.version = version;
    this.shapeType = shapeType;
    this.xMin = xMin;
    this.yMin = yMin;
    this.xMax = xMax;
    this.yMax = yMax;
    this.zMin = zMin;
    this.zMax = zMax;
    this.mMin = mMin;
    this.mMax = mMax;
    polygons = new LinkedList<Polygon>();
  }

  public void addPolygon(final Polygon polygon) {
    polygons.add(polygon);
  }

  public Collection<Polygon> getPolygons() {
    return polygons;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Shapefile - Code: ");
    sb.append(fileCode);
    sb.append(" Length: ");
    sb.append(length);
    sb.append(" Version: ");
    sb.append(version);
    sb.append(" Shape Type: ");
    sb.append(shapeType);
    sb.append(" x: [");
    sb.append(xMin);
    sb.append(";");
    sb.append(xMax);
    sb.append("] y: [");
    sb.append(yMin);
    sb.append(";");
    sb.append(yMax);
    sb.append("] z: [");
    sb.append(zMin);
    sb.append(";");
    sb.append(zMax);
    sb.append("] m: [");
    sb.append(mMin);
    sb.append(";");
    sb.append(mMax);
    sb.append("]");
    sb.append(" Num polygons: ");
    sb.append(polygons.size());
    return sb.toString();
  }
}

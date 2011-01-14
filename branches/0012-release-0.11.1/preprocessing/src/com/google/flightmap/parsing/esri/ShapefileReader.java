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

package com.google.flightmap.parsing.esri;

import com.google.flightmap.common.io.StreamUtils;
import com.google.flightmap.parsing.esri.data.Point;
import com.google.flightmap.parsing.esri.data.Polygon;
import com.google.flightmap.parsing.esri.data.Shapefile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ShapefileReader {
  private final static int NULL_SHAPE = 0;
  private final static int POLYGON = 5;

  private final File shapefile;

  /**
   * Input buffer.  Initialized by calling {@link #open()}.
   */
  private ByteBuffer buf;

  public ShapefileReader(File shapefile) {
    this.shapefile = shapefile;
  }

  /**
   * Reads shape file data.  This method cannot be called twice.
   *
   * @return Contents of shape file
   */
  public synchronized Shapefile read() throws IOException {
    open();
    final Shapefile shapefile = readFileHeader();
    readRecords(shapefile);
    return shapefile;
  }

  /**
   * Loads contents of shapefile in memory and initializes input buffer.
   */
  private synchronized void open() throws IOException {
    final byte[] contents = StreamUtils.getBytes(shapefile);
    buf = ByteBuffer.wrap(contents);
  }

  /**
   * Reads shape file header and returns new Shapefile object.
   */
  private synchronized Shapefile readFileHeader() throws IOException {
    buf.order(ByteOrder.BIG_ENDIAN);
    final int fileCode = buf.getInt();
    for (int i = 0; i < 5; ++i) {
      final int unused = buf.getInt();
      if (unused != 0) {
        throw new RuntimeException("Unexpected unused value: " + unused +  " (should be 0).");
      }
    }
    final int length = buf.getInt();
    buf.order(ByteOrder.LITTLE_ENDIAN);
    final int version = buf.getInt();
    final int shapeType = buf.getInt();
    final double xMin = buf.getDouble();
    final double yMin = buf.getDouble();
    final double xMax = buf.getDouble();
    final double yMax = buf.getDouble();
    final double zMin = buf.getDouble();
    final double zMax = buf.getDouble();
    final double mMin = buf.getDouble();
    final double mMax = buf.getDouble();
    return new Shapefile(fileCode, length, version, shapeType, xMin, yMin, xMax, yMax, zMin, zMax, 
        mMin, mMax);
  }

  /**
   * Reads shape records from the buffer and populates {@code shapefile}.
   */
  private synchronized void readRecords(final Shapefile shapefile) {
    while (buf.hasRemaining()) {
      buf.order(ByteOrder.BIG_ENDIAN);
      final int recordNumber = buf.getInt();
      final int contentLengthWords = buf.getInt();
      buf.order(ByteOrder.LITTLE_ENDIAN);
      final int shapeType = peekInt();
      if (shapeType != 0 && shapeType != shapefile.shapeType) {
        throw new RuntimeException("Unexpected record shape type: " + shapeType + 
            " (should be " + shapefile.shapeType + " or 0).");
      }
      switch (shapeType) {
        case NULL_SHAPE:
          continue;
        case POLYGON:
          final Polygon polygon = readPolygon(recordNumber);
          shapefile.addPolygon(polygon);
          break;
        default:
          throw new RuntimeException("Shape type not supported: " + shapeType);
      }
    }
  }

  /**
   * Reads an int from the buffer without changing its position.
   */
  private synchronized int peekInt() {
    final int position = buf.position();
    final int val = buf.getInt();
    buf.position(position);
    return val;
  }

  /**
   * Reads a polygon shape record from the buffer.
   */
  private synchronized Polygon readPolygon(final int recordNumber) {
    // Byte order MUST be LITTLE_ENDIAN
    final double shapeType = buf.getInt();
    if (shapeType != POLYGON) {
      throw new IllegalStateException("Invalid shape type for polygon: " + shapeType);
    }
    final double xMin = buf.getDouble();
    final double yMin = buf.getDouble();
    final double xMax = buf.getDouble();
    final double yMax = buf.getDouble();
    final int numParts = buf.getInt(); 
    final int numPoints = buf.getInt();

    // Get number of points per part
    final int[] pointsPerPart = new int[numParts];
    final int[] partsStartingIndices = new int[numParts];
    for (int i = 0; i < numParts; ++i) {
      partsStartingIndices[i] = buf.getInt();
      if (i > 0) {
        pointsPerPart[i-1] = partsStartingIndices[i] - partsStartingIndices[i-1];
      }
    }
    pointsPerPart[numParts-1] = numPoints - partsStartingIndices[numParts-1];

    // Get parts (and points...)
    final Polygon.Part[] parts = new Polygon.Part[numParts];
    for (int currentPart = 0; currentPart < numParts; ++currentPart) {
      final int pointsInPart = pointsPerPart[currentPart];
      final Point[] points = new Point[pointsInPart];
      for (int i = 0; i < pointsInPart; ++i) {
        points[i] = readPoint();
      }
      parts[currentPart] = new Polygon.Part(points);
    }
    return new Polygon(recordNumber, xMin, yMin, xMax, yMax, parts);
  }

  /**
   * Reads a {@link Point} from the buffer.
   */
  private synchronized Point readPoint() {
    final double x = buf.getDouble();
    final double y = buf.getDouble();
    return new Point(x,y);
  }

  public static void main(String args[]) {
    try {
    final String filename = args[0];
    final ShapefileReader reader = new ShapefileReader(new File(filename));
    System.out.println(reader.read());
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }
}

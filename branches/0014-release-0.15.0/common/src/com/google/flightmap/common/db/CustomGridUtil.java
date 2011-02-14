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

package com.google.flightmap.common.db;

import java.util.LinkedList;

import com.google.flightmap.common.ThreadUtils;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;

/**
 * Provides utility functions for spatial indexing of geographical data using a basic grid
 * algorithm.
 * <p>
 * <b>Concept</b>
 * <p>
 * Geographical coordinates are mapped on a two dimensional space, with latitude and longitude as
 * axes.  The cell corresponding to a coordinate is determined by recursively dividing the area
 * into quadrants and appending a corresponding number to the cell id.  The following figure
 * illustrates the first two steps of this process.
 * <p align="center">
 * <a href="doc-files/CustomGridUtil-0.png" target="_blank">
 * <img src="doc-files/CustomGridUtil-0.png" width="40%" />
 * </a>
 * <p>
 * Values 0, 1, 2, 3 are attributed to the NW, NE, SW, SE quadrants respectively: each level of
 * recursion adds two bits to the previous id.  In base 4 notation, the id of the four level 2
 * cells of the north-western quadrant of the world are: 00, 01, 02, 03. Going one level deeper in
 * the last cell would give the following level 3 cells: 030, 031, 032 and 033, as shown in the
 * following figure.
 * <p align="center">
 * <a href="doc-files/CustomGridUtil-0a.png" target="_blank">
 * <img src="doc-files/CustomGridUtil-0a.png" width="40%" />
 * </a>
 * <p>
 * The number of iterations is limited by the size of integers.  For a typical size of 32 bits,
 * there can be 16 iterations, resulting in cells of 611 m. of longitude
 * by 305 m. of latitude on Earth's equator.
 * <p>
 * <b>Area Search</b>
 * A common geographical operation consists of retrieving elements situated within a certain area.
 * With spatial indexing, this can be done easily if the set of cells covering the area is
 * available.  The {@link CustomGridUtil#getCellsInRectangle getCellsInRectangle} method does that
 * by defining a coverage threshold (currently 70%) and doing the following recursively,
 * starting from the entire world:
 * <ol>
 * <li>If the area does not cover any part of the current cell, return nothing.</li>
 * <li>If the surface of the cell covered by the area is greater than the threshold:</li>
 * return current cell.
 * <li>If the current cell cannot be split further, return the current cell.</li>
 * <li>Otherwise, split the current cell into quadrants (sub-cells), and apply algorithm to each
 * one.  Return cells obtained by those calls.</li>
 * </ol>
 * This process is illustrated in the following figures. Notes:
 * <ul>
 * <li>Coverage threshold is set to 50% for this example.</li>
 * <li>Lightly filled box illustrates the searched area.</li>
 * <li>Continuous black lines represent the virtual border of cells.</li>
 * <li>Angled dashed lines cover the cells returned by the algorithm.</li>
 * </ul>
 * <p align="center">
 * <a href="doc-files/CustomGridUtil-1.png" target="_blank">
 * <img src="doc-files/CustomGridUtil-1.png" width="40%" />
 * </a>
 * <p>
 * The area does not cover more than 50% of the entire world (or "level 0 cell"), so a first split
 * is done (step 4 of the algorithm).<br />
 * Both eastern level 1 cells (NE and SE) do not intersect the searched area: they are
 * ignored (step 1).
 * <p align="center">
 * <a href="doc-files/CustomGridUtil-2.png" target="_blank">
 * <img src="doc-files/CustomGridUtil-2.png" width="40%" />
 * </a>
 * <p>
 * None of the western level 1 cells are covered enough, so a second split occurs.  The coverage in
 * two level 2 cells is now above the 50% threshold: they are added to the set of covering cells.
 * <p align="center">
 * <a href="doc-files/CustomGridUtil-3.png" target="_blank">
 * <img src="doc-files/CustomGridUtil-3.png" width="40%" />
 * </a>
 * <p>
 * The algorithm continues until either the maximum level of splits has occured (step 3, depends on
 * the number of bits in an integer) or all cells intersecting with the area have sufficient
 * coverage (step 4).
 * <p align="center">
 * <a href="doc-files/CustomGridUtil-4.png" target="_blank">
 * <img src="doc-files/CustomGridUtil-4.png" width="40%" />
 * </a>
 * <p>
 * Reducing the threshold results in less splits but the returned set of cells would
 * cover a (much) larger area than the searched area.  Inversely, a greater threshold would
 * reduce the difference between the covered and searched areas at the cost of additional splits
 * (and increased runtime).
 */
public class CustomGridUtil {
  /**
   * Maximum number of iterations for grid algorithm (see class description)
   */
  static private final int MAX_LEVEL = (int)(Math.log(Integer.MAX_VALUE)/Math.log(4));

  /**
   * Label for North-Western quadrant of split
   */
  static private final Integer NW = 0;

  /**
   * Label for North-Eastern quadrant of split
   */
  static private final Integer NE = 1;

  /**
   * Label for South-Western quadrant of split
   */
  static private final Integer SW = 2;

  /**
   * Label for South-Eastern quadrant of split
   */
  static private final Integer SE = 3;

  /**
   *  Utility class: default and only constructor is private.
   */
  private CustomGridUtil() { }

  /**
   * Represents a cell that can potentially be split into quadrants.
   * <p>
   * A partial cell is represented by a {@code partialCellId}, that represents the cell id prefix
   * of all cells of the next level included in the same area.  For instance, all cell ids
   * North-West of the latitude, longitude origin will start with the value
   * {@link CustomGridUtil#NW}.  The id of the corresponding level 1 partial cell is therefore 
   * {@link CustomGridUtil#NW}.
   */
  private static class PartialCell {
    final int partialCellId;
    final int level;
    final int centerLat;
    final int centerLng;
    final long width;
    final long height;

    public PartialCell(int partialCellId, int level, int centerLat, int centerLng,
                       long width, long height) {
      this.partialCellId = partialCellId;
      this.level = level;
      this.centerLat = centerLat;
      this.centerLng = centerLng;
      this.width = width;
      this.height = height;
    }
  }

  /**
   * Returns the length of the intersection of ({@code a}, {@code b}) and ({@code A}, {@code B}).
   * <p>
   * This is a mathematical utility method: no particular unit is assumed on the parameters.
   * Obviously, for this call to make sense, all arguments should have the same unit, which is also
   * the unit of the returned value.
   * <p>
   * Examples: <br />
   * <table border="1">
   * <tr><th>Arguments: <code>(a, b) (A, B)</code></th><th>Return value</th></tr>
   * <tr><td><code>(0, 10) (5, 15) </code></td><td><code>5</code> (length of (5, 10))</td> </tr>
   * <tr><td><code>(0, 20) (5, 15) </code></td><td><code>10</code> (length of (5, 15))</td> </tr>
   * <tr><td><code>(0, 10) (10, 20) </code></td><td><code>0</code> (length of (10, 10))</td> </tr>
   * <tr><td><code>(0, 5) (10, 20) </code></td><td><code>0</code> (no intersection)</td> </tr>
   * </table> 
   * <p>
   * If {@code a} > {@code b} or {@code A} > {@code B}, the values are swapped accordingly:
   * <code>intersectRange(10, 1, 20, 5) == intersectRange(1, 10, 5, 20)</code>
   */
  static private long intersectRange(long a, long b, long A, long B) {
    if (a > b) {
      long temp = a;
      a = b;
      b = temp;
    }

    if (A > B) {
      long temp = A;
      A = B;
      B = temp;
    }

    if (b<A || a>B)
      return 0;

    return Math.min(b,B) - Math.max(a,A);
  }

  /**
   * Returns the id of the (maximum level) cell that contains {@code position}.
   * See class description.
   */
  public static int getCellId(final LatLng position) {
    return getCellId(position.lat, position.lng);
  }

  /**
   * Returns the id of the (maximum level) cell that contains the point at
   * {@code latE6}, {@code lngE6}.
   *
   * @param latE6 Latitude, in E6 format (decimal degrees * 1E6)
   * @param lngE6 Longitude, in E6 format (decimal degrees * 1E6)
   */
  public static int getCellId(final int latE6, final int lngE6) {
    int northLatE6 = (int)+90E6;
    int southLatE6 = (int)-90E6;
    int westLngE6 = (int)-180E6;
    int eastLngE6 = (int)+180E6;

    int cellId = 0;
    int level = 0;

    while (level++ < MAX_LEVEL) {
      int centerLatE6 = (int)Math.ceil((northLatE6+southLatE6)/2.0);
      int centerLngE6 = (int)Math.ceil((westLngE6 + eastLngE6)/2.0);

      Integer quadrant = null;
      if (lngE6 > centerLngE6) { // East
        if (latE6 > centerLatE6) {
          quadrant = NE;
          southLatE6 = centerLatE6;
          westLngE6 = centerLngE6;
        } else {
          quadrant = SE;
          northLatE6 = centerLatE6;
          westLngE6 = centerLngE6;
        }
      } else { // West
        if (latE6 > centerLatE6) {
          quadrant = NW;
          southLatE6 = centerLatE6;
          eastLngE6 = centerLngE6;
        } else {
          quadrant = SW;
          northLatE6 = centerLatE6;
          eastLngE6 = centerLngE6;
        }
      }

      if (quadrant == null)
        throw new RuntimeException("Could not determine quadrant.");

      cellId <<= 2;
      cellId += quadrant;
    }

    return cellId;
  }

  /**
   * Returns set of cells that cover at least the area within {@code radius} of {@code origin}.
   *
   * @param origin  Center of circle in Lat,Lng space
   * @param radius  Radius of circle, in degrees * 1E6
   * @return     List of cell intervals that cover the area.
   * @see #getCellsInRectangle(LatLngRect)
   */
  static LinkedList<int[]> getCellsInRadius(final LatLng origin, final int radius)
      throws InterruptedException {
    return getCellsInRectangle(LatLngRect.getBoundingBox(origin, radius));
  }

  /**
   * Returns set of cells that cover at least the given area.
   *
   * @param area Rectangular area in Lat,Lng space.
   * @return     List of cell intervals that cover the area.
   *             Each element of the list is an array of two integers: int[]{cellMin, cellMax}
   *             The set of all cellIds such that cellMin <= cellId < cellMax covers area.
   */
  static LinkedList<int[]> getCellsInRectangle(final LatLngRect area) throws InterruptedException {
    final double threshold = 0.7;

    // Boundaries of next cell to inspect
    int northLatE6 = (int)+90E6;
    int southLatE6 = (int)-90E6;
    int westLngE6 = (int)-180E6;
    int eastLngE6 = (int)+180E6;
    long heightE6 = northLatE6 - southLatE6;
    long widthE6 = eastLngE6 - westLngE6;

    PartialCell root = new PartialCell(0, 0, 0, 0, widthE6, heightE6);
    LinkedList<PartialCell> remainingCells = new LinkedList<PartialCell>();
    remainingCells.add(root);

    LinkedList<int[]> coveredCells = new LinkedList<int[]>();

    final int areaTop = area.getNorth();
    final int areaBottom = area.getSouth();
    final int areaLeft = area.getWest();
    final int areaRight = area.getEast();

    while (!remainingCells.isEmpty()) {
      ThreadUtils.checkIfInterrupted();
      PartialCell currentCell = remainingCells.remove();

      heightE6 = currentCell.height;
      widthE6 = currentCell.width;
      northLatE6 = currentCell.centerLat + (int)(heightE6/2);
      southLatE6 = (int)(northLatE6 - heightE6);
      eastLngE6 = currentCell.centerLng + (int)(widthE6/2);
      westLngE6 = (int)(eastLngE6 - widthE6);

      long latIntersect = intersectRange(areaBottom, areaTop, southLatE6, northLatE6);
      long lngIntersect = intersectRange(areaLeft, areaRight, westLngE6, eastLngE6);

      double coverage = latIntersect * lngIntersect * 1.0/(heightE6*widthE6);

      if (coverage == 0)
        continue;

      if (currentCell.level == MAX_LEVEL || coverage >= threshold) {
        int cellRangeMin = 0;
        int cellRangeMax = Integer.MAX_VALUE;

        if (currentCell.level > 0) {
          cellRangeMin = currentCell.partialCellId << (2 * (MAX_LEVEL - currentCell.level));
          cellRangeMax = (currentCell.partialCellId + 1) << (2 * (MAX_LEVEL - currentCell.level));
        }
        coveredCells.add(new int[]{cellRangeMin, cellRangeMax});
      } else {
        final int sublevel = currentCell.level + 1;
        final int subcellIdBase = currentCell.partialCellId << 2;

        final long northSubcellHeightE6 = currentCell.height/2;
        final long southSubcellHeightE6 = currentCell.height - northSubcellHeightE6;
        final long eastSubcellWidthE6 = currentCell.width/2;
        final long westSubcellWidthE6 = currentCell.width - eastSubcellWidthE6;

        final PartialCell subcellNW = new PartialCell(
            subcellIdBase + NW,
            sublevel,
            currentCell.centerLat + (int)Math.ceil(northSubcellHeightE6/2.0),
            currentCell.centerLng - (int)(westSubcellWidthE6/2),
            westSubcellWidthE6,
            northSubcellHeightE6);

        final PartialCell subcellNE = new PartialCell(
            subcellIdBase + NE,
            sublevel,
            currentCell.centerLat + (int)Math.ceil(northSubcellHeightE6/2.0),
            currentCell.centerLng + (int)Math.ceil(eastSubcellWidthE6/2.0),
            eastSubcellWidthE6,
            northSubcellHeightE6);

        final PartialCell subcellSW = new PartialCell(
            subcellIdBase + SW,
            sublevel,
            currentCell.centerLat - (int)(southSubcellHeightE6/2),
            currentCell.centerLng - (int)(westSubcellWidthE6/2),
            westSubcellWidthE6,
            southSubcellHeightE6);

        final PartialCell subcellSE = new PartialCell(
            subcellIdBase + SE,
            sublevel,
            currentCell.centerLat - (int)(southSubcellHeightE6/2),
            currentCell.centerLng + (int)Math.ceil(eastSubcellWidthE6/2.0),
            eastSubcellWidthE6,
            southSubcellHeightE6);

        remainingCells.add(subcellNW);
        remainingCells.add(subcellNE);
        remainingCells.add(subcellSW);
        remainingCells.add(subcellSE);
      }
    }

    return coveredCells;
  }
}

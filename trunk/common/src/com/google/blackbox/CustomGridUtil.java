// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.blackbox;

import java.util.LinkedList;

import com.google.blackbox.data.LatLng;

/**
 *
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class CustomGridUtil {
  static private final int MAX_LEVEL = (int)(Math.log(Integer.MAX_VALUE)/Math.log(4));

  static private final Integer NW = 0;
  static private final Integer NE = 1;
  static private final Integer SW = 2;
  static private final Integer SE = 3;

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

  static private long IntersectRange(long a, long b, long A, long B) {
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

  public static int GetCellId(final LatLng position) {
    return GetCellId(position.lat, position.lng);
  }

  public static int GetCellId(final int latE6, final int lngE6) {
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
   * radius: in degrees (latitude and longitude) * 1E6
   */
  static LinkedList<int[]> GetCellsInRadius(LatLng origin, int radius) {
    final double threshold = 0.5;

    int northLatE6 = (int)+90E6;
    int southLatE6 = (int)-90E6;
    int westLngE6 = (int)-180E6;
    int eastLngE6 = (int)+180E6;
    long heightE6 = northLatE6-southLatE6;
    long widthE6 = eastLngE6-westLngE6;

    PartialCell root = new PartialCell(0, 0, 0, 0, widthE6, heightE6);
    LinkedList<PartialCell> remainingCells = new LinkedList<PartialCell>();
    remainingCells.add(root);

    LinkedList<int[]> coveredCells = new LinkedList<int[]>();

    final long areaTop = origin.lat + radius;
    final long areaBottom = origin.lat - radius;
    final long areaLeft = origin.lng - radius;
    final long areaRight = origin.lng + radius;

    while (!remainingCells.isEmpty()) {
      PartialCell currentCell = remainingCells.remove();

      heightE6 = currentCell.height;
      widthE6 = currentCell.width;
      northLatE6 = currentCell.centerLat + (int)(heightE6/2);
      southLatE6 = (int)(northLatE6 - heightE6);
      eastLngE6 = currentCell.centerLng + (int)(widthE6/2);
      westLngE6 = (int)(eastLngE6 - widthE6);

      long latIntersect = IntersectRange(areaBottom, areaTop, southLatE6, northLatE6);
      long lngIntersect = IntersectRange(areaLeft, areaRight, westLngE6, eastLngE6);

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
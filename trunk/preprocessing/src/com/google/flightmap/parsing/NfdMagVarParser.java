/* 
 * Copyright (C) 2009 Google Inc.
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

package com.google.flightmap.parsing;

import com.google.flightmap.common.CustomGridUtil;

import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.*;
import java.io.*;

/**
 * Extract magnetic variation data points from ARINC 424-18 file.
 *
 */
public class NfdMagVarParser {
  /**
   * Precision for latitude and longitude.
   */
  private final static int LAT_LNG_PRECISION = 6;

  /**
   * Absolute tolerance for magnetic variation equality.
   */
  private final static double EQUAL_MAG_VAR_EPSILON = 0.1;

  /**
   * Regular expressions that contain magnetic variation data.
   * Each regular expression must have at least three groups:
   * 1. latitude
   * 2. longitude
   * 3. magnetic variation
   */
  private final static List<Pattern> patterns;

  /**
   * Path to source file in ARINC 424-18 format.
   */
  private String nfdFile;

  static {
    patterns = new LinkedList<Pattern>();
    // NDB Navaid
    patterns.add(Pattern.compile("^S...DB.{6} .{4}  .{13}(.{9})(.{10}) {23}(.{5}) {6}.{47}$"));
    patterns.add(Pattern.compile("^S...PN.{6} .{4}  .{13}(.{9})(.{10}) {23}(.{5}) {6}.{47}$"));
    // Waypoint
    patterns.add(
        Pattern.compile("^S...EA.{12} .{3} {4}.{5} (.{9})(.{10}) {23}(.{5}).{8} {8}.{37}$"));
    patterns.add(
        Pattern.compile("^S...PC.{12} .{3} {4}.{5} (.{9})(.{10}) {23}(.{5}).{8} {8}.{37}$"));
    // Airport
    patterns.add(Pattern.compile("^S...P .{6}A.{5}   .{11}(.{9})(.{10})(.{5}).{76}$"));
    // Heliport
    patterns.add(Pattern.compile("^S...H .{6}A.{18} (.{9})(.{10})(.{5}).{76}$"));
  }

  /**
   * @param nfdFile  Source database in ARINC 424-18 format (eg NFD)
   */
  private NfdMagVarParser(String nfdFile) {
    this.nfdFile = nfdFile;
  }

  public static void main(String args[]) {
    if (args.length != 1) {
      System.err.println("Usage: java NfdMagVarParser <NFD file>");
      System.exit(1);
    }

    (new NfdMagVarParser(args[0])).execute();
  }

  /**
   * Parse degrees string in [D]DDMMSSSS format.
   */
  private static double parseDegrees(final String degreesStr) {
    final StringBuilder degBuffer = new StringBuilder(degreesStr);

    final int secHundreths = Integer.parseInt(degBuffer.substring(degBuffer.length() - 4));
    degBuffer.delete(degBuffer.length() - 4, degBuffer.length());

    final int minutes = Integer.parseInt(degBuffer.substring(degBuffer.length() - 2));
    degBuffer.delete(degBuffer.length() - 2, degBuffer.length());

    final int degrees = Integer.parseInt(degBuffer.toString());

    final double deg = degrees + minutes/60.0 + secHundreths / 360000.0;
    return deg;
  }

  /**
   * Parse degrees of latitude in NDDMMSSSS or SDDMMSSSS format.
   *
   * @returns Degrees: positive for North, negative for South.
   */
  private static double parseLatitude(final String latitude) {
    double lat;

    if (latitude.startsWith("N")) {
      lat = 1;
    } else if (latitude.startsWith("S")) {
      lat = -1;
    } else {
      throw new IllegalArgumentException("Invalid latitude: " + latitude);
    }

    lat *= parseDegrees(latitude.substring(1));
    return lat;
  }

  /**
   * Parse degrees of longitude in EDDDMMSSSS or WDDDMMSSSS format.
   *
   * @returns Degrees: positive for East, negative for West.
   */
  private static double parseLongitude(final String longitude) {
    double lng;

    if (longitude.startsWith("E")) {
      lng = 1;
    } else if (longitude.startsWith("W")) {
      lng = -1;
    } else {
      throw new IllegalArgumentException("Invalid longitude: " + longitude);
    }

    lng *= parseDegrees(longitude.substring(1));
    return lng;
  }

  /**
   * Parse degrees of magnetic variation.
   *
   * @ param magneticVariation String representation of magnetic variation.  Tenths of degrees
   * without the period, preceded by 'W' or 'E'.  For example: "W123"
   *
   * @return Degrees of magnetic variation.  Positive for West, negative for east.
   */
  private static double parseMagVar(final String magneticVariation) {
    double magVar;

    if (magneticVariation.startsWith("E")) {
      magVar = -0.1;
    } else if (magneticVariation.startsWith("W")) {
      magVar = 0.1;
    } else {
      throw new IllegalArgumentException("Invalid magnetic variation: " + magneticVariation);
    }

    magVar *= Double.parseDouble(magneticVariation.substring(1));
    return magVar;
  }

  /**
   * Parse source file.
   *
   * @return a map of degrees of magnetic variation by position (in string format)
   */
  private Map<String, Double> parseNdfFile() throws IOException {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(this.nfdFile));

      final Map<String, Double> magVarByPosition = new HashMap<String, Double>();
      final Set<String> invalidPositions = new HashSet<String>();

      String line;
      while ((line = in.readLine()) != null) {
        Matcher lineMatcher = null;
        for (Pattern pattern: patterns) {
          lineMatcher = pattern.matcher(line);
          if (lineMatcher.matches()) {
            break;
          }
        }

        if (lineMatcher == null || !lineMatcher.matches()) {
          continue;
        }

        final String latitudeStr = lineMatcher.group(1);
        final String longitudeStr = lineMatcher.group(2);
        final String magVarStr = lineMatcher.group(3);

        try {
          final double latitude = parseLatitude(latitudeStr);
          final double longitude = parseLongitude(longitudeStr);
          final double magVar = parseMagVar(magVarStr);

          final String position = String.format("%." + LAT_LNG_PRECISION + "f," +
                                                "%." + LAT_LNG_PRECISION + "f",
                                                latitude, longitude);

          if (invalidPositions.contains(position)) {
            System.err.println("Skipping duplicate position: " + position);
            continue;
          }

          if (magVarByPosition.containsKey(position)) {
            final double existingMagVar = magVarByPosition.get(position);
            if (Math.abs(existingMagVar - magVar) > EQUAL_MAG_VAR_EPSILON) {
              System.err.print("Found invalid duplicate mag var: ");
              System.err.println(position + ": " + existingMagVar + " vs. " + magVar);

              invalidPositions.add(position);
              magVarByPosition.remove(position);
            }
          } else {
            magVarByPosition.put(position, magVar);
          }
        } catch (IllegalArgumentException nfe) {
          // Skip over errors
          System.err.println("Error parsing: " + line);
        }
      }

      return magVarByPosition;
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  /**
   * Prints mag var data on standard output.
   */
  private void dumpMagVarFile(final Map<String, Double> magVarByPosition) {
    for (Map.Entry<String, Double> magVarPositionEntry: magVarByPosition.entrySet()) {
      final String position = magVarPositionEntry.getKey();
      final double magVar = magVarPositionEntry.getValue();
      System.out.println(position + "," + magVar);
    }
  }

  /**
   * Extracts magnetic variation data from source file and print to standard output
   */
  private void execute() {
    try {
      final Map<String, Double> magVarByPosition = parseNdfFile();
      dumpMagVarFile(magVarByPosition);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}

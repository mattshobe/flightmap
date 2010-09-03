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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.*;
import java.io.*;

import com.google.flightmap.parsing.data.MagVar;


/*
 * Magnetic variation matching object
 */
class NfdMagVarMatcher {

  private final String type;
  private final Pattern pattern;

  private final int identGroup;
  private final int latGroup;
  private final int lngGroup;
  private final Integer elevationGroup;
  private final int magVarGroup;

  NfdMagVarMatcher(final String type,
                   final String regex,
                   final int identGroup,
                   final int latGroup,
                   final int lngGroup,
                   final Integer elevationGroup,
                   final int magVarGroup) {
    this.type = type;
    this.pattern = Pattern.compile(regex);
    this.identGroup = identGroup;
    this.latGroup = latGroup;
    this.lngGroup = lngGroup;
    this.elevationGroup = elevationGroup;
    this.magVarGroup = magVarGroup;
  }

  /*
   * Try to parse data from line.
   *
   * @return Magnetic variation data, or null
   */
  MagVar match(final String line) {
    Matcher matcher = pattern.matcher(line);
    if (!matcher.matches()) {
      return null;
    }

    try {
      return new MagVar(type,
                        getIdent(matcher),
                        getLat(matcher),
                        getLng(matcher),
                        getElevation(matcher),
                        getMagVar(matcher),
                        line);
    } catch (RuntimeException ex) {
      System.err.println("Error while processing line: " + line);
      throw ex;
    }
  }

  private String getIdent(Matcher matcher) {
    return matcher.group(identGroup);
  }

  private double getLat(Matcher matcher) {
    return parseLatitude(matcher.group(latGroup));
  }

  private double getLng(Matcher matcher) {
    return parseLongitude(matcher.group(lngGroup));
  }

  private Integer getElevation(Matcher matcher) {
    if (elevationGroup == null) {
      return null;
    }

    final String elevationString = matcher.group(elevationGroup).trim();

    if (elevationString.isEmpty()) {
      return null;
    }

    return new Integer(elevationString);
  }

  private double getMagVar(Matcher matcher) {
    return parseMagVar(matcher.group(magVarGroup));
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
}


/**
 * Extract magnetic variation data points from ARINC 424-18 file.
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
  private final static List<NfdMagVarMatcher> matchers;

  /**
   * Path to source file in ARINC 424-18 format.
   */
  private String nfdFile;

  static {
    matchers = new LinkedList<NfdMagVarMatcher>();
    matchers.add(new NfdMagVarMatcher(
        "NDB Navaid",
        "^S...DB.{6} (.{4})  .{13}(.{9})(.{10}) {23}(.{5}) {6}.{47}$",
        1, 2, 3, null, 4));

    matchers.add(new NfdMagVarMatcher(
        "Terminal NDB",
        "^S...PN.{6} (.{4})  .{13}(.{9})(.{10}) {23}(.{5}) {6}.{47}$",
        1, 2, 3, null, 4));

    matchers.add(new NfdMagVarMatcher(
        "Waypoint",
        "^S...EA.{7}(.{5}) .{3} {4}.{5} (.{9})(.{10}) {23}(.{5})(.{5}).{3} {8}.{37}$",
        1, 2, 3, 5, 4));

    matchers.add(new NfdMagVarMatcher(
        "Terminal Waypoint",
        "^S...PC.{7}(.{5}) .{3} {4}.{5} (.{9})(.{10}) {23}(.{5})(.{5}).{3} {8}.{37}$",
        1, 2, 3, 5, 4));

    matchers.add(new NfdMagVarMatcher(
        "Airport",
        "^S...P (.{4}).{2}A.{5}   .{11}(.{9})(.{10})(.{5})(.{5}).{71}$",
        1, 2, 3, 5, 4));

    matchers.add(new NfdMagVarMatcher(
        "Helipad",
        "^S...H (.{4}).{2}A.{18} (.{9})(.{10})(.{5})(.{5}).{71}$",
        1, 2, 3, 5, 4));
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
   * Parse source file.
   *
   * @return a map of degrees of magnetic variation by position (in string format)
   */
  private Collection<MagVar> parseNdfFile() throws IOException {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(this.nfdFile));

      final Map<String, MagVar> magVarByPosition = new HashMap<String, MagVar>();
      final Set<String> invalidPositions = new HashSet<String>();

      String line;
      while ((line = in.readLine()) != null) {
        MagVar data = null;
        for (NfdMagVarMatcher matcher: matchers) {
          data = matcher.match(line);
          if (data != null) {
            break;
          }
        }

        if (data == null) {
          continue;
        }

        final String position = data.getPosition(LAT_LNG_PRECISION);

        // Check if position is marked as invalid
        if (invalidPositions.contains(position)) {
          System.err.println("Skipping duplicate position: " + position);
          continue;
        }

        // Check if same position already exists.
        if (magVarByPosition.containsKey(position)) {
          final double magVar = data.magVar;
          final double existingMagVar = magVarByPosition.get(position).magVar;
          // If same position already exists, check if magVar is identical.
          if (Math.abs(existingMagVar - magVar) > EQUAL_MAG_VAR_EPSILON) {
            System.err.print("Found invalid duplicate mag var: ");
            System.err.println(position + ": " + existingMagVar + " vs. " + magVar);

            // If same position has different magVar, dismiss position as invalid.
            invalidPositions.add(position);
            magVarByPosition.remove(position);
          }
        } else {
          magVarByPosition.put(position, data);
        }
      }

      return new Vector<MagVar>(magVarByPosition.values());
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }

  /**
   * Prints mag var data on standard output.
   */
  private void dumpMagVarFile(final Collection<MagVar> magVarCollection) {
    System.out.println(MagVar.getHeaderString());
    for (MagVar magVarData: magVarCollection) {
      System.out.println(magVarData);
    }
  }

  /**
   * Extracts magnetic variation data from source file and print to standard output
   */
  private void execute() {
    try {
      final Collection<MagVar> magVarCollection = parseNdfFile();
      dumpMagVarFile(magVarCollection);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}

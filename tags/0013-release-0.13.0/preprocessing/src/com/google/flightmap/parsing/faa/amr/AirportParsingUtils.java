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

package com.google.flightmap.parsing.faa.amr;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Parsing methods for airports (and runways, runway ends).
 * <p>
 * All {@code parse...} methods:
 * <ul>
 * <li>handle {@code null} input by returning {@code null}.</li>
 * <li>throw {@link RuntimeException} if the input doesn't correspond to any valid input</li>
 * </ul>
 */
class AirportParsingUtils {

  private final static Map<String, String> RUNWAY_SURFACE_MAP;

  static {
    // Runway type
    RUNWAY_SURFACE_MAP = new HashMap<String, String>();
    RUNWAY_SURFACE_MAP.put("CONC", "Portland cement concrete");
    RUNWAY_SURFACE_MAP.put("ASPH", "Asphalt or bituminous concrete");
    RUNWAY_SURFACE_MAP.put("PEM", "Asphalt concrete");
    RUNWAY_SURFACE_MAP.put("SNOW", "Snow");
    RUNWAY_SURFACE_MAP.put("ICE", "Ice");
    RUNWAY_SURFACE_MAP.put("MATS", "Pierced steel planking, landing mats");
    RUNWAY_SURFACE_MAP.put("TREATED", "Oiled, soil cement or lime stabilized");
    RUNWAY_SURFACE_MAP.put("GRAVEL", "Gravel, cinders, crushed rock, ...");
    RUNWAY_SURFACE_MAP.put("TURF", "Grass, sod");
    RUNWAY_SURFACE_MAP.put("DIRT", "Natural soil");
    RUNWAY_SURFACE_MAP.put("WATER", "Water");
    RUNWAY_SURFACE_MAP.put("E", "Excellent condition");
    RUNWAY_SURFACE_MAP.put("G", "Good condition");
    RUNWAY_SURFACE_MAP.put("F", "Fair condition");
    RUNWAY_SURFACE_MAP.put("P", "Poor condition");
    RUNWAY_SURFACE_MAP.put("L", "Failed condition");
  }

  /**
   *  Utility class: default and only constructor is private.
   */
  private AirportParsingUtils() { }

  static String parseAirportBeaconColor(final String beaconColor) {
    if (beaconColor == null || beaconColor.isEmpty()) {
      return null;
    }

    if ("CG".equals(beaconColor) || "G".equals(beaconColor)) {
      return "White-Green (Lighted land airport)";
    } else if ("CY".equals(beaconColor) || "Y".equals(beaconColor)) {
      return "White-Yellow (Lighted seaplane base)";
    } else if ("CGY".equals(beaconColor)) {
      return "White-Green-Yellow (Heliport)";
    } else if ("SCG".equals(beaconColor)) {
      return "Split White-Green (Lighted military airport)";
    } else if ("C".equals(beaconColor)) {
      return "White (Unlighted land airport)";
    } else {
      throw new RuntimeException("Unknown Beacon Color: " + beaconColor);
    }
  }

  static String parseAirportFuelTypes(final String fuelTypes) {
    if (fuelTypes == null) {
      return null;
    }
    final StringBuilder fuelTypesBuffer = new StringBuilder(fuelTypes);
    final StringBuilder fuelTypesPropertyBuffer = new StringBuilder(fuelTypes.length());
    final String separator = "\n";
    int length;
    while ( (length = fuelTypesBuffer.length()) > 0) {
      final int nextMark = Math.min(length, 5);
      String fuelType = fuelTypesBuffer.substring(0, nextMark).trim();
      fuelTypesBuffer.delete(0, nextMark);
      if ("".equals(fuelType)) {
        continue;
      } else if ("80".equals(fuelType)) {
        fuelType += ": Grade 80 gasoline (red)";
      } else if ("100".equals(fuelType)) {
        fuelType += ": Grade 100 gasoline (green)";
      } else if ("100LL".equals(fuelType)) {
        fuelType += ": Grade 100LL gasoline (blue)";
      } else if ("115".equals(fuelType)) {
        fuelType += ": Grade 115 gasoline";
      } else if ("A".equals(fuelType)) {
        fuelType += ": Jet A kerozene (freeze point -40C)";
      } else if ("A1".equals(fuelType)) {
        fuelType += ": Jet A-1 kerozene (freeze point -50C)";
      } else if ("A1+".equals(fuelType)) {
        fuelType += ": Jet A-1 kerozene with icing inhibitor (freeze point -50C)";
      } else if ("B".equals(fuelType)) {
        fuelType += ": Jet B wide-cut turbine fuel (freeze point -50C)";
      } else if ("B+".equals(fuelType)) {
        fuelType += ": Jet B wide-cut turbine fuel with icing inhibitor (freeze point -50C)";
      } else if ("MOGAS".equals(fuelType)) {
        fuelType = "Automotive gasoline";
      }  // Some fuel types are unknown (eg. A+).  Keep as is.
      fuelTypesPropertyBuffer.append(fuelType);
      fuelTypesPropertyBuffer.append(separator);
    }
    // Remove trailing ", "
    length = fuelTypesPropertyBuffer.length();

    if (length > 0) {
      fuelTypesPropertyBuffer.delete(length-separator.length(), length);
      return fuelTypesPropertyBuffer.toString();
    } else {
      return null;
    }
  }

  static String parseAirportWindIndicator(final String windIndicator) {
    if (windIndicator == null) {
      return null;
    }
    if ("Y-L".equals(windIndicator)) {
      return "Yes, Lighted";
    } else {
      try {
        return parseBoolean(windIndicator);
      } catch (RuntimeException rex) {
        throw new RuntimeException("Unknown wind indicator value: " + windIndicator, rex);
      }
    }
  }

  static String parseBoolean(final String booleanText) {
    if (booleanText == null || booleanText.isEmpty()) {
      return null;
    }
    if ("Y".equals(booleanText)) {
      return "Yes";
    } else if ("N".equals(booleanText)) {
      return "No";
    } else {
      throw new RuntimeException("Unknown boolean value: " + booleanText);
    }
  }

  static String parseRunwaySurface(final String typeAndCondition) {
    if (typeAndCondition == null) {
      return null;
    }
    final StringBuilder typeAndConditionBuffer = new StringBuilder(typeAndCondition);
    final StringBuilder runwaySurfaceBuffer = new StringBuilder();
    final String separator = "\n";

    int length;
    while ((length = typeAndConditionBuffer.length()) > 0) {
      int nextMark = typeAndConditionBuffer.indexOf("-");
      if (nextMark == -1) {
        nextMark = length;
      }

      String runwayTypeOrCondition = typeAndConditionBuffer.substring(0, nextMark).trim();
      typeAndConditionBuffer.delete(0, nextMark+1);

      // Normalization
      if ("GRVL".equals(runwayTypeOrCondition)) {
        runwayTypeOrCondition = "GRAVEL";
      }

      if (RUNWAY_SURFACE_MAP.containsKey(runwayTypeOrCondition)) {
        runwayTypeOrCondition += ": " + RUNWAY_SURFACE_MAP.get(runwayTypeOrCondition);
      }
      runwaySurfaceBuffer.append(runwayTypeOrCondition);
      runwaySurfaceBuffer.append(separator);
    }
    // Remove trailing ", "
    length = runwaySurfaceBuffer.length();

    if (length > 0) {
      runwaySurfaceBuffer.delete(length-separator.length(), length);
      return runwaySurfaceBuffer.toString();
    } else {
      return "None";
    }
  }

  static String parseTrafficPattern(final String rightTrafficPattern) {
    if (rightTrafficPattern == null || rightTrafficPattern.isEmpty()) {
      return null;
    }

    if ("Y".equals(rightTrafficPattern)) {
      return "Right";
    } else if ("N".equals(rightTrafficPattern)) {
      return "Left";
    } else {
      System.out.println("Unknown right traffic pattern value: " + rightTrafficPattern);
      return rightTrafficPattern;
    }
  }

  static String parseVasi(final String vasi) {
    if (vasi == null || vasi.isEmpty()) {
      return null;
    }
    if ("SAVASI".equals(vasi)) {
      return "Simplified abbreviated visual approach slope indicator";
    } else if ("VASI".equals(vasi)) {
      return "Visual approach slope indicator";
    } else if ("PAPI".equals(vasi)) {
      return "Precision approach path indicator";
    } else if ("tri".equals(vasi)) {
      return "Tri-color visual approach slope indicator)";
    } else if ("PSI".equals(vasi)) {
      return "Pulsating visual approach slope indicator";
    } else {
      return vasi;
    }
  }
}

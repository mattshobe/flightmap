package com.google.flightmap.parsing;

import com.google.flightmap.common.data.LatLng;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

/**
 * Parses magnetic variation data from custom (simple) text file.
 */
public class CsvMagVarParser {
  private final static int MAG_VAR_FIELD_COUNT = 3;
  private final static int MAG_VAR_FIELD_INDEX_LAT = 0;
  private final static int MAG_VAR_FIELD_INDEX_LNG = 1;
  private final static int MAG_VAR_FIELD_INDEX_MAG_VAR = 2;

  private final static Logger log = Logger.getLogger(CsvMagVarParser.class.getSimpleName());

  /**
   * Parses magnetic variation data file.
   *
   * Expects one entry per line, with the following format:
   * <lat> <lng> <magVar>
   */
  public static Map<LatLng, Double> parse(final String filename) throws IOException {
    final Map<LatLng, Double>  magVars = new HashMap<LatLng, Double>();
    final BufferedReader in = new BufferedReader(new FileReader(filename));

    try {
      String line;
      while ((line = in.readLine()) != null) {
        final String[] magVarFields = line.split(",");
        if (magVarFields.length != MAG_VAR_FIELD_COUNT) {
          continue;
        }

        try {
          final double lat = Double.parseDouble(magVarFields[MAG_VAR_FIELD_INDEX_LAT]);
          final double lng = Double.parseDouble(magVarFields[MAG_VAR_FIELD_INDEX_LNG]);
          final double magVar = Double.parseDouble(magVarFields[MAG_VAR_FIELD_INDEX_MAG_VAR]);

          final LatLng position = LatLng.fromDouble(lat, lng);
          magVars.put(position, magVar);
        } catch (NumberFormatException nfEx) {
          // Skip over problematic lines
          log.info("Could not parse line: " + line);
        }
      }
    } finally {
      in.close();
    }

    return magVars;
  }
}

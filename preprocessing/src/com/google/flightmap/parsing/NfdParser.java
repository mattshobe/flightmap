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

import java.sql.*;
import java.util.regex.*;
import java.io.*;

/**
 * Parses airports from ARINC 424-18 file and adds them to a SQLite database
 * 
 */
public class NfdParser {
  private String sourceFile;

  /**
   * @param sourceFile
   *          Source database in ARINC 424-18 format (eg NFD)
   */
  public NfdParser(String sourceFile) {
    this.sourceFile = sourceFile;
  }

  public static void main(String args[]) {
    if (args.length != 1) {
      System.err.println("Usage: java NfdParser <NFD file>");
      System.exit(1);
    }

    (new NfdParser(args[0])).execute();
  }

  private void execute() {
    try {
      addAirportDataToDb();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(2);
    }
  }

  private void addAirportDataToDb() throws SQLException, IOException {
    BufferedReader in = new BufferedReader(new FileReader(this.sourceFile));

    Pattern airportArincPattern = Pattern
        .compile("S...P (.{4})..A(.{3}).*");
    Matcher airportArincMatcher;

    // Airport data variables
    String icao, iata;

    String line;
    while ((line = in.readLine()) != null) {
      airportArincMatcher = airportArincPattern.matcher(line);
      if (!airportArincMatcher.matches()) {
        // Not an airport entry
        continue;
      }

      icao = airportArincMatcher.group(1).trim();
      iata = airportArincMatcher.group(2).trim();
      if ( !(iata.isEmpty() || icao.isEmpty()) && !iata.equals(icao) )
        System.out.println(iata + " " + icao);
      continue;
    }
    in.close();
  }
}

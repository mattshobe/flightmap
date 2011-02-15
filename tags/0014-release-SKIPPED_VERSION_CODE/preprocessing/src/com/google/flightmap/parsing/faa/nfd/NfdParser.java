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

package com.google.flightmap.parsing.faa.nfd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


/**
 * Parses airports from ARINC 424-18 file and adds them to a SQLite database
 * 
 */
public class NfdParser {
  // Command line options
  private final static Options OPTIONS = new Options();
  private final static String HELP_OPTION = "help";
  private final static String NFD_OPTION = "nfd";
  private final static String IATA_TO_ICAO_OPTION = "iata_to_icao";

  // IATA to ICAO pattern regex
  private final static Pattern airportArincPattern = Pattern.compile("S...P (.{4})..A(.{3}).*");

  static {
    // Command Line options definitions
    OPTIONS.addOption("h", "help", false, "Print this message.");
    OPTIONS.addOption(OptionBuilder.withLongOpt(NFD_OPTION)
                                   .withDescription("FAA National Flight Database.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("nfd.dat")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(IATA_TO_ICAO_OPTION)
                                   .withDescription("IATA to ICAO codes text file.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("iata_to_icao.txt")
                                   .create());
  }
  private final File nfd;
  private final File iataToIcao;

  /**
   * @param nfd Source database in ARINC 424-18 format (eg NFD)
   */
  public NfdParser(final File nfd, final File iataToIcao) {
    this.nfd = nfd;
    this.iataToIcao = iataToIcao;
  }

  public static void main(String args[]) {
    CommandLine line = null;
    try {
      final CommandLineParser parser = new PosixParser();
      line = parser.parse(OPTIONS, args);
    } catch (ParseException pEx) {
      System.err.println(pEx.getMessage());
      printHelp(line);
      System.exit(1);
    }

    if (line.hasOption(HELP_OPTION)) {
      printHelp(line);
      System.exit(0);
    }

    final String nfdPath = line.getOptionValue(NFD_OPTION);
    final File nfd = new File(nfdPath);
    final String iataToIcaoPath = line.getOptionValue(IATA_TO_ICAO_OPTION);
    final File iataToIcao = new File(iataToIcaoPath);

    (new NfdParser(nfd, iataToIcao)).execute();
  }

  private static void printHelp(final CommandLine line) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(100);
    formatter.printHelp("NfdParser", OPTIONS, true);
  }

  private void execute() {
    try {
      parseAndDumpIataToIcao();
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(2);
    }
  }

  private void parseAndDumpIataToIcao() throws IOException {
    final BufferedReader in = new BufferedReader(new FileReader(nfd));
    final PrintStream out = new PrintStream(iataToIcao);

    Matcher airportArincMatcher;

    // Airport data variables
    String icao, iata;
    String line;
    try {
      while ((line = in.readLine()) != null) {
        airportArincMatcher = airportArincPattern.matcher(line);
        if (!airportArincMatcher.matches()) {
          // Not an airport entry
          continue;
        }

        icao = airportArincMatcher.group(1).trim();
        iata = airportArincMatcher.group(2).trim();
        if ( !(iata.isEmpty() || icao.isEmpty()) && !iata.equals(icao) )
          out.println(iata + " " + icao);
      }
    } finally {
      out.close();
      in.close();
    }
  }
}

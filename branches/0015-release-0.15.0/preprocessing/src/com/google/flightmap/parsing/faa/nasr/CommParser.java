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

package com.google.flightmap.parsing.faa.nasr;

import com.google.flightmap.common.io.StreamUtils;
import com.google.flightmap.db.JdbcAviationDbAdapter;
import com.google.flightmap.db.JdbcAviationDbWriter;
import com.google.flightmap.parsing.data.AirportComm;
import com.google.flightmap.parsing.db.AviationDbReader;
import com.google.flightmap.parsing.db.AviationDbWriter;
import com.google.flightmap.parsing.util.IcaoUtils;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

/**
 * Parses airports from compiled AFD file.
 */
public class CommParser {
  // Commandline options
  private final static Options OPTIONS = new Options();
  private final static String HELP_OPTION = "help";
  private final static String TWR_OPTION = "twr";
  private final static String IATA_TO_ICAO_OPTION = "iata_to_icao";
  private final static String FREQ_USES_NORMALIZATION_OPTION = "freq_uses_normalization";
  private final static String AVIATION_DB_OPTION = "aviation_db";

  private final static Pattern freqPattern = Pattern.compile("^(\\d+\\.\\d+\\S?)((?: |\\().*)?$");

  static {
    OPTIONS.addOption("h", "help", false, "Print this message.");
    OPTIONS.addOption(OptionBuilder.withLongOpt(TWR_OPTION)
                                   .withDescription("Terminal Communications Services (TWR) file.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("TWR.txt")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(IATA_TO_ICAO_OPTION)
                                   .withDescription("IATA to ICAO codes text file.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("iata_to_icao.txt")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(FREQ_USES_NORMALIZATION_OPTION)
                                   .withDescription("Normalized frequency uses csv file.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("distinct_freq_uses.csv")
                                   .create());
    OPTIONS.addOption(OptionBuilder.withLongOpt(AVIATION_DB_OPTION)
                                   .withDescription("FlightMap aviation database")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("aviation.db")
                                   .create());
  }

  private final AviationDbWriter dbWriter;
  private final AviationDbReader dbReader;
  private final Map<String, String> iataToIcao;
  private final Map<String, NormalizedFrequencyUseBean> normalizedFreqUses;
  private final String data;

  /**
   * @param twrFile NASR Terminal Communications Services database file.
   * @param iataToIcaoFile IATA to ICAO codes file
   * @param dbFile Aviation database file
   */
  public CommParser(final String twrFile, final String iataToIcaoFile,
      final String freqUsesNormalizationFile, final String dbFile) throws ClassNotFoundException,
      IOException, SQLException {
    dbWriter = new JdbcAviationDbWriter(new File(dbFile));
    dbWriter.open();
    dbReader = new JdbcAviationDbAdapter(dbWriter.getConnection());
    data = StreamUtils.read(new File(twrFile));
    iataToIcao = IcaoUtils.parseIataToIcao(iataToIcaoFile);
    normalizedFreqUses = NormalizedFrequencyUseUtils.parse(freqUsesNormalizationFile);
  }

  private void execute() throws Exception {
    dbWriter.beginTransaction();
    try {
      addCommData();
      dbWriter.commit();
    } catch (Exception ex) {
      dbWriter.rollback();
      throw ex;
    } finally {
      dbWriter.close();
    }
  }

  private void addCommData() throws IOException, SQLException {
    final BufferedReader in = new BufferedReader(new StringReader(data));
    String line;

    while ((line = in.readLine()) != null) {
      if (Twr3.matches(line)) {
        final Twr3 record = new Twr3(line);
        final List<AirportComm> comms = new LinkedList<AirportComm>();
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency1, record.sectorization1));
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency2, record.sectorization2));
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency3, record.sectorization3));
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency4, record.sectorization4));
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency5, record.sectorization5));
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency6, record.sectorization6));
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency7, record.sectorization7));
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency8, record.sectorization8));
        comms.addAll(
            getCommFromTwr3(record.facilityIdentifier, record.frequency9, record.sectorization9));
        for (AirportComm comm: comms) {
          addAirportCommToDb(comm);
        }
      } else if (Twr7.matches(line)) {
        final Twr7 record = new Twr7(line);
        final AirportComm comm = getCommFromTwr7(record);
        addAirportCommToDb(comm);
      }
    }
  }

  private List<AirportComm> getCommFromTwr3(final String ident, final String frequencyAndRemarks,
      final String sectorization) {
    final List<AirportComm> airportComms = new LinkedList<AirportComm>();
    if (frequencyAndRemarks == null || frequencyAndRemarks.isEmpty() || 
        sectorization == null || sectorization.isEmpty()) {
      return airportComms;  
    }
    final int airportId = getAirportId(ident);
    if (airportId == -1) {
      return airportComms;
    }
    final String[] freqRemarks = getFrequencyAndRemarks(frequencyAndRemarks);
    if (freqRemarks != null) {
      airportComms.add(new AirportComm(airportId, sectorization, freqRemarks[0], freqRemarks[1]));
    }
    return airportComms;
  }

  private AirportComm getCommFromTwr7(final Twr7 record) {
    final int airportId = getAirportId(record.satelliteIdentifier);
    if (airportId == -1) {
      return null;
    }
    final String[] freqRemarks = getFrequencyAndRemarks(record.frequency);
    if (freqRemarks == null) {
      return null;
    }
    return new AirportComm(airportId, record.frequencyUse, freqRemarks[0], freqRemarks[1]);
  }

  /**
   * Extract frequency and remarks from input.
   * @param input Raw input (eg. {@code 132.8(241-269)})
   */
  private static String[] getFrequencyAndRemarks(final String input) {
    final Matcher freqMatcher = freqPattern.matcher(input);
    if (!freqMatcher.matches()) {
      return null;
    }
    final String frequency = freqMatcher.group(1);
    final String remarks = freqMatcher.group(2);
    return new String[] {frequency, remarks != null ? remarks.trim() : null};
  }

  private int getAirportId(final String iata) {
    final String icao = iataToIcao.get(iata);
    return dbReader.getAirportIdByIcao(icao == null ? iata : icao);
  }

  private void addAirportCommToDb(AirportComm comm) throws SQLException {
    if (comm == null) {
      return;
    }
    try {
      final int frequency = Integer.parseInt(comm.frequency.split("\\.")[0]);
      if (frequency < 108 || frequency > 137) { // Skip (military?) frequency
        return;
      }
      // Normalize frequency use ("APP/P DEP/P" -> "APP/DEP")
      comm = getNormalizedAirportComm(comm);
      // Merge identical CTAF, TWR records
      final int id = comm.airportId;
      if ("TWR".equals(comm.identifier) && comm.frequency.equals(dbReader.getCtaf(id))) {
        final StringBuilder sb = new StringBuilder();
        if (comm.remarks != null && !comm.remarks.isEmpty()) {
          sb.append(comm.remarks);
          sb.append(" ");
        }
        sb.append("(also CTAF)");
        comm = new AirportComm(id, comm.identifier, comm.frequency, sb.toString());
        dbWriter.deleteCtaf(id);
      }
      dbWriter.insertAirportComm(comm.airportId, comm.identifier, comm.frequency, comm.remarks);
    } catch (NumberFormatException nfe) {
      System.err.println("Could not parse frequency: " + comm.frequency);
    }
  }

  private AirportComm getNormalizedAirportComm(final AirportComm comm) {
    final NormalizedFrequencyUseBean normalizedFreq = normalizedFreqUses.get(comm.identifier);
    if (normalizedFreq == null) {
      return comm;
    }
    final String identifier = normalizedFreq.getNormalizedUse();
    final String originalRemarks = comm.remarks;
    final String normalizedRemarks = normalizedFreq.getRemarks();
    String remarks = "";
    if (originalRemarks != null && !originalRemarks.isEmpty()) {
      remarks = originalRemarks;
    }
    if (normalizedRemarks != null && !normalizedRemarks.isEmpty()) {
      if (remarks.isEmpty()) {
        remarks = normalizedRemarks;
      } else {
        remarks += " " + normalizedRemarks;
      }
    }

    final AirportComm normalizedComm = 
      new AirportComm(comm.airportId, identifier, comm.frequency, remarks);
    return normalizedComm;
  }

  private static void printHelp(final CommandLine line) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(100);
    formatter.printHelp("CommParser", OPTIONS, true);
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

    final String twrFile =  line.getOptionValue(TWR_OPTION);
    final String iataToIcaoFile = line.getOptionValue(IATA_TO_ICAO_OPTION);
    final String freqUsesNormalizationFile = line.getOptionValue(FREQ_USES_NORMALIZATION_OPTION);
    final String dbFile = line.getOptionValue(AVIATION_DB_OPTION);

    try {
      (new CommParser(twrFile, iataToIcaoFile, freqUsesNormalizationFile, dbFile)).execute();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

}

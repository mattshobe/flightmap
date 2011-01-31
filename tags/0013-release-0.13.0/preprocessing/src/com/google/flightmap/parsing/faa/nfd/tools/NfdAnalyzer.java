/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.flightmap.parsing.faa.nfd.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;


/**
 * Analyzes ARINC 424-18 file and prints record statistics.
 * 
 */
public class NfdAnalyzer {
  // Command line options
  private final static Options OPTIONS = new Options();
  private final static String HELP_OPTION = "help";
  private final static String NFD_OPTION = "nfd";

  // IATA to ICAO pattern regex
  private final static Pattern volRecordPattern = Pattern.compile("VOL.{129}");
  private final static Pattern hdrRecordPattern = Pattern.compile("(?:HDR|EOF)(\\d).{128}");
  private final static Pattern dataRecordPattern = Pattern.compile("(S|T)(.{3})(\\S).{127}");

  static {
    // Command Line options definitions
    OPTIONS.addOption("h", "help", false, "Print this message.");
    OPTIONS.addOption(OptionBuilder.withLongOpt(NFD_OPTION)
                                   .withDescription("FAA National Flight Database.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("nfd.dat")
                                   .create());
  }
  private final File nfd;


  /**
   * @param nfd Source database in ARINC 424-18 format (eg NFD)
   */
  public NfdAnalyzer(final File nfd) {
    this.nfd = nfd;
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

    try {
      (new NfdAnalyzer(nfd)).execute();
    } catch (IOException ioEx) {
      ioEx.printStackTrace();
      System.exit(2);
    }
  }

  private static void printHelp(final CommandLine line) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(100);
    formatter.printHelp("NfdParser", OPTIONS, true);
  }

  private void execute() throws IOException {
    final BufferedReader in = new BufferedReader(new FileReader(nfd));
    NfdFileStats stats = new NfdFileStats();
    String line;
    Matcher m;
    try {
      while ((line = in.readLine()) != null) {
        ++stats.totalLines;
        // Handle generic data record
        m = dataRecordPattern.matcher(line);
        if (m.matches()) {
          processDataRecord(m, stats);
          continue;
        }

        // Handle header record
        m = hdrRecordPattern.matcher(line);
        if (m.matches()) {
          processHdrRecord(m, stats);
          continue;
        }

        // Handle volume header record
        m = volRecordPattern.matcher(line);
        if (m.matches()) {
          processVolRecord(m, stats);
          continue;
        }

        // Else, handle unknown record
        processUnknownRecord(line, stats);
      }
      // Display results of analysis
      printStats(stats);
    } finally {
      in.close();
    }
  }

  /**
   * Updates {@code stats} with unknown record corresponding to read {@code line}.
   */
  private void processUnknownRecord(final String line, final NfdFileStats s) {
    s.unknownRecords.put(s.totalLines, line);
  }

  /**
   * Updates {@code stats} with volume header record matched by {@link #volRecordPattern}.
   */
  private void processVolRecord(final Matcher m, final NfdFileStats s) {
    ++s.totalRecords;
    ++s.volRecords;
  }

  /**
   * Updates {@code stats} with header record matched by {@link #hdrRecordPattern}.
   */
  private void processHdrRecord(final Matcher m, final NfdFileStats s) {
    ++s.totalRecords;
    ++s.hdrRecords;
  }

  /**
   * Updates {@code stats} with data record matched by {@link #dataRecordPattern}.
   */
  private void processDataRecord(final Matcher m, final NfdFileStats s) {
    ++s.totalRecords;
    final String type = m.group(1);
    final String area = m.group(2);
    final String secCode = m.group(3);
   
    // Increment area count
    Integer count = s.recordsPerArea.get(area);
    if (count == null) count = 0;
    count = count + 1;
    s.recordsPerArea.put(area, count);

    // Increment section count
    Character subCode = m.group(0).charAt(5);
    if (subCode.equals(' ') && (secCode.equals("P") || secCode.equals("H"))) {
      subCode = m.group(0).charAt(12);
    }
    if (!NfdFileStats.isValidCategory(secCode, subCode)) {
      processUnknownRecord(m.group(0), s);
      return;
    }

    Map<Character, Integer> recordsPerSubsection = s.recordsPerSection.get(secCode);
    if (recordsPerSubsection == null) {
      recordsPerSubsection = new TreeMap<Character, Integer>();
      s.recordsPerSection.put(secCode, recordsPerSubsection);
      count = 0;
    } else {
      count = recordsPerSubsection.get(subCode);
      if (count == null) count = 0;
    }
    count = count + 1;
    recordsPerSubsection.put(subCode, count);

    ++s.dataRecords;
  }

  /**
   * Prints results from {@code s} in a nice format.
   */
  private void printStats(final NfdFileStats s) {
    System.out.println("Lines read: " + s.totalLines);
    System.out.println("Records");
    System.out.println("  Vol: " + s.volRecords);
    System.out.println("  Hdr: " + s.hdrRecords);
    System.out.println("  Data: " + s.dataRecords);
    System.out.println("  Unknown: " + s.unknownRecords.size());
    for (Map.Entry<Integer, String> unknownEntry: s.unknownRecords.entrySet()) {
      final Integer lineCount = unknownEntry.getKey();
      final String line = unknownEntry.getValue();
      System.out.println("    l." + lineCount + ": " + line);
    }
    System.out.println("Areas");
    for (Map.Entry<String, Integer> areaEntry: s.recordsPerArea.entrySet()) {
      final String area = areaEntry.getKey();
      final Integer count = areaEntry.getValue();
      System.out.println("  " + area + ": " + count);
    }
    System.out.println("Categories");
    for (Map.Entry<String, Map<Character, Integer>> secEntry: s.recordsPerSection.entrySet()) {
      final String sec = secEntry.getKey();
      final String secLabel = NfdFileStats.getLabel(sec, null);
      System.out.println("  " + sec + " (" + secLabel + ")");
      for (Map.Entry<Character, Integer> subEntry: secEntry.getValue().entrySet()) {
        final Character sub = subEntry.getKey();
        final String subLabel = NfdFileStats.getLabel(sec, sub);
        final Integer count = subEntry.getValue();
        System.out.println("    " + sub + " (" + subLabel + "): " + count);
      }
    }
  }

  /**
   * Data holder class for NFD file stats.
   */
  private static final class NfdFileStats {
    /**
     * Maps section codes to their label.
     */
    final static Map<String, String> SEC_LABELS;

    /**
     * Maps section codes to subsection codes to their label.
     */
    final static Map<String, Map<Character, String>> SUB_LABELS;

    static {
      SEC_LABELS = new LinkedHashMap<String, String>();
      SEC_LABELS.put("A", "MORA");
      SEC_LABELS.put("D", "Navaid");
      SEC_LABELS.put("E", "Enroute");
      SEC_LABELS.put("H", "Heliport");
      SEC_LABELS.put("P", "Airport");
      SEC_LABELS.put("R", "Company Routes");
      SEC_LABELS.put("T", "Tables");
      SEC_LABELS.put("U", "Airspace");

      SUB_LABELS = new LinkedHashMap<String, Map<Character, String>>();

      final Map<Character, String> moraSubLabels = new LinkedHashMap<Character, String>();
      SUB_LABELS.put("A", moraSubLabels);
      moraSubLabels.put('S', "Grid MORA");

      final Map<Character, String> navaidSubLabels = new LinkedHashMap<Character, String>();
      SUB_LABELS.put("D", navaidSubLabels);
      navaidSubLabels.put(' ', "VHF Navaid");
      navaidSubLabels.put('B', "NDB Navaid");

      final Map<Character, String> enrouteSubLabels = new LinkedHashMap<Character, String>();
      SUB_LABELS.put("E", enrouteSubLabels);
      enrouteSubLabels.put('A', "Waypoints");
      enrouteSubLabels.put('M', "Airway Markers");
      enrouteSubLabels.put('P', "Holding Patterns");
      enrouteSubLabels.put('R', "Airways and Routes");
      enrouteSubLabels.put('T', "Preferred Routes");
      enrouteSubLabels.put('U', "Airway Restrictions");
      enrouteSubLabels.put('V', "Communications");

      final Map<Character, String> heliportSubLabels = new LinkedHashMap<Character, String>();
      SUB_LABELS.put("H", heliportSubLabels);
      heliportSubLabels.put('A', "Pads");
      heliportSubLabels.put('C', "Terminal Waypoints");
      heliportSubLabels.put('D', "SIDs");
      heliportSubLabels.put('E', "STARs");
      heliportSubLabels.put('F', "Approach Procedures");
      heliportSubLabels.put('K', "TAA");
      heliportSubLabels.put('S', "MSA");
      heliportSubLabels.put('V', "Communications");

      final Map<Character, String> airportSubLabels = new LinkedHashMap<Character, String>();
      SUB_LABELS.put("P", airportSubLabels);
      airportSubLabels.put('A', "Reference Points");
      airportSubLabels.put('B', "Gates");
      airportSubLabels.put('C', "Terminal Waypoints");
      airportSubLabels.put('D', "SIDs");
      airportSubLabels.put('E', "STARs");
      airportSubLabels.put('F', "Approach Procedures");
      airportSubLabels.put('G', "Runways");
      airportSubLabels.put('I', "Localizer/Glide Slope");
      airportSubLabels.put('K', "TAA");
      airportSubLabels.put('L', "MLS");
      airportSubLabels.put('M', "Localizer Marker");
      airportSubLabels.put('N', "Terminal NDB");
      airportSubLabels.put('P', "Path Point");
      airportSubLabels.put('R', "Flt Planning ARR/DEP");
      airportSubLabels.put('S', "MSA");
      airportSubLabels.put('T', "GLS Station");
      airportSubLabels.put('V', "Communications");

      final Map<Character, String> companyRoutesSubLabels = new LinkedHashMap<Character, String>();
      SUB_LABELS.put("R", companyRoutesSubLabels);
      companyRoutesSubLabels.put(' ', "Company Routes");
      companyRoutesSubLabels.put('A', "Alternate Records");

      final Map<Character, String> tablesSubLabels = new LinkedHashMap<Character, String>();
      SUB_LABELS.put("T", tablesSubLabels);
      tablesSubLabels.put('C', "Cruising Tables");
      tablesSubLabels.put('G', "Geographical Reference");
      tablesSubLabels.put('N', "RNAV Name Table");

      final Map<Character, String> airspaceSubLabels = new LinkedHashMap<Character, String>();
      SUB_LABELS.put("U", airspaceSubLabels);
      airspaceSubLabels.put('C', "Controlled Airspace");
      airspaceSubLabels.put('F', "FIR/UIR");
      airspaceSubLabels.put('R', "Restrictive Airspace");
    }

    /**
     * Number of lines read in file.
     */
    int totalLines;

    /**
     * Number of records read in file.
     */
    int totalRecords;

    /**
     * Number of volume header records found in file.
     */
    int volRecords;

    /**
     * Number of header records found in file.
     */
    int hdrRecords;

    /**
     * Number of data records found in file.
     */
    int dataRecords;

    /**
     * Maps section code and subsection code to the number of corresponding records found.
     */
    Map<String, Map<Character, Integer>> recordsPerSection; // Sec code -> Sub code -> Count

    /**
     * Maps area/customer codes to the number of corresponding records found.
     */
    Map<String, Integer> recordsPerArea;

    /**
     * Maps line numbers to their text for lines that corresponded to unknown records.
     */
    SortedMap<Integer, String> unknownRecords;

    NfdFileStats() {
      recordsPerSection = new TreeMap<String, Map<Character, Integer>>();
      recordsPerArea = new TreeMap<String, Integer>();
      unknownRecords = new TreeMap<Integer, String>();
    }

    /**
     * Returns label for section {@code sec} and (optionaly) subsection {@code sub}.
     *
     * @param sec Section code, MUST NOT be {@code null}.
     * @param sub Subsection code, MAY be {@code null}.
     * @return If {@code sub} is {@code null}, the label corresponding to section {@code sec}.
     * Otherwise, the label of subsection {@code sub} in section {@code sec}.
     */
    static String getLabel(final String sec, final Character sub) {
      if (sub == null) {
        return SEC_LABELS.get(sec);
      }
      return SUB_LABELS.get(sec).get(sub);
    }

    /**
     * Checks if the given section and (optionaly) subsection codes are valid.
     * @param sec Section code
     * @param sub Subsection code
     * @return If {@code sub} is {@code null}: {@code true} if section {@code sec} is valid. If
     * {@code sub} is not {@code null}: {@code true} is the corresponding section and subsection are
     * valid. Otherwise, {@code false}.
     */
    static boolean isValidCategory(final String sec, final Character sub) {
      try {
        final String label = getLabel(sec, sub);
        return label != null;
      } catch (Exception ex) {
        return false;
      }
    }
  }
  
}

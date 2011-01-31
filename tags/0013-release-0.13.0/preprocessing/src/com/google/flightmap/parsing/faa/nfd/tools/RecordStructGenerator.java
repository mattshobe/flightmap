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


public class RecordStructGenerator {
  // Command line options
  private final static Options OPTIONS = new Options();
  private final static String HELP_OPTION = "help";
  private final static String DEF_FILE_OPTION = "def";

  private final static Pattern fieldDefPattern = Pattern.compile("(\\w+) (\\d+)");
  private final static String BLANK_FIELD_NAME = "BLANK";
  private final static String PACKAGE = "com.google.flightmap.parsing.faa.nfd.data";
  private final static String COPYRIGHT = 
      "/*\n" +
      " * Copyright (C) 2011 Google Inc.\n" +
      " *\n" +
      " * Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
      " * you may not use this file except in compliance with the License.\n" +
      " * You may obtain a copy of the License at\n" +
      " *\n" +
      " *     http://www.apache.org/licenses/LICENSE-2.0\n" +
      " *\n" +
      " * Unless required by applicable law or agreed to in writing, software\n" +
      " * distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
      " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
      " * See the License for the specific language governing permissions and\n" +
      " * limitations under the License.\n" +
      " */";

  static {
    // Command Line options definitions
    OPTIONS.addOption("h", "help", false, "Print this message.");
    OPTIONS.addOption(OptionBuilder.withLongOpt(DEF_FILE_OPTION)
                                   .withDescription("Struct definition file.")
                                   .hasArg()
                                   .isRequired()
                                   .withArgName("StructClas.def")
                                   .create());
  }

  private final File def;
  private Map<String, int[]> fields; // field name -> begin, end index
  private String className;
  private PrintStream out;
  private int indent = 0;

  public RecordStructGenerator(final File def) {
    this.def = def;
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

    final String defPath = line.getOptionValue(DEF_FILE_OPTION);
    final File def = new File(defPath);

    try {
      (new RecordStructGenerator(def)).execute();
    } catch (IOException ioEx) {
      ioEx.printStackTrace();
      System.exit(2);
    }
  }

  private static void printHelp(final CommandLine line) {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.setWidth(100);
    formatter.printHelp("RecordStructGenerator", OPTIONS, true);
  }

  private void execute() throws IOException {
    parseFields();
    initOutputFile();
    writeClass();
    closeOutputFile();
  }

  private void parseFields() throws IOException {
    final BufferedReader in = new BufferedReader(new FileReader(def));
    fields = new LinkedHashMap<String, int[]>();
    Matcher m;
    String line;
    int currentIndex = 0;
    try {
      while ((line = in.readLine()) != null) {
        if ( (m = fieldDefPattern.matcher(line)).matches()) {
          final String name = m.group(1);
          final int length = Integer.parseInt(m.group(2));
          final int nextIndex = currentIndex + length;
          if (!BLANK_FIELD_NAME.equals(name)) {
            final int[] indices = new int[] {currentIndex, nextIndex};
            fields.put(name, indices);
          }
          currentIndex = nextIndex;
        }
      }
    } finally {
      in.close();
    }
  }

  private void initOutputFile() throws IOException {
    className = def.getName().split("\\.")[0];
    out = new PrintStream(className + ".java");
    print(COPYRIGHT);
    print("");
  }

  private void writeClass() throws IOException {
    print("package " + PACKAGE + ";");
    print("");
    print("public class " + className + " {");
    ++indent;
    writeFields();
    print("");
    writeConstructor();
    print("");
    writeMethods();
  }

  private void writeFields() throws IOException {
    for (String field: fields.keySet()) {
      print("public String " + field +";");
    }
  }

  private void writeConstructor() throws IOException {
    print("public " + className + "(");
    indent += 2;
    final int fieldCount = fields.size();
    int count = 0;
    for (String field: fields.keySet()) {
      String text = "final String " + field;
      if (++count < fieldCount) {
        text += ",";
      } else {
        text += ") {";
      }
      print(text);
    }
    --indent;
    for (String field: fields.keySet()) {
      print("this." + field + " = " + field +";");
    }
    --indent;
    print("}");
  }

  private void writeMethods() throws IOException {
    print("public static " + className + " parse(final String line) {");
    ++indent;
    for (Map.Entry<String, int[]> entry: fields.entrySet()) {
      final String name = entry.getKey();
      final int[] indices = entry.getValue();
      print("final String " + name + " = line.substring(" + indices[0] + ", " + indices[1] + ");");
    }

    print("return new " + className + "(");
    indent += 2;
    final int fieldCount = fields.size();
    int count = 0;
    for (String field: fields.keySet()) {
      String text = field;
      if (++count < fieldCount) {
        text += ",";
      } else {
        text += ");";
      }
      print(text);
    }
    indent -= 2;
    print("}");
  }

  private void closeOutputFile() throws IOException {
    --indent;
    print("}");
    out.close();
  }


  private void print(final int indent, final String text) {
    for (int i = 0; i < indent; ++i) {
      out.print("  ");
    }
    out.println(text);
  }

  private void print(final String text) {
    print(indent, text);
  }
}

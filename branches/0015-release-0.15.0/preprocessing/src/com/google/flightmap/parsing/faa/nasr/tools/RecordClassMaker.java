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

package com.google.flightmap.parsing.faa.nasr.tools;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.LinkedList;

import org.apache.commons.lang.WordUtils;

public class RecordClassMaker {
  private final String name;
  private final List<RecordEntry> entries;

  private RecordClassMaker(final String name, final List<RecordEntry> entries) {
    this.name = name;
    this.entries = entries;
  }

  public static void main(String[] args) {
    try {
      final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      final String name = in.readLine();
      final List<RecordEntry> entries = new LinkedList<RecordEntry>();
      String line;
      while ((line = in.readLine()) != null) {
        final String[] parts = line.split("\\s+");
        final int length = Integer.parseInt(parts[0]);
        final int start = Integer.parseInt(parts[1]);
        final String entryName = parts[2];
        entries.add(new RecordEntry(length, start, entryName));
      }
      (new RecordClassMaker(name, entries)).execute();
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }

  private void execute() throws FileNotFoundException {
    final String className = WordUtils.capitalize(name.toLowerCase());
    final PrintWriter out = new PrintWriter(className + ".java");
    out.println("class " + className + " {");
    for (RecordEntry entry: entries) {
      out.println("  final String " + entry.name + ";");
    }
    out.println();
    out.println("  " + className + "(final String line) {");
    for (RecordEntry entry: entries) {
      final int start = entry.start - 1;
      out.println("    " + entry.name + " = line.substring(" + start + ", " + start + " + " + 
          entry.length + ").trim();");
    }
    out.println("  }");
    out.println("");
    out.println("  static boolean matches(final String line) {");
    out.println("    return line.startsWith(\"" + name + "\");");
    out.println("  }");
    out.println("}");
    out.close();
  }

  private static class RecordEntry {
    final int length;
    final int start;
    final String name;

    RecordEntry(final int length, final int start, final String name) {
      this.length = length;
      this.start = start;
      this.name = name;
    }
  }
}


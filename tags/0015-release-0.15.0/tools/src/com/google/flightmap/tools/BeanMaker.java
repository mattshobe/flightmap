/*
 * Copyright (C) 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.flightmap.tools;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Set;
import java.util.TreeSet;


/**
 * Utility class for Java Bean creation.
 * Reads members from stdin and creates a Java Bean with the corresponding accessors.
 * Must be called with the Bean class name as single argument.
 */
public class BeanMaker {

  public static void main(String[] args) {
    try {
      final String beanName = args[0];
      final BufferedReader in = new BufferedReader(
          new InputStreamReader(System.in));
      final PrintStream out = new PrintStream(beanName + ".java");

      Set<String> fields = new TreeSet<String>();
      String line;
      while ((line = in.readLine()) != null) {
        fields.add(line);
      }

      out.println("public class " + beanName + " {");
      for (String field: fields) {
        out.println("  private String " + field + ";");
      }
      for (String field: fields) {
        final StringBuilder sb = new StringBuilder(field);
        sb.setCharAt(0, Character.toUpperCase(field.charAt(0)));
        final String capitalizedField = sb.toString();
        out.println();
        out.println("  public void set" + capitalizedField + "(final String " + field + ") {");
        out.println("    this." + field + " = " + field + ";");
        out.println("  }");
        out.println();
        out.println("  public String get" + capitalizedField + "() {");
        out.println("    return " + field + ";");
        out.println("  }");
      }
      out.println();
      out.println("  @Override");
      out.println("  public String toString() {");
      out.println("    final StringBuilder sb = new StringBuilder();");
      for (String field: fields) {
        out.println("    sb.append(" + field + ");");
        out.println("    sb.append(\" \");");
      }
      out.println("    return sb.toString();");
      out.println("  }");
      out.println("}");
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
  }
}

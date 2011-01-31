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

package com.google.flightmap.parsing.util;

import org.apache.commons.lang.WordUtils;

public class StringUtils {

  /**
   *  Utility class: default and only constructor is private.
   */
  private StringUtils() { }

  /**
   * Returns mixed-case version of {@code text}.
   * <p>
   * The first letter of each word is capitalized, the rest are lower case.
   * Words are delimited by spaces and special characters (except single-quote).
   * "REID-HILLVIEW" becomes "Reid-Hillview".
   * 
   * @return mixed-case version of {@code text} with each word capitalized.
   */
  public static String capitalize(final String text) {
    final StringBuilder sb = new StringBuilder(WordUtils.capitalize(text.toLowerCase()));
    boolean makeNextLetterUpper = false;
    for (int i = 0; i < sb.length(); ++i) {
      final char cur = sb.charAt(i);
      if (Character.isWhitespace(cur)) {
        continue;  // Skip whitespace
      } else if (Character.isLetterOrDigit(cur)) {
        if (makeNextLetterUpper) {
          sb.setCharAt(i, Character.toUpperCase(cur));
          makeNextLetterUpper = false;
        } else {
          continue;  // Skip character if no change is neded
        }
      } else {  // Not whitespace, letter or digit:  we assume punctuation.
        makeNextLetterUpper = cur != '\'';  // Ignore single quote (John'S, Susie'S, ...)
      }
    }
    return sb.toString();
  }

}

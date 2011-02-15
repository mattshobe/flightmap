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

package com.google.flightmap.parsing.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class IcaoUtils {

  /**
   *  Utility class: default and only constructor is private.
   */
  private IcaoUtils() { }

  public static BiMap<String, String> parseIataToIcao(final String file) throws IOException {
    final BiMap<String, String> iataToIcao = HashBiMap.<String, String>create();
    BufferedReader in = null;
    try {
      in = new BufferedReader(new FileReader(file));
      String line;
      while ((line = in.readLine()) != null) {
        final String[] codes = line.split(" ");
        iataToIcao.put(codes[0], codes[1]);
      }
      return iataToIcao;
    } finally {
      if (in != null) {
        in.close();
      }
    }
  }
}

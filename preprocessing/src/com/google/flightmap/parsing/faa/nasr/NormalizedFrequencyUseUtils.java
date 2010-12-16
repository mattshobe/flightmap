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

import com.google.flightmap.parsing.util.CsvParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class NormalizedFrequencyUseUtils {
  private NormalizedFrequencyUseUtils() { }

  static Map<String, NormalizedFrequencyUseBean> parse(final String file) throws IOException {
    final Map<String, NormalizedFrequencyUseBean> freqUses =
        new HashMap<String, NormalizedFrequencyUseBean>();
    final Parser parser = new Parser(new File(file));
    List<NormalizedFrequencyUseBean> beans = parser.getAll();
    for (NormalizedFrequencyUseBean bean: beans) {
      freqUses.put(bean.getOriginalUse(), bean);
    }
    return freqUses;
  }

  /**
   * Parser for CSV files with normalized frequency usage data.
   */
  private static class Parser extends CsvParser<NormalizedFrequencyUseBean> {
    private final static Map<String, String> CSV_COLUMN_MAPPING;

    static {
      CSV_COLUMN_MAPPING = new HashMap<String, String>();
      CSV_COLUMN_MAPPING.put("original use", "originalUse");
      CSV_COLUMN_MAPPING.put("normalized use", "normalizedUse");
      CSV_COLUMN_MAPPING.put("remarks", "remarks");
    }

    Parser(final File file) {
      super(file, CSV_COLUMN_MAPPING, NormalizedFrequencyUseBean.class);
    }
  }
}


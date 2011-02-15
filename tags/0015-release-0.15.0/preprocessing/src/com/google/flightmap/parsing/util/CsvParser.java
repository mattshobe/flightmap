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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import au.com.bytecode.opencsv.bean.CsvToBean;
import au.com.bytecode.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;


public class CsvParser<T> {
  private final File file;
  private final Map<String, String> columnMap;
  private final Class<T> tClass;

  public CsvParser(final File file, final Map<String, String> columnMap, final Class<T> tClass) {
    this.file = file;
    this.columnMap = columnMap;
    this.tClass = tClass;
  }

  public List<T> getAll() throws IOException {
    final HeaderColumnNameTranslateMappingStrategy<T> strat = 
        new HeaderColumnNameTranslateMappingStrategy<T>();
    strat.setType(tClass);
    strat.setColumnMapping(columnMap);
    final CsvToBean<T> csv = new CsvToBean<T>();
    return csv.parse(strat, new BufferedReader(new FileReader(file)));
  }
}

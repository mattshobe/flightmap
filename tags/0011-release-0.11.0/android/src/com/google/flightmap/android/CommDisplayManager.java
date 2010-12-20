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

package com.google.flightmap.android;

import com.google.flightmap.common.data.Comm;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

class CommDisplayManager {
  private final static String ATIS = "ATIS";
  private final static String CTAF = "CTAF";
  private final static String CLEARANCE = "CLDC";
  private final static String GROUND = "GND";
  private final static String TOWER = "TWR";
  private final static String APPDEP = "APP/DEP";
  private final static String OTHER = "OTHER";
  private final static Set<String> COMM_CATEGORIES_ORDER;

  static {
    COMM_CATEGORIES_ORDER = new LinkedHashSet<String>();
    COMM_CATEGORIES_ORDER.add(ATIS);
    COMM_CATEGORIES_ORDER.add(CTAF);
    COMM_CATEGORIES_ORDER.add(CLEARANCE);
    COMM_CATEGORIES_ORDER.add(GROUND);
    COMM_CATEGORIES_ORDER.add(TOWER);
    COMM_CATEGORIES_ORDER.add(APPDEP);
    COMM_CATEGORIES_ORDER.add(OTHER);
  }

  private final Map<String, SortedSet<Comm>> commsPerCategory;
  private final List<Comm> sortedComms;

  CommDisplayManager(final Collection<Comm> comms) {
    commsPerCategory = getCommsPerCategory(comms);
    sortedComms = sortComms(commsPerCategory);
  }

  /**
   * Returns Comm frequencies sorted according to their corresponding category.
   */
  List<Comm> getSortedComms() {
    return sortedComms;
  }

  /**
   * Returns comm frequencies sorted by category.
   */
  private static Map<String, SortedSet<Comm>> getCommsPerCategory(final Collection<Comm> comms) {
    final Map<String, SortedSet<Comm>> commsPerCategory = new HashMap<String, SortedSet<Comm>>();
    for (Comm comm: comms) {
      final String category = COMM_CATEGORIES_ORDER.contains(comm.identifier) ? 
          comm.identifier : 
          OTHER;
      SortedSet<Comm> commsInSameCategory = commsPerCategory.get(category);
      if (commsInSameCategory == null) {
        commsInSameCategory = new TreeSet<Comm>();
        commsPerCategory.put(category, commsInSameCategory);
      }
      commsInSameCategory.add(comm);
    }
    return commsPerCategory;
  }

  /**
   * Sorts comm frequencies according to the category order.
   */
  private List<Comm> sortComms(final Map<String, SortedSet<Comm>> commsPerCategory) {
    final List<Comm> sortedComms = new LinkedList<Comm>();
    for (String category: COMM_CATEGORIES_ORDER) {
      final SortedSet<Comm> commsInCategory = commsPerCategory.get(category);
      if (commsInCategory != null) {
        sortedComms.addAll(commsInCategory);
      }
    }
    return sortedComms;
  }
}

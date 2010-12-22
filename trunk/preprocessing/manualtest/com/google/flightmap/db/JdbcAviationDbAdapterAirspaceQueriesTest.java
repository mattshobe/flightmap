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

package com.google.flightmap.db;

import java.sql.*;
import java.util.*;

import com.google.flightmap.common.data.*;
import com.google.flightmap.common.db.AviationDbAdapter;

public class JdbcAviationDbAdapterAirspaceQueriesTest {
  private final AviationDbAdapter dbAdapter;
  private final LatLngRect rect;

  private JdbcAviationDbAdapterAirspaceQueriesTest(final String dbPath, final LatLngRect rect)
      throws Exception {
    System.out.println("Initializing JdbcAviationDbAdapter...");
    dbAdapter = new JdbcAviationDbAdapter(initDb(dbPath));
    this.rect = rect;
  }

  private void runTest() throws Exception {

    final Collection<Airspace> airspaces = dbAdapter.getAirspacesInRectangle(rect);
    System.out.println("Airspace count: " + airspaces.size());
    for (Airspace airspace: airspaces) {
      System.out.println(airspace);
    }
  }

  /**
   * Gets a connection to the database.
   */
  private static Connection initDb(final String path) throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    return DriverManager.getConnection("jdbc:sqlite:" + path);
  }

  public static void main(String[] args) {
    try {
      final String dbPath = args[0];
      final int lat = Integer.parseInt(args[1]);
      final int lng = Integer.parseInt(args[2]);
      final LatLng position = new LatLng(lat, lng);
      System.out.println("Position: " + position);
      final int radius = Integer.parseInt(args[3]);
      final LatLngRect rect = LatLngRect.getBoundingBox(position, radius);
      System.out.println("Rect: " + rect);
      new JdbcAviationDbAdapterAirspaceQueriesTest(dbPath, rect).runTest();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }


}

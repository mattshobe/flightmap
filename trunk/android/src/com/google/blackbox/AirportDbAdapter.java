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
package com.google.blackbox;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.blackbox.data.Airport;
import com.google.blackbox.data.LatLng;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

/**
 * Provides read-only access to the airport database. The database is created
 * offline by the parsing code.
 * 
 * @author Phil Verghese
 */
public class AirportDbAdapter {
  private static final String TAG = AirportDbAdapter.class.getSimpleName();
  public static final String ID_COLUMN = "_id";
  public static final String ICAO_COLUMN = "icao";
  public static final String NAME_COLUMN = "name";
  public static final String LAT_COLUMN = "lat";
  public static final String LNG_COLUMN = "lng";
  public static final String CELL_ID_COLUMN = "cell_id";
  private static final String DATABASE_PATH = "/sdcard/com.google.blackbox/aviation.db";
  private static final String AIRPORT_TABLE = "airports";
  private static final String CELL_ID_WHERE =
      String.format("%s >= ? and %s < ?", CELL_ID_COLUMN, CELL_ID_COLUMN);
  private static final String ID_WHERE = ID_COLUMN + " = ?";
  private static final String[] LOCATION_COLUMNS =
      new String[] {ID_COLUMN, LAT_COLUMN, LNG_COLUMN, CELL_ID_COLUMN};
  private static final String[] AIRPORT_COLUMNS =
      new String[] {ID_COLUMN, ICAO_COLUMN, NAME_COLUMN};
  private SQLiteDatabase database;

  /**
   * Opens the airport database.
   * 
   * @return this object for chaining.
   * @throws SQLException on database error.
   */
  public synchronized AirportDbAdapter open() throws SQLException {
    database = SQLiteDatabase.openDatabase(DATABASE_PATH, null, SQLiteDatabase.OPEN_READONLY);
    return this;
  }

  public synchronized void close() {
    database.close();
  }

  /**
   * Find the nearest airports to a given position.
   * 
   * @param position Target position.
   * @param numAirports Maximum number of airports to return.
   * @return Airports sorted by increasing distance. Returns an empty set on
   *         error.
   */
  public SortedSet<AirportDistance> getNearestAirports(final LatLng position, final int numAirports) {
    Log.e(TAG, "getNearestAirports not implemented yet.");
    return new TreeSet<AirportDistance>();
  }

  /**
   * Find the airports within {@code radius} from {@code position}.
   * 
   * @param position position to search from.
   * @param radius in nautical miles.
   * @return Airports within radius, sorted by increasing distance. Returns an
   *         empty set on error.
   */
  public synchronized SortedSet<AirportDistance> getAirportsWithinRadius(final LatLng position,
      final double radius) {
    List<int[]> cellRanges =
        CustomGridUtil.GetCellsInRadius(position, getRadiusE6(position, radius));
    Log.d(TAG, "cellRanges.size=" + cellRanges.size());
    final SortedSet<AirportDistance> result = Sets.newTreeSet();

    final String[] stringRange = new String[2]; // query requires a String[].
    for (int[] range : cellRanges) {
      stringRange[0] = Integer.toString(range[0]);
      stringRange[1] = Integer.toString(range[1]);
      Cursor locations =
          database.query(AIRPORT_TABLE, LOCATION_COLUMNS, CELL_ID_WHERE, stringRange, null, null,
              null);
      try {
        if (!locations.moveToFirst()) { // No airports in cell_id range.
          continue;
        }

        final int idColumn = locations.getColumnIndexOrThrow(ID_COLUMN);
        final int latColumn = locations.getColumnIndexOrThrow(LAT_COLUMN);
        final int lngColumn = locations.getColumnIndexOrThrow(LNG_COLUMN);
        do {
          int lat = locations.getInt(latColumn);
          int lng = locations.getInt(lngColumn);
          LatLng location = new LatLng(lat, lng);
          double distance = NavigationUtil.computeDistance(position, location);
          if (distance <= radius) {
            int id = locations.getInt(idColumn);
            result.add(new AirportDistance(getAirport(id, location), (float) distance));
          }
        } while (locations.moveToNext());
      } finally {
        locations.close();
      }
    }
    return result;
  }

  private int getRadiusE6(final LatLng position, final double radius) {
    double earthRadiusAtLat =
        NavigationUtil.EARTH_RADIUS * Math.sin(Math.PI / 2 - position.latRad());
    double longRadius = radius / (2 * Math.PI * earthRadiusAtLat) * 360;
    double latRadius = radius / 60;
    return (int) (Math.max(longRadius, latRadius) * 1E6);
  }

  /**
   * Returns the airport with ICAO code and name for the given database id, and
   * set to the given location.
   */
  private Airport getAirport(int id, LatLng location) {
    String[] stringId = {Integer.toString(id)};
    Cursor airports =
        database.query(AIRPORT_TABLE, AIRPORT_COLUMNS, ID_WHERE, stringId, null, null, null);
    try {
      if (!airports.moveToFirst()) {
        Log.e(TAG, "No airport for id =" + id);
        return null;
      }
      String icao = airports.getString(airports.getColumnIndexOrThrow(ICAO_COLUMN));
      String name = airports.getString(airports.getColumnIndexOrThrow(NAME_COLUMN));
      return new Airport(icao, name, location);
    } finally {
      airports.close();
    }
  }

  /**
   * An airport and its distance.
   */
  public static class AirportDistance implements Comparable<AirportDistance> {
    public final Airport airport;
    /** Distance to {@link #airport} in nautical miles. */
    public final float distance;

    public AirportDistance(Airport airport, float distance) {
      this.airport = airport;
      this.distance = distance;
    }

    @Override
    public int compareTo(AirportDistance o) {
      int sign = (int) (Math.signum(this.distance - o.distance));
      return sign != 0 ? sign : this.airport.icao.compareTo(o.airport.icao);
    }

    @Override
    public String toString() {
      return String.format("%s - %.1f", airport.toString(), distance);
    }
  }
}

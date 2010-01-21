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

import java.util.SortedSet;

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
  public static final String ID_COLUMN = "id";
  public static final String ICAO_COLUMN = "icao";
  public static final String NAME_COLUMN = "name";
  public static final String LAT_COLUMN = "lat";
  public static final String LNG_COLUMN = "lng";
  private static final String DATABASE_PATH = "/sdcard/com.google.blackbox/aviation.db";
  private static final String AIRPORT_TABLE = "airports";
  private static final String[] AIRPORT_COLUMNS =
      new String[] {ID_COLUMN, ICAO_COLUMN, NAME_COLUMN, LAT_COLUMN, LNG_COLUMN};
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

  public synchronized Cursor fetchAllAirports() {
    return database.query(AIRPORT_TABLE, AIRPORT_COLUMNS, null, null, null, null, null);
  }

  /**
   * Find the nearest airports to a given position.
   * <p>
   * Currently does a naive linear scan over all (~14'000) airports.
   * 
   * @param position Target position.
   * @param numAirports Maximum number of airports to return.
   * @return array of results, or null on error.
   */
  public AirportDistance[] getNearestAirports(final LatLng position, final int numAirports) {
    final SortedSet<AirportDistance> nearestAirportIDs = Sets.newTreeSet();
    boolean ok = applyPredicateToAirports(new Predicate<Airport>() {
      @Override
      public boolean apply(Airport airport) {
        double distance = NavigationUtil.computeDistance(position, airport.location);
        nearestAirportIDs.add(new AirportDistance(airport, distance));
        if (nearestAirportIDs.size() > numAirports) {
          nearestAirportIDs.remove(nearestAirportIDs.last());
        }
        return true;
      }
    });
    return ok ? nearestAirportIDs.toArray(new AirportDistance[1]) : null;
  }

  /**
   * Find the airports within {@code radius} from {@code position}.
   * <p>
   * Currently does a naive linear scan over all (~14'000) airports.
   * </p>
   * 
   * @return Airports within radius, sorted by increasing distance. Returns null
   *         on error.
   */
  public AirportDistance[] getAirportsWithinRadius(final LatLng position, final double radius) {
    final SortedSet<AirportDistance> airportsWithinRadius = Sets.newTreeSet();
    boolean ok = applyPredicateToAirports(new Predicate<Airport>() {
      @Override
      public boolean apply(Airport airport) {
        double distance = NavigationUtil.computeDistance(position, airport.location);
        if (distance <= radius) {
          airportsWithinRadius.add(new AirportDistance(airport, distance));
        }
        return true;
      }
    });
    return ok ? airportsWithinRadius.toArray(new AirportDistance[1]) : null;
  }

  /**
   * Loops over all airports in the database and applies {@code predicate} to
   * each one.
   * 
   * @return true on success, false on failure.
   */
  private boolean applyPredicateToAirports(final Predicate<Airport> predicate) {
    Cursor airports = fetchAllAirports();
    if (!airports.moveToFirst()) {
      Log.e(TAG, "No airports in database");
      return false;
    }

    final int icaoColumn = airports.getColumnIndexOrThrow(ICAO_COLUMN);
    final int nameColumn = airports.getColumnIndexOrThrow(NAME_COLUMN);
    final int latColumn = airports.getColumnIndexOrThrow(LAT_COLUMN);
    final int lngColumn = airports.getColumnIndexOrThrow(LNG_COLUMN);
    do {
      String icao = airports.getString(icaoColumn);
      String name = airports.getString(nameColumn);
      int lat = airports.getInt(latColumn);
      int lng = airports.getInt(lngColumn);
      Airport airport = new Airport(icao, name, lat, lng);
      if (!predicate.apply(airport)) {
        Log.w(TAG, "Predicate failure for " + airport);
        return false;
      }
    } while (airports.moveToNext());
    return true;
  }

  /**
   * An airport and its distance.
   */
  public static class AirportDistance implements Comparable<AirportDistance> {
    public final Airport airport;
    /** Distance to {@link #airport} in nautical miles. */
    public final double distance;

    public AirportDistance(Airport airport, double distance) {
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

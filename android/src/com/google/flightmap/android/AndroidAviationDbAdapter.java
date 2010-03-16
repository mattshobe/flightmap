package com.google.flightmap.android;

import java.util.LinkedList;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLng;

public class AndroidAviationDbAdapter implements AviationDbAdapter {
  private static final String TAG = AndroidAviationDbAdapter.class.getSimpleName();
  public static final String ID_COLUMN = "_id";
  public static final String ICAO_COLUMN = "icao";
  public static final String NAME_COLUMN = "name";
  public static final String LAT_COLUMN = "lat";
  public static final String LNG_COLUMN = "lng";
  public static final String CELL_ID_COLUMN = "cell_id";
  private static final String DATABASE_PATH = "/sdcard/com.google.flightmap/aviation.db";
  private static final String AIRPORT_TABLE = "airports";
  private static final String CELL_ID_WHERE =
      String.format("%s >= ? and %s < ?", CELL_ID_COLUMN, CELL_ID_COLUMN);
  private static final String ID_WHERE = ID_COLUMN + " = ?";
  private static final String[] LOCATION_COLUMNS =
      new String[] {ID_COLUMN, LAT_COLUMN, LNG_COLUMN, CELL_ID_COLUMN};
  private static final String[] AIRPORT_COLUMNS =
      new String[] {ID_COLUMN, ICAO_COLUMN, NAME_COLUMN};
  private static final String[] AIRPORT_LOCATION_COLUMNS =
      new String[] {ID_COLUMN, ICAO_COLUMN, NAME_COLUMN, LAT_COLUMN, LNG_COLUMN};

  private SQLiteDatabase database;

  /**
   * Opens the airport database.
   * 
   * @throws SQLException on database error.
   */
  public synchronized void open() {
    database = SQLiteDatabase.openDatabase(DATABASE_PATH, null, SQLiteDatabase.OPEN_READONLY);
  }

  public synchronized void close() {
    database.close();
  }


  @Override
  public Airport getAirport(int id) {
    String[] stringId = {Integer.toString(id)};
    Cursor airports =
        database.query(AIRPORT_TABLE, AIRPORT_LOCATION_COLUMNS, ID_WHERE, stringId, null, null,
            null);
    try {
      if (!airports.moveToFirst()) {
        Log.e(TAG, "No airport for id =" + id);
        return null;
      }
      String icao = airports.getString(airports.getColumnIndexOrThrow(ICAO_COLUMN));
      String name = airports.getString(airports.getColumnIndexOrThrow(NAME_COLUMN));
      int latE6 = airports.getInt(airports.getColumnIndexOrThrow(LAT_COLUMN));
      int lngE6 = airports.getInt(airports.getColumnIndexOrThrow(LNG_COLUMN));
      return new Airport(icao, name, new LatLng(latE6, lngE6));
    } finally {
      airports.close();
    }
  }

  @Override
  public LinkedList<Airport> getAirportsInCells(int startCell, int endCell) {
    String[] stringRange = {Integer.toString(startCell), Integer.toString(endCell)};
    Cursor locations =
        database.query(AIRPORT_TABLE, LOCATION_COLUMNS, CELL_ID_WHERE, stringRange, null, null,
            null);
    LinkedList<Airport> result = new LinkedList<Airport>();
    try {
      if (!locations.moveToFirst()) { // No airports in cell_id range.
        return result;
      }

      final int idColumn = locations.getColumnIndexOrThrow(ID_COLUMN);
      final int latColumn = locations.getColumnIndexOrThrow(LAT_COLUMN);
      final int lngColumn = locations.getColumnIndexOrThrow(LNG_COLUMN);
      do {
        int id = locations.getInt(idColumn);
        int lat = locations.getInt(latColumn);
        int lng = locations.getInt(lngColumn);
        LatLng location = new LatLng(lat, lng);
        result.add(getAirport(id, location));
      } while (locations.moveToNext());
    } finally {
      locations.close();
    }
    return result;
  }

  private Airport getAirport(int id, LatLng knownLocation) {
    String[] stringId = {Integer.toString(id)};
    Cursor airports =
        database.query(AIRPORT_TABLE, AIRPORT_COLUMNS, ID_WHERE, stringId, null, null,
            null);
    try {
      if (!airports.moveToFirst()) {
        Log.e(TAG, "No airport for id =" + id);
        return null;
      }
      String icao = airports.getString(airports.getColumnIndexOrThrow(ICAO_COLUMN));
      String name = airports.getString(airports.getColumnIndexOrThrow(NAME_COLUMN));
      return new Airport(icao, name, knownLocation);
    } finally {
      airports.close();
    }
  }
}

package com.google.flightmap.android;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeSet;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.Runway;
import com.google.flightmap.common.data.RunwayEnd;

public class AndroidAviationDbAdapter implements AviationDbAdapter {
  private static final String TAG = AndroidAviationDbAdapter.class.getSimpleName();
  // database
  private static final String DATABASE_PATH = "/sdcard/com.google.flightmap/aviation.db";
  //   airports
  private static final String AIRPORTS_TABLE = "airports";
  private static final String ID_COLUMN = "_id";
  private static final String ICAO_COLUMN = "icao";
  private static final String NAME_COLUMN = "name";
  private static final String TYPE_COLUMN = "type";
  private static final String CITY_COLUMN = "city";
  private static final String LAT_COLUMN = "lat";
  private static final String LNG_COLUMN = "lng";
  private static final String CELL_ID_COLUMN = "cell_id";
  private static final String CELL_ID_WHERE =
      String.format("%s >= ? and %s < ?", CELL_ID_COLUMN, CELL_ID_COLUMN);
  private static final String ID_WHERE = ID_COLUMN + " = ?";
  private static final String[] LOCATION_COLUMNS =
      new String[] {ID_COLUMN, LAT_COLUMN, LNG_COLUMN, CELL_ID_COLUMN};
  private static final String[] AIRPORT_COLUMNS =
      new String[] {ID_COLUMN, ICAO_COLUMN, NAME_COLUMN, TYPE_COLUMN, CITY_COLUMN};
  private static final String[] AIRPORT_LOCATION_COLUMNS =
      new String[] {ID_COLUMN, ICAO_COLUMN, NAME_COLUMN, TYPE_COLUMN, CITY_COLUMN,
                    LAT_COLUMN, LNG_COLUMN};
  //   constants
  private static final String CONSTANTS_TABLE = "constants";
  private static final String CONSTANT_COLUMN = "constant";
  private static final String[] CONSTANT_COLUMNS = new String[] {CONSTANT_COLUMN};
  //   airport_properties
  private static final String AIRPORT_PROPERTIES_TABLE = "airport_properties";
  private static final String AIRPORT_ID_COLUMN = "airport_id";
  private static final String KEY_COLUMN = "key";
  private static final String VALUE_COLUMN = "value";
  private static final String AIRPORT_ID_WHERE = AIRPORT_ID_COLUMN + " = ?";
  private static final String[] PROPERTY_COLUMNS = new String[] {KEY_COLUMN, VALUE_COLUMN};
  private static final HashSet<String> INTEGER_AIRPORT_PROPERTIES;
  //   runways
  private static final String RUNWAYS_TABLE = "runways";
  private static final String RUNWAY_LETTERS_COLUMN = "letters";
  private static final String RUNWAY_LENGTH_COLUMN = "length";
  private static final String RUNWAY_WIDTH_COLUMN = "width";
  private static final String RUNWAY_SURFACE_COLUMN = "surface";
  private static final String[] RUNWAY_COLUMNS =
      new String[] {ID_COLUMN, RUNWAY_LETTERS_COLUMN, RUNWAY_LENGTH_COLUMN,
                    RUNWAY_WIDTH_COLUMN, RUNWAY_SURFACE_COLUMN};
  //  runway_ends
  private static final String RUNWAY_ENDS_TABLE = "runway_ends";
  private static final String RUNWAY_ID_COLUMN = "runway_id";
  private static final String RUNWAY_END_LETTERS_COLUMN = "letters";
  private static final String RUNWAY_ID_WHERE = RUNWAY_ID_COLUMN + " = ?";
  private static final String[] RUNWAY_END_COLUMNS =
      new String[] {ID_COLUMN, RUNWAY_END_LETTERS_COLUMN};
  //  runway_end_properties
  private static final String RUNWAY_END_PROPERTIES_TABLE = "runway_end_properties";
  private static final String RUNWAY_END_ID_COLUMN = "runway_end_id";
  private static final HashSet<String> INTEGER_RUNWAY_END_PROPERTIES;
  private static final String RUNWAY_END_ID_WHERE = RUNWAY_END_ID_COLUMN + " = ? ";

  static {
    INTEGER_AIRPORT_PROPERTIES = new HashSet<String>();
    INTEGER_AIRPORT_PROPERTIES.add("Elevation");

    INTEGER_RUNWAY_END_PROPERTIES = new HashSet<String>();
    INTEGER_RUNWAY_END_PROPERTIES.add("True Alignment");
  }

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
        database.query(AIRPORTS_TABLE, AIRPORT_LOCATION_COLUMNS, ID_WHERE, stringId, null, null,
            null);
    try {
      if (!airports.moveToFirst()) {
        Log.e(TAG, "No airport for id =" + id);
        return null;
      }
      String icao = airports.getString(airports.getColumnIndexOrThrow(ICAO_COLUMN));
      String name = airports.getString(airports.getColumnIndexOrThrow(NAME_COLUMN));
      int typeConstantId = airports.getInt(airports.getColumnIndexOrThrow(TYPE_COLUMN));
      String city = airports.getString(airports.getColumnIndexOrThrow(CITY_COLUMN));
      String type = getConstant(typeConstantId);
      int latE6 = airports.getInt(airports.getColumnIndexOrThrow(LAT_COLUMN));
      int lngE6 = airports.getInt(airports.getColumnIndexOrThrow(LNG_COLUMN));

      return new Airport(id, icao, name, type, city, new LatLng(latE6, lngE6), getRunways(id));
    } finally {
      airports.close();
    }
  }

  @Override
  public LinkedList<Airport> getAirportsInCells(int startCell, int endCell) {
    String[] stringRange = {Integer.toString(startCell), Integer.toString(endCell)};
    Cursor locations =
        database.query(AIRPORTS_TABLE, LOCATION_COLUMNS, CELL_ID_WHERE, stringRange, null, null,
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

  /**
   * @return Map of airport properties, null if none
   */
  @Override
  public HashMap<String, String> getAirportProperties(int airportId) {
    String[] stringAirportId = {Integer.toString(airportId)};
    Cursor airportPropertiesCursor =
        database.query(AIRPORT_PROPERTIES_TABLE,
                       PROPERTY_COLUMNS,
                       AIRPORT_ID_WHERE,
                       stringAirportId, null, null,
            null);
    try {
      if (!airportPropertiesCursor.moveToFirst()) {
        Log.e(TAG, "No airport properties for id =" + airportId);
        return null;
      }
      final HashMap<String, String> airportProperties = new HashMap<String, String>();

      do {
        final int keyConstantId = airportPropertiesCursor.getInt(
            airportPropertiesCursor.getColumnIndexOrThrow(KEY_COLUMN));
        final String key = getConstant(keyConstantId);
        final int valueConstantId = airportPropertiesCursor.getInt(
            airportPropertiesCursor.getColumnIndexOrThrow(VALUE_COLUMN));
        String value;
        if (INTEGER_AIRPORT_PROPERTIES.contains(key)) {
          value = Integer.toString(valueConstantId);
        } else {
          value = getConstant(valueConstantId);
        }
        airportProperties.put(key, value);
      } while (airportPropertiesCursor.moveToNext());

      return airportProperties;
    } finally {
      airportPropertiesCursor.close();
    }
  }

  private Airport getAirport(int id, LatLng knownLocation) {
    String[] stringId = {Integer.toString(id)};
    Cursor airports =
        database.query(AIRPORTS_TABLE, AIRPORT_COLUMNS, ID_WHERE, stringId, null, null,
            null);
    try {
      if (!airports.moveToFirst()) {
        Log.e(TAG, "No airport for id =" + id);
        return null;
      }
      String icao = airports.getString(airports.getColumnIndexOrThrow(ICAO_COLUMN));
      String name = airports.getString(airports.getColumnIndexOrThrow(NAME_COLUMN));
      int typeConstantId = airports.getInt(airports.getColumnIndexOrThrow(TYPE_COLUMN));
      String city = airports.getString(airports.getColumnIndexOrThrow(CITY_COLUMN));
      String type = getConstant(typeConstantId);

      return new Airport(id, icao, name, type, city, knownLocation, getRunways(id));
    } finally {
      airports.close();
    }
  }

  private String getConstant(int constantId) {
    String[] stringConstantId = {Integer.toString(constantId)};
    Cursor constant =
        database.query(CONSTANTS_TABLE, CONSTANT_COLUMNS, ID_WHERE, stringConstantId, null, null,
            null);
    try {
      if (!constant.moveToFirst()) {
        Log.e(TAG, "No constant for id =" + constantId);
        return null;
      }

      return constant.getString(constant.getColumnIndexOrThrow(CONSTANT_COLUMN));
    } finally {
      constant.close();
    }
  }

  private SortedSet<Runway> getRunways(final int airportId) {
    String[] stringAirportId = {Integer.toString(airportId)};
    Cursor runwayCursor =
        database.query(RUNWAYS_TABLE,
                       RUNWAY_COLUMNS,
                       AIRPORT_ID_WHERE,
                       stringAirportId, null, null,
            null);
    try {
      if (!runwayCursor.moveToFirst()) {
        Log.e(TAG, "No runway for airport id =" + airportId);
        return null;
      }
      final TreeSet<Runway> runways = new TreeSet<Runway>(Collections.reverseOrder());

      do {
        final int runwayId = runwayCursor.getInt(
                    runwayCursor.getColumnIndexOrThrow(ID_COLUMN));
        final String runwayLetters = runwayCursor.getString(
            runwayCursor.getColumnIndexOrThrow(RUNWAY_LETTERS_COLUMN));
        final int runwayLength = runwayCursor.getInt(
                    runwayCursor.getColumnIndexOrThrow(RUNWAY_LENGTH_COLUMN));
        final int runwayWidth = runwayCursor.getInt(
                    runwayCursor.getColumnIndexOrThrow(RUNWAY_WIDTH_COLUMN));
        final String runwaySurface= getConstant(runwayCursor.getInt(
            runwayCursor.getColumnIndexOrThrow(RUNWAY_SURFACE_COLUMN)));

        final SortedSet<RunwayEnd> runwayEnds = getRunwayEnds(runwayId);

        runways.add(
            new Runway(airportId, runwayLetters, runwayLength, runwayWidth,  runwaySurface,
                       runwayEnds));
      } while (runwayCursor.moveToNext());

      // Return runways in descending order of length
      return runways;
    } finally {
      runwayCursor.close();
    }
  }

  private SortedSet<RunwayEnd> getRunwayEnds(final int runwayId) {
    String[] stringRunwayId = {Integer.toString(runwayId)};
    Cursor runwayEndCursor =
        database.query(RUNWAY_ENDS_TABLE,
                       RUNWAY_END_COLUMNS,
                       RUNWAY_ID_WHERE,
                       stringRunwayId, null, null,
            null);
    try {
      if (!runwayEndCursor.moveToFirst()) {
        Log.e(TAG, "No runway end for runway = " + runwayId);
        return null;
      }

      final TreeSet<RunwayEnd> runwayEnds = new TreeSet<RunwayEnd>();

      do {
        final int runwayEndId = runwayEndCursor.getInt(
            runwayEndCursor.getColumnIndexOrThrow(ID_COLUMN));
        final String runwayEndLetters = runwayEndCursor.getString(
            runwayEndCursor.getColumnIndexOrThrow(RUNWAY_END_LETTERS_COLUMN));

        runwayEnds.add(new RunwayEnd(runwayEndId, runwayEndLetters));
      } while (runwayEndCursor.moveToNext());

      if (runwayEnds.size() > 2) {
        throw new RuntimeException("Invalid number of runway ends.  runway id: " + runwayId);
      }

      return runwayEnds;
    } finally {
      runwayEndCursor.close();
    }
  }

  /**
   * @return Map of runway end properties, null if none
   */
  @Override
  public HashMap<String, String> getRunwayEndProperties(int runwayEndId) {
    String[] stringRunwayEndId = {Integer.toString(runwayEndId)};
    Cursor runwayEndPropertiesCursor =
        database.query(RUNWAY_END_PROPERTIES_TABLE,
                       PROPERTY_COLUMNS,
                       RUNWAY_END_ID_WHERE,
                       stringRunwayEndId, null, null,
            null);
    try {
      if (!runwayEndPropertiesCursor.moveToFirst()) {
        Log.e(TAG, "No runway end properties for id =" + runwayEndId);
        return null;
      }
      final HashMap<String, String> runwayEndProperties = new HashMap<String, String>();

      do {
        final int keyConstantId = runwayEndPropertiesCursor.getInt(
            runwayEndPropertiesCursor.getColumnIndexOrThrow(KEY_COLUMN));
        final String key = getConstant(keyConstantId);
        final int valueConstantId = runwayEndPropertiesCursor.getInt(
            runwayEndPropertiesCursor.getColumnIndexOrThrow(VALUE_COLUMN));
        String value;
        if (INTEGER_RUNWAY_END_PROPERTIES.contains(key)) {
          value = Integer.toString(valueConstantId);
        } else {
          value = getConstant(valueConstantId);
        }
        runwayEndProperties.put(key, value);
      } while (runwayEndPropertiesCursor.moveToNext());

      return runwayEndProperties;
    } finally {
      runwayEndPropertiesCursor.close();
    }
  }

}
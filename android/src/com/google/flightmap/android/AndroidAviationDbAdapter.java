package com.google.flightmap.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.Runway;
import com.google.flightmap.common.data.RunwayEnd;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class AndroidAviationDbAdapter implements AviationDbAdapter {
  private static final String TAG = AndroidAviationDbAdapter.class.getSimpleName();

  // database
  static final String DATABASE_PATH = "/sdcard/com.google.flightmap/aviation.db";

  // airports
  private static final String AIRPORTS_TABLE = "airports";
  private static final String ID_COLUMN = "_id";
  private static final String ICAO_COLUMN = "icao";
  private static final String NAME_COLUMN = "name";
  private static final String TYPE_COLUMN = "type";
  private static final String CITY_COLUMN = "city";
  private static final String RANK_COLUMN = "rank";
  private static final String IS_OPEN_COLUMN = "is_open";
  private static final String IS_PUBLIC_COLUMN = "is_public";
  private static final String IS_TOWERED_COLUMN = "is_towered";
  private static final String IS_MILITARY_COLUMN = "is_military";
  private static final String LAT_COLUMN = "lat";
  private static final String LNG_COLUMN = "lng";
  private static final String CELL_ID_COLUMN = "cell_id";
  private static final String CELL_ID_WHERE =
      String.format("%s >= ? and %s < ? and %s >= ?", CELL_ID_COLUMN, CELL_ID_COLUMN, RANK_COLUMN);
  private static final String ID_WHERE = ID_COLUMN + " = ?";
  private static final String[] LOCATION_COLUMNS =
      new String[] {ID_COLUMN, LAT_COLUMN, LNG_COLUMN, CELL_ID_COLUMN, RANK_COLUMN};
  private static final String[] AIRPORT_COLUMNS =
      new String[] {ID_COLUMN, ICAO_COLUMN, NAME_COLUMN, TYPE_COLUMN, CITY_COLUMN, RANK_COLUMN,
          IS_OPEN_COLUMN, IS_PUBLIC_COLUMN, IS_TOWERED_COLUMN, IS_MILITARY_COLUMN};
  private static final String[] AIRPORT_LOCATION_COLUMNS =
      new String[] {ID_COLUMN, ICAO_COLUMN, NAME_COLUMN, TYPE_COLUMN, CITY_COLUMN, RANK_COLUMN,
          IS_OPEN_COLUMN, IS_PUBLIC_COLUMN, IS_TOWERED_COLUMN, IS_MILITARY_COLUMN, LAT_COLUMN,
          LNG_COLUMN};
  // constants
  private static final String CONSTANTS_TABLE = "constants";
  private static final String CONSTANT_COLUMN = "constant";
  private static final String[] CONSTANT_COLUMNS = new String[] {CONSTANT_COLUMN};
  // airport_properties
  private static final String AIRPORT_PROPERTIES_TABLE = "airport_properties";
  private static final String AIRPORT_ID_COLUMN = "airport_id";
  private static final String KEY_COLUMN = "key";
  private static final String VALUE_COLUMN = "value";
  private static final String AIRPORT_ID_WHERE = AIRPORT_ID_COLUMN + " = ?";
  private static final String[] PROPERTY_COLUMNS = new String[] {KEY_COLUMN, VALUE_COLUMN};
  private static final HashSet<String> INTEGER_AIRPORT_PROPERTIES;
  // airport_comm
  private static final String AIRPORT_COMM_TABLE = "airport_comm";
  private static final String COMM_COLUMN = "comm";
  private static final String[] COMM_COLUMNS = new String[] {COMM_COLUMN};
  // runways
  private static final String RUNWAYS_TABLE = "runways";
  private static final String RUNWAY_LETTERS_COLUMN = "letters";
  private static final String RUNWAY_LENGTH_COLUMN = "length";
  private static final String RUNWAY_WIDTH_COLUMN = "width";
  private static final String RUNWAY_SURFACE_COLUMN = "surface";
  private static final String[] RUNWAY_COLUMNS =
      new String[] {ID_COLUMN, RUNWAY_LETTERS_COLUMN, RUNWAY_LENGTH_COLUMN, RUNWAY_WIDTH_COLUMN,
          RUNWAY_SURFACE_COLUMN};
  // runway_ends
  private static final String RUNWAY_ENDS_TABLE = "runway_ends";
  private static final String RUNWAY_ID_COLUMN = "runway_id";
  private static final String RUNWAY_END_LETTERS_COLUMN = "letters";
  private static final String RUNWAY_ID_WHERE = RUNWAY_ID_COLUMN + " = ?";
  private static final String[] RUNWAY_END_COLUMNS =
      new String[] {ID_COLUMN, RUNWAY_END_LETTERS_COLUMN};
  // runway_end_properties
  private static final String RUNWAY_END_PROPERTIES_TABLE = "runway_end_properties";
  private static final String RUNWAY_END_ID_COLUMN = "runway_end_id";
  private static final HashSet<String> INTEGER_RUNWAY_END_PROPERTIES;
  private static final String RUNWAY_END_ID_WHERE = RUNWAY_END_ID_COLUMN + " = ? ";

  private static final List<String> HARD_SURFACE_PREFIXES;

  static {
    INTEGER_AIRPORT_PROPERTIES = new HashSet<String>();
    INTEGER_AIRPORT_PROPERTIES.add("Elevation");

    INTEGER_RUNWAY_END_PROPERTIES = new HashSet<String>();
    INTEGER_RUNWAY_END_PROPERTIES.add("True Alignment");

    HARD_SURFACE_PREFIXES = new LinkedList<String>();
    HARD_SURFACE_PREFIXES.add("ASPH");
    HARD_SURFACE_PREFIXES.add("CONC");
    HARD_SURFACE_PREFIXES.add("PEM");
  }

  private SQLiteDatabase database;
  private final UserPrefs userPrefs;

  public AndroidAviationDbAdapter(final UserPrefs userPrefs) {
    this.userPrefs = userPrefs;
  }

  @Override
  /**
   * Opens the airport database.
   *
   * @throws SQLException on database error.
   */
  public synchronized void open() {
    database = SQLiteDatabase.openDatabase(DATABASE_PATH, null, SQLiteDatabase.OPEN_READONLY);
  }

  @Override
  public synchronized void close() {
    database.close();
  }

  /**
   * Return Airport.Type enum value corresponding to the given string.
   */
  private static Airport.Type getAirportType(final String typeString) {
    if ("Airport".equals(typeString)) {
      return Airport.Type.AIRPORT;
    } else if ("Seaplane Base".equals(typeString)) {
      return Airport.Type.SEAPLANE_BASE;
    } else if ("Heliport".equals(typeString)) {
      return Airport.Type.HELIPORT;
    } else if ("Ultralight".equals(typeString)) {
      return Airport.Type.ULTRALIGHT;
    } else if ("Gliderport".equals(typeString)) {
      return Airport.Type.GLIDERPORT;
    } else if ("Balloonport".equals(typeString)) {
      return Airport.Type.BALLOONPORT;
    } else {
      return Airport.Type.OTHER;
    }
  }

  @Override
  public Airport getAirport(final int id) {
    final String[] stringId = {Integer.toString(id)};
    final Cursor airports = database.query(
        AIRPORTS_TABLE, AIRPORT_LOCATION_COLUMNS, ID_WHERE, stringId, null, null, null);
    try {
      if (!airports.moveToFirst()) {
        Log.e(TAG, "No airport for id =" + id);
        return null;
      }
      final String icao = airports.getString(airports.getColumnIndexOrThrow(ICAO_COLUMN));
      final String name = airports.getString(airports.getColumnIndexOrThrow(NAME_COLUMN));
      final int typeConstantId = airports.getInt(airports.getColumnIndexOrThrow(TYPE_COLUMN));
      final String city = airports.getString(airports.getColumnIndexOrThrow(CITY_COLUMN));
      final int rank = airports.getInt(airports.getColumnIndexOrThrow(RANK_COLUMN));

      final String typeString = getConstant(typeConstantId);
      final Airport.Type type = getAirportType(typeString);
      final int latE6 = airports.getInt(airports.getColumnIndexOrThrow(LAT_COLUMN));
      final int lngE6 = airports.getInt(airports.getColumnIndexOrThrow(LNG_COLUMN));
      final boolean isOpen = airports.getInt(airports.getColumnIndexOrThrow(IS_OPEN_COLUMN)) == 1;
      final boolean isPublic =
          airports.getInt(airports.getColumnIndexOrThrow(IS_PUBLIC_COLUMN)) == 1;
      final boolean isTowered =
          airports.getInt(airports.getColumnIndexOrThrow(IS_TOWERED_COLUMN)) == 1;
      final boolean isMilitary =
          airports.getInt(airports.getColumnIndexOrThrow(IS_MILITARY_COLUMN)) == 1;

      Airport airport =
          new Airport(id, icao, name, type, city, new LatLng(latE6, lngE6), isOpen, isPublic,
              isTowered, isMilitary, getRunways(id), rank);
      return airport;
    } finally {
      airports.close();
    }
  }

  @Override
  public List<Airport> getAirportsInCells(int startCell, int endCell, int minRank) {
    final String[] stringRange =
        {Integer.toString(startCell), Integer.toString(endCell), Integer.toString(minRank)};
    final Cursor locations = database.query(
        AIRPORTS_TABLE, LOCATION_COLUMNS, CELL_ID_WHERE, stringRange, null, null, null);
    final LinkedList<Airport> result = new LinkedList<Airport>();
    try {
      if (!locations.moveToFirst()) { // No airports in cell_id range.
        return result;
      }

      final int idColumn = locations.getColumnIndexOrThrow(ID_COLUMN);
      final int latColumn = locations.getColumnIndexOrThrow(LAT_COLUMN);
      final int lngColumn = locations.getColumnIndexOrThrow(LNG_COLUMN);
      do {
        final int id = locations.getInt(idColumn);
        final int lat = locations.getInt(latColumn);
        final int lng = locations.getInt(lngColumn);
        final LatLng location = new LatLng(lat, lng);
        final Airport airport = getAirport(id, location);
        if (shouldInclude(airport)) {
          result.add(airport);
        }
      } while (locations.moveToNext());
    } finally {
      locations.close();
    }
    return result;
  }

  /**
   * Returns true if the airport should be included in database results. User
   * preferences are applied here to filter the database results.
   */
  private boolean shouldInclude(final Airport airport) {
    if (airport.type.equals(Airport.Type.HELIPORT) && !userPrefs.showHeliport()) {
      return false;
    }
    if (airport.type.equals(Airport.Type.SEAPLANE_BASE) && !userPrefs.showSeaplane()) {
      return false;
    }
    // No user prefs to control these. Always hide.
    if (airport.type.equals(Airport.Type.ULTRALIGHT) || airport.type.equals(Airport.Type.GLIDERPORT)
        || airport.type.equals(Airport.Type.BALLOONPORT)) {
      return false;
    }

    if (airport.isMilitary && !userPrefs.showMilitary()) {
      return false;
    }
    if (!airport.isPublic && !userPrefs.showPrivate()) {
      if (!(airport.isMilitary && userPrefs.showMilitary())) {
        return false;
      }
    }
    if (!userPrefs.showSoft()) {
      final String longestRunwaySurface = airport.runways.first().surface;
      for(String hardSurfacePrefix: HARD_SURFACE_PREFIXES) {
        if (longestRunwaySurface.startsWith(hardSurfacePrefix)) {
          return true;
        }
      }
      return false;
    }
    final int minRunwayLength = userPrefs.getMinRunwayLength();
    if (minRunwayLength > 0) {
      final int length = airport.runways.first().length;
      if (length < minRunwayLength) {
        return false;
      }
    }
    return true;
  }

  /**
   * @return Map of airport properties, null if none
   */
  @Override
  public Map<String, String> getAirportProperties(final int airportId) {
    final String[] stringAirportId = {Integer.toString(airportId)};
    final Cursor airportPropertiesCursor =
        database.query(AIRPORT_PROPERTIES_TABLE, PROPERTY_COLUMNS, AIRPORT_ID_WHERE,
            stringAirportId, null, null, null);
    try {
      if (!airportPropertiesCursor.moveToFirst()) {
        Log.e(TAG, "No airport properties for id =" + airportId);
        return null;
      }
      final Map<String, String> airportProperties = new HashMap<String, String>();

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

  /**
   * @return List of communication frequencies with description at given airport
   */
  @Override
  public List<String> getAirportComms(int airportId) {
    final String[] stringAirportId = {Integer.toString(airportId)};
    final Cursor airportCommsCursor = database.query(
        AIRPORT_COMM_TABLE, COMM_COLUMNS, AIRPORT_ID_WHERE, stringAirportId, null, null, null);
    try {
      if (!airportCommsCursor.moveToFirst()) {
        Log.e(TAG, "No airport comms for id =" + airportId);
        return null;
      }
      final LinkedList<String> airportComms = new LinkedList<String>();

      do {
        final String comm =
            airportCommsCursor.getString(airportCommsCursor.getColumnIndexOrThrow(COMM_COLUMN));
        airportComms.add(comm);
      } while (airportCommsCursor.moveToNext());

      return airportComms;
    } finally {
      airportCommsCursor.close();
    }
  }

  private Airport getAirport(int id, LatLng knownLocation) {
    final String[] stringId = {Integer.toString(id)};
    final Cursor airports =
        database.query(AIRPORTS_TABLE, AIRPORT_COLUMNS, ID_WHERE, stringId, null, null, null);
    try {
      if (!airports.moveToFirst()) {
        Log.e(TAG, "No airport for id =" + id);
        return null;
      }
      final String icao = airports.getString(airports.getColumnIndexOrThrow(ICAO_COLUMN));
      final String name = airports.getString(airports.getColumnIndexOrThrow(NAME_COLUMN));
      final int typeConstantId = airports.getInt(airports.getColumnIndexOrThrow(TYPE_COLUMN));
      final String city = airports.getString(airports.getColumnIndexOrThrow(CITY_COLUMN));
      final int rank = airports.getInt(airports.getColumnIndexOrThrow(RANK_COLUMN));
      final String typeString = getConstant(typeConstantId);
      final Airport.Type type = getAirportType(typeString);
      final boolean isOpen = airports.getInt(airports.getColumnIndexOrThrow(IS_OPEN_COLUMN)) == 1;
      final boolean isPublic =
          airports.getInt(airports.getColumnIndexOrThrow(IS_PUBLIC_COLUMN)) == 1;
      final boolean isTowered =
          airports.getInt(airports.getColumnIndexOrThrow(IS_TOWERED_COLUMN)) == 1;
      final boolean isMilitary =
          airports.getInt(airports.getColumnIndexOrThrow(IS_MILITARY_COLUMN)) == 1;

      Airport airport =
          new Airport(id, icao, name, type, city, knownLocation, isOpen, isPublic, isTowered,
              isMilitary, getRunways(id), rank);
      return airport;
    } finally {
      airports.close();
    }
  }

  public String getConstant(final int constantId) {
    String[] stringConstantId = {Integer.toString(constantId)};
    Cursor constant = database.query(
        CONSTANTS_TABLE, CONSTANT_COLUMNS, ID_WHERE, stringConstantId, null, null, null);
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
    final String[] stringAirportId = {Integer.toString(airportId)};
    final Cursor runwayCursor = database.query(
        RUNWAYS_TABLE, RUNWAY_COLUMNS, AIRPORT_ID_WHERE, stringAirportId, null, null, null);
    try {
      if (!runwayCursor.moveToFirst()) {
        Log.e(TAG, "No runway for airport id =" + airportId);
        return null;
      }
      final SortedSet<Runway> runways = new TreeSet<Runway>(Collections.reverseOrder());

      do {
        final int runwayId = runwayCursor.getInt(runwayCursor.getColumnIndexOrThrow(ID_COLUMN));
        final String runwayLetters =
            runwayCursor.getString(runwayCursor.getColumnIndexOrThrow(RUNWAY_LETTERS_COLUMN));
        final int runwayLength =
            runwayCursor.getInt(runwayCursor.getColumnIndexOrThrow(RUNWAY_LENGTH_COLUMN));
        final int runwayWidth =
            runwayCursor.getInt(runwayCursor.getColumnIndexOrThrow(RUNWAY_WIDTH_COLUMN));
        final String runwaySurface = getConstant(
            runwayCursor.getInt(runwayCursor.getColumnIndexOrThrow(RUNWAY_SURFACE_COLUMN)));

        final SortedSet<RunwayEnd> runwayEnds = getRunwayEnds(runwayId);

        runways.add(new Runway(
            airportId, runwayLetters, runwayLength, runwayWidth, runwaySurface, runwayEnds));
      } while (runwayCursor.moveToNext());

      // Return runways in descending order of length
      return runways;
    } finally {
      runwayCursor.close();
    }
  }

  private SortedSet<RunwayEnd> getRunwayEnds(final int runwayId) {
    final String[] stringRunwayId = {Integer.toString(runwayId)};
    final Cursor runwayEndCursor = database.query(
        RUNWAY_ENDS_TABLE, RUNWAY_END_COLUMNS, RUNWAY_ID_WHERE, stringRunwayId, null, null, null);
    try {
      if (!runwayEndCursor.moveToFirst()) {
        Log.e(TAG, "No runway end for runway = " + runwayId);
        return null;
      }

      final TreeSet<RunwayEnd> runwayEnds = new TreeSet<RunwayEnd>();

      do {
        final int runwayEndId =
            runwayEndCursor.getInt(runwayEndCursor.getColumnIndexOrThrow(ID_COLUMN));
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
  public Map<String, String> getRunwayEndProperties(int runwayEndId) {
    final String[] stringRunwayEndId = {Integer.toString(runwayEndId)};
    final Cursor runwayEndPropertiesCursor =
        database.query(RUNWAY_END_PROPERTIES_TABLE, PROPERTY_COLUMNS, RUNWAY_END_ID_WHERE,
            stringRunwayEndId, null, null, null);
    try {
      if (!runwayEndPropertiesCursor.moveToFirst()) {
        Log.e(TAG, "No runway end properties for id =" + runwayEndId);
        return null;
      }
      final Map<String, String> runwayEndProperties = new HashMap<String, String>();

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

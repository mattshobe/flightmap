package com.google.flightmap.android;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.ThreadUtils;
import com.google.flightmap.common.data.Airport;
import com.google.flightmap.common.data.Airspace;
import com.google.flightmap.common.data.Comm;
import com.google.flightmap.common.data.LatLng;
import com.google.flightmap.common.data.LatLngRect;
import com.google.flightmap.common.data.Runway;
import com.google.flightmap.common.data.RunwayEnd;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class AndroidAviationDbAdapter implements AviationDbAdapter {
  private static final String TAG = AndroidAviationDbAdapter.class.getSimpleName();

  // database
  static final String DATABASE_PATH = "/sdcard/com.google.flightmap/aviation.db";

  // metadata
  private static final String METADATA_TABLE = "metadata";
  private static final String KEY_COLUMN = "key";
  private static final String VALUE_COLUMN = "value";
  private static final String KEY_WHERE = KEY_COLUMN + " = ?";
  private static final String[] VALUE_COLUMNS = new String[] {VALUE_COLUMN};

  // airports
  private static final String AIRPORTS_TABLE = "airports";
  // airports - columns
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
  // airports - conditions
  private static final String CELL_ID_WHERE = String.format("%s >= ? and %s < ? and %s >= ?",
      CELL_ID_COLUMN, CELL_ID_COLUMN, RANK_COLUMN);
  private static final String ID_WHERE = ID_COLUMN + " = ?";
  private static final String ICAO_WHERE = ICAO_COLUMN + " = ?";
  private static final String NAME_LIKE = NAME_COLUMN + " LIKE ?";
  private static final String CITY_LIKE = CITY_COLUMN + " LIKE ?";
  // airports - return columns
  private static final String[] ID_COLUMNS = new String[] {ID_COLUMN};
  private static final String[] LOCATION_COLUMNS = new String[] {ID_COLUMN, LAT_COLUMN, LNG_COLUMN,
       CELL_ID_COLUMN, RANK_COLUMN};
  private static final String[] AIRPORT_COLUMNS = new String[] {ID_COLUMN, ICAO_COLUMN,
      NAME_COLUMN, TYPE_COLUMN, CITY_COLUMN, RANK_COLUMN, IS_OPEN_COLUMN, IS_PUBLIC_COLUMN,
      IS_TOWERED_COLUMN, IS_MILITARY_COLUMN};
  private static final String[] AIRPORT_LOCATION_COLUMNS = new String[] {ID_COLUMN, ICAO_COLUMN,
      NAME_COLUMN, TYPE_COLUMN, CITY_COLUMN, RANK_COLUMN, IS_OPEN_COLUMN, IS_PUBLIC_COLUMN,
      IS_TOWERED_COLUMN, IS_MILITARY_COLUMN, LAT_COLUMN, LNG_COLUMN};
  // constants
  private static final String CONSTANTS_TABLE = "constants";
  private static final String CONSTANT_COLUMN = "constant";
  private static final String[] CONSTANT_COLUMNS = new String[] {CONSTANT_COLUMN};
  // airport_properties
  private static final String AIRPORT_PROPERTIES_TABLE = "airport_properties";
  private static final String AIRPORT_ID_COLUMN = "airport_id";
  private static final String AIRPORT_ID_WHERE = AIRPORT_ID_COLUMN + " = ?";
  private static final String[] PROPERTY_COLUMNS = new String[] {KEY_COLUMN, VALUE_COLUMN};
  private static final HashSet<String> INTEGER_AIRPORT_PROPERTIES;
  // airport_comm
  private static final String AIRPORT_COMM_TABLE = "airport_comm";
  private static final String IDENTIFIER_COLUMN = "identifier";
  private static final String FREQUENCY_COLUMN = "frequency";
  private static final String REMARKS_COLUMN = "remarks";
  private static final String[] COMM_COLUMNS = new String[] {IDENTIFIER_COLUMN, FREQUENCY_COLUMN,
      REMARKS_COLUMN};
  // runways
  private static final String RUNWAYS_TABLE = "runways";
  private static final String RUNWAY_LETTERS_COLUMN = "letters";
  private static final String RUNWAY_LENGTH_COLUMN = "length";
  private static final String RUNWAY_WIDTH_COLUMN = "width";
  private static final String RUNWAY_SURFACE_COLUMN = "surface";
  private static final String[] RUNWAY_COLUMNS = new String[] {ID_COLUMN, RUNWAY_LETTERS_COLUMN,
      RUNWAY_LENGTH_COLUMN, RUNWAY_WIDTH_COLUMN, RUNWAY_SURFACE_COLUMN};
  // runway_ends
  private static final String RUNWAY_ENDS_TABLE = "runway_ends";
  private static final String RUNWAY_ID_COLUMN = "runway_id";
  private static final String RUNWAY_END_LETTERS_COLUMN = "letters";
  private static final String RUNWAY_ID_WHERE = RUNWAY_ID_COLUMN + " = ?";
  private static final String[] RUNWAY_END_COLUMNS = new String[] {ID_COLUMN,
      RUNWAY_END_LETTERS_COLUMN};
  // runway_end_properties
  private static final String RUNWAY_END_PROPERTIES_TABLE = "runway_end_properties";
  private static final String RUNWAY_END_ID_COLUMN = "runway_end_id";
  private static final HashSet<String> INTEGER_RUNWAY_END_PROPERTIES;
  private static final String RUNWAY_END_ID_WHERE = RUNWAY_END_ID_COLUMN + " = ? ";
  // airspaces
  private static final String AIRSPACES_TABLE = "airspaces";
  private static final String CLASS_COLUMN = "class";
  private static final String LOW_ALT_COLUMN = "low_alt";
  private static final String HIGH_ALT_COLUMN = "high_alt";
  private static final String[] AIRSPACE_COLUMNS = new String[] {
      NAME_COLUMN, CLASS_COLUMN, LOW_ALT_COLUMN, HIGH_ALT_COLUMN};
  // TODO(aristidis): Check why getAirspacesInRectangle can't use the following.
  /*
  private static final String MIN_LAT_COLUMN = "min_lat";
  private static final String MAX_LAT_COLUMN = "max_lat";
  private static final String MIN_LNG_COLUMN = "min_lng";
  private static final String MAX_LNG_COLUMN = "max_lng";
  private static final String LAT_LNG_IN_RANGE_WHERE = String.format(
      "(MAX(%s, ?) < MIN(%s, ?)) AND (MAX(%s, ?) < MIN(%s, ?))", 
      MIN_LAT_COLUMN, MAX_LAT_COLUMN, MIN_LNG_COLUMN, MAX_LNG_COLUMN);
  */
  private static final String AIRSPACE_ID_RECT_QUERY = "SELECT _id FROM airspaces WHERE " +
     "(MAX(min_lat, %d) < MIN(max_lat, %d)) AND (MAX(min_lng, %d) < MIN(max_lng, %d))";

  // airspace_points
  private static final String AIRSPACE_POINTS_TABLE = "airspace_points";
  private static final String AIRSPACE_ID_COLUMN = "airspace_id";
  private static final String NUM_COLUMN = "num";
  private static final String[] LAT_LNG_COLUMNS = new String[] {LAT_COLUMN, LNG_COLUMN};
  private static final String AIRSPACE_ID_WHERE = AIRSPACE_ID_COLUMN + " = ?";

  static {
    INTEGER_AIRPORT_PROPERTIES = new HashSet<String>();
    INTEGER_AIRPORT_PROPERTIES.add("Elevation");

    INTEGER_RUNWAY_END_PROPERTIES = new HashSet<String>();
    INTEGER_RUNWAY_END_PROPERTIES.add("True Alignment");
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
    try {
      database.close();
    } catch (android.database.sqlite.SQLiteException sqlEx) {
      sqlEx.printStackTrace();
    }
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
    final Cursor airports =
        database.query(AIRPORTS_TABLE, AIRPORT_LOCATION_COLUMNS, ID_WHERE, stringId, null, null,
            null);
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
  public int getAirportIdByIcao(final String icao) {
    final String[] stringICAO = {icao.toUpperCase()};
    final Cursor result =
        database.query(AIRPORTS_TABLE, ID_COLUMNS, ICAO_WHERE, stringICAO, null, null, null);
    try {
      if (!result.moveToFirst()) {
        return -1;
      }
      return result.getInt(result.getColumnIndexOrThrow(ID_COLUMN));
    } finally {
      result.close();
    }
  }

  @Override
  public List<Integer> getAirportIdsWithCityLike(final String pattern) {
    return getAirportIdsWithPattern(CITY_LIKE, pattern);
  }

  @Override
  public List<Integer> getAirportIdsWithNameLike(final String pattern) {
    return getAirportIdsWithPattern(NAME_LIKE, pattern);
  }

  /**
   * Returns airspace with given {@code id}.
   *
   * @return Airspace with corresponding id, {@code null} if none.
   */
  private Airspace getAirspace(final int id) {
    final Cursor result = database.query(AIRSPACES_TABLE, AIRSPACE_COLUMNS, ID_WHERE,
        new String[] {Integer.toString(id)}, null, null, null);
    try {
      if (!result.moveToNext()) {
        return null;
      }
      final String name = result.getString(result.getColumnIndexOrThrow(NAME_COLUMN));
      final String classString =
          getConstant(result.getInt(result.getColumnIndexOrThrow(CLASS_COLUMN)));
      final Airspace.Class airspaceClass = Airspace.Class.valueOf(classString);
      final int lowAlt = result.getInt(result.getColumnIndexOrThrow(LOW_ALT_COLUMN));
      final int highAlt = result.getInt(result.getColumnIndexOrThrow(HIGH_ALT_COLUMN));
      final List<LatLng> points = getAirspacePoints(id);
      return new Airspace(id, name, airspaceClass, lowAlt, highAlt, points);
    } finally {
      result.close();
    }
  }

  /**
   * Returns polygon points for airspace with given {@code id}.
   */
  private List<LatLng> getAirspacePoints(final int id) {
    final Cursor result = database.query(AIRSPACE_POINTS_TABLE, LAT_LNG_COLUMNS, AIRSPACE_ID_WHERE,
        new String[] {Integer.toString(id)}, null, null, NUM_COLUMN);
    final List<LatLng> points = new LinkedList<LatLng>();
    try {
      final int latColumn = result.getColumnIndexOrThrow(LAT_COLUMN);
      final int lngColumn = result.getColumnIndexOrThrow(LNG_COLUMN);
      while (result.moveToNext()) {
        final int lat = result.getInt(latColumn);
        final int lng = result.getInt(lngColumn);
        points.add(new LatLng(lat, lng));
      }
      return points;
    } finally {
      result.close();
    }
  }

  @Override
  public Collection<Airspace> getAirspacesInRectangle(final LatLngRect rect)
      throws InterruptedException {
    final int minLat = rect.getSouth();
    final int maxLat = rect.getNorth();
    final int minLng = rect.getWest();
    final int maxLng = rect.getEast();
    final String query = String.format(AIRSPACE_ID_RECT_QUERY, minLat, maxLat, minLng, maxLng);
    final Cursor result = database.rawQuery(query, null);
    Log.d(TAG, "Airspaces found in rect: " + result.getCount());
    Collection<Airspace> airspaces = new LinkedList<Airspace>();
    try {
      final int idColumn = result.getColumnIndexOrThrow(ID_COLUMN);
      while (result.moveToNext()) {
        ThreadUtils.checkIfInterrupted();
        final int id = result.getInt(idColumn);
        airspaces.add(getAirspace(id));
      }
      return airspaces;
    } finally {
      result.close();
    }
  }

  /**
   * Returns ids of {@link Airport}s for which {@code condition} applied to
   * {@code pattern} is true.
   * 
   * @param condition A column condition requiring one pattern (e.g.
   *        "name LIKE ?")
   * @param pattern A match pattern for the condition (e.g. "John _ Kennedy%");
   */
  private List<Integer> getAirportIdsWithPattern(final String condition, final String pattern) {
    final String[] stringPattern = {pattern};
    final Cursor result =
        database.query(AIRPORTS_TABLE, ID_COLUMNS, condition, stringPattern, null, null, null);
    List<Integer> airportIds = new LinkedList<Integer>();
    try {
      final int idColumn = result.getColumnIndexOrThrow(ID_COLUMN);
      while (result.moveToNext()) {
        airportIds.add(result.getInt(idColumn));
      }
      return airportIds;
    } finally {
      result.close();
    }
  }

  /**
   * Returns a Map of {@link AirportId} and Rank to put in search results. An
   * exact match will return a single-element Map. The rank is incremented each
   * time the query matches search criteria, if the airport Rank is high
   * according to the db and if the airport is in close proximity. If no match
   * is found, return a Map with a single element with airportId -1.
   * 
   * @param query A string to try to match.
   */
  public Map<Integer, Integer> doSearch(final String query) {
    Map<Integer, Integer> airportResults = new HashMap<Integer, Integer>();
    int airportId = getAirportIdByIcao(query);
    // If we found an exact match for the ICAO, return it alone.
    if (airportId != -1) {
      airportResults.put(airportId, 1);
      return airportResults;
    } else if (airportId == -1 && query.length() == 3) {
      // Try again with 'K' prepended if it's only 3 letters.
      airportId = getAirportIdByIcao("K" + query);
      if (airportId != -1) {
        // The first time we find an airport set the rank.
        airportResults.put(airportId, getAirport(airportId).rank);
      }
    }
    // Replaces spaces with % for the %LIKE% db search.
    String[] querySplit = query.split(" ");
    String queryLike = "";
    for (int i = 0; i < querySplit.length; i++) {
      queryLike += '%' + querySplit[i];
    }
    queryLike += '%';
    List<Integer> airportsNameCity = getAirportIdsWithNameLike(queryLike);
    airportsNameCity.addAll(getAirportIdsWithCityLike(queryLike));
    // If no results are returned, send existing result or add one with -1.
    if (airportsNameCity.isEmpty()) {
      if (airportResults.isEmpty()) {
        airportResults.put(-1, 0);
      }
      return airportResults;
    }
    Iterator<Integer> nameIterator = airportsNameCity.iterator();
    // Merge Name and City matches into the airportResults Map and
    // increments the rank appropriately.
    while (nameIterator.hasNext()) {
      Integer id = nameIterator.next();
      Integer count = airportResults.get(id);
      if (count == null) {
        // Initially the rank is the airport rank.
        airportResults.put(id, getAirport(id).rank);
      } else {
        // Increment the rank if there is more than 1 criteria match.
        airportResults.put(id, count + 1);
      }
    }
    return airportResults;
  }

  @Override
  public List<Airport> getAirportsInCells(int startCell, int endCell, int minRank) {
    final String[] stringRange =
        {Integer.toString(startCell), Integer.toString(endCell), Integer.toString(minRank)};
    final Cursor locations =
        database.query(AIRPORTS_TABLE, LOCATION_COLUMNS, CELL_ID_WHERE, stringRange, null, null,
            null);
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
        if (userPrefs.shouldInclude(airport)) {
          result.add(airport);
        }
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
        final int keyConstantId =
            airportPropertiesCursor.getInt(airportPropertiesCursor
                .getColumnIndexOrThrow(KEY_COLUMN));
        final String key = getConstant(keyConstantId);
        final int valueConstantId =
            airportPropertiesCursor.getInt(airportPropertiesCursor
                .getColumnIndexOrThrow(VALUE_COLUMN));
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
  public List<Comm> getAirportComms(int airportId) {
    final String[] stringAirportId = {Integer.toString(airportId)};
    final Cursor airportCommsCursor =
        database.query(AIRPORT_COMM_TABLE, COMM_COLUMNS, AIRPORT_ID_WHERE, stringAirportId, null,
            null, null);
    try {
      if (!airportCommsCursor.moveToFirst()) {
        Log.e(TAG, "No airport comms for id =" + airportId);
        return null;
      }
      final List<Comm> airportComms = new LinkedList<Comm>();

      do {
        final String identifier =
            airportCommsCursor.getString(airportCommsCursor
                .getColumnIndexOrThrow(IDENTIFIER_COLUMN));
        final String frequency =
            airportCommsCursor
                .getString(airportCommsCursor.getColumnIndexOrThrow(FREQUENCY_COLUMN));
        final String remarks =
            airportCommsCursor.getString(airportCommsCursor.getColumnIndexOrThrow(REMARKS_COLUMN));
        final Comm comm = new Comm(identifier, frequency, remarks);
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
    final String[] stringAirportId = {Integer.toString(airportId)};
    final Cursor runwayCursor =
        database.query(RUNWAYS_TABLE, RUNWAY_COLUMNS, AIRPORT_ID_WHERE, stringAirportId, null,
            null, null);
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
        final String runwaySurface =
            getConstant(runwayCursor.getInt(runwayCursor
                .getColumnIndexOrThrow(RUNWAY_SURFACE_COLUMN)));

        final SortedSet<RunwayEnd> runwayEnds = getRunwayEnds(runwayId);

        runways.add(new Runway(airportId, runwayLetters, runwayLength, runwayWidth, runwaySurface,
            runwayEnds));
      } while (runwayCursor.moveToNext());

      // Return runways in descending order of length
      return runways;
    } finally {
      runwayCursor.close();
    }
  }

  private SortedSet<RunwayEnd> getRunwayEnds(final int runwayId) {
    final String[] stringRunwayId = {Integer.toString(runwayId)};
    final Cursor runwayEndCursor =
        database.query(RUNWAY_ENDS_TABLE, RUNWAY_END_COLUMNS, RUNWAY_ID_WHERE, stringRunwayId,
            null, null, null);
    try {
      if (!runwayEndCursor.moveToFirst()) {
        Log.e(TAG, "No runway end for runway = " + runwayId);
        return null;
      }

      final TreeSet<RunwayEnd> runwayEnds = new TreeSet<RunwayEnd>();

      do {
        final int runwayEndId =
            runwayEndCursor.getInt(runwayEndCursor.getColumnIndexOrThrow(ID_COLUMN));
        final String runwayEndLetters =
            runwayEndCursor.getString(runwayEndCursor
                .getColumnIndexOrThrow(RUNWAY_END_LETTERS_COLUMN));

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
        final int keyConstantId =
            runwayEndPropertiesCursor.getInt(runwayEndPropertiesCursor
                .getColumnIndexOrThrow(KEY_COLUMN));
        final String key = getConstant(keyConstantId);
        final int valueConstantId =
            runwayEndPropertiesCursor.getInt(runwayEndPropertiesCursor
                .getColumnIndexOrThrow(VALUE_COLUMN));
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

  @Override
  public String getMetadata(final String key) {
    final String[] keyArray = {key};
    final Cursor metadataValueCursor =
        database.query(METADATA_TABLE, VALUE_COLUMNS, KEY_WHERE, keyArray, null, null, null);
    try {
      if (!metadataValueCursor.moveToFirst()) {
        Log.e(TAG, "No metadata value for key =" + key);
        return null;
      }

      return metadataValueCursor.getString(metadataValueCursor.getColumnIndexOrThrow(VALUE_COLUMN));
    } finally {
      metadataValueCursor.close();
    }
  }
}

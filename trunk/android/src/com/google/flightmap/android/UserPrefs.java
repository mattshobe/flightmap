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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.google.flightmap.common.NavigationUtil.DistanceUnits;
import com.google.flightmap.common.data.Airport;

/**
 * Provides read-only access to shared user preferences.
 * <p>
 * Write access can easily be added later. It's just not needed now, so it's not
 * here.
 */
public class UserPrefs {
  // Preference keys. These match keys in preferences.xml
  private static final String NORTH_UP = "North_Up";
  private static final String DISTANCE_UNITS = "Distance_Units";
  private static final String SHOW_SEAPLANE = "Show_Seaplane";
  private static final String SHOW_MILITARY = "Show_Military";
  private static final String SHOW_SOFT = "Show_Soft";
  private static final String SHOW_PRIVATE = "Show_Private";
  private static final String SHOW_HELIPORT = "Show_Heliports";
  private static final String RUNWAY_LENGTH = "Runway_Length";

  // Preference values. These match values in arrays.xml
  private static final String DISTANCE_UNITS_MILES = "1";
  private static final String DISTANCE_UNITS_KILOMETERS = "2";
  private static final String DISTANCE_UNITS_NAUTICAL_MILES = "3";
  private static final String RUNWAY_LENGTH_NONE = "1";
  private static final String RUNWAY_LENGTH_2000 = "2";
  private static final String RUNWAY_LENGTH_4000 = "3";

  /** Filename to store preferences. */
  public static final String PREFERENCES_FILE = "com.google.flightmap.android_preferences";

  private final SharedPreferences sharedPrefs;

  public UserPrefs(Context context) {
    sharedPrefs = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
  }

  public synchronized boolean isNorthUp() {
    return sharedPrefs.getBoolean(NORTH_UP, false);
  }

  /**
   * Units to show speeds and distances.
   */
  public synchronized DistanceUnits getDistanceUnits() {
    String distanceUnitsString =
        sharedPrefs.getString(DISTANCE_UNITS, DISTANCE_UNITS_NAUTICAL_MILES);
    return getDistanceUnitFromPreference(distanceUnitsString);
  }

  public synchronized boolean showSeaplane() {
    return sharedPrefs.getBoolean(SHOW_SEAPLANE, false);
  }

  public synchronized boolean showMilitary() {
    return sharedPrefs.getBoolean(SHOW_MILITARY, true);
  }

  public synchronized boolean showSoft() {
    return sharedPrefs.getBoolean(SHOW_SOFT, true);
  }

  public synchronized boolean showPrivate() {
    return sharedPrefs.getBoolean(SHOW_PRIVATE, false);
  }

  public synchronized boolean showHeliport() {
    return sharedPrefs.getBoolean(SHOW_HELIPORT, false);
  }

  /**
   * Minimum runway length in feet to show.
   */
  public synchronized int getMinRunwayLength() {
    String runwayString = sharedPrefs.getString(RUNWAY_LENGTH, RUNWAY_LENGTH_2000);
    return getMinRunwayLengthFromPreference(runwayString);
  }

  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    sharedPrefs.registerOnSharedPreferenceChangeListener(listener);
  }

  public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
    sharedPrefs.unregisterOnSharedPreferenceChangeListener(listener);
  }

  /**
   * Returns units corresponding to the distance units preference string.
   */
  private static DistanceUnits getDistanceUnitFromPreference(String distancePrefsString) {
    if (DISTANCE_UNITS_MILES.equals(distancePrefsString)) {
      return DistanceUnits.MILES;
    } else if (DISTANCE_UNITS_KILOMETERS.equals(distancePrefsString)) {
      return DistanceUnits.KILOMETERS;
    } else {
      // Return Nautical miles even on a parse failure.
      return DistanceUnits.NAUTICAL_MILES;
    }
  }

  /**
   * Returns minimum runway length corresponding to preferences string
   */
  private static int getMinRunwayLengthFromPreference(String runwayPrefsString) {
    if (RUNWAY_LENGTH_NONE.equals(runwayPrefsString)) {
      return 0;
    } else if (RUNWAY_LENGTH_4000.equals(runwayPrefsString)) {
      return 4000;
    } else {
      // Return 2000 as minimum even on parse failure.
      return 2000;
    }
  }

  /**
   * Returns true if the airport should be included in database results. User
   * preferences are applied here to filter the database results.
   */
  public synchronized boolean shouldInclude(final Airport airport) {
    if (airport.type.equals(Airport.Type.HELIPORT) && !showHeliport()) {
      return false;
    }
    if (airport.type.equals(Airport.Type.SEAPLANE_BASE) && !showSeaplane()) {
      return false;
    }
    // No user prefs to control these. Always hide.
    if (airport.type.equals(Airport.Type.ULTRALIGHT) || airport.type.equals(Airport.Type.GLIDERPORT)
        || airport.type.equals(Airport.Type.BALLOONPORT)) {
      return false;
    }

    if (airport.isMilitary && !showMilitary()) {
      return false;
    }
    if (!airport.isPublic && !showPrivate()) {
      if (!(airport.isMilitary && showMilitary())) {
        return false;
      }
    }
    if (!showSoft()) {
      return airport.runways.first().isHardSurface();
    }
    final int minRunwayLength = getMinRunwayLength();
    if (minRunwayLength > 0) {
      final int length = airport.runways.first().length;
      if (length < minRunwayLength) {
        return false;
      }
    }
    return true;
  }
}

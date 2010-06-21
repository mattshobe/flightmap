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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class UserPrefs extends PreferenceActivity implements OnSharedPreferenceChangeListener{
	
    public static final String PREFS_NAME = "FlightMapPrefs";
    
    /* 
     * Default Preferences Values - Matches keys in preferences.xml
     */
    public static final String NORTH_UP = "North_Up";
    public static final String DISTANCE_UNITS = "Distance_Units";
    public static final String SHOW_SEAPLANE = "Show_Seaplane";
    public static final String SHOW_MILITARY = "Show_Military";
    public static final String SHOW_SOFT = "Show_Soft";
    public static final String SHOW_PRIVATE = "Show_Private";
    public static final String SHOW_HELI = "Show_Heliports";
    public static final String RUNWAY_LENGTH = "Runway_Length";  
    
    /*
     * Keep track of whether or not FlightMap has seen updated preferences
     */
    public static boolean PREFS_UPDATED = true;
        
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(PREFS_NAME);
		preferenceManager.setSharedPreferencesMode(0);
		addPreferencesFromResource(R.xml.preferences);
		SharedPreferences sharedPreferences = preferenceManager.getSharedPreferences();
		sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void onResume() {
    	super.onResume();
    	SharedPreferences sharedPreferences = this.getPreferences(0);
    	FlightMap.isNorthUp = sharedPreferences.getBoolean(UserPrefs.NORTH_UP, false);
		FlightMap.units = sharedPreferences.getString(UserPrefs.DISTANCE_UNITS, "3");
		FlightMap.showSeaplane = sharedPreferences.getBoolean(UserPrefs.SHOW_SEAPLANE, false);
		FlightMap.showMilitary = sharedPreferences.getBoolean(UserPrefs.SHOW_MILITARY, true);
		FlightMap.showSoft = sharedPreferences.getBoolean(UserPrefs.SHOW_SOFT, true);
		FlightMap.showPrivate = sharedPreferences.getBoolean(UserPrefs.SHOW_PRIVATE, true);
		FlightMap.showHeli = sharedPreferences.getBoolean(UserPrefs.SHOW_HELI, false);
		FlightMap.runwayLength = sharedPreferences.getString(UserPrefs.RUNWAY_LENGTH, "1");
	}
    
    public boolean isPrefsUpdated() {
    	synchronized(this) {
    		if(UserPrefs.PREFS_UPDATED)
    			return true;
    		else
    			return false;
    	}
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		UserPrefs.PREFS_UPDATED = true;
		if (key.equals(UserPrefs.NORTH_UP))
			FlightMap.isNorthUp = sharedPreferences.getBoolean(UserPrefs.NORTH_UP, false);
		if (key.equals(UserPrefs.DISTANCE_UNITS)) 
			FlightMap.units = sharedPreferences.getString(UserPrefs.DISTANCE_UNITS, "3");
		if (key.equals(UserPrefs.SHOW_SEAPLANE))
			FlightMap.showSeaplane = sharedPreferences.getBoolean(UserPrefs.SHOW_SEAPLANE, false);
		if (key.equals(UserPrefs.SHOW_MILITARY))
			FlightMap.showMilitary = sharedPreferences.getBoolean(UserPrefs.SHOW_MILITARY, true);
		if (key.equals(UserPrefs.SHOW_SOFT))
			FlightMap.showSoft = sharedPreferences.getBoolean(UserPrefs.SHOW_SOFT, true);
		if (key.equals(UserPrefs.SHOW_PRIVATE))
			FlightMap.showPrivate = sharedPreferences.getBoolean(UserPrefs.SHOW_PRIVATE, true);
		if (key.equals(UserPrefs.SHOW_HELI))
			FlightMap.showHeli = sharedPreferences.getBoolean(UserPrefs.SHOW_HELI, false);
		if (key.equals(UserPrefs.RUNWAY_LENGTH))
			  FlightMap.runwayLength = sharedPreferences.getString(UserPrefs.RUNWAY_LENGTH, "1");
	}
	
	public void onDestroy()
	{
		super.onDestroy();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}
	 
}
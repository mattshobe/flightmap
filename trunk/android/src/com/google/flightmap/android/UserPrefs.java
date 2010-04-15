package com.google.flightmap.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class UserPrefs extends PreferenceActivity {
	
    public static final String PREFS_NAME = "FlightMapPrefs";
    
    /* 
     * Default Preferences Values - Needs to be synced with preferences.xml
     */
    public static final boolean NORTH_UP = true;
    public static final int UPDATE_INTERVAL = 100; //milliseconds
    public static final String DISTANCE_UNITS = "mile"; // mile, km, nm
    public static final boolean SHOW_SEAPLANE = true;
    public static final boolean SHOW_MILITARY = true;
    public static final boolean SHOW_SOFT = true;
    public static final boolean SHOW_PRIVATE = true;
    public static final int RUNWAY_LENGTH = 2000; //in feet
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(PREFS_NAME);
		preferenceManager.setSharedPreferencesMode(0);
		addPreferencesFromResource(R.xml.preferences);
	}
 
}
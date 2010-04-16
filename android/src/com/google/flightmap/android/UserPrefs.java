package com.google.flightmap.android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class UserPrefs extends PreferenceActivity {
	
    public static final String PREFS_NAME = "FlightMapPrefs";
    
    /* 
     * Default Preferences Values - Matches preferences.xml
     */
    public static final String NORTH_UP = "North_Up";
    public static final String UPDATE_INTERVAL = "Update_Interval";
    public static final String DISTANCE_UNITS = "Distance_Units";
    public static final String SHOW_SEAPLANE = "Show_Seaplane";
    public static final String SHOW_MILITARY = "Show_Military";
    public static final String SHOW_SOFT = "Show_Soft";
    public static final String SHOW_PRIVATE = "Show_Private";
    public static final String RUNWAY_LENGTH = "Runway_Length";  
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(PREFS_NAME);
		preferenceManager.setSharedPreferencesMode(0);
		addPreferencesFromResource(R.xml.preferences);
		
		
	}
	 
}
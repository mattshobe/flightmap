package com.google.flightmap.android;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class UserPrefs extends PreferenceActivity implements OnSharedPreferenceChangeListener{
	
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
    public static final String SHOW_HELI = "Show_Heliports";
    public static final String RUNWAY_LENGTH = "Runway_Length";  
        
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
//    	sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    	FlightMap.isNorthUp = sharedPreferences.getBoolean(UserPrefs.NORTH_UP, false);
		FlightMap.updateInterval = sharedPreferences.getLong(UserPrefs.UPDATE_INTERVAL, 100);
		FlightMap.units = sharedPreferences.getString(UserPrefs.DISTANCE_UNITS, "Nautical Miles");
		FlightMap.showSeaplane = sharedPreferences.getBoolean(UserPrefs.SHOW_SEAPLANE, false);
		FlightMap.showMilitary = sharedPreferences.getBoolean(UserPrefs.SHOW_MILITARY, true);
		FlightMap.showSoft = sharedPreferences.getBoolean(UserPrefs.SHOW_SOFT, true);
		FlightMap.showPrivate = sharedPreferences.getBoolean(UserPrefs.SHOW_PRIVATE, true);
		FlightMap.showHeli = sharedPreferences.getBoolean(UserPrefs.SHOW_HELI, false);
		FlightMap.runwayLength = sharedPreferences.getInt(UserPrefs.RUNWAY_LENGTH, 2000);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(UserPrefs.NORTH_UP))
			FlightMap.isNorthUp = sharedPreferences.getBoolean(UserPrefs.NORTH_UP, false);
		if (key.equals(UserPrefs.UPDATE_INTERVAL))
			FlightMap.updateInterval = sharedPreferences.getLong(UserPrefs.UPDATE_INTERVAL, 100);
		if (key.equals(UserPrefs.DISTANCE_UNITS))
		 FlightMap.units = sharedPreferences.getString(UserPrefs.DISTANCE_UNITS, "Nautical Miles");
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
			  FlightMap.runwayLength = sharedPreferences.getInt(UserPrefs.RUNWAY_LENGTH, 2000);
	}
	
	public void onDestroy()
	{
		super.onDestroy();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}
	 
}
package com.google.flightmap.android;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class UserPrefs extends PreferenceActivity {
	
    public static final String PREFS_NAME = "FlightMapPrefs";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		PreferenceManager preferenceManager = getPreferenceManager();
		preferenceManager.setSharedPreferencesName(PREFS_NAME);
		preferenceManager.setSharedPreferencesMode(0);
		addPreferencesFromResource(R.xml.preferences);
	}
 
}
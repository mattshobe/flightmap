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

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * UI activity for user preferences. If you want to read user preferences, use
 * the {@link UserPrefs} class.
 */
public class UserPrefsActivity extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Make sure we're using the same filename as UserPrefs.
    PreferenceManager preferenceManager = getPreferenceManager();
    preferenceManager.setSharedPreferencesName(UserPrefs.PREFERENCES_FILE);

    // Add the preferences.
    addPreferencesFromResource(R.xml.preferences);
  }
}

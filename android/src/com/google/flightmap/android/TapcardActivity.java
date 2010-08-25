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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Shows details about an airport.
 */
public class TapcardActivity extends Activity {
  private static final String TAG = TapcardActivity.class.getSimpleName();

  // Keys used in the bundle passed to this activity.
  public static final String AIRPORT_ICAO = "icao";
  public static final String AIRPORT_NAME = "name";
  public static final String AIRPORT_LAT = "lat";
  public static final String AIRPORT_LNG = "lng";
  
  private LinearLayout tapcardLayout;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    tapcardLayout = new LinearLayout(this);
    tapcardLayout.setOrientation(LinearLayout.VERTICAL);
    
    
    final Intent startingIntent = getIntent();
    final Bundle tapcardData = startingIntent.getExtras();
    
    TextView icaoText = new TextView(this);
    icaoText.setText(tapcardData.getString(AIRPORT_ICAO));
    Log.i(TAG, "Airport: " + tapcardData.getString(AIRPORT_ICAO));
    tapcardLayout.addView(icaoText);
    
    TextView airportName = new TextView(this);
    airportName.setText(tapcardData.getString(AIRPORT_NAME));
    tapcardLayout.addView(airportName);
    setContentView(tapcardLayout);
  }

}

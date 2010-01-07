/* 
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.blackbox;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

public class BlackBox extends ListActivity {
    private static final String TAG = BlackBox.class.getSimpleName();
    private AirportDbAdapter airportReader;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.airport_list);
        airportReader = new AirportDbAdapter(this);
        airportReader.open();
        readAirportData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        airportReader.close();
    }

    private void readAirportData() {
        Cursor c = airportReader.fetchAllAirports();
//        int icaoColumn = c.getColumnIndex(AirportDbAdapter.ICAO_COLUMN);
//        int nameColumn = c.getColumnIndex(AirportDbAdapter.NAME_COLUMN);
//        Log.d(TAG, "icao column = " + icaoColumn);
//        Log.d(TAG, "name column = " + nameColumn);
//        if (null == c) {
//            Log.e(TAG, "Null cursor");
//            return;
//        }
//
//        boolean ok = c.moveToFirst();
//        Log.d(TAG, "  moveToFirst status " + ok);
//        if (!ok) {
//            Log.e(TAG, "Returning early -- moveToFirst failed");
//            return;
//        }
//
//        Log.d(TAG, "Before looping");
//        do {
//            String icao = c.getString(icaoColumn);
//            String name = c.getString(nameColumn);
//            Log.d(TAG, String.format("%s: %s", icao, name));
//        } while (c.moveToNext());
//        Log.d(TAG, "After looping");

        startManagingCursor(c);

        // Create an adapter to display the cursor using the airport_row layout.
        String[] from = new String[] { AirportDbAdapter.ICAO_COLUMN,
                AirportDbAdapter.NAME_COLUMN };
        int[] to = new int[] { R.id.ident, R.id.name };
        SimpleCursorAdapter airports = new SimpleCursorAdapter(this,
                R.layout.airport_row, c, from, to);
        setListAdapter(airports);
    }
}

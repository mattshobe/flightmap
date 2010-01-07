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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

/**
 * Provides read-only access to the airport database. The database is created
 * offline by the parsing code.
 * 
 * @author Phil Verghese
 */
public class AirportDbAdapter {
    private static final String TAG = AirportDbAdapter.class.getSimpleName();
    public static final String ID_COLUMN = "_id";
    public static final String ICAO_COLUMN = "icao";
    public static final String NAME_COLUMN = "name";
    private static final String DATABASE_PATH = "/sdcard/com.google.blackbox/airports.db";
    private static final int DATABASE_VERSION = 5;
    private static final String AIRPORT_TABLE = "airports";
    private static final String[] AIRPORT_COLUMNS = new String[] { ID_COLUMN,
            ICAO_COLUMN, NAME_COLUMN };
    private Context context;
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public AirportDbAdapter(Context context) {
        this.context = context;
    }

    /**
     * Opens the airport database.
     * 
     * @return this object for chaining.
     * @throws SQLException on database error.
     */
    public synchronized AirportDbAdapter open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public synchronized void close() {
        dbHelper.close();
    }

    public synchronized Cursor fetchAllAirports() {
        return database.query(AIRPORT_TABLE, AIRPORT_COLUMNS, null, null, null,
                null, null);
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, "hardcodedairports", null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(//
                    "CREATE TABLE airports (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                            + " icao TEXT UNIQUE NOT NULL,"
                            + " name TEXT NOT NULL," + " lat INTEGER NOT NULL,"
                            + " lng INTEGER NOT NULL);");
            addAirport(db, "FJDG","DIEGO GARCIA NSF",-7300000,72400000);
            addAirport(db, "00AK","LOWELL FIELD",59856111,-151696264);
            addAirport(db, "01A","PURKEYPILE",62943611,-152270017);
            addAirport(db, "02AK","RUSTIC WILDERNESS",61876911,-150097639);
            addAirport(db, "03AK","JOE CLOUDS",60727222,-151132778);
            addAirport(db, "05AK","WASILLA CREEK AIRPARK",61668661,-149187389);
            addAirport(db, "06AK","JUNE LAKE AIRPARK",61627619,-149575331);
            addAirport(db, "08AK","FISHER",61569639,-149724439);
            addAirport(db, "09AK","WEST BEAVER",61589361,-149847333);
            addAirport(db, "0AK","PILOT STATION",61934556,-162899556);
        }

        //            
        // +
        // "INSERT INTO airports VALUES('00AK','LOWELL FIELD',59856111,-151696264);"
        // +
        // "INSERT INTO airports VALUES('01A','PURKEYPILE',62943611,-152270017);"
        // +
        // "INSERT INTO airports VALUES('02AK','RUSTIC WILDERNESS',61876911,-150097639);"
        // +
        // "INSERT INTO airports VALUES('03AK','JOE CLOUDS',60727222,-151132778);"
        // +
        // "INSERT INTO airports VALUES('05AK','WASILLA CREEK AIRPARK',61668661,-149187389);"
        // +
        // "INSERT INTO airports VALUES('06AK','JUNE LAKE AIRPARK',61627619,-149575331);"
        // + "INSERT INTO airports VALUES('08AK','FISHER',61569639,-149724439);"
        // +
        // "INSERT INTO airports VALUES('09AK','WEST BEAVER',61589361,-149847333);"
        // +
        // "INSERT INTO airports VALUES('0AK','PILOT STATION',61934556,-162899556);");
        // }

        private void addAirport(SQLiteDatabase db, String icao, String name, int lat, int lng) {
            ContentValues values = new ContentValues();
            values.put(ICAO_COLUMN, icao);
            values.put(NAME_COLUMN, name);
            values.put("lat", lat);
            values.put("lng", lng);
            long rowId = db.insert(AIRPORT_TABLE, null, values);
            Log.d(TAG, String.format("%s inserted. rowid=%d", icao, rowId));
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS airports");
            onCreate(db);
        }

    }
}

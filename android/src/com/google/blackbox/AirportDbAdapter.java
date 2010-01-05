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

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

/**
 * Provides read-only access to the airport database. The database is created
 * offline by the parsing code.
 * 
 * @author Phil Verghese
 */
public class AirportDbAdapter {
    public static final String ID_COLUMN = "id";
    public static final String NAME_COLUMN = "name";
    public static final String ICAO_COLUMN = "icao";
    private static final String DATABASE_PATH = "/sdcard/com.google.blackbox/airports.db";
    private static final String AIRPORT_TABLE = "airports";
    private static final String[] AIRPORT_COLUMNS = new String[] { ID_COLUMN,
            ICAO_COLUMN, NAME_COLUMN };
    private SQLiteDatabase database;

    /**
     * Opens the airport database.
     * 
     * @return this object for chaining.
     * @throws SQLException on database error.
     */
    public synchronized AirportDbAdapter open() throws SQLException {
        database = SQLiteDatabase.openDatabase(DATABASE_PATH, null,
                SQLiteDatabase.OPEN_READONLY);
        return this;
    }

    public synchronized void close() {
        database.close();
    }

    public synchronized Cursor fetchAllAirports() {
        return database.query(AIRPORT_TABLE, AIRPORT_COLUMNS, null, null, null,
                null, "icao ASC");
    }
}

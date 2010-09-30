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

package com.google.flightmap.common.net;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.google.flightmap.common.DbAdapter;

/**
 * Extends {@code FileUpdater} with functionality related to database files.
 */
public class DbUpdater extends FileUpdater {
  /**
   * Low level interface to target database.
   */
   private final DbAdapter dbAdapter;
   private final int requiredSchemaVersion;

  /**
   * Initializes updater for database.
   *
   * @param localFile Path of local file.
   * @param remoteUrl URL of remote file that will be used to update local
   *        {@code localFile}
   * @param workingDir Path of local working directory.
   * @param dbAdapter Low level interface to database
   * @param requiredSchemaVersion Minimum required schema version.
   */
  public DbUpdater(final File localFile, final URL remoteUrl, final File workingDir,
      final DbAdapter dbAdapter, final int requiredSchemaVersion) throws IOException {
    super(localFile, remoteUrl, workingDir);
    this.dbAdapter = dbAdapter;
    this.requiredSchemaVersion = requiredSchemaVersion;
  }

  /**
   * Checks if the database needs to be updated. This is determined based on
   * the database expiration time and schema version (available as metadata).
   */
  public synchronized boolean isUpdateNeeded() throws IOException {
    if (dbAdapter == null) {
      return super.isUpdateNeeded();
    }
    boolean openedDbAdapter = false;
    try {
      dbAdapter.open();
      openedDbAdapter = true;
      final String expirationTimestampString =
          dbAdapter.getMetadata(DbAdapter.EXPIRATION_TIMESTAMP_KEY);
      final String schemaVersionString = 
          dbAdapter.getMetadata(DbAdapter.SCHEMA_VERSION_KEY);

      final long expirationTimestamp = Long.parseLong(expirationTimestampString);
      final int schemaVersion = Integer.parseInt(schemaVersionString);

      return (schemaVersion < requiredSchemaVersion ||
              expirationTimestamp <= System.currentTimeMillis());
    } catch (Exception ex) {
      ex.printStackTrace();
      return true;
    } finally {
      if (openedDbAdapter) {
        try {
          dbAdapter.close();
        } catch (Exception ex) {
          // Do not crash because db could not be closed.
          ex.printStackTrace();
        }
      }
    }
  }

}

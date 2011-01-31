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

package com.google.flightmap.common.db;

/**
 * Low level interface to database entities.
 */
public interface DbAdapter {
  /**
   * Database schema version metadata key
   */
  public static final String SCHEMA_VERSION_KEY = "schema version";

  /**
   * Database expiration data (timestamp) key
   */
  public static final String EXPIRATION_TIMESTAMP_KEY = "expires";

  /**
   * Prepares this object for future calls.
   * <p>
   * This method must be called prior to any other call
   */
  public void open();

  /**
   * Closes this object.
   * <p>
   * This method must be called after all other calls.  No other method should be called on this
   * without calling {@link DbAdapter#open} first.
   */
  public void close();

  /**
   * Returns metadata value.
   */
  public String getMetadata(String key);
}

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

import com.google.flightmap.android.location.LocationHandler;

import android.app.Application;

/**
 * Owns data that is shared within the Flight Map application.
 */
public class FlightMap extends Application {
  private LocationHandler locationHandler;

  /**
   * Default constructor required. This class is instantiated by Android because
   * it's set as the android:name attribute of the <application>.
   */
  public FlightMap() {
  }

  public synchronized void setLocationHandler(LocationHandler locationHandler) {
    this.locationHandler = locationHandler;
  }

  public  synchronized LocationHandler getLocationHandler() {
    return locationHandler;
  }
}

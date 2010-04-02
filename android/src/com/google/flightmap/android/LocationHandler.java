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

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Provides location information to the app.
 * 
 * @author Phil Verghese
 */
public class LocationHandler implements LocationListener {
  private static final String TAG = LocationHandler.class.getSimpleName();
  private final LocationManager locationManager;
  private Location location;

  public LocationHandler(LocationManager locationManager) {
    this.locationManager = locationManager;
    // Seed with last known coarse location.
    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
  }

  /**
   * Returns current location or null if not available.
   */
  public synchronized Location getLocation() {
    return location;
  }

  public void startListening() {
    Log.d(TAG, "requesting location updates");
    // Get GPS updates as frequently as possible.
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
  }

  public void stopListening() {
    Log.d(TAG, "no longer listening for location updates");
    locationManager.removeUpdates(this);
  }

  @Override
  public synchronized void onLocationChanged(Location location) {
    this.location = location;
  }

  @Override
  public void onProviderDisabled(String provider) {
    Log.d(TAG, "Provider disabled: " + provider);
  }

  @Override
  public void onProviderEnabled(String provider) {
    Log.d(TAG, "Provider enabled: " + provider);
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    Log.d(TAG, provider + " status changed to " + status);
  }
}

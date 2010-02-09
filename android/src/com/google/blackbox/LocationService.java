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
package com.google.blackbox;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * Service to return current location.
 * 
 * @author Phil Verghese
 */
public class LocationService extends Service {
  private static final String TAG = LocationService.class.getSimpleName();
  private static final long MIN_TIME_DIFF = 1000;
  private static final float MIN_DIST_DIFF = 50; // meters
  private final LocationTask locationTask = new LocationTask();

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate");
    locationTask.startListening();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");
    locationTask.stopListening();
  }

  @Override
  public IBinder onBind(Intent intent) {
    Log.d(TAG, "onBind intent=" + intent.getAction());
    if (ILocationService.class.getName().equals(intent.getAction())) {
      Log.d(TAG, "Returned location task");
      return locationTask;
    }
    Log.w(TAG, "Unknown intent, returned null");
    return null;
  }

  /**
   * Registers for location updates and makes them available. Provides the
   * ILocationService implementation and listens for location updates.
   */
  private final class LocationTask extends ILocationService.Stub implements LocationListener {
    private LocationManager locationManager;
    private Location location;

    /**
     * Returns current location or null if not available.
     * 
     * @throws RemoteException on RPC error.
     */
    @Override
    public synchronized Location getLocation() throws RemoteException {
      return location;
    }

    /**
     * 
     */
    public void startListening() {
      Log.d(TAG, "requesting location updates");
      locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_DIFF,
          MIN_DIST_DIFF, this);
    }

    public void stopListening() {
      Log.d(TAG, "no longer listening for location updates");
      locationManager.removeUpdates(this);
    }

    @Override
    public synchronized void onLocationChanged(Location location) {
      Log.d(TAG, "location changed");
      this.location = location;
    }

    @Override
    public void onProviderDisabled(String provider) {
      Log.d(TAG, "provider disabled");
    }

    @Override
    public void onProviderEnabled(String provider) {
      Log.d(TAG, "provider enabled");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
      Log.d(TAG, provider + "status changed to " + status);
    }
  }
}

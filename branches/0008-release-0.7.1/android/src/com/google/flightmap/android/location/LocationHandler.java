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
package com.google.flightmap.android.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Provides location information to the application. The source of location
 * information may be real or simulated.
 * <p>
 * Simulated locations are provided by the LocationSimulator class.
 */
public class LocationHandler implements LocationListener {
  private static final String TAG = LocationHandler.class.getSimpleName();
  // Get coarse location updates every 5 minutes or 1 km.
  private static final long COARSE_LOCATION_TIME = 300000; // 5 minutes.
  private static final float COARSE_LOCATION_DIST = 1000; // 1 km.
  private final LocationManager locationManager;
  private final LocationSimulator locationSimulator;
  private Location location;
  private Source locationSource;

  /**
   * Creates an instance using real location data (as opposed to simulated).
   */
  public LocationHandler(LocationManager locationManager, Context context) {
    this.locationManager = locationManager;
    locationSimulator = new LocationSimulator(context);
    locationSource = Source.REAL;
  }

  /**
   * Source of location information.
   */
  public enum Source {
    /** Real data based on GPS, network, etc. */
    REAL,
    /** Simulated data from {@link LocationSimulator}. */
    SIMULATED;
  }

  /**
   * Returns current location or null if not available.
   */
  public synchronized Location getLocation() {
    if (location == null && !isLocationSimulated()) {
      // Seed location with last known coarse location.
      Criteria criteria = new Criteria();
      criteria.setAccuracy(Criteria.ACCURACY_COARSE);
      location =
          locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, true));
    }
    return location;
  }

  /**
   * Returns true if the location is simulated (as opposed to the real physical
   * location). This convenience method gives the same result as {@code
   * getLocationSource() == Source.SIMULATED}.
   */
  public boolean isLocationSimulated() {
    return getLocationSource() == Source.SIMULATED;
  }

  public void setLocationSource(Source locationSource) {
    if (locationSource == this.locationSource) {
      return;
    }
    // Switch between simulator and actual LocationProvider.
    if (isLocationSimulated()) {
      stopSimulator();
      startRealLocationUpdates();
    } else {
      stopRealLocationUpdates();
      startSimulator();
    }
    this.locationSource = locationSource;
  }

  public Source getLocationSource() {
    return locationSource;
  }

  public void startListening() {
    if (isLocationSimulated()) {
      startSimulator();
    } else {
      startRealLocationUpdates();
    }
  }
  
  public void stopListening() {
    if (isLocationSimulated()) {
      stopSimulator();
    } else {
      stopRealLocationUpdates();
    }
  }

  /**
   * Starts the simulator.
   */
  private void startSimulator() {
    locationSimulator.start(this);
  }

  /**
   * Stops the simulator.
   */
  private void stopSimulator() {
    locationSimulator.stop();
  }

  /**
   * Requests updates on real location.
   */
  private void startRealLocationUpdates() {
    Log.d(TAG, "requesting location updates");
    // Get fine (GPS) updates as frequently as possible.
    Criteria criteria = new Criteria();
    criteria.setAccuracy(Criteria.ACCURACY_FINE);
    locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), 0, 0,
        this);
    // As a back up, get coarse (network) location periodically.
    criteria.setAccuracy(Criteria.ACCURACY_COARSE);
    locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true),
        COARSE_LOCATION_TIME, COARSE_LOCATION_DIST, this);
  }

  /**
   * Stops getting real location updates.
   */
  private void stopRealLocationUpdates() {
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

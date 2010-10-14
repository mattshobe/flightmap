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

import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.flightmap.common.MercatorProjection;
import com.google.flightmap.common.NavigationUtil;
import com.google.flightmap.common.data.LatLng;

/**
 * Provides a simulated {@link Location}. This class does not extend
 * {@link android.location.LocationProvider} nor does it get added as a test
 * provider through {@link android.location.LocationManager#addTestProvider}.
 * That is deliberate.
 * <p>
 * For this application we want users to be able to use the simulator mode to
 * learn how it works on the ground while stationary, rather than only being
 * able to learn it while moving. Portable aviation GPS units have built in
 * simulator modes for the same reason.
 */
class LocationSimulator {
  private static final String TAG = LocationSimulator.class.getSimpleName();
  /**
   * Milliseconds between position updates. This matches the rate from the real
   * GPS provider.
   */
  private static final long UPDATE_RATE = 1000;
  /**
   * Turn rate in degrees per second.
   */
  private static final int TURN_RATE = 6; // 2x standard rate.
  /**
   * Speed change rate in meters per second.
   */
  private static final float SPEED_RATE = 10.28889f; // 20 kts per second.
  /**
   * Climb/descent rate in meters per second.
   */
  private static final float CLIMB_RATE = 10.16f; // 2000 feet per minute.

  private static final float MIN_SPEED = 0;
  private static final float MAX_SPEED = (float) (900 / NavigationUtil.METERS_PER_SEC_TO_KNOTS);

  private final PositionUpdater updater = new PositionUpdater();
  private boolean isRunning;
  private Location location;
  private LocationHandler locationHandler;

  // Desired values for position. Units are as follows...
  // Speed: m/s ; Altitude: meters ; Track: true degrees
  private float desiredSpeed = (float) (120.0 / NavigationUtil.METERS_PER_SEC_TO_KNOTS);
  private float desiredTrack = Float.NaN;
  private float desiredAltitude = (float) (1500.0 / NavigationUtil.METERS_TO_FEET);

  LocationSimulator() {
  }

  /**
   * Starts the simulator. Position updates will be sent to the {@code
   * locationHandler} by calling {@link LocationHandler#onLocationChanged}
   */
  synchronized void start(LocationHandler locationHandler) {
    Log.d(TAG, "Started simualator");
    this.locationHandler = locationHandler;
    setRunning(true);
    updater.scheduleUpdate(UPDATE_RATE);
  }

  void stop() {
    Log.d(TAG, "Stopped simualator");
    setRunning(false);
  }

  /**
   * Updates position every {@link #UPDATE_RATE} milliseconds using
   * {@link PositionUpdater}.
   */
  public void update() {
    updater.scheduleUpdate(UPDATE_RATE);
    if (!isRunning()) {
      return;
    }
    updateLocation();
  }

  /**
   * Returns a default location to use if there's no real location to start
   * from. (The Googleplex in Mountain View, CA.)
   */
  public static Location getDefaultLocation() {
    Location result = new Location(TAG);
    result.setLatitude(37.422006);
    result.setLongitude(-122.084095);
    return result;
  }

  /**
   * Updates location and posts to {@link LocationHandler#onLocationChanged}.
   */
  private synchronized void updateLocation() {
    if (location == null) {
      // Initialize location from last known location
      location = locationHandler.getLocation();
      // If it's still null, use the default location.
      if (location == null) {
        location = getDefaultLocation();
      }
    }

    location.setProvider(TAG);
    location.setTime(System.currentTimeMillis());
    location.setAccuracy(10);
    location.setSpeed(updateValue(location.getSpeed(), desiredSpeed, SPEED_RATE));

    // Just in case desiredTrack is unset, set it here.
    if (Float.isNaN(desiredTrack)) {
      desiredTrack = location.getBearing();
    }

    location.setBearing(updateBearingValue(location.getBearing(), desiredTrack, TURN_RATE));
    location.setAltitude(updateValue((float) location.getAltitude(), desiredAltitude, CLIMB_RATE));
    changeLatLng();

    locationHandler.onLocationChanged(location);
    return;
  }

  /**
   * Updates the latitude and longitude of {@code location} based on the current
   * location, speed and track.
   */
  private synchronized void changeLatLng() {
    final float track = location.getBearing();
    final float speed = location.getSpeed();
    // Use Mercator projection and trig to compute new location.
    double trackRadians = Math.toRadians(NavigationUtil.normalizeBearing(track));
    double deltaX = speed * Math.sin(trackRadians);
    double deltaY = -speed * Math.cos(trackRadians); // -speed to adjust origin.
    int[] point = new int[2];
    final float zoom = 15; // arbitrarily chosen.
    MercatorProjection.toPoint(zoom, LatLng.fromDouble(location.getLatitude(), location
        .getLongitude()), point);
    point[0] = (int) (point[0] + deltaX + 0.5);
    point[1] = (int) (point[1] + deltaY + 0.5);
    LatLng latLng = MercatorProjection.fromPoint(zoom, point);
    location.setLatitude(latLng.latDeg());
    location.setLongitude(latLng.lngDeg());
  }

  /**
   * Returns the new value that should be set for a rate-based value.
   * 
   * @param currentValue current value.
   * @param desiredValue desired value.
   * @param changeRate rate of change per second.
   */
  private float updateValue(final float currentValue, final float desiredValue,
      final float changeRate) {
    if (Math.abs(desiredValue - currentValue) < changeRate) {
      return desiredValue;
    }
    return currentValue + (changeRate * Math.signum(desiredValue - currentValue));
  }

  /**
   * Returns the new value for a bearing. Handles special cases like the desired
   * bearing being 10 when the current bearing is 350.
   * 
   * @param bearing current bearing
   * @param desiredBearing desired bearing
   * @param turnRate rate of turn in degrees per second.
   */
  private float updateBearingValue(float bearing, float desiredBearing, int turnRate) {
    float absDiff = Math.abs(desiredBearing - bearing);
    if (absDiff < turnRate) {
      return desiredBearing;
    }
    if (absDiff >= 180) {
      bearing = bearing < 180 ? bearing + 360 : bearing;
      desiredBearing = desiredBearing < 180 ? desiredBearing + 360 : desiredBearing;
    }
    return (float) NavigationUtil.normalizeBearing(bearing
        + (turnRate * Math.signum(desiredBearing - bearing)));
  }

  /**
   * Updates the simulated position using a delayed message.
   */
  private class PositionUpdater extends Handler {
    private static final int POSITION_UPDATE_MESSAGE = 1;

    @Override
    public void handleMessage(Message msg) {
      update();
    }

    /**
     * Call {@link #update} after {@code delay} milliseconds.
     */
    private void scheduleUpdate(long delay) {
      removeMessages(POSITION_UPDATE_MESSAGE);
      sendMessageDelayed(obtainMessage(POSITION_UPDATE_MESSAGE), delay);
    }
  }


  private synchronized void setRunning(boolean isRunning) {
    this.isRunning = isRunning;
  }


  private synchronized boolean isRunning() {
    return isRunning;
  }

  synchronized float getDesiredSpeed() {
    return desiredSpeed;
  }

  synchronized void setDesiredSpeed(float desiredSpeed) {
    this.desiredSpeed = Math.min(desiredSpeed, MAX_SPEED);
    this.desiredSpeed = Math.max(this.desiredSpeed, MIN_SPEED);
  }

  synchronized void stopMoving() {
    setDesiredTrack(location.getBearing());
    setDesiredAltitude((float) location.getAltitude());
    setDesiredSpeed(0);
    location.setSpeed(0);
  }

  synchronized float getDesiredTrack() {
    return desiredTrack;
  }

  synchronized void setDesiredTrack(float desiredTrack) {
    this.desiredTrack = desiredTrack;
  }

  synchronized float getDesiredAltitude() {
    return desiredAltitude;
  }

  synchronized void setDesiredAltitude(float desiredAltitude) {
    this.desiredAltitude = desiredAltitude;
  }
}

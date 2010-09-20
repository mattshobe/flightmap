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

import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
  private boolean isRunning;
  private final PositionUpdater updater = new PositionUpdater();
  private XmlResourceParser xmlParser;
  private final Context context;
  private Location location;
  // Reused for distance and bearing calculations.
  private float results[] = new float[2];
  private LocationHandler locationHandler;

  LocationSimulator(Context context) {
    this.context = context;
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
    initializeParser();
    if (!isRunning()) {
      return;
    }
    updateLocation();
  }

  /**
   * Initializes the XML parser.
   */
  private synchronized void initializeParser() {
    if (xmlParser == null) {
      xmlParser = context.getResources().getXml(com.google.flightmap.android.R.xml.simtrack);
    }
  }


  /**
   * Parses the next location from the GPX file and sends it to
   * {@link LocationHandler}. Loops back to the start when the end of the XML is
   * encountered.
   */
  private void updateLocation() {
    boolean foundPosition = false;
    float lat = Float.NaN;
    float lng = Float.NaN;
    float alt = Float.NaN;
    try {
      int eventType = xmlParser.getEventType();
      while (!foundPosition) {
        switch (eventType) {
          case XmlResourceParser.END_DOCUMENT:
            synchronized (this) {
              xmlParser = null;
              initializeParser();
            }
            break;

          case XmlResourceParser.START_DOCUMENT:
            Log.d(TAG, "Start document");
            break;

          case XmlResourceParser.START_TAG:
            String tag = xmlParser.getName();
            Log.d(TAG, "start tag " + tag);
            if ("trkpt".equals(tag)) {
              String latAttr = xmlParser.getAttributeValue(null, "lat");
              String lngAttr = xmlParser.getAttributeValue(null, "lon");
              Log.d(TAG, "lat=" + latAttr + " lng=" + lngAttr);
              lat = Float.parseFloat(latAttr);
              lng = Float.parseFloat(lngAttr);
              if (!Float.isNaN(alt)) {
                foundPosition = true;
              }
            } else if ("ele".equals(tag)) {
              String eleText = xmlParser.nextText();
              Log.d(TAG, "ele=" + eleText);
              alt = Float.parseFloat(eleText);
              if (!Float.isNaN(lat) && !Float.isNaN(lng)) {
                foundPosition = true;
              }
            }
            break;
        }
        eventType = xmlParser.next();
      }
    } catch (XmlPullParserException e) {
      Log.e(TAG, "XML exception", e);
      synchronized (this) {
        xmlParser = null;
        initializeParser();
      }
    } catch (IOException e) {
      Log.e(TAG, "IO exception", e);
      synchronized (this) {
        xmlParser = null;
        initializeParser();
      }
    }
    Location newLocation = new Location(LocationSimulator.class.getSimpleName());
    newLocation.setTime(System.currentTimeMillis());
    newLocation.setAltitude(alt);
    newLocation.setLatitude(lat);
    newLocation.setLongitude(lng);
    synchronized (this) {
      // Compute bearing and speed if possible.
      if (location != null) {
        Location
            .distanceBetween(location.getLatitude(), location.getLongitude(), lat, lng, results);
        float meters = results[0];
        float seconds = (newLocation.getTime() - location.getTime()) / 1000.0f;
        if (seconds > 0) {
          newLocation.setSpeed(meters / seconds);
        }
        newLocation.setBearing(results[1]);
      }
      location = newLocation;
      locationHandler.onLocationChanged(newLocation);
    }
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
}

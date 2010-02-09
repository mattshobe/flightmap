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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class BlackBox extends Activity {
  private static final String TAG = BlackBox.class.getSimpleName();
  /** Milliseconds beteween screen updates. */
  private static final int UPDATE_RATE = 100;
  private static final double METERS_PER_FOOT = 3.2808399;
  private static final double METERS_PER_SEC_TO_KNOTS = 2.2369;
  private AirportDbAdapter airportReader;
  private final LocationServiceConnection locationServiceConnection =
      new LocationServiceConnection();
  private boolean isRunning;
  private long lastUpdateTime;
  private UpdateHandler updater = new UpdateHandler();

  // Debugging frequency of location updates
  private long numLocationUpdates;
  private long timeBetweenUpdatesTotal;
  private long lastLocationTime;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    airportReader = new AirportDbAdapter();
    airportReader.open();
  }

  @Override
  protected void onResume() {
    super.onResume();
    bindService(new Intent(ILocationService.class.getName()), locationServiceConnection,
        Context.BIND_AUTO_CREATE);
    isRunning = true;
    update();
  }

  @Override
  protected void onPause() {
    super.onPause();
    isRunning = false;
    unbindService(locationServiceConnection);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    airportReader.close();
  }

  /**
   * Updates the view every {@link #UPDATE_RATE} milliseconds using
   * {@link UpdateHandler}.
   */
  private void update() {
    if (!isRunning) {
      return;
    }
    long now = System.currentTimeMillis();
    if (now - lastUpdateTime > UPDATE_RATE) {
      drawUi();
      lastUpdateTime = now;
    }
    updater.scheduleUpdate(UPDATE_RATE);
  }

  private void drawUi() {
    TableLayout table = new TableLayout(this);
    table.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
        LinearLayout.LayoutParams.FILL_PARENT));
    table.setColumnStretchable(1, true);
    Location location = locationServiceConnection.getLocation();
    if (null == location) {
      table.addView(createLabelValueRow("Location", "Unknown"));
      setContentView(table);
      return;
    }
    table.addView(createLabelValueRow("Lat", String.format("%4.6f", location.getLatitude())));
    table.addView(createLabelValueRow("Lng", String.format("%4.6f", location.getLongitude())));
    table.addView(createLabelValueRow("Alt", String.format("%.0f", location.getAltitude()
        * METERS_PER_FOOT)));
    table.addView(createLabelValueRow("Knots", String.format("%.0f", location.getSpeed()
        * METERS_PER_SEC_TO_KNOTS)));
    table.addView(createLabelValueRow("Bearing", String.format("%.1f", location.getBearing())));
    table.addView(createLabelValueRow("Accuracy", String.format("%.3f", location.getAccuracy())));
    table.addView(createLabelValueRow("Location time", "" + location.getTime()));
    table.addView(createLabelValueRow("Current time", "" + System.currentTimeMillis()));
    if (lastLocationTime != location.getTime()) {
      numLocationUpdates++;
      if (lastLocationTime > 0) {
        timeBetweenUpdatesTotal += location.getTime() - lastLocationTime;
      }
      lastLocationTime = location.getTime();
    }
    if (numLocationUpdates > 0) {
      table.addView(createLabelValueRow("Avg update rate", ""
          + Math.round((float) timeBetweenUpdatesTotal / numLocationUpdates)));
    }
    setContentView(table);
  }

  private TableRow createLabelValueRow(String label, String value) {
    TableRow result = new TableRow(this);
    result.addView(createTextView(label));
    result.addView(createTextView(value));
    return result;
  }

  private View createTextView(String text) {
    TextView result = new TextView(this);
    result.setText(text);
    result.setPadding(3, 3, 3, 3);
    return result;
  }

  /**
   * Updates the UI using a delayed message.
   */
  private class UpdateHandler extends Handler {
    private static final int UPDATE_MESSAGE = 1;

    @Override
    public void handleMessage(Message msg) {
      update();
    }

    /**
     * Call {@link #update} after {@code delay} milliseconds.
     */
    public void scheduleUpdate(long delay) {
      removeMessages(UPDATE_MESSAGE);
      sendMessageDelayed(obtainMessage(UPDATE_MESSAGE), delay);
    }
  }

  /**
   * Connects to the ILocationService.
   */
  private class LocationServiceConnection implements ServiceConnection {
    private ILocationService locationService;

    public Location getLocation() {
      if (null != locationService) {
        try {
          return locationService.getLocation();
        } catch (RemoteException e) {
          Log.e(TAG, "RPC exception", e);
        }
      } else {
        Log.d(TAG, "locationService is null");
      }
      return null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      locationService = ILocationService.Stub.asInterface(service);
      Toast.makeText(BlackBox.this, "Connected to ILocationService", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Toast.makeText(BlackBox.this, "Disconnected from ILocationService", Toast.LENGTH_LONG).show();
      locationService = null;
    }

  }
}

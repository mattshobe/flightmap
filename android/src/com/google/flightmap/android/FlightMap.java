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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.flightmap.common.AirportDirectory;
import com.google.flightmap.common.AviationDbAdapter;
import com.google.flightmap.common.CachedAirportDirectory;
import com.google.flightmap.common.CachedAviationDbAdapter;
import com.google.flightmap.common.CustomGridAirportDirectory;
import com.google.flightmap.common.ProgressListener;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class FlightMap extends Activity {
  private static final String TAG = FlightMap.class.getSimpleName();
  /**
   * Milliseconds between screen updates. Note that the fastest I've seen GPS
   * updates arrive is once per second.
   */
  private static long UPDATE_RATE = 100;

  private static final int MENU_SETTINGS = 0;
  private static final int DOWNLOAD_FAILED = 1;

  // Saved instance state constants
  private static final String DISCLAIMER_ACCEPTED = "disclaimer-accepted";
  private static final String DATABASE_DOWNLOADED = "database-downloaded";

  private static final String AVIATION_DATABASE_URL = 
      "http://sites.google.com/site/flightmapdata/aviation-db/aviation.db";

  private boolean disclaimerAccepted;
  private boolean isRunning;
  private UpdateHandler updater = new UpdateHandler();
  private LocationHandler locationHandler;
  private MapView mapView;
  AviationDbAdapter aviationDbAdapter;
  AirportDirectory airportDirectory;
  UserPrefs userPrefs;
  private boolean initializationDone;
  private boolean databaseDownloaded;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    locationHandler =
        new LocationHandler((LocationManager) getSystemService(Context.LOCATION_SERVICE));

    setDatabaseDownloaded(
        (null != savedInstanceState && savedInstanceState.getBoolean(DATABASE_DOWNLOADED)));

    // Show the disclaimer screen if there's no previous state, or the user
    // didn't accept the disclaimer.
    if (null == savedInstanceState || !savedInstanceState.getBoolean(DISCLAIMER_ACCEPTED)) {
      setDisclaimerAccepted(false);
      showDisclaimerView();
    } else { // disclaimer accepted.
      setDisclaimerAccepted(true);
      downloadDatabase();
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    switch (id) {
      case DOWNLOAD_FAILED:
        return new AlertDialog.Builder(this).setMessage(R.string.download_failed)
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
              }
            }).create();

      default:
        return null;
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(DISCLAIMER_ACCEPTED, isDisclaimerAccepted());
    outState.putBoolean(DATABASE_DOWNLOADED, isDatabaseDownloaded());
    MapView map = getMapView();
    if (null != map) {
      map.saveInstanceState(outState);
    }
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    super.onRestoreInstanceState(savedInstanceState);

    MapView map = getMapView();
    if (null != map) {
      map.restoreInstanceState(savedInstanceState);
    }
  }


  private void showDisclaimerView() {
    setContentView(R.layout.disclaimer);

    // Set disclaimer "agree" button to switch to map view.
    Button agreeButton = (Button) findViewById(R.id.agree);
    agreeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        setDisclaimerAccepted(true);
        downloadDatabase();
      }
    });
  }

  /**
   * If there is a network connection, try to update the database.
   */
  private void downloadDatabase() {
    if (isDatabaseDownloaded()) {
      downloadDatabaseDone();
      return;
    }

    final File localFile = new File(AndroidAviationDbAdapter.DATABASE_PATH);
    final File workingDir = new File(AndroidAviationDbAdapter.DATABASE_PATH + ".work");
    URL url = null;
    try {
      url = new URL(AVIATION_DATABASE_URL);
    } catch (MalformedURLException e) {
      Log.e(TAG, "Bad url", e);
      return;
    }

    final ProgressDialog dialog = new ProgressDialog(this);
    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    dialog.setMessage(this.getString(R.string.updating_database));
    dialog.show();

    final ProgressListener dbUpdateListener = new ProgressListener() {
      @Override
      public void hasCompleted(boolean success) {
        dialog.dismiss();
        Log.i(TAG, "Download completed.  Success: " + success);
        if (!success) {
          showDialog(DOWNLOAD_FAILED);
          Log.e(TAG, "Download failed");
        }
        downloadDatabaseDone();
      }

      @Override
      public void hasProgressed(int percent) {
        dialog.setProgress(percent);
      }
    };

    final FileUpdaterTask.Params params = new FileUpdaterTask.Params(localFile, url, workingDir);
    new FileUpdaterTask(dbUpdateListener).execute(params);
  }

  private void downloadDatabaseDone() {
    setDatabaseDownloaded(true);
    initializeApplication();
  }

  private void initializeApplication() {
    userPrefs = new UserPrefs(this);
    aviationDbAdapter = new CachedAviationDbAdapter(new AndroidAviationDbAdapter(userPrefs));
    airportDirectory = new CustomGridAirportDirectory(aviationDbAdapter);
    new CachedAirportDirectory(new CustomGridAirportDirectory(aviationDbAdapter));

    // TODO: handle the case of this throwing when there's no database.
    airportDirectory.open();

    if (null == getMapView()) {
      setMapView(new MapView(this));
    }

    setInitializationDone(true);
    setRunning(true);

    // Now show the map.
    setContentView(getMapView());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    menu.add(0, MENU_SETTINGS, 0, "Settings");
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case MENU_SETTINGS:
        Intent startIntent = new Intent(this, UserPrefsActivity.class);
        startActivity(startIntent);
        return true;
    }
    return false;
  }

  @Override
  protected void onResume() {
    super.onResume();
    locationHandler.startListening();
    setRunning(isInitializationDone());
    update();
  }

  @Override
  protected void onPause() {
    super.onPause();
    setRunning(false);
    locationHandler.stopListening();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (airportDirectory != null) {
      airportDirectory.close();
    }
  }

  /**
   * Updates the view every {@link #UPDATE_RATE} milliseconds using
   * {@link UpdateHandler}.
   */
  private void update() {
    updater.scheduleUpdate(UPDATE_RATE);
    if (!isRunning()) {
      return;
    }
    drawUi();
  }

  private void drawUi() {
    MapView map = getMapView();
    if (!isDisclaimerAccepted()) {
      return;
    }
    Location location = locationHandler.getLocation();
    map.drawMap(location);
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

  private synchronized boolean isRunning() {
    return isRunning;
  }

  private synchronized void setRunning(boolean isRunning) {
    this.isRunning = isRunning;
  }

  private synchronized MapView getMapView() {
    return mapView;
  }

  private synchronized void setMapView(MapView mapView) {
    this.mapView = mapView;
  }

  public synchronized void setDisclaimerAccepted(boolean disclaimerAccepted) {
    this.disclaimerAccepted = disclaimerAccepted;
  }

  public synchronized boolean isDisclaimerAccepted() {
    return disclaimerAccepted;
  }


  private synchronized boolean isInitializationDone() {
    return initializationDone;
  }

  private synchronized void setInitializationDone(final boolean initializationDone) {
    this.initializationDone = initializationDone;
  }

  private synchronized boolean isDatabaseDownloaded() {
    return databaseDownloaded;
  }

  private synchronized void setDatabaseDownloaded(final boolean databaseDownloaded) {
    this.databaseDownloaded = databaseDownloaded;
  }
}
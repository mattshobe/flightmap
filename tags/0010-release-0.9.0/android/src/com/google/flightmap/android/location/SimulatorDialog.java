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

import java.util.LinkedList;

import android.app.Dialog;
import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.google.flightmap.android.MapView;
import com.google.flightmap.android.R;
import com.google.flightmap.common.CachedMagneticVariation;
import com.google.flightmap.common.NavigationUtil;
import com.google.flightmap.common.NavigationUtil.DistanceUnits;
import com.google.flightmap.common.data.LatLng;

/**
 * Handles the UI for controlling the {@link LocationSimulator}.
 */
public class SimulatorDialog extends Dialog implements Button.OnClickListener {
  public static final String TAG = SimulatorDialog.class.getSimpleName();

  private final LocationHandler locationHandler;
  private final LinkedList<Button> buttons = new LinkedList<Button>();
  private final CachedMagneticVariation magneticVariation = new CachedMagneticVariation();
  private DistanceUnits distanceUnits;
  private CheckBox enabled;
  private TextView speedText;
  private TextView speedUnits;
  private TextView trackText;
  private TextView trackUnits;
  private TextView altText;
  private TextView altUnits;

  public SimulatorDialog(Context context, LocationHandler locationHandler) {
    super(context);
    this.locationHandler = locationHandler;
  }

  @Override
  protected void onCreate(android.os.Bundle savedInstanceState) {
    setContentView(R.layout.simulator);
    setTitle("Simulator Settings");

    speedText = (TextView) findViewById(R.id.sim_speed_value);
    speedUnits = (TextView) findViewById(R.id.sim_speed_units);
    trackText = (TextView) findViewById(R.id.sim_track_value);
    trackUnits = (TextView) findViewById(R.id.sim_track_units);
    altText = (TextView) findViewById(R.id.sim_alt_value);
    altUnits = (TextView) findViewById(R.id.sim_alt_units);

    initializeButtonList();
    initializeButtonListeners();
    enabled = (CheckBox) findViewById(R.id.enable_simulator);
    enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setSimulatorButtonsEnabled(isChecked);
        if (isChecked) {
          locationHandler.setLocationSource(LocationHandler.Source.SIMULATED);
        } else {
          locationHandler.setLocationSource(LocationHandler.Source.REAL);
          SimulatorDialog.this.dismiss();
        }
      }
    });
  };

  /**
   * Updates the values shown on the dialog.
   */
  public void updateDialog() {
    final DistanceUnits units = getUnits();
    final boolean simEnabled = locationHandler.isLocationSimulated();
    enabled.setChecked(simEnabled);
    setSimulatorButtonsEnabled(simEnabled);

    LocationSimulator simulator = locationHandler.getLocationSimulator();
    // Speed
    speedText.setText(String.format("%.0f", units.getSpeed(simulator.getDesiredSpeed())));
    speedUnits.setText(" " + units.speedAbbreviation);

    // Track - convert true to magnetic for display.
    Location location = locationHandler.getLocation();
    if (location == null) {
      location = LocationSimulator.getDefaultLocation();
    }
    float variation =
        magneticVariation.getMagneticVariation(LatLng.fromDouble(location.getLatitude(), location
            .getLongitude()), (float) location.getAltitude());
    float magneticTrack = simulator.getDesiredTrack();
    if (Float.isNaN(magneticTrack)) {
      // Initialize to the current magnetic track.
      magneticTrack = location.getBearing() + variation;
      // Round to nearest 10 degrees.
      magneticTrack = (int) (Math.round(magneticTrack / 10.0) * 10);
      simulator.setDesiredTrack(magneticTrack - variation); // true degrees.
    } else {
      magneticTrack += variation;
    }
    magneticTrack = (float) NavigationUtil.normalizeBearing(magneticTrack);

    // Displayed degrees are always in magnetic.
    trackText.setText(String.format(" %03.0f", magneticTrack));
    trackUnits.setText(MapView.DEGREES_SYMBOL);

    // Altitude - round to nearest 10 foot increment.
    int altitudeNearestTen =
        (int) (Math.round(simulator.getDesiredAltitude() * NavigationUtil.METERS_TO_FEET / 10.0) * 10);
    altText.setText(String.format("%,5d", altitudeNearestTen));
    altUnits.setText(" ft");
  }

  private void initializeButtonListeners() {
    for (Button button : buttons) {
      button.setOnClickListener(this);
    }
  }

  /**
   * Toggles the simulator controls on or off.
   */
  private void setSimulatorButtonsEnabled(boolean enable) {
    for (Button button : buttons) {
      button.setEnabled(enable);
    }
  }

  /**
   * Adds all the buttons to {@code buttons}.
   */
  private void initializeButtonList() {
    buttons.add((Button) findViewById(R.id.sim_speed_increment));
    buttons.add((Button) findViewById(R.id.sim_track_increment));
    buttons.add((Button) findViewById(R.id.sim_altitude_increment));
    buttons.add((Button) findViewById(R.id.sim_speed_decrement));
    buttons.add((Button) findViewById(R.id.sim_track_decrement));
    buttons.add((Button) findViewById(R.id.sim_altitude_decrement));
    buttons.add((Button) findViewById(R.id.sim_stop));
  }

  @Override
  public void onClick(View v) {
    final LocationSimulator simulator = locationHandler.getLocationSimulator();
    final DistanceUnits units = getUnits();
    switch (v.getId()) {
      case R.id.sim_speed_increment:
        simulator
            .setDesiredSpeed((float) (simulator.getDesiredSpeed() + (10.0 / units.speedMultiplier)));
        break;
      case R.id.sim_speed_decrement:
        simulator
            .setDesiredSpeed((float) (simulator.getDesiredSpeed() - (10.0 / units.speedMultiplier)));
        break;
      case R.id.sim_stop:
        simulator.stopMoving();
        break;
      case R.id.sim_track_increment:
        simulator.setDesiredTrack((float) NavigationUtil.normalizeBearing(simulator
            .getDesiredTrack() + 10));
        break;
      case R.id.sim_track_decrement:
        simulator.setDesiredTrack((float) NavigationUtil.normalizeBearing(simulator
            .getDesiredTrack() - 10));
        break;
      case R.id.sim_altitude_increment:
        simulator
            .setDesiredAltitude((float) (simulator.getDesiredAltitude() + 100.0 / NavigationUtil.METERS_TO_FEET));
        break;
      case R.id.sim_altitude_decrement:
        simulator
            .setDesiredAltitude((float) (simulator.getDesiredAltitude() - 100.0 / NavigationUtil.METERS_TO_FEET));
        break;

      default:
        Log.e(TAG, "Unknown id: " + v.getId());
        break;
    }
    updateDialog();
  }

  public synchronized DistanceUnits getUnits() {
    return distanceUnits;
  }

  public synchronized void setUnits(DistanceUnits distanceUnits) {
    this.distanceUnits = distanceUnits;
  }
}

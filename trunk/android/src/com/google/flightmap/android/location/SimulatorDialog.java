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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.google.flightmap.android.R;

/**
 * Utility class to handle the UI for controlling the {@link LocationSimulator}.
 */
public class SimulatorDialog {
  public static final String TAG = SimulatorDialog.class.getSimpleName();

  private SimulatorDialog() {
    // Utility class.
  }

  public static Dialog getDialog(Context context, final LocationHandler locationHandler) {
    final Dialog result = new Dialog(context);
    result.setContentView(R.layout.simulator);
    result.setTitle("Simulator Settings");
    final boolean simEnabled = locationHandler.isLocationSimulated();
    final LinkedList<Button> buttons = getAllButtons(result);
    initializeButtonListeners(buttons, new ButtonListener(result));
    setSimulatorButtonsEnabled(buttons, simEnabled);

    CheckBox enabled = (CheckBox) result.findViewById(R.id.enable_simulator);
    enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setSimulatorButtonsEnabled(buttons, isChecked);
        if (isChecked) {
          locationHandler.setLocationSource(LocationHandler.Source.SIMULATED);
        } else {
          locationHandler.setLocationSource(LocationHandler.Source.REAL);
          result.dismiss();
        }
      }
    });

    return result;
  }

  private static void initializeButtonListeners(LinkedList<Button> buttons,
      Button.OnClickListener listener) {
    for (Button button : buttons) {
      button.setOnClickListener(listener);
    }
  }

  /**
   * Toggles the simulator controls on or off.
   */
  private static void setSimulatorButtonsEnabled(LinkedList<Button> buttons, boolean enable) {
    for (Button button : buttons) {
      button.setEnabled(enable);
    }
  }

  /**
   * Returns all the buttons on the dialog.
   */
  private static LinkedList<Button> getAllButtons(Dialog simDialog) {
    LinkedList<Button> simButtons = new LinkedList<Button>();
    simButtons.add((Button) simDialog.findViewById(R.id.sim_speed_increment));
    simButtons.add((Button) simDialog.findViewById(R.id.sim_track_increment));
    simButtons.add((Button) simDialog.findViewById(R.id.sim_altitude_increment));
    simButtons.add((Button) simDialog.findViewById(R.id.sim_speed_decrement));
    simButtons.add((Button) simDialog.findViewById(R.id.sim_track_decrement));
    simButtons.add((Button) simDialog.findViewById(R.id.sim_altitude_decrement));
    simButtons.add((Button) simDialog.findViewById(R.id.sim_stop));
    return simButtons;
  }

  private static class ButtonListener implements Button.OnClickListener {
    private final Dialog simDialog;

    private ButtonListener(Dialog simDialog) {
      this.simDialog = simDialog;
    }

    @Override
    public void onClick(View v) {
      switch (v.getId()) {
        case R.id.sim_speed_increment:
          Log.i(TAG, "sim_speed_increment");
          break;
        case R.id.sim_speed_decrement:
          Log.i(TAG, "sim_speed_decrement");
          break;
        case R.id.sim_stop:
          Log.i(TAG, "sim_stop");
          break;
        case R.id.sim_track_decrement:
          Log.i(TAG, "sim_track_decrement");
          break;
        case R.id.sim_track_increment:
          Log.i(TAG, "sim_track_increment");
          break;
        case R.id.sim_altitude_increment:
          Log.i(TAG, "sim_altitude_increment");
          break;
        case R.id.sim_altitude_decrement:
          Log.i(TAG, "sim_altitude_decrement");
          break;

        default:
          Log.e(TAG, "Unknown id: " + v.getId());
          break;
      }
    }
  }
}

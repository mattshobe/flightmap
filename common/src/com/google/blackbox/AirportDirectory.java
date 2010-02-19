// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.blackbox;

import com.google.blackbox.data.AirportDistance;
import com.google.blackbox.data.LatLng;

import java.sql.SQLException;
/**
 *
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public interface AirportDirectory {
  public AirportDistance[] getNearestAirports(final LatLng position, final int numAirports)
      throws SQLException;

  public AirportDistance[] getAirportsWithinRadius(final LatLng position, final double radius)
      throws SQLException;
}

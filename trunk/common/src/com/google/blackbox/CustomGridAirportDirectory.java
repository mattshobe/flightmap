// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.blackbox;


import com.google.blackbox.data.Airport;
import com.google.blackbox.data.AirportDistance;
import com.google.blackbox.data.LatLng;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.TreeSet;
/**
 *
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class CustomGridAirportDirectory implements AirportDirectory {
  private final Connection dbConnection;

  public CustomGridAirportDirectory(Connection dbConnection) {
    this.dbConnection = dbConnection;
  }

  public AirportDistance[] getNearestAirports(final LatLng position, final int numAirports)
      throws SQLException{
    throw new RuntimeException("Not implemented yet");
  }

  public AirportDistance[] getAirportsWithinRadius(final LatLng position, final double radius)
      throws SQLException {

    double earthRadiusAtLat = NavigationUtil.EARTH_RADIUS * Math.sin(Math.PI/2-position.latRad());
    double longRadius = radius/(2*Math.PI*earthRadiusAtLat)*360;
    double latRadius = radius/60;
    int radiusE6 = (int)(Math.max(longRadius, latRadius)*1E6);

    LinkedList<int[]> cellRanges = CustomGridUtil.GetCellsInRadius(position, radiusE6);
    TreeSet<AirportDistance> airportsInRange = new TreeSet<AirportDistance>();

    PreparedStatement getAirportsInCellStmt = null;
    try{
      getAirportsInCellStmt = this.dbConnection.prepareStatement(
          "SELECT _id, lat, lng FROM airports WHERE cell_id >= ? and cell_id < ?");

      for (int[] range: cellRanges) {
        getAirportsInCellStmt.setInt(1, range[0]);
        getAirportsInCellStmt.setInt(2, range[1]);
        ResultSet airportsInCell = getAirportsInCellStmt.executeQuery();
        while (airportsInCell.next()) {
          int id = airportsInCell.getInt(1);
          int latE6 = airportsInCell.getInt(2);
          int lngE6 = airportsInCell.getInt(3);

          LatLng airportPosition = new LatLng(latE6, lngE6);
          double distance = NavigationUtil.computeDistance(position, airportPosition);
          if (distance <= radius) {
            Airport newAirportInRange = this.getAirportFromId(id, airportPosition);
            airportsInRange.add(new AirportDistance(newAirportInRange, distance));
          }
        }
      }
    } finally {
      if (getAirportsInCellStmt != null) {
        try {
          getAirportsInCellStmt.close();
        } catch (SQLException sqlex) {
          //TODO(aristidis): Log failure
          sqlex.printStackTrace();
        }
      }
    }

    AirportDistance[] airportsInRangeArray = new AirportDistance[airportsInRange.size()];

    return airportsInRange.toArray(airportsInRangeArray);
  }

  Airport getAirportFromId(int airportID, LatLng knownPosition) throws SQLException {
    String icao;
    String name;
    LatLng location;

    final Statement getAirportFromIdStmt = this.dbConnection.createStatement();
    ResultSet airportFromIdResultSet;
    if (knownPosition == null) {
       airportFromIdResultSet = getAirportFromIdStmt.executeQuery(
          "SELECT icao, name, lat, lng FROM airports WHERE _id = " + airportID);
       if (!airportFromIdResultSet.next())
         return null;

       icao = airportFromIdResultSet.getString(1);
       name = airportFromIdResultSet.getString(2);
       int lat = airportFromIdResultSet.getInt(3);
       int lng = airportFromIdResultSet.getInt(4);
       location = new LatLng(lat, lng);
    } else {
       airportFromIdResultSet = getAirportFromIdStmt.executeQuery(
          "SELECT icao, name FROM airports WHERE _id = " + airportID);
       if (!airportFromIdResultSet.next())
         return null;

       icao = airportFromIdResultSet.getString(1);
       name = airportFromIdResultSet.getString(2);
       location = knownPosition;
    }

    return new Airport(icao, name, location);
  }

  Airport getAirportFromId(int airportID) throws SQLException {
    return getAirportFromId(airportID, null);
  }

  protected static class AirportIdDistance implements Comparable<AirportIdDistance> {
    int id;
    int lat, lng;
    double distance;

    AirportIdDistance(int id, int lat, int lng, double distance) {
      this.reset(id, lat, lng, distance);
    }

    void reset(int id, int lat, int lng, double distance) {
      this.id = id;
      this.lat = lat;
      this.lng = lng;
      this.distance = distance;
    }

    public int compareTo(AirportIdDistance o) {
      int sign = (int)(Math.signum(this.distance - o.distance));
      return sign != 0 ? sign : this.id - o.id;
    }
  }
}

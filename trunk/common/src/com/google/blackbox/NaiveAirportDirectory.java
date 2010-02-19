// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.blackbox;
import com.google.blackbox.data.Airport;
import com.google.blackbox.data.AirportDistance;
import com.google.blackbox.data.LatLng;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Provides access to the airport database.
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class NaiveAirportDirectory implements AirportDirectory {
  private final Connection dbConnection;

  public NaiveAirportDirectory(Connection dbConnection) {
    this.dbConnection = dbConnection;
  }

  /** Find the nearest airports to a given position.
   * <p>Currently does a naive linear scan over all (~14'000) airports.</p>
   * @param position Target position.
   * @param numAirports Maximum number of airports to return.
   */
  public AirportDistance[] getNearestAirports(LatLng position, int numAirports)
    throws SQLException {

    class AirportIdDistance implements Comparable<AirportIdDistance> {
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


    TreeSet<AirportIdDistance> nearestAirportIDs = new TreeSet<AirportIdDistance>();

    int count = 0;
    double maxDistance = Double.POSITIVE_INFINITY;

    Connection dbConn = this.dbConnection;
    Statement dbStmt = dbConn.createStatement();
    ResultSet rs = dbStmt.executeQuery("SELECT _id, lat, lng FROM airports");
    while (rs.next()) {
      int id = rs.getInt(1);
      int lat = rs.getInt(2);
      int lng = rs.getInt(3);
      double distance = NavigationUtil.computeDistance(position, lat, lng);


      if (count < numAirports) {
        nearestAirportIDs.add(new AirportIdDistance(id, lat, lng, distance));
        ++count;
      } else if (distance < maxDistance) {
        AirportIdDistance last = nearestAirportIDs.last();
        nearestAirportIDs.remove(last);
//        nearestAirportIDs.add(new AirportIdDistance(id, lat, lng, distance));
        last.reset(id, lat, lng, distance);
        nearestAirportIDs.add(last);
      }
      maxDistance = nearestAirportIDs.last().distance;
    }
    dbStmt.close();

    PreparedStatement airportFromIdStmt = dbConn.prepareStatement(
      "SELECT icao, name FROM airports WHERE id = ?");
    AirportDistance[] nearestAirports = new AirportDistance[count];
    int index = 0;
    for (AirportIdDistance nearestAirportIdDistance: nearestAirportIDs) {
      airportFromIdStmt.setInt(1,nearestAirportIdDistance.id);
      ResultSet airportRS = airportFromIdStmt.executeQuery();
      if (!airportRS.next()) {
        throw new IllegalStateException(
            "Could not find airport corresponding to ID.");
      }

      String icao = airportRS.getString(1);
      String name = airportRS.getString(2);
      int lat = nearestAirportIdDistance.lat;
      int lng = nearestAirportIdDistance.lng;
      double distance = nearestAirportIdDistance.distance;

      Airport airport = new Airport(icao,name,lat,lng);
      nearestAirports[index++] = new AirportDistance(airport,distance);
    }

    return nearestAirports;

  }

  /** Find the airports within {@code radius} from {@code position}.
   * <p>Currently does a naive linear scan over all (~14'000) airports.</p>
   * @return Airports within radius, sorted by increasing distance.
   */
  public AirportDistance[] getAirportsWithinRadius(LatLng position, double radius)
      throws SQLException {

    TreeSet<AirportDistance> airportsWithinRadius =
        new TreeSet<AirportDistance>();

    Connection dbConn = this.dbConnection;
    PreparedStatement airportFromIdStmt = dbConn.prepareStatement(
      "SELECT icao, name FROM airports WHERE _id = ?");

    Statement dbStmt = dbConn.createStatement();

    ResultSet rs = dbStmt.executeQuery("SELECT _id, lat, lng FROM airports");
    while (rs.next()) {
      int id = rs.getInt(1);
      int lat = rs.getInt(2);
      int lng = rs.getInt(3);
      double distance = NavigationUtil.computeDistance(position, lat, lng);

      if (distance <= radius) {
        airportFromIdStmt.setInt(1, id);
        ResultSet airportRS = airportFromIdStmt.executeQuery();
        if (!airportRS.next()) {
          throw new IllegalStateException(
              "Could not find airport corresponding to ID.");
        }

        String icao = airportRS.getString(1);
        String name = airportRS.getString(2);

        Airport airport = new Airport(icao,name,lat,lng);
        AirportDistance airportDistance =
            new AirportDistance(airport, distance);
        airportsWithinRadius.add(airportDistance);
      }
    }
    AirportDistance[] airportsWithinRadiusArray =
        new AirportDistance[airportsWithinRadius.size()];

    return airportsWithinRadius.toArray(airportsWithinRadiusArray);
  }
}

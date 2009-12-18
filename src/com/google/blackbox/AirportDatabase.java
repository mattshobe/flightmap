// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.blackbox;
import com.google.blackbox.data.Airport;
import com.google.blackbox.data.LatLng;
import java.util.LinkedList;
import java.util.TreeSet;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
/**
 * Provides access to the airport database.
 * @author aristidis@google.com (Aristidis Papaioannou)
 */
public class AirportDatabase {

  /**
   * Holds a reference to an Airport and its corresponding distance.
   */
  public class AirportDistance implements Comparable<AirportDistance>{
    public final Airport airport;
    /** Distance to {@link #airport} in nautical miles.
     * Caution: Value is staticaly set on creation, and cannot be updated.
     */
    public final double distance;

    private String _toString;

    public AirportDistance(Airport airport, double distance) {
      this.airport = airport;
      this.distance = distance;
    }

    public int compareTo(AirportDistance o) {
      int sign = (int)(Math.signum(this.distance - o.distance));
      return sign != 0 ? sign : this.airport.icao.compareTo(o.airport.icao);
    }

    public String toString() {
      if (this._toString == null) {
        StringBuilder sb  = new StringBuilder(45);
        sb.append(airport.toString());
        sb.append(" - ");
        sb.append(distance);
        this._toString = sb.toString();
      }

      return this._toString;
    }
  }

  private final String _dbFilename;
  private final int _distanceMetric;

  /**
   * @param dbFilename Path to SQLite airport database.
   */
  public AirportDatabase(String dbFilename) {
    this(dbFilename, 0);
  }

  /**
   * @param dbFilename {@link #AirportDatabase(String)}
   * @param distanceMetric Distance metric to use (EXPERIMENTAL). See 
   * {@link #ComputeDistance(LatLng, int, int)}
   */
  public AirportDatabase(String dbFilename, int distanceMetric) {
    this._dbFilename = dbFilename;
    this._distanceMetric = distanceMetric;
  }


  private Connection getDbConnection() throws SQLException {
    try {
      Class.forName("org.sqlite.JDBC");
      return DriverManager.getConnection("jdbc:sqlite:"+this._dbFilename);
    } catch (ClassNotFoundException cnfex) {
      throw new SQLException("Could not load sqlite driver.");
    }
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


    TreeSet<AirportIdDistance> nearestAirportIDs = 
        new TreeSet<AirportIdDistance>();

    int count = 0;
    double maxDistance = Double.POSITIVE_INFINITY;

    Connection dbConn = this.getDbConnection();
    Statement dbStmt = dbConn.createStatement();
    ResultSet rs = dbStmt.executeQuery("SELECT id, lat, lng FROM airports");
    while (rs.next()) {
      int id = rs.getInt(1);
      int lat = rs.getInt(2);
      int lng = rs.getInt(3);
      double distance = ComputeDistance(position, lat,lng);


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
    dbConn.close();

    return nearestAirports;

  }

  /** Find the airports within {@code radius} from {@code position}.
   * <p>Currently does a naive linear scan over all (~14'000) airports.</p>
   * @return Airports within radius, sorted by increasing distance.
   */
  public AirportDistance[] getAirportsWithinRadius(LatLng position,
                                                   double radius)
    throws SQLException {

    TreeSet<AirportDistance> airportsWithinRadius =
        new TreeSet<AirportDistance>();

    Connection dbConn = this.getDbConnection();
    PreparedStatement airportFromIdStmt = dbConn.prepareStatement(
      "SELECT icao, name FROM airports WHERE id = ?");

    Statement dbStmt = dbConn.createStatement();

    ResultSet rs = dbStmt.executeQuery("SELECT id, lat, lng FROM airports");
    while (rs.next()) {
      int id = rs.getInt(1);
      int lat = rs.getInt(2);
      int lng = rs.getInt(3);
      double distance = ComputeDistance(position, lat,lng);

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
    dbConn.close();
    AirportDistance[] airportsWithinRadiusArray =
        new AirportDistance[airportsWithinRadius.size()];

    return airportsWithinRadius.toArray(airportsWithinRadiusArray);

  }

  private static double ComputePseudoEuclidianDistance(LatLng origin, int lat, int lng) {
    long latDiff = (origin.lat - lat)%180000000;
    long lngDiff = (origin.lng - lng)%180000000;
    return  latDiff * latDiff + lngDiff*lngDiff;
  }

  private static double ComputeSphericalLawOfCosinesDistance(
      LatLng origin, int lat, int lng) {
    final double lat1 = origin.latRad();
    final double lng1 = origin.lngRad();
    final double lat2 = Math.toRadians(lat*1e-6);
    final double lng2 = Math.toRadians(lng*1e-6);

    final double R = 6371.009/1.852;

    return Math.acos(Math.sin(lat1)*Math.sin(lat2) +
                     Math.cos(lat1)*Math.cos(lat2) *
                     Math.cos(lng2-lng1)) * R;
  }

  private static double ComputeHaversineDistance(LatLng origin,int lat,int lng){
    final double lat1 = origin.latRad();
    final double lng1 = origin.lngRad();
    final double lat2 = Math.toRadians(lat*1e-6);
    final double lng2 = Math.toRadians(lng*1e-6);

    final double R = 6371.009/1.852;

    final double dLat = (lat2-lat1);
    final double dLng = (lng2-lng1);
    final double a = Math.pow(Math.sin(dLat/2),2) +
                     Math.cos(lat1) * Math.cos(lat2) *
                     Math.pow(Math.sin(dLng/2),2);
    final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    return R * c;
  }

  /** Compute distance between two points on Earth (in nautical miles).
   *<p>
   * Several distance metrics are available.  The one used can be chosen by
   * giving the appropriate value to the
   * {@linkplain #AirportDatabase(String, int) experimental constructor}.
   *</p>
   *<p>
   * Possible values:
   * <dl>
   * <dt>0 (default)</dt>
   * <dd>{@link
   * <a href="http://en.wikipedia.org/wiki/Haversine_formula">
   * Haversine distance</a>}
   * </dd>
   *
   * <dt>1</dt>
   * <dd>{@link
   * <a href="http://en.wikipedia.org/wiki/Great_circle_distance">
   * Spherical Law of Cosines distance</a>}
   * </dd>
   *
   * <dt>2</dt>
   * <dd>Pseudo-Euclidian distance in lat/lng space.  This value has
   * <b>no physical meaning whatsoever</b> and should only be used for
   * experimental reasons.
   * </dd>
   * </dl>
   * @return Distance between origin and given lat/lng, in nautical miles.
   */
  public double ComputeDistance(LatLng origin, int lat, int lng) {
    switch (this._distanceMetric) {
      case 2:
        return  ComputePseudoEuclidianDistance(origin, lat, lng);
      case 1:
        return  ComputeSphericalLawOfCosinesDistance(origin, lat, lng);
      case 0:
      default:
        return  ComputeHaversineDistance(origin, lat, lng);
    }
  }
  /** Test nearest / within radius airport function.
   * <p>
   * Usage:
   * {@code java com.google.blackbox.AirportDatabase
   * <db> <lat> <long> <numAirports (int) | radius (float)> [metric] [num trials]}
   * </p>
   */
  public static void main(String args[]) {
    if (args.length < 4) {
      System.err.println("Usage java AirportDatabase <db> <lat> <long> <numAirports (int) | radius (float)> [metric] [num trials]");
      System.exit(1);
    }
    String dbFilename = args[0];
    int lat = Integer.parseInt(args[1]);
    int lng = Integer.parseInt(args[2]);
    int numAirports = 0;
    double radius = Double.NaN;

    try {
      numAirports = Integer.parseInt(args[3]);
    } catch (NumberFormatException nfex) {
      radius = Double.parseDouble(args[3]);
    }

    int distanceMetric = args.length > 4 ? Integer.parseInt(args[4]) : 0;
    int numTrials = args.length > 5 ? Integer.parseInt(args[5]) : 1;

    LatLng position = new LatLng(lat, lng);

    while (numTrials-- > 0) {
      AirportDatabase airDB = new AirportDatabase(dbFilename, distanceMetric);

      try {
        AirportDistance[] nearestAirports =
            Double.isNaN(radius) ?
            airDB.getNearestAirports(position, numAirports) :
            airDB.getAirportsWithinRadius(position, radius);

        for (AirportDistance nearestAirport: nearestAirports) {
          System.out.println(nearestAirport.toString());
        }
      } catch (SQLException sqlex) {
        sqlex.printStackTrace();
      }
    }
  }
}

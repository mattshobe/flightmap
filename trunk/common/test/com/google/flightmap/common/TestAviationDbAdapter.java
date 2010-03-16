package com.google.flightmap.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;

import com.google.flightmap.common.data.Airport;

public class TestAviationDbAdapter implements AviationDbAdapter {
  private Connection dbConnection;
  private final static String AIRPORT_DB_FILENAME = "aviation.db";

  public TestAviationDbAdapter() throws ClassNotFoundException, SQLException {
    Class.forName("org.sqlite.JDBC");
    dbConnection = DriverManager.getConnection("jdbc:sqlite:" + AIRPORT_DB_FILENAME);
  }
  @Override
  public void open() {
    // TODO Auto-generated method stub

  }

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

  @Override
  public Airport getAirport(int id) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LinkedList<Airport> getAirportsInCells(int startCell, int endCell) {
    // TODO Auto-generated method stub
    return null;
  }

}

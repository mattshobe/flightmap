package com.google.blackbox;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

public class ManageLocation extends Service implements LocationListener {
  
  private LocationManager locationManager = null;
  private double myAltitude = 0;
  private double myLat = 0;
  private double myLng = 0;
  private double mySpeed = 0;
  private float myBearing = 0;
  private Location lastLocation = null;
  protected final long MIN_DIST_DIFF = 15/3; // approximately 5 feet
  protected final long MIN_TIME_DIFF = 5000; // 5 seconds
  protected List<Location> myPath = new ArrayList<Location>();
  private long time = 0; 
  private boolean isAlerting = false;
  private boolean isRecording  = false;
  private long startTime = 0;
  private FileOutputStream fs = null;

    @Override
    public void onCreate() {
      super.onCreate();
      setForeground(true);
      try {
      locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
      lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_DIFF, MIN_DIST_DIFF, this);

      Notification notification = new Notification(R.drawable.icon, null, System.currentTimeMillis());
      isRecording = true;
      startTime = System.currentTimeMillis();
      isAlerting = true;
      
      File appDir = new File("/sdcard/blackbox");
      if(!appDir.exists()){
        appDir.mkdirs();
      }
      try {
        fs = new FileOutputStream(appDir.getPath() + "/myPath.txt");
      } catch (FileNotFoundException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    
    
    }  
  
  /*
   * Returns a Location containing Longitude, Latitude, Altitude in meters
   * Time, Speed in m/s & Bearing also if available but may be null
   */
  public Location getLocation()
  {
    return lastLocation;
      
  }
  
  public int getSatelliteInfo()
  {
    // TODO: return number of satellites used to derive latest fix
    return lastLocation.getExtras().getInt("satellites");
  }
  
  public void recordLocation()
  {
    // TODO write out lastLocation to file or memory
  }

public void startLocationListener() {
  
  locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
  locationManager.requestLocationUpdates("gps", MIN_TIME_DIFF, MIN_DIST_DIFF, ManageLocation.this);
  
}

@Override
public void onDestroy() {
  isRecording = false;
  isAlerting = false;
  try {
    fs.flush();
  } catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
  try {
    fs.close();
  } catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
  locationManager.removeUpdates(this);
  super.onDestroy();
}

@Override
public void onLocationChanged(Location location) {
  lastLocation = locationManager.getLastKnownLocation("gps");
  
  myAltitude = lastLocation.getAltitude() * 3.2808;
  
  myLat = lastLocation.getLatitude();
  myLng = lastLocation.getLongitude();
  mySpeed = lastLocation.getSpeed() * 2.2369;
  time = lastLocation.getTime();
  myBearing = lastLocation.getBearing();
  
  
  String myStats = "TIME: " + time + "\tALT: " + myAltitude + "\tLAT: " +
  myLat + "\tLONG: " + myLng + "\tSPD: " + mySpeed + "\tHDING: " + 
  myBearing + "\n";
  
  if(isAlerting)
    Toast.makeText(this, myStats, Toast.LENGTH_LONG).show();
  try {
    fs.write(myStats.getBytes());
  } catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
  
  if((System.currentTimeMillis() - startTime) > 500000)
    this.onDestroy();
}

@Override
public void onProviderDisabled(String provider) {
  // TODO Auto-generated method stub
  
}

@Override
public void onProviderEnabled(String provider) {
  // TODO Auto-generated method stub
  
}

@Override
public void onStatusChanged(String provider, int status, Bundle extras) {
  // TODO Auto-generated method stub
  
}

@Override
public IBinder onBind(Intent intent) {
  // TODO Auto-generated method stub
  return null;
}
}
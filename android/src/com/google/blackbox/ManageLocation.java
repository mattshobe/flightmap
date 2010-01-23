package com.google.blackbox;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Notification;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

public class ManageLocation extends Activity implements LocationListener {
    /** Called when the activity is first created. */
  
  private LocationManager locationManager = null;
  private Context context;
  private double myAltitude = 0;
  private double myLat = 0;
  private double myLng = 0;
  private double mySpeed = 0;
  private float myBearing = 0;
  private Location lastLocation = null;
  protected final long MIN_DIST_DIFF = 15/3; // approximately 5 feet
  protected final long MIN_TIME_DIFF = 5000; // milliseconds
  protected List<Location> myPath = new ArrayList<Location>();
  private long time = 0; 
  private boolean isAlerting = false;
  private boolean isRecording  = false;
  private long startTime = 0;

  
  public ManageLocation(Context context){
    this.context = context;
  
  /** Called when the activity is first created. */
/*  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
     setContentView(R.layout.main); */

    locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_DIFF, MIN_DIST_DIFF, this);

    Notification notification = new Notification(R.drawable.icon, null, System.currentTimeMillis());
    isRecording = true;
    startTime = System.currentTimeMillis();
    isAlerting = true;
    startLocationListener();
    

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
public void onResume() {
  super.onResume();
  isAlerting = true;
}

@Override
public void onPause() {
  super.onPause();
  isAlerting = false;
}

@Override
public void onDestroy() {
  isRecording = false;
  isAlerting = false;
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
  
  
  String myStats = "ALT: " + myAltitude + "\nLAT: " + myLat + "\nLONG: " + 
  myLng + "\nSPD: " + mySpeed + "\nHDING: " + myBearing + "\nTIME: " + time;
  
  if(isAlerting)
    Toast.makeText(ManageLocation.this, myStats, Toast.LENGTH_LONG).show();
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
}
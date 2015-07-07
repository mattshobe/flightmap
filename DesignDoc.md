

# Objective #
Flight Map is a moving map Android application for pilots. It's meant to be a demonstration of what's possible with the Android SDK. Since we want it to work in the air, the application must work in airplane mode.

We use public-domain data from the [FAA](http://www.faa.gov).

No user data is collected or sent from the phone to anywhere else.

# Overview #
There are 3 top-level packages to Flight Map: preprocessing, Android, and common.

The preprocessing code takes data from the FAA and produces data that's been parsed and suitable to deliver to the phone. This code is currently manually run to produce the files in the `data` directory. In future, we may be able to support other sources of data (e.g. from other countries).

The Android code is what actually runs on the phone. The main component is a moving map that shows the airports, airspace and other aviation data near the current location. This is very similar to Google Maps, except it's just for aviation data. It's also possible to get details on an airport and search the data on the phone.

The common code is shared by both the preprocessing and Android components.

# Detailed Design #
## Preprocessing Design ##
The preprocessing code generates a [SQLite](http://sqlite.org) database that's checked into subversion as `data/aviation.db`.

The AviationMasterRecordParser reads information about airports from the [FAA Aviation Master Record](http://www.faa.gov/airports/airport_safety/airportdata_5010/), normalizes it and stores it in the aviation.db.

Communications data comes from the [National Flight Data Center](http://nfdc.faa.gov/) (NFDC) in the NASR (National Airspace System Resources) as a set of text files. That data is parsed by the CommParser class and stored in aviation.db.

Airspace data also comes from the NFDC and is parsed by the NfdAirspaceParser class. Airspace is described in a vector format and is stored in the aviation.db in a way that makes it easy to query and display.

The aviation database has an expiration date and schema version. When the Android application launches, it checks its local copy of the database and downloads a new one if it's expired. The schema version an integer that is incremented when the database is changed in a way that's incompatible with old versions of the application. The Android application has a constant specifying which database schema version it's expecting to use, and if that doesn't match what's stored locally, the database is downloaded via http.

## Android Design ##
### Android SDK Level ###
For our first release, we've set the minimum Android SDK version to 7 (Android 2.1 / Eclair). The target SDK is set to version 8 (Android 2.2 / Froyo) solely to allow installation to the SD card for users running Android 2.2 and higher.

We do not currently use any SDK version 8 functionality.

### Activity Overview ###
There are 4 activities in the application: MainActivity, SearchActivity, TapcardActivity and UserPrefsActivity. The MainActivity (surprisingly) is the one that's launched when you start Flight Map. The main activity has 4 tasks:
  1. Show the legal disclaimer.
  1. Download a database update if needed (as described above in the Preprocessing Design section.
  1. Run the main event loop which updates the map.
  1. Handle Android lifecycle events such as pausing and resuming.

SearchActivity is launched when you press the "search" button on your phone. It lets you search for airports by name, identifier or city name. It closely follows the examples on the Android developer page regarding [search](http://developer.android.com/guide/topics/search/index.html).

TapcardActivity is what displays details about an airport when you tap on the airport symbol on the map. It's also launched when you search for an airport and tap on it. In addition to showing airport details, it displays a small strip of navigation data showing a pointer to the airport from your current position, your distance and estimated time enroute (if moving).

The UserPrefsActivity handles the UI for changing user preferences. The API to the user preferences is in the UserPrefs class which has accessor methods.

### Moving Map ###
The moving map classes are in the `map` subpackage. We use the Model-View-Presenter pattern.

MapView shows the moving map and handles simple UI actions. It communicates with the model and presenter as needed for anything that's not a simple UI action. The `drawMap()` method is called by the event loop that MainActivity runs.

The MapPresenter communicates with the MapView and handles complex UI actions, such as panning or launching the TapcardActivity when the user taps on the map. It also determines what the current screen area is and handles the database queries to fetch the airports and airspace in view. The MapPresenter handles the somewhat tricky canvas transformations to get everything drawn in the right place. As needed, it tells the MapView when to draw certain things, such as the top panel with navigation information.

### Tapcard ###
This is the activity that is launched when you tap on an airport in the map or search results. It shows information about the airport such as runways and communications frequencies. As small navigation display is also shown that gives a graphical pointer, magnetic bearing, distance and estimated time enroute to that airport from your current location. The estimated time enroute is omitted when you are not moving.

### Location Handling ###
During initial development, we found that the speed reported by the Android GPS was sometimes inaccurate (e.g. you'd see a speed of 250 knots when walking). Also the speed was always wrong when climbing or descending. The LocationHandler class ignores the speed reported by the Android GPS and just calculates it using successive positions. This class also smooths out the bearing reported by the GPS.

### Location Simulator ###
To help with testing, we developed an in-application location simulator. This lets you set the speed, track and altitude of the simulated position and see how the application operates. When the simulator mode is active, the moving map shows a prominent message that the position is simulated.

## Overview of Common classes ##
The `common` project has code that's shared by the Android code and the preprocessing code. The latter does not run on the phone, so the common code cannot use any Android APIs.

The `common/data` directory has classes such as Airport, Airspace, and Runway that hold details about those entities. The LatLng and LatLngRect classes perform operations on either a single latitude/longitude point or a rectangle defined by the southwest and northeast corners.

In `common/db` are classes that provide the interface to the database of airport and aviation information. Database entries are stored using a spatial index and the [documentation for CustomGridUtil](http://flightmap.googlecode.com/svn/trunk/common/javadoc/com/google/flightmap/common/db/CustomGridUtil.html) goes into detail on how that works. This spatial indexing makes it possible to quickly find entities within a geographic area.

Classes that provide geographic utilities such as navigation computation, magnetic variation calculation and Mercator projection are in `common/geo`.

Finally, the common code includes classes that retrieve database updates via http. Those can be found in `common/io` and `common/net`.

# Privacy #
No user data is sent from the application to anywhere.
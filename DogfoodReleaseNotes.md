
# Flight Map - Google Dogfood Edition - Release Notes #

## Intro ##
Flight Map is an Android application created by Googler pilots who wanted to build a fun, simple-to-use aid to navigation and fair-weather pilotage. It is in its absolute earliest stages of development/feature maturity, but it's ready for pilots and other interested parties to dogfood in flight.

Flight Map offers these features:
  * Works in **Airplane Mode**. All the data is kept locally on your phone.
  * Current track, speed, and altitude
  * GPS-plotted position on a moving, zoom-able map with nearby airport display
  * Detailed airport/facility information available by tapping on airports shown on screen
  * Pilot-controlled settings for north/track up orientation, facility types to display
  * Automatic facility database download and updating (data currently provided from official FAA sources; others are being considered)

We plan to add the following features in future releases:
  * Record and save in-flight GPS tracks using the Android MyTracks application, if installed
  * Terrain, road, and water detail layers in 2D mode
  * Separate Airport/Facility database browse and search
…and the big one: a Sky Map-like augmented reality view through the device camera that can help reveal airports. Here is a [conceptual screenshot](https://sites.google.com/a/google.com/blackbox/home/user-experience).

## Find bugs? Awesome. Tell us everything. ##
Please file a bug using our official [issues list](http://code.google.com/p/flightmap/issues/list) on the Google code site. (There are multiple bugs filed already – be sure to double-check for duplicates).

Also, you should consider joining the Flight Map [mailing list](http://g/flightmap). The development team will monitor this list for discussion and provide updates on development plans and launch timetables through it.

# Coming Soon #
  * Navaids
  * AWOS/ASOS frequencies
  * State Boundaries.

# Release: v0.15.0 [Cardinal](http://en.wikipedia.org/wiki/Cessna_177) - 2/14/11 #
  * New features:
    * Alert user when GPS signal lost.
      * Text at the top of the screen replaces track, altitude, speed. Show time since last GPS update.
      * Airplane icon is shown hollowed out with a slash through it. Text indicates last known position.
    * Alert user when GPS accuracy is low.
      * Text at the top of the screen replaces track, altitude, speed. Show accuracy distance.
      * Map does continue to update.
    * Simulator can emulate the above two conditions.
      * There are two new buttons at the bottom of the simulator control dialog. The first toggles between "No GPS" and "Restore GPS". The second toggles between "Low Accuracy" and "Normal Accuracy".
  * Improvements
    * Map does not immediately pan when you begin the two-finger scale gesture.
    * Slightly larger pan reset button with airplane icon (to match original UI mocks).

# Release: v0.13.0 [Dufaux 4](http://en.wikipedia.org/wiki/Dufaux_4) - 1/30/11 #
  * New features:
    * Controlled airspace contours are now displayed.  [Issue 80](https://code.google.com/p/flightmap/issues/detail?id=80).
      * Airspace more than 500 feet from your current altitude is dimmed.
  * Improvements:
    * Airport communication frequencies are better organized:
      * Merged tower and CTAF frequencies when possible.  [Issue 135](https://code.google.com/p/flightmap/issues/detail?id=135).
      * Removed trailing zeroes from CTAF frequencies.  [Issue 134](https://code.google.com/p/flightmap/issues/detail?id=134).
  * Bug fixes:
    * Fixed null pointer exception if the user selected an airport without communication frequencies.  [Issue 146](https://code.google.com/p/flightmap/issues/detail?id=146).

# Release: v0.11.1 [Mooney](http://en.wikipedia.org/wiki/Mooney_Airplane_Company) - 1/14/11 #
  * Improvements:
    * Better pinch-to-zoom behavior. If you pan your fingers while pinching, the map pans.
    * Preserve pan state and panned-to map location when app is restored (such as when changing orientation).
  * Bug fixes:
    * Fixed null pointer exception if the user panned the map when there was no current location.
# Release: v0.11.0 [Mooney](http://en.wikipedia.org/wiki/Mooney_Airplane_Company) - 12/20/10 #
  * New features:
    * Can install to SD card (requires Android 2.2).
    * Pan the map by dragging.
    * Pinch to zoom in or out.
    * **Much** improved airport communication frequency information.
    * Better organization of search results.
    * Local search history.

# Release: v0.9.0 [Citabria](http://en.wikipedia.org/wiki/Citabria) - 10/15/10 #
  * New features:
    * Search by airport code, city name or airport name.
      * Known issues with search:
        1. Performance can be slow when there are lots of matches.
        1. Results are not sorted in a useful way.
        1. User preferences (e.g. hide heliports) are not applied to the search results.
    * Simulator mode. Useful for testing, but can also help you explore how the app works on the ground.
      * To use the simulator:
        1. Enable the simulator preference (Menu > Prefs > Enable Simulator). You only have to do this once. We chose to hide the simulator by default.
        1. Bring up the simulator control dialog (Menu > Sim). Check the "Activate simulator" box to start simulating flight.
        1. Set the desired speed, track and altitude and the airplane will move. A flashing "SIMULATOR" indicator will appear on the map to remind you that you're in simulator mode.
    * Larger icons and text on the map screen.
      * We're still experimenting with how the map should look, so please give us your feedback on this.
    * Faster map updates.
      * New code can interrupt a slow database query that is no longer needed. [Issue 95](https://code.google.com/p/flightmap/issues/detail?id=95).
    * Rounded scale text.  [Issue 120](https://code.google.com/p/flightmap/issues/detail?id=120).
  * Bug fixes
    * As zoom in, scale icon gets shorter, but continues to display "1nm".  [Issue 100](https://code.google.com/p/flightmap/issues/detail?id=100).

# Release: v0.7.2 - 10/8/10 #
  * Bug fixes
    * Speed sometimes jumps to 250+ knots (when going much slower than that).  [Issue 10](https://code.google.com/p/flightmap/issues/detail?id=10).
    * Time to airport in tapcard needs to provide a more general estimate. [Issue 97](https://code.google.com/p/flightmap/issues/detail?id=97).
    * In tapcard, last digits of frequencies are hidden. [Issue 90](https://code.google.com/p/flightmap/issues/detail?id=90).
    * In tapcard, comm frequencies are too close to station identifier. [Issue 109](https://code.google.com/p/flightmap/issues/detail?id=109).

# Release: v0.7.1 - 9/30/10 #
  * Bug fixes
    * Database was getting corrupted during download.  [Issue 101](https://code.google.com/p/flightmap/issues/detail?id=101).
    * When zooming in/out, previously displayed airports were not filtered correctly.  [Issue 108](https://code.google.com/p/flightmap/issues/detail?id=108).

# Release: v0.7.0 - 9/24/10 #
  * Bug fixes
    * Screen stays on the airport details page. It was already kept on on the map page.
      * General decision: any page that updates automatically with map or navigation data will keep the screen on.
    * Map incorrectly rotated by magnetic variation amount. [Issue 89](https://code.google.com/p/flightmap/issues/detail?id=89).
    * Show integer scale values and make zoom scale more visible. [Issue 77](https://code.google.com/p/flightmap/issues/detail?id=77).
    * Better visibility of text on the airport details page. [Issue 75](https://code.google.com/p/flightmap/issues/detail?id=75).
    * Move zoom scale control so it doesn't overlap with zoom scale text. [Issue 45](https://code.google.com/p/flightmap/issues/detail?id=45).
  * New functionality
    * Database query to fetch airports in view is on a separate thread.
      * After changing zoom levels, airports may appear or disappear based on their ranking.
      * Currently no indication to user of areas of the screen with database queries in progress.
    * Search by airport code (later name, city, etc.)
    * Aviation database is only updated on schema changes or if data is past its expiration date.
      * Database downloads cannot be interrupted by hitting the "back" button.

# Release: v0.5.1 - 9/9/10 #
  * Bug fixes
    * Solves reported issue:  "Database update failed" error dialog would appear even though download completed successfully.

# Release: v0.5.0 - 9/2/10 #
Initial dogfood release.
  * Known issues
    * Communication frequency data for airports is widely incomplete/inconsistent. This is our #1 motivator for a better data source.
    * GPS-derived altitude reads 100 feet low on Nexus One (this inaccuracy may occur on other hardware, too).
    * GPS-derived ground speed is sometimes very wrong, especially when changing altitude.
    * Facility database download continues in the background even if the application is closed prior to completion.
    * Map view when standing still or moving slowly (e.g., walking or taxiing very slowly) is a bit erratic. The view may shift position unexpectedly until you are moving at a more consistent speed.
# Introduction #

Each release has a minimum set of tests that should be conducted either manually or through automation to verify the core functionality is intact and verify new features and bug fixes work as expected.

# Details #

**Core Functionality To Verify (Manually for now)**

**Clean install**

DISCLAIMER:
  * Disclaimer shows up and can be scrolled.
  * Accept button

DATABASE:
  * New database is downloaded over wifi and 3G.


**Update**

DATABASE:
  * New database is _not_ downloaded if existing DB is still valid.
  * New database is downloaded if existing DB is corrupted.
  * New database is downloaded if existing DB has outdated schema.

**Functionality**
  * Verify both landscape and portrait views for all behavior.

MOVING MAP:
  * Check airports in known locations.
    * Verify speed, heading and altitude are correctly displayed.
    * Use emulator to check multiple locations.
    * Simulator for airspeed/distance checks.
  * Verify changing preferences hides/shows appropriate airports without changing zoom level.
    * North Up.
    * Distance Units
    * Show Seaplane Bases
    * Show Military Bases
    * Show Soft Fields
    * Show Private Airports
    * Show Heliports
    * Minimum Runway Length
  * Zoom in/out and verify zoom scale and new airports in range populate quickly.

TAPCARD:
  * Check known airport and verify Comm frequencies are displayed and visible in landscape and portrate views.
  * Check that Runway information is shown and correct.
  * Check that tapcard works in both north up and track up preferences settings.

SEARCH:
  * Search for airport by icao with and without preceding K.
  * Search for 'san' and verify results
  * Search for 'san jose' and verify results
  * Search for nonexistent icao and airport and verify no results and no crash.

SIMULATOR:
  * Verify it can be hidden in preferences.
    * Default is 'hidden' in clean install.
  * Increase/decrease speed, heading, altitude.
  * Verify behavior of 'stop' button.
  * Verify enable/disable.
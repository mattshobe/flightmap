# Introduction #

This iteration is to just show the list of the 10 nearest airports. Much of the work is in getting our feet with with Android development, Ant build files, etc.

# Current Status (updated 1/29/10) #

Aris created the coding in `parsing` that parses FAA data into an SQLite database. That database is checked into version control (for now) at `data/aviation.db`.

[r54](https://code.google.com/p/flightmap/source/detail?r=54) has the UI code to read the database and show the list of 10 nearest airports. Currently the location is hard coded, and Bonnie is working on getting the location stuff wired up.

## Getting the SQLite database onto the phone ##
Normally an application's data is stored in the /data/data/<package.name>/databases directory which is in the limited built-in memory and constrained to the application's data space, not on the SD card. The database containing just the airport code, name and latlng is 948 KB which is probably pretty big already for just application data. Once we start adding airspace and terrain data, our space needs are going to grow dramatically. Phil looked into just copying the `airports.db` file to the SD card, and found a few helpful posts on the topic. See [here](https://groups.google.com/group/android-developers/browse_thread/thread/f8a030cf8841b111) and [here](https://groups.google.com/group/android-developers/browse_thread/thread/87facd5ed49ac4ce).

## Fun with emulated SD cards ##
Phil jumped through a few hoops trying to get an emulated SD card to work. Quick notes: creating a 4MB card gives you an error that it's too small, 8MB crashes the emulator, and 16MB appears to work.

Here's what Phil did to get an emulated SD card created in his emulator named `droid`. This has a 400x800 resolution (WVGA800) which matches the Droid and Nexus One.
```
$ android list targets
Available Android targets:
id: 1 or "android-4"
     Name: Android 1.6
     Type: Platform
     API level: 4
     Revision: 1
     Skins: HVGA (default), QVGA, WVGA800, WVGA854
id: 2 or "android-5"
     Name: Android 2.0
     Type: Platform
     API level: 5
     Revision: 1
     Skins: HVGA (default), QVGA, WQVGA400, WQVGA432, WVGA800, WVGA854
id: 3 or "android-6"
     Name: Android 2.0.1
     Type: Platform
     API level: 6
     Revision: 1
     Skins: HVGA (default), QVGA, WQVGA400, WQVGA432, WVGA800, WVGA854
id: 4 or "Google Inc.:Google APIs:6"
     Name: Google APIs
     Type: Add-On
     Vendor: Google Inc.
     Revision: 1
     Description: Android + Google APIs
     Based on Android 2.0.1 (API level 6)
     Libraries:
      * com.google.android.maps (maps.jar)
          API for Google Maps
     Skins: WVGA854, WQVGA400, HVGA (default), WQVGA432, WVGA800, QVGA

$ android create avd --name droid --target 3 --skin WVGA800 --sdcard 16M
Android 2.0.1 is a basic Android platform.
Do you wish to create a custom hardware profile [no]
Created AVD 'droid' based on Android 2.0.1, with the following hardware config:
hw.lcd.density=240
vm.heapSize=24
```

After doing the above, the `droid` Android Virtual Device will have a 16 MB SD card available when it's started. Now to push the `airports.db` file to the card, do this.

```
$ emulator -avd droid &  # Start the android emulator
$ cd ~/src/blackbox
$ adb push data/aviation.db /sdcard/com.google.blackbox/aviation.db
238 KB/s (722944 bytes in 2.958s)
$ adb shell
# cd sdcard
# ls -l
d---rwxr-x system   sdcard_rw          2010-01-21 14:14 LOST.DIR
d---rwxr-x system   sdcard_rw          2010-01-21 14:14 com.google.blackbox
# cd com.google.blackbox/
# ls -l
----rwxr-x system   sdcard_rw   722944 2010-01-21 13:49 aviation.db
```
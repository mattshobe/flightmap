# Introduction #

This documents tracks _significant differences_ between the code of released versions (in tags/xxxx-release-x.x.x) and the corresponding code in trunk/ at the time of release.

Significant differences are changes that are not reflected in trunk, such as quick fixes (hacks) needed for a particular release, etc.  Cherrypicking a revision from trunk to a release branch usually does **not** constitute such a difference: the change is already in trunk.

Notation:
  * **Release x (rY -> rZ)**: Revision Y was tagged as release x in revision Z.

## Release 0.15.0 (trunk [r555](https://code.google.com/p/flightmap/source/detail?r=555) -> branch [r556](https://code.google.com/p/flightmap/source/detail?r=556) -> [r560](https://code.google.com/p/flightmap/source/detail?r=560)) ##
  * No changes from trunk.

## Release 0.13.0 (trunk [r521](https://code.google.com/p/flightmap/source/detail?r=521) -> branch [r522](https://code.google.com/p/flightmap/source/detail?r=522) -> [r524](https://code.google.com/p/flightmap/source/detail?r=524)) ##
  * No changes from trunk.

## Release 0.11.1 (trunk [r481](https://code.google.com/p/flightmap/source/detail?r=481) -> branch [r486](https://code.google.com/p/flightmap/source/detail?r=486) -> [r488](https://code.google.com/p/flightmap/source/detail?r=488)) ##
  * Cherry picked [r481](https://code.google.com/p/flightmap/source/detail?r=481) to get improved panning.
  * The above rev also fixed a NPE if you panned w/o a current location.

## Release 0.11.0 (trunk [r461](https://code.google.com/p/flightmap/source/detail?r=461) -> branch [r463](https://code.google.com/p/flightmap/source/detail?r=463) -> [r464](https://code.google.com/p/flightmap/source/detail?r=464)) ##
  * No changes from trunk.

## Release 0.9.0 (trunk [r416](https://code.google.com/p/flightmap/source/detail?r=416) -> branch [r419](https://code.google.com/p/flightmap/source/detail?r=419) -> [r420](https://code.google.com/p/flightmap/source/detail?r=420)) ##
  * No changes from trunk.

## Release 0.7.2 (trunk [r377](https://code.google.com/p/flightmap/source/detail?r=377) -> branch [r393](https://code.google.com/p/flightmap/source/detail?r=393) -> [r394](https://code.google.com/p/flightmap/source/detail?r=394)) ##
  * Disabled simulator menu (see [r367](https://code.google.com/p/flightmap/source/detail?r=367), [r393](https://code.google.com/p/flightmap/source/detail?r=393))

## Release 0.7.1 ([r359](https://code.google.com/p/flightmap/source/detail?r=359) -> [r360](https://code.google.com/p/flightmap/source/detail?r=360)) ##
  * Same as 0.7.0

## Release 0.7.0 ([r344](https://code.google.com/p/flightmap/source/detail?r=344) -> [r345](https://code.google.com/p/flightmap/source/detail?r=345)) ##
  * Removed simulator code ([r336](https://code.google.com/p/flightmap/source/detail?r=336))

## Release 0.5.1 ([r293](https://code.google.com/p/flightmap/source/detail?r=293) -> [r294](https://code.google.com/p/flightmap/source/detail?r=294)) ##
  * Improved database download hack: Hide exceptions raised by showing dialog when application is closed. ([r292](https://code.google.com/p/flightmap/source/detail?r=292), [r293](https://code.google.com/p/flightmap/source/detail?r=293))

## Release 0.5 ([r268](https://code.google.com/p/flightmap/source/detail?r=268) -> [r270](https://code.google.com/p/flightmap/source/detail?r=270)) ##
  * Remove keypad heading ([r249](https://code.google.com/p/flightmap/source/detail?r=249), done in trunk/ in [r274](https://code.google.com/p/flightmap/source/detail?r=274))
  * Database download hack: do not show dialog while checking for update, hide errors ([r266](https://code.google.com/p/flightmap/source/detail?r=266))
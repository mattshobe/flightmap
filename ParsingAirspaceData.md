


# Introduction #

Parsing airspace data is a non trivial process, that requires balancing the following requirements:
  * understanding and parsing data from authoritative sources
  * storing it in a way that can be efficiently retrieved
  * minimizing required disk space.
  * allow fast drawing (UI performance)


# Data sources #

## FAA NASR ##

The FAA National Airspace System Resources offers a free, authoritative, subscription based 56-day cycle database.

It requires registration on https://nfdc.faa.gov (which, apparently, requires a manual validation that can take several days).

Airspace data is provided in [shapefiles](http://en.wikipedia.org/wiki/Shapefile) as plain polygons.  The representation is far from compact: all segments (lines, arcs) are sampled at a pretty high resolution.  Even dropping half the points increases the aviation database size to approximately 20 Mb (without compression).

Along with their shape, airspaces have the following information:
  * name
  * low altitude (floor)
  * high altitude (ceiling)
  * bounding box

## FAA NFD ##

The National Flight Database is a non-free subscription based, authoritative 28-day cycle database produced by the FAA and delivered by CD or E-Commerce.

Its format follows the [ARINC 424-18](http://en.wikipedia.org/wiki/ARINC_424) specifications, which defines fixed-length records of 132 characters.

Records are classified in sections and subsections.  Controlled airspace records belong to section **U**, subsection **C**.

Airspace are split in areas (Area A, ...) and each area is defined by consecutive points and one of the following ways of joining them:
  * Rhumb line
  * Great circle
  * Arc (clockwise/counter-clockwise)

Along with their shape, airspace areas have the following information:
  * name
  * low altitude (floor)
  * high altitude (ceiling)
  * center (i.e. corresponding airport)

Because of how compact this representation is, this is currently the data source we are considering using.


# Data extraction #

  1. Filter records: section U, subsection C.
  1. Treat each area independently, in order given by sequence number.

## Dealing with arcs ##
  * Input from NFD
    * Center coordinates [[lat/lng]]
    * Radius [[nm](nm.md)]
    * Initial bearing [[degrees](degrees.md)]
    * Initial coordinates [[lat/lng]]

  * Input needed by `android.graphics`
    * Bounding box [[RectF](RectF.md)]
    * Start angle [[degrees](degrees.md)]
    * Swep angle [[degrees, clockwise]]

### Bounding box ###
  1. Initialize `LatLngRect`
  1. Extend it by adding all single points.
  1. Handle arcs:
    1. Compute start, sweep angles.
    1. If cardinal directions (N, E, S, W) are included in [[start, start + sweep]], then add the corresponding point.

### Start angle ###
Should be equal to the initial bearing provided in the record.  _Should probably recompute and check value (with a margin of error.)_

### Sweep angle ###
Compute angle to next point, measure sweep angle by taking direction (clockwise/counter-clockwise) into account.

/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.flightmap.parsing.faa.nfd.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ControlledAirspaceRecord {
  private final static Pattern MATCHING_REGEX = Pattern.compile("(?:S|T)...UC.{126}");

  public final String type;
  public final String area;
  public final String secCode;
  public final String subCode;
  public final String icaoCode;
  public final String airspaceType;
  public final String airspaceCenter;
  public final String airspaceCenterSecCode;
  public final String airspaceCenterSubCode;
  public final String airspaceClass;
  public final String multipleCode;
  public final String sequenceNumber;
  public final String continuationRecordNumber;
  public final String level;
  public final String timeCode;
  public final String notam;
  public final String boundaryVia;
  public final String latitude;
  public final String longitude;
  public final String arcOriginLatitude;
  public final String arcOriginLongitude;
  public final String arcDistance;
  public final String arcBearing;
  public final String rnp;
  public final String lowerLimit;
  public final String lowerLimitUnitIndicator;
  public final String upperLimit;
  public final String upperLimitUnitIndicator;
  public final String controlledAirspaceName;
  public final String fileRecordNumber;
  public final String cycle;
  
  public ControlledAirspaceRecord(
      final String type,
      final String area,
      final String secCode,
      final String subCode,
      final String icaoCode,
      final String airspaceType,
      final String airspaceCenter,
      final String airspaceCenterSecCode,
      final String airspaceCenterSubCode,
      final String airspaceClass,
      final String multipleCode,
      final String sequenceNumber,
      final String continuationRecordNumber,
      final String level,
      final String timeCode,
      final String notam,
      final String boundaryVia,
      final String latitude,
      final String longitude,
      final String arcOriginLatitude,
      final String arcOriginLongitude,
      final String arcDistance,
      final String arcBearing,
      final String rnp,
      final String lowerLimit,
      final String lowerLimitUnitIndicator,
      final String upperLimit,
      final String upperLimitUnitIndicator,
      final String controlledAirspaceName,
      final String fileRecordNumber,
      final String cycle) {
    this.type = type;
    this.area = area;
    this.secCode = secCode;
    this.subCode = subCode;
    this.icaoCode = icaoCode;
    this.airspaceType = airspaceType;
    this.airspaceCenter = airspaceCenter;
    this.airspaceCenterSecCode = airspaceCenterSecCode;
    this.airspaceCenterSubCode = airspaceCenterSubCode;
    this.airspaceClass = airspaceClass;
    this.multipleCode = multipleCode;
    this.sequenceNumber = sequenceNumber;
    this.continuationRecordNumber = continuationRecordNumber;
    this.level = level;
    this.timeCode = timeCode;
    this.notam = notam;
    this.boundaryVia = boundaryVia;
    this.latitude = latitude;
    this.longitude = longitude;
    this.arcOriginLatitude = arcOriginLatitude;
    this.arcOriginLongitude = arcOriginLongitude;
    this.arcDistance = arcDistance;
    this.arcBearing = arcBearing;
    this.rnp = rnp;
    this.lowerLimit = lowerLimit;
    this.lowerLimitUnitIndicator = lowerLimitUnitIndicator;
    this.upperLimit = upperLimit;
    this.upperLimitUnitIndicator = upperLimitUnitIndicator;
    this.controlledAirspaceName = controlledAirspaceName;
    this.fileRecordNumber = fileRecordNumber;
    this.cycle = cycle;
  }

  public static boolean matches(final String line) {
    return MATCHING_REGEX.matcher(line).matches();
  }
  
  public static ControlledAirspaceRecord parse(final String line) {
    final String type = line.substring(0, 1);
    final String area = line.substring(1, 4);
    final String secCode = line.substring(4, 5);
    final String subCode = line.substring(5, 6);
    final String icaoCode = line.substring(6, 8);
    final String airspaceType = line.substring(8, 9);
    final String airspaceCenter = line.substring(9, 14);
    final String airspaceCenterSecCode = line.substring(14, 15);
    final String airspaceCenterSubCode = line.substring(15, 16);
    final String airspaceClass = line.substring(16, 17);
    final String multipleCode = line.substring(19, 20);
    final String sequenceNumber = line.substring(20, 24);
    final String continuationRecordNumber = line.substring(24, 25);
    final String level = line.substring(25, 26);
    final String timeCode = line.substring(26, 27);
    final String notam = line.substring(27, 28);
    final String boundaryVia = line.substring(30, 32);
    final String latitude = line.substring(32, 41);
    final String longitude = line.substring(41, 51);
    final String arcOriginLatitude = line.substring(51, 60);
    final String arcOriginLongitude = line.substring(60, 70);
    final String arcDistance = line.substring(70, 74);
    final String arcBearing = line.substring(74, 78);
    final String rnp = line.substring(78, 81);
    final String lowerLimit = line.substring(81, 86);
    final String lowerLimitUnitIndicator = line.substring(86, 87);
    final String upperLimit = line.substring(87, 92);
    final String upperLimitUnitIndicator = line.substring(92, 93);
    final String controlledAirspaceName = line.substring(93, 123);
    final String fileRecordNumber = line.substring(123, 128);
    final String cycle = line.substring(128, 132);
    return new ControlledAirspaceRecord(
        type,
        area,
        secCode,
        subCode,
        icaoCode,
        airspaceType,
        airspaceCenter,
        airspaceCenterSecCode,
        airspaceCenterSubCode,
        airspaceClass,
        multipleCode,
        sequenceNumber,
        continuationRecordNumber,
        level,
        timeCode,
        notam,
        boundaryVia,
        latitude,
        longitude,
        arcOriginLatitude,
        arcOriginLongitude,
        arcDistance,
        arcBearing,
        rnp,
        lowerLimit,
        lowerLimitUnitIndicator,
        upperLimit,
        upperLimitUnitIndicator,
        controlledAirspaceName,
        fileRecordNumber,
        cycle);
  }
}

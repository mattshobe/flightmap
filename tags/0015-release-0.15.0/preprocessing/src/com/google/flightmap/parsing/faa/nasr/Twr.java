/* 
 * Copyright (C) 2010 Google Inc.
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

package com.google.flightmap.parsing.faa.nasr;

/**
 * Base data
 */
class Twr1 {
  final String type;
  final String facilityIdentifier;
  final String effectiveDate;
  final String siteNumber;
  final String masterAirportIdentifier;
  final String masterAirportName;

  Twr1(final String line) {
    type = line.substring(0, 0 + 4).trim();
    facilityIdentifier = line.substring(4, 4 + 4).trim();
    effectiveDate = line.substring(8, 8 + 10).trim();
    siteNumber = line.substring(18, 18 + 11).trim();
    masterAirportIdentifier = line.substring(219, 219 + 4).trim();
    masterAirportName = line.substring(223, 223 + 42).trim();
  }

  static boolean matches(final String line) {
    return line.startsWith("TWR1");
  }
}

/**
 * Operation hours data
 */
class Twr2 {
  final String type;
  final String facilityIdentifier;
  final String atisHours;
  final String primaryApproachHours;
  final String secondaryApproachHours;
  final String primaryDepartureHours;
  final String secondaryDepartureHours;
  final String towerHours1;
  final String towerHours2;
  final String towerHours3;
  final String towerHours4;
  final String towerHours5;
  final String towerHours6;
  final String towerHours7;

  Twr2(final String line) {
    type = line.substring(0, 0 + 4).trim();
    facilityIdentifier = line.substring(4, 4 + 4).trim();
    atisHours = line.substring(8, 8 + 50).trim();
    primaryApproachHours = line.substring(208, 208 + 50).trim();
    secondaryApproachHours = line.substring(258, 258 + 50).trim();
    primaryDepartureHours = line.substring(308, 308 + 50).trim();
    secondaryDepartureHours = line.substring(358, 358 + 50).trim();
    towerHours1 = line.substring(408, 408 + 50).trim();
    towerHours2 = line.substring(458, 458 + 50).trim();
    towerHours3 = line.substring(508, 508 + 50).trim();
    towerHours4 = line.substring(558, 558 + 50).trim();
    towerHours5 = line.substring(608, 608 + 50).trim();
    towerHours6 = line.substring(658, 658 + 50).trim();
    towerHours7 = line.substring(708, 708 + 50).trim();
  }

  static boolean matches(final String line) {
    return line.startsWith("TWR2");
  }
}

/**
 * Communications frequencies and use data (Master Airport use only)
 */
class Twr3 {
  final String type;
  final String facilityIdentifier;
  final String frequency1;
  final String sectorization1;
  final String frequency2;
  final String sectorization2;
  final String frequency3;
  final String sectorization3;
  final String frequency4;
  final String sectorization4;
  final String frequency5;
  final String sectorization5;
  final String frequency6;
  final String sectorization6;
  final String frequency7;
  final String sectorization7;
  final String frequency8;
  final String sectorization8;
  final String frequency9;
  final String sectorization9;

  Twr3(final String line) {
    type = line.substring(0, 0 + 4).trim();
    facilityIdentifier = line.substring(4, 4 + 4).trim();
    frequency1 = line.substring(8, 8 + 44).trim();
    sectorization1 = line.substring(52, 52 + 50).trim();
    frequency2 = line.substring(102, 102 + 44).trim();
    sectorization2 = line.substring(146, 146 + 50).trim();
    frequency3 = line.substring(196, 196 + 44).trim();
    sectorization3 = line.substring(240, 240 + 50).trim();
    frequency4 = line.substring(290, 290 + 44).trim();
    sectorization4 = line.substring(334, 334 + 50).trim();
    frequency5 = line.substring(384, 384 + 44).trim();
    sectorization5 = line.substring(428, 428 + 50).trim();
    frequency6 = line.substring(478, 478 + 44).trim();
    sectorization6 = line.substring(522, 522 + 50).trim();
    frequency7 = line.substring(572, 572 + 44).trim();
    sectorization7 = line.substring(616, 616 + 50).trim();
    frequency8 = line.substring(666, 666 + 44).trim();
    sectorization8 = line.substring(710, 710 + 50).trim();
    frequency9 = line.substring(760, 760 + 44).trim();
    sectorization9 = line.substring(804, 804 + 50).trim();
  }

  static boolean matches(final String line) {
    return line.startsWith("TWR3");
  }
}

/**
 * Terminal communications facility remarks data
 */
class Twr6 {
  final String type;
  final String facilityIdentifier;
  final String elementNumber;
  final String text;

  Twr6(final String line) {
    type = line.substring(0, 0 + 4).trim();
    facilityIdentifier = line.substring(4, 4 + 4).trim();
    elementNumber = line.substring(8, 8 + 5).trim();
    text = line.substring(13, 13 + 400).trim();
  }

  static boolean matches(final String line) {
    return line.startsWith("TWR6");
  }
}

/**
 * Satellite Airport data
 */
class Twr7 {
  final String type;
  final String facilityIdentifier;
  final String frequency;
  final String frequencyUse;
  final String satelliteIdentifier;

  Twr7(final String line) {
    type = line.substring(0, 0 + 4).trim();
    facilityIdentifier = line.substring(4, 4 + 4).trim();
    frequency = line.substring(8, 8 + 44).trim();
    frequencyUse = line.substring(52, 52 + 50).trim();
    satelliteIdentifier = line.substring(113, 113 + 4).trim();
  }

  static boolean matches(final String line) {
    return line.startsWith("TWR7");
  }
}

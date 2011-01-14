
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<!-- Begin Omniture Code -->
<script language="javascript" src="/scripts/s_code.js" type="text/javascript"></script>
<script language="javascript" type="text/javascript">
<!--
/* Copyright 1997-2004 Omniture, Inc. */
s.pageName=""
/************* DO NOT ALTER ANYTHING BELOW THIS LINE ! **************/
var s_code=s.t();if(s_code)document.write(s_code)
//--></script>
<!-- End Omniture Code -->
<title>NFDC Airport Facilities Data Dictionary</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>

<body>
<table border="1" cellpadding="3" cellspacing="0" align="center">
  <caption><strong>Data Dictionary for the NFDC Airport Facilities Table</strong></caption>
  <thead>
  <tr>
    <th scope="col">Field</th>
    <th scope="col">Number</th>
    <th scope="col">Description</th>
  </tr>
  </thead>
  <tbody>
  
    <tr>
      <td valign="top" scope="row">ActiviationDate</td>
      <td valign="top" scope="row">E157</td>
      <td valign="top">Airport activation date (mm/yyyy). Provides the month and year that the facility was added to the NFDC airport database. Note: this information is only available for those facilities opened since 1981. (ex. 06/1981)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AirframeRepair</td>
      <td valign="top" scope="row">A71</td>
      <td valign="top">Airframe repair service availability/type. (ex. MAJOR, MINOR, NONE)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AirportElevationSource</td>
      <td valign="top" scope="row">NONE</td>
      <td valign="top">Airport elevation source.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AirportElevationSourceDate</td>
      <td valign="top" scope="row">NONE</td>
      <td valign="top">Airport elevation source date (mm/dd/yyyy).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AirportPositionSource</td>
      <td valign="top" scope="row">NONE</td>
      <td valign="top">Airport position source.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AirportPositionSourceDate</td>
      <td valign="top" scope="row">NONE</td>
      <td valign="top">Airport position source date (mm/dd/yyyy).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AirportStatusCode</td>
      <td valign="top" scope="row">N/A</td>
      <td valign="top">Airport status code: CI - closed indefinitely; CP - closed permanently; O - operational</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AirportToFSSPhoneNumber</td>
      <td valign="top" scope="row">A88</td>
      <td valign="top">Local phone number from airport to FSS for adminstrative services</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AirspaceDetermination</td>
      <td valign="top" scope="row">E111</td>
      <td valign="top">Airport airspace analysis determination. (ex. CONDL (conditional), NOT ANALYZED, NO OBJECTION, OBJECTIONABLE)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AlternateFSSID</td>
      <td valign="top" scope="row">A86</td>
      <td valign="top">Alternate FSS identifier provides the identifier of a full-time flight service station that assumes responsibility for the airport during the off hours of a part-time primary FSS. (ex. 'DCA' for Washington FSS)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AlternateFSSName</td>
      <td valign="top" scope="row">A86A</td>
      <td valign="top">Alternate FSS name. (ex. 'Washington' for Washington FSS)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">AlternateFSSTollFreeNumber</td>
      <td valign="top" scope="row">E3A</td>
      <td valign="top">Toll free phone number from airport to FSS for pilot briefing services the data describes the type of toll-free communications and the number to dial. The data formats and their meanings are: 1-nnn-nnnn, dial 1-800- then nnn-nnnn; 8-nnn-nnnn, dial 800 then nnn-nnnn; e-nnnnnnnn, enterprise number dial 0 & ask for enterprise nnnnnnnn; lcnnn-nnnn, local call - dial nnn-nnnn; dl, direct line telephone at the airport - no dialing required; z-nnnnnnnn, zenith number - dial 0 and ask for zenith nnnnnnnn; w-nnnnnnnn, dial 0 and ask for wx nnnnnnnn; c-nnnnnnnn, dial 0 and ask for commerce nnnnnnnn; ld-nnnnnnnn, long distance call - dial (area code) then nnnnnnn; lt-nnnnnnnn, long distal call dial 1-nnnnnnn; 1-wx-brief, dial 1-800-wx-brief; 8-wx-brief, dial 800-wx-brief.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ARPElevation</td>
      <td valign="top" scope="row">A21</td>
      <td valign="top">Airport elevation (nearest foot MSL). Elevation is measured at the highest point on the centerline of the usable landing surface. (ex. 1200; -10 for 10 feet below sea level)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ARPElevationMethod</td>
      <td valign="top" scope="row">A21</td>
      <td valign="top">Airport elevation determination method. (ex. E - estimated, S - surveyed)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ARPLatitude</td>
      <td valign="top" scope="row">A19</td>
      <td valign="top">Airport reference point latitude (formatted).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ARPLatitudeS</td>
      <td valign="top" scope="row">A19S</td>
      <td valign="top">Airport reference point latitude (seconds).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ARPLongitude</td>
      <td valign="top" scope="row">A20</td>
      <td valign="top">Airport reference point longitude (formatted).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ARPLongitudeS</td>
      <td valign="top" scope="row">A20S</td>
      <td valign="top">Airport reference point longitude (seconds).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ARPMethod</td>
      <td valign="top" scope="row">A19A</td>
      <td valign="top">Airport reference point determination method. (ex. E - estimated, S - surveyed)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ATCT</td>
      <td valign="top" scope="row">A85</td>
      <td valign="top">Air traffic control tower located on airport. (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">BeaconColor</td>
      <td valign="top" scope="row">A80</td>
      <td valign="top">Lens color of operable beacon located on the airport. (ex. CG - clear-green (lighted land airport); CY - clear-yellow (lighted seaplane base); CGY - clear-green-yellow (heliport); SCG - split-clear-green (lighted military airport); C - clear (unlighted la</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">BottledOxygenType</td>
      <td valign="top" scope="row">A73</td>
      <td valign="top">Type of bottled oxygen available (value represents high and/or low pressure replacement bottle). (ex. HIGH, LOW, HIGH/LOW, NONE)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">BoundaryARTCCComputerID</td>
      <td valign="top" scope="row">E146B</td>
      <td valign="top">Boundary ARTCC (FAA) computer identifier. (ex. ZCW for Washington ARTCC)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">BoundaryARTCCID</td>
      <td valign="top" scope="row">E146A</td>
      <td valign="top">Boundary ARTCC Identifier.  The boundary ARTCC is the FAA air route traffic control center within whose published boundaries the airport lies. It may not be the controlling ARTCC for the airport if a letter of agreement exists between the boundary ARTCC and another ARTCC. (ex. ZDC for Washington ARTCC)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">BoundaryARTCCName</td>
      <td valign="top" scope="row">E146C</td>
      <td valign="top">Boundary ARTCC name. (ex. Washington)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">BulkOxygenType</td>
      <td valign="top" scope="row">A74</td>
      <td valign="top">Type of bulk oxygen available (value represents high and/or low pressure cylinders). (ex. HIGH, LOW, HIGH/LOW, NONE)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">CertificationTypeDate</td>
      <td valign="top" scope="row">A26</td>
      <td valign="top">Airport certification type and date. Format is the class code ('I', 'II', 'III' or 'IV') followed by a one characther code A, B, C, D, E, or L, followed by a one character code S or U, followed by the month and year of certification. (ex. 'I A S 07/1980', 'I C S 01/1983' or 'I A U 09/1983').  Codes A, B, C, D, and E are for airports having a full certificate under CFR Part 139, and receiving scheduled air carrier service from carriers certificated by the Civil Aeronautics Board.  The A, B, C, D, and E identify the aircraft rescue and firefighting index for the airport.  Code L is for airports having limited certification under CFR Part 139.  Code S is for Airports receiving scheduled air carrier service from carriers certificated by the Civil Aeronautics Board.  Code U is for airports not receiving this scheduled service.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ChartName</td>
      <td valign="top" scope="row">A7</td>
      <td valign="top">Aeronautical sectional chart on which facility appears. (ex. Washington)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">City</td>
      <td valign="top" scope="row">A1</td>
      <td valign="top">Associated city name. (ex. Chicago)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ContractFuelAvailable</td>
      <td valign="top" scope="row">NONE</td>
      <td valign="top">Contract fuel available. (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">County</td>
      <td valign="top" scope="row">A5</td>
      <td valign="top">Associated county (or parish) name. (ex. Cook)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">CountyState</td>
      <td valign="top" scope="row">A5</td>
      <td valign="top">Associated county's state (post office code) state where the associated county is located; may not be the same as the associated city's state code. (ex. IL)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">CTAFFrequency</td>
      <td valign="top" scope="row">E100</td>
      <td valign="top">Common traffic advisory frequency. (CTAF) (ex. 122.800)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">CustomsAirportOfEntry</td>
      <td valign="top" scope="row">E79</td>
      <td valign="top">Facility has been designated by the U.S. Treasury as an international airport of entry for customs (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">CustomsLandingRights</td>
      <td valign="top" scope="row">E80</td>
      <td valign="top">Facility has been designated by the U.S. Treasury as a customs landing rights airport (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">DirectionFromCBD</td>
      <td valign="top" scope="row">A3</td>
      <td valign="top">Direction of airport from central business district of associated city (nearest 1/8 compass point - ex. NE).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">DistanceFromCBD</td>
      <td valign="top" scope="row">A3</td>
      <td valign="top">Distance from central business district of the associated city to the airport (nearest nautical mile - ex. 08).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">DistrictOffice</td>
      <td valign="top" scope="row">A6A</td>
      <td valign="top">FAA district or field office code. (ex. CHI)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">EffectiveDate</td>
      <td valign="top" scope="row">N/A</td>
      <td valign="top">Information effective date (mm/dd/yyyy). This date coincides with the 56-day charting and publication cycle date.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">FacilityName</td>
      <td valign="top" scope="row">A2</td>
      <td valign="top">Official facility name. (ex. Chicago O'Hare Intl)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">FederalAgreements</td>
      <td valign="top" scope="row">A25</td>
      <td valign="top">NPIAS/Federal Agreement Code.  A combination of 1 to 7 codes that indicate the type of federal agreements existing at the airport. (ex. NGH). N - national plan of integrated airport systems (NPIAS); B - installation of navigational facilities on privately owned airports under F&E program; G - grant agreements under FAAP/ADAP/AIP; H - compliance with accessibility to the handicapped; P - surplus property agreement under Public Law 289; R - surplus property agreement under Regulation 16-WAA; S - conveyance under section 16, Federal Airport Act of 1946 or Section 23, Airport and Airway Development Act of 1970; V - advance planning agreement under FAAP; X - obligations assumed by transfer; Y - assurances pursuant to Title VI, Civil Rights Act of 1964; Z - conveyance under Section 303(C), Federal Aviation Act of 1958; 1 - grant agreement has expired, however, agreement remains in effect for this facility as long as it is public use.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">FuelTypes</td>
      <td valign="top" scope="row">A70</td>
      <td valign="top">Fuel types available for public use at the airport. There can be up to 8 occurrences of a fixed 5 character field (ex. 80___100__100LL115__). 80 - grade 80 gasoline (red), 100 - grade 100 gasoline (green), 100LL - grade 100LL gasoline (low lead blue), 115 - grade 115 gasoline, A - jet A - kerosene, freeze point -40C, A1 - jet A-1 - kerosene, freeze point -50C, A1+ - jet A-1 - kerosene, with icing inhibitor freeze point -50C, B - jet B - wide-cut turbine fuel, freeze point -50C, B+ - jet B - wide-cut turbine fuel with icing inhibitor, freeze point -50C, MOGAS - automotive gasoline.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">GlidersOperational</td>
      <td valign="top" scope="row">A94</td>
      <td valign="top">Number of operational gliders.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">HelicoptersGA</td>
      <td valign="top" scope="row">A93</td>
      <td valign="top">Number of general aviation helicopter.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">IcaoIdentifier</td>
      <td valign="top" scope="row">NONE</td>
      <td valign="top">International coding for airport.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">InspectionGroup</td>
      <td valign="top" scope="row">A111</td>
      <td valign="top">Agency/group performing physical inspection (ex. F - faa airports field personnel, s - state aeronautical personnel, c - private contract personnel, n - owner)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">InspectionMethod</td>
      <td valign="top" scope="row">E155</td>
      <td valign="top">Airport inspection method. (ex. F - federal, S - state, C - contractor, 1 - 5010-1 public use mail out program, 2 - 5010-2 private use mail out program)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">JetEngineGA</td>
      <td valign="top" scope="row">A92</td>
      <td valign="top">Number of jet engine general aviation aircraft.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">LandAreaCoveredByAirport</td>
      <td valign="top" scope="row">A22</td>
      <td valign="top">Amount of land owned by the airport in acres.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">LastInspectionDate</td>
      <td valign="top" scope="row">A112</td>
      <td valign="top">Last physical inspection date (mmddyyyy)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">LastOwnerInformationDate</td>
      <td valign="top" scope="row">A113</td>
      <td valign="top">Last date information request was completed by facility owner or manager (mmddyyyy)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">LightingSchedule</td>
      <td valign="top" scope="row">A81</td>
      <td valign="top">Airport lighting schedule value is the beginning-ending times (local time) that lights are operated. Format can be 1900-2300, DUSK-0100, ALL, DUSK-DAWN, NONE, etc.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">LocationID</td>
      <td valign="top" scope="row">E7</td>
      <td valign="top">Location identifier unique 3-4 character alphanumeric identifier assigned to the landing facility. (ex. 'ORD' for Chicago O'Hare)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">MagneticVariation</td>
      <td valign="top" scope="row">E28</td>
      <td valign="top">Magnetic variation and direction magnetic variation to nearest degree. (ex. 03W)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">MagneticVariationYear</td>
      <td valign="top" scope="row">E28</td>
      <td valign="top">Magnetic variation epoch year. (ex. 1985)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">Manager</td>
      <td valign="top" scope="row">A14</td>
      <td valign="top">Facility manager's name.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ManagerAddress</td>
      <td valign="top" scope="row">A15</td>
      <td valign="top">Manager's address.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ManagerCSZ</td>
      <td valign="top" scope="row">A15A</td>
      <td valign="top">Manager's city, state and zip code.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ManagerPhone</td>
      <td valign="top" scope="row">A16</td>
      <td valign="top">Manager's phone number. (data formats: nnn-nnn-nnnn (area code + phone number), 1-nnn-nnnn (dial 1-800 then number), 8-nnn-nnnn (dial 800 then number)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">MedicalUse</td>
      <td valign="top" scope="row">NONE</td>
      <td valign="top">Landing facility is used for medical purposes. (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">MilitaryJointUse</td>
      <td valign="top" scope="row">E115</td>
      <td valign="top">Facility has military/civil joint use agreement that allows civil operations at a military airport or military operations at a civil airport (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">MilitaryLandingRights</td>
      <td valign="top" scope="row">E116</td>
      <td valign="top">Airport has entered into an agreement that grants landing rights to the military (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">MilitaryOperational</td>
      <td valign="top" scope="row">A95</td>
      <td valign="top">Number operational military aircraft (includingg helicopters).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">MultiEngineGA</td>
      <td valign="top" scope="row">A92</td>
      <td valign="top">Number of multi engine general aviation aircraft.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">NationalEmergencyInterest</td>
      <td valign="top" scope="row">E141B</td>
      <td valign="top">Military department(s) that maintain national emergency use interest in this civil facility (ex. A/R R N). R - Army; A - Air Force; N - Navy; X - none</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">NationalEmergencyStatus</td>
      <td valign="top" scope="row">E141A</td>
      <td valign="top">Status of airport that is available for use during a national emergency. These are civil airports that were formerly military airfields but the military services have a continuing interest in their use during national emergencies. Data values are one or a series of alphanumeric codes indicating national emergency status (ex. 6  1/4  3E/8/1).  1 - Airports certificated under CFR Part 139.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">NonCommercialLandingFee</td>
      <td valign="top" scope="row">A24</td>
      <td valign="top">Landing fee charged to non-commercial users of airport. (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">NOTAMFacilityID</td>
      <td valign="top" scope="row">E2B</td>
      <td valign="top">Identifier of the facility responsible for issuing notices to airmen (NOTAMS) and weather information for the airport. (ex. ORD)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">NOTAMService</td>
      <td valign="top" scope="row">E139</td>
      <td valign="top">Availability of NOTAM 'd' service at airport. (ex. Y - yes, N - no)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OperationsAirTaxi</td>
      <td valign="top" scope="row">A102</td>
      <td valign="top">Air taxi. Air taxi operators carrying passengers, mail, or mail for revenue.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OperationsCommercial</td>
      <td valign="top" scope="row">A100</td>
      <td valign="top">Commercial services. Scheduled operations by cab-certificated carriers or intrastate carriers.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OperationsCommuter</td>
      <td valign="top" scope="row">A101</td>
      <td valign="top">Commuter services. Scheduled commuter and cargo carriers.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OperationsDate</td>
      <td valign="top" scope="row">NONE</td>
      <td valign="top">12-month ending date on which annual operations data in above six field is based (mm/dd/yyyy).</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OperationsGAItin</td>
      <td valign="top" scope="row">A104</td>
      <td valign="top">General aviation itinerant operations. Those general aviation operations (excluding commuter or air taxi) not qualifying as local.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OperationsGALocal</td>
      <td valign="top" scope="row">A103</td>
      <td valign="top">General aviation local operations. Those operating in the local traffic pattern or within a 20-mile radius of the airport.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OperationsMilitary</td>
      <td valign="top" scope="row">A105</td>
      <td valign="top">Military aircraft operations.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OtherServices</td>
      <td valign="top" scope="row">A76</td>
      <td valign="top">Other services. (ex. Y - yes, N - no, none)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">Owner</td>
      <td valign="top" scope="row">A11</td>
      <td valign="top">Facility owner's name.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OwnerAddress</td>
      <td valign="top" scope="row">A12</td>
      <td valign="top">Owner's address.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OwnerCSZ</td>
      <td valign="top" scope="row">A12A</td>
      <td valign="top">Owner's city, state and zip code.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">OwnerPhone</td>
      <td valign="top" scope="row">A13</td>
      <td valign="top">Owner's phone number. (data formats: nnn-nnn-nnnn (area code + phone number), 1-nnn-nnnn (dial 1-800 then number), 8-nnn-nnnn (dial 800 then number)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">Ownership</td>
      <td valign="top" scope="row">A10</td>
      <td valign="top">Airport ownership type. (ex. PU - publicly owned, PR - privately owned, MA - air force owned, MN - navy owned, MR - army owned)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">PowerPlantRepair</td>
      <td valign="top" scope="row">A72</td>
      <td valign="top">Power plant (engine) repair availability/type. (ex. MAJOR, MINOR, NONE)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">Region</td>
      <td valign="top" scope="row">A6</td>
      <td valign="top">FAA region code. (ex. AAL - Alaska, ACE - Central, AEA - Eastern, AGL - Great Lakes, AIN - International, ANE - New England, ANM - Northwest Mountain, ASO - Southern, ASW - Southwest, AWP - Western-Pacific)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ResponsibleARTCCComputerID</td>
      <td valign="top" scope="row">E156B</td>
      <td valign="top">Responsible ARTCC (FAA) computer identifier. (ex. ZCW for Washington ARTCC)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ResponsibleARTCCID</td>
      <td valign="top" scope="row">E156A</td>
      <td valign="top">Responsible ARTCC identifier the responsible ARTCC is the FAA air route traffic control center who has assumed control over the airport through a letter of agreement with the boundary ARTCC. (ex. ZDC for Washington ARTCC)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">ResponsibleARTCCName</td>
      <td valign="top" scope="row">E156C</td>
      <td valign="top">Responsible ARTCC name. (ex. Washington)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">SegmentedCircle</td>
      <td valign="top" scope="row">A84</td>
      <td valign="top">Segmented circle airport marker system on the airport. (ex. Y - yes, N - no, none)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">SingleEngineGA</td>
      <td valign="top" scope="row">A90</td>
      <td valign="top">Number of single engine general aviation aircraft.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">SiteNumber</td>
      <td valign="top" scope="row">DLID</td>
      <td valign="top">Landing facility site number - a unique identifying number which, together with the landing facility type code, forms the key to the airport record. (ex. 04508.*A)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">State</td>
      <td valign="top" scope="row">A4</td>
      <td valign="top">Associated state post office code standard two letter abbreviation for u.s. states and territories. (ex. IL, PR, CQ)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">StateName</td>
      <td valign="top" scope="row">A4</td>
      <td valign="top">Associated state name. (ex. Illinois)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">TieInFSS</td>
      <td valign="top" scope="row">A87</td>
      <td valign="top">Tie-in FSS physically located on facility. (ex. Y - tie-in FSS is on the airport, n - tie-in FSS is not on the airport)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">TieInFSSID</td>
      <td valign="top" scope="row">A86</td>
      <td valign="top">Tie-in flight service station (FSS) identifier. (ex. DCA for Washington FSS)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">TieInFSSName</td>
      <td valign="top" scope="row">A86</td>
      <td valign="top">Tie-in FSS name. (ex. Washington)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">TieInFSSTollFreeNumber</td>
      <td valign="top" scope="row">A89</td>
      <td valign="top">Toll free phone number from airport to FSS for pilot briefing services the data describes the type of toll-free communications and the number to dial. The data formats and their meanings are: 1-nnn-nnnn, dial 1-800- then nnn-nnnn; 8-nnn-nnnn, dial 800 then nnn-nnnn; e-nnnnnnnn, enterprise number dial 0 & ask for enterprise nnnnnnnn; lcnnn-nnnn, local call - dial nnn-nnnn; dl, direct line telephone at the airport - no dialing required; z-nnnnnnnn, zenith number - dial 0 and ask for zenith nnnnnnnn; w-nnnnnnnn, dial 0 and ask for wx nnnnnnnn; c-nnnnnnnn, dial 0 and ask for commerce nnnnnnnn; ld-nnnnnnnn, long distance call - dial (area code) then nnnnnnn; lt-nnnnnnnn, long distal call dial 1-nnnnnnn; 1-wx-brief, dial 1-800-wx-brief; 8-wx-brief, dial 800-wx-brief</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">TrafficPatternAltitude</td>
      <td valign="top" scope="row">E147</td>
      <td valign="top">Traffic pattern altitude (whole feet AGL). (ex. 1000)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">TransientStorage</td>
      <td valign="top" scope="row">A75</td>
      <td valign="top">Transient storage. (ex. Y - yes, N - no, none)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">Type</td>
      <td valign="top" scope="row">DLID</td>
      <td valign="top">Landing facility type. (ex. Airport, Balloonport, Seaplane Base, Gliderport, Heliport, Stolport, Ultralight)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">Ultralights</td>
      <td valign="top" scope="row">A96</td>
      <td valign="top">Number of ultralight aircraft.</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">UNICOMFrequencies</td>
      <td valign="top" scope="row">A82</td>
      <td valign="top">Unicom frequencies available at the airport there can be up to 6 occurrences of a fixed 7 character field. (ex. 122.700 or 122.700122.800 or NONE)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">Use</td>
      <td valign="top" scope="row">A18</td>
      <td valign="top">Facility use. (ex. PU - open to the public, PR - private)</td>
    </tr>
  
    <tr>
      <td valign="top" scope="row">WindIndicator</td>
      <td valign="top" scope="row">A83</td>
      <td valign="top">Wind direction indicator. (ex. Y - yes, N - no, none)</td>
    </tr>
  
  </tbody>
</table>
</body>
</html>

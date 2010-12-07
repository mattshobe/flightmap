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

package com.google.flightmap.parsing.data;

public class AirportComm {
  public final int airportId;
  public final String identifier;
  public final String frequency;
  public final String remarks;

  public AirportComm(final int airportId, final String identifier, final String frequency,
      final String remarks) {
    this.airportId = airportId;
    this.identifier = identifier;
    this.frequency = frequency;
    this.remarks = remarks;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("\"");
    sb.append(airportId);
    sb.append("\",\"");
    sb.append(identifier);
    sb.append("\",\"");
    sb.append(frequency);
    sb.append("\",\"");
    sb.append(remarks);
    sb.append("\"");
    return sb.toString();
  }
}

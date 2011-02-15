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


public class AirspaceAttributeBean {
  private String highAlt;
  private String lowAlt;
  private String name;

  public void setHighAlt(final String highAlt) {
    this.highAlt = highAlt;
  }

  public String getHighAlt() {
    return highAlt;
  }

  public void setLowAlt(final String lowAlt) {
    this.lowAlt = lowAlt;
  }

  public String getLowAlt() {
    return lowAlt;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(highAlt);
    sb.append(" ");
    sb.append(lowAlt);
    sb.append(" ");
    sb.append(name);
    sb.append(" ");
    return sb.toString();
  }
}

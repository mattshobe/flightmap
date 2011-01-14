/*
 * Copyright (C) 2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.flightmap.common.data;

/**
 * RunwayEnd data structure.
 */
public class RunwayEnd implements Comparable<RunwayEnd> {
  public final int id;

  public final String letters;

  public RunwayEnd(int id, String letters) {
    this.id = id;
    this.letters = letters;
  }

  @Override
  public String toString() {
    return String.format("%s", letters);
  }

  @Override
  public int compareTo(RunwayEnd other) {
    return letters.compareTo(other.letters);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if ( !(obj instanceof RunwayEnd)) return false;

    RunwayEnd other = (RunwayEnd)obj;
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return this.letters.hashCode();
  }
}

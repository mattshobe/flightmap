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

public class Comm implements Comparable<Comm> {
  public final String identifier;
  public final String frequency;
  public final String remarks;

  /**
   * Creates a comm instance. Any null params will be set to "".
   */
  public Comm(final String identifier, final String frequency, final String remarks) {
    this.identifier = (identifier == null) ? "" : identifier;
    this.frequency = (frequency == null) ? "" : frequency;
    this.remarks = (remarks == null) ? "" : remarks;
  }

  @Override
  public int compareTo(final Comm o) {
    final int identifierDiff = identifier.compareTo(o.identifier);
    if (identifierDiff != 0) {
      return identifierDiff;
    }
    final int remarksDiff = remarks.compareTo(o.remarks);
    if (remarksDiff != 0) {
      return remarksDiff;
    }
    final int frequencyDiff = frequency.compareTo(o.frequency);
    return frequencyDiff;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof Comm)) return false;

    final Comm other = (Comm) obj;
    return compareTo(other) == 0;
  }

  @Override
  public int hashCode() {
    return identifier.hashCode() ^ frequency.hashCode() ^ remarks.hashCode();
  }
}

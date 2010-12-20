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

public class NormalizedFrequencyUseBean {
  private String normalizedUse;
  private String originalUse;
  private String remarks;
  
  public void setNormalizedUse(final String normalizedUse) {
    this.normalizedUse = normalizedUse;
  }
  
  public String getNormalizedUse() {
    return normalizedUse;
  }
  
  public void setOriginalUse(final String originalUse) {
    this.originalUse = originalUse;
  }
  
  public String getOriginalUse() {
    return originalUse;
  }
  
  public void setRemarks(final String remarks) {
    this.remarks = remarks;
  }
  
  public String getRemarks() {
    return remarks;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(normalizedUse);
    sb.append(" ");
    sb.append(originalUse);
    sb.append(" ");
    sb.append(remarks);
    sb.append(" ");
    return sb.toString();
  }
}


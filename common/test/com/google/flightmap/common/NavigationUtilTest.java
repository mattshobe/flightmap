/*
 * Copyright (C) 2010 Google Inc.
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

package com.google.flightmap.common;

import junit.framework.TestCase;

public class NavigationUtilTest extends TestCase {

  private static final double MAX_ERROR = 1E-5;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  public void testNormalizeBearing() {
    assertEquals(0, NavigationUtil.normalizeBearing(0), MAX_ERROR);
    assertEquals(0, NavigationUtil.normalizeBearing(1080), MAX_ERROR);
    assertEquals(0, NavigationUtil.normalizeBearing(-720), MAX_ERROR);
    assertEquals(10, NavigationUtil.normalizeBearing(370), MAX_ERROR);
    assertEquals(180.01, NavigationUtil.normalizeBearing(-179.99), MAX_ERROR);
    assertEquals(350, NavigationUtil.normalizeBearing(-10), MAX_ERROR);
    assertEquals(350, NavigationUtil.normalizeBearing(-370), MAX_ERROR);
  }

}

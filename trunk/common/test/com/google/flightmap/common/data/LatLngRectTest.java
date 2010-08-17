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
package com.google.flightmap.common.data;

import junit.framework.TestCase;

import java.util.LinkedList;

public class LatLngRectTest extends TestCase {
  public LatLngRectTest(final String name) {
    super(name);
  }

  private static final int IN = 1000000;
  private static final int ON = 2000000;
  private static final int OUT = 3000000;
 

  private static final LatLng A = new LatLng(ON, -ON);
  private static final LatLng B = new LatLng(ON, -IN);
  private static final LatLng D = new LatLng(ON, IN);
  private static final LatLng E = new LatLng(ON, ON);
  private static final LatLng F = new LatLng(IN, ON);
  private static final LatLng H = new LatLng(-IN, ON);
  private static final LatLng I = new LatLng(-ON, ON);
  private static final LatLng J = new LatLng(-ON, IN);
  private static final LatLng L = new LatLng(-ON, -IN);
  private static final LatLng P = new LatLng(-IN, -ON);
  private static final LatLng R = new LatLng(IN, -ON);
  private static final LatLng nw = new LatLng(IN, -IN);
  private static final LatLng ne = new LatLng(IN, IN);
  private static final LatLng se = new LatLng(-IN, IN);
  private static final LatLng sw = new LatLng(-IN, -IN);
  private static final LatLng NW = new LatLng(OUT, -OUT);
  private static final LatLng NNE = new LatLng(OUT, IN);
  private static final LatLng ENE = new LatLng(IN, OUT);
  private static final LatLng ESE = new LatLng(-IN, OUT);
  private static final LatLng SE = new LatLng(-OUT, OUT);
  private static final LatLng SSE = new LatLng(-OUT, IN);
  private static final LatLng SSW = new LatLng(-OUT, -IN); 
  private static final LatLng WSW = new LatLng(-IN, -OUT);
  private static final LatLng WNW = new LatLng(IN, -OUT);

  public void testIntersect() {
    final LatLngRect area = new LatLngRect(A, I);
    assertEquals(area.intersect(new LatLngRect(NW, nw)), new LatLngRect(A, nw));
    assertEquals(area.intersect(new LatLngRect(nw, se)), new LatLngRect(nw, se));

    // Intersect all area
    assertEquals(area.intersect(new LatLngRect(NW, SE)), area);
    assertNotSame(area.intersect(new LatLngRect(NW, SE)), area);
  }

  public void testNoIntersect() {
    final LatLngRect area = new LatLngRect(nw, se);
    assertTrue(area.intersect(new LatLngRect(NW, A)).isEmpty());
  }

  public void testRemoveAllArea() {
    final LatLngRect area = new LatLngRect(A, I);

    assertTrue(area.remove(area).isEmpty());
    assertTrue(area.remove(new LatLngRect(A, I)).isEmpty());
  }

  public void testRemoveAreaWithoutInnerPoints() {
    final LatLngRect area = new LatLngRect(A, I);

    LinkedList<LatLngRect> complement = area.remove(new LatLngRect(NW, ENE));
    assertEquals(complement.size(), 1);
    assertEquals(complement.poll(), new LatLngRect(R, I));

    complement = area.remove(new LatLngRect(WSW, SE));
    assertEquals(complement.size(), 1);
    assertEquals(complement.poll(), new LatLngRect(P, E));

    complement = area.remove(new LatLngRect(WNW, ESE));
    assertEquals(complement.size(), 2);
    assertEquals(complement.poll(), new LatLngRect(A, F));
    assertEquals(complement.poll(), new LatLngRect(P, I));

    complement = area.remove(new LatLngRect(NW, SSW));
    assertEquals(complement.size(), 1);
    assertEquals(complement.poll(), new LatLngRect(B, I));

    complement = area.remove(new LatLngRect(NNE, I));
    assertEquals(complement.size(), 1);
    assertEquals(complement.poll(), new LatLngRect(A, J));

    complement = area.remove(new LatLngRect(B, J));
    assertEquals(complement.size(), 2);
    assertEquals(complement.poll(), new LatLngRect(A, L));
    assertEquals(complement.poll(), new LatLngRect(D, I));
  }

  public void testRemoveAreaWithInnerPoints() {
    final LatLngRect area = new LatLngRect(A, I);
    
    LinkedList<LatLngRect> complement = area.remove(new LatLngRect(NW, nw));
    assertEquals(complement.size(), 2);
    assertEquals(complement.poll(), new LatLngRect(B, F));
    assertEquals(complement.poll(), new LatLngRect(R, I));

    complement = area.remove(new LatLngRect(B, ne));
    assertEquals(complement.size(), 3);
    assertEquals(complement.poll(), new LatLngRect(A, nw));
    assertEquals(complement.poll(), new LatLngRect(D, F));
    assertEquals(complement.poll(), new LatLngRect(R, I));

    complement = area.remove(new LatLngRect(NNE, H));
    assertEquals(complement.size(), 2);
    assertEquals(complement.poll(), new LatLngRect(A, se));
    assertEquals(complement.poll(), new LatLngRect(P, I));

    complement = area.remove(new LatLngRect(nw, ESE));
    assertEquals(complement.size(), 3);
    assertEquals(complement.poll(), new LatLngRect(A, F));
    assertEquals(complement.poll(), new LatLngRect(R, sw));
    assertEquals(complement.poll(), new LatLngRect(P, I));
    
    complement = area.remove(new LatLngRect(se, SE));
    assertEquals(complement.size(), 2);
    assertEquals(complement.poll(), new LatLngRect(A, H));
    assertEquals(complement.poll(), new LatLngRect(P, J));

    complement = area.remove(new LatLngRect(sw, J));
    assertEquals(complement.size(), 3);
    assertEquals(complement.poll(), new LatLngRect(A, H));
    assertEquals(complement.poll(), new LatLngRect(P, L));
    assertEquals(complement.poll(), new LatLngRect(se, I));

    complement = area.remove(new LatLngRect(WNW, SSE));
    assertEquals(complement.size(), 2);
    assertEquals(complement.poll(), new LatLngRect(A, F));
    assertEquals(complement.poll(), new LatLngRect(ne, I));

    complement = area.remove(new LatLngRect(R, sw));
    assertEquals(complement.size(), 3);
    assertEquals(complement.poll(), new LatLngRect(A, F));
    assertEquals(complement.poll(), new LatLngRect(nw, H));
    assertEquals(complement.poll(), new LatLngRect(P, I));

    complement = area.remove(new LatLngRect(sw, ne));
    assertEquals(complement.size(), 4);
    assertEquals(complement.poll(), new LatLngRect(A, F));
    assertEquals(complement.poll(), new LatLngRect(R, sw));
    assertEquals(complement.poll(), new LatLngRect(ne, H));
    assertEquals(complement.poll(), new LatLngRect(P, I));
  }

  public void testNoRemove() {
    final LatLngRect area = new LatLngRect(nw, se);

    // Remove area without intersect
    assertEquals(area.remove(new LatLngRect(NW, A)).poll(), area);
    assertNotSame(area.remove(new LatLngRect(NW, A)).poll(), area);

    // Remove empty area
    assertEquals(area.remove(new LatLngRect()).poll(), area);
    assertNotSame(area.remove(new LatLngRect()).poll(), area);
  }

  public void testRemoveOnEmptyArea() {
    final LatLngRect area = new LatLngRect();

    // Remove empty area from empty area
    LinkedList<LatLngRect> complement = area.remove(new LatLngRect());
    assertEquals(complement.size(), 1);
    assertEquals(complement.poll(), new LatLngRect());

    // Remove non empty area from empty area
    complement = area.remove(new LatLngRect(NW, A));
    assertEquals(complement.size(), 1);
    assertEquals(complement.poll(), new LatLngRect());
  }
}

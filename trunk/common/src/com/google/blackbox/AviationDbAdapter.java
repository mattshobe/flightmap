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

package com.google.blackbox;

import com.google.blackbox.data.Airport;

import java.util.LinkedList;

/**
 * Low level interface to database entities.
 */
public interface AviationDbAdapter {
  public void open();

  public void close();

  public Airport getAirport(int id);

  public LinkedList<Airport> getAirportsInCells(int startCell, int endCell);

}

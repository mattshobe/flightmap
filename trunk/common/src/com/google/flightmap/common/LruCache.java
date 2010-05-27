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

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * Least Recently Used cache.
 */
public class LruCache<K, V> extends LinkedHashMap<K, V> {
  /** Never allow HashMap to grow. */
  private static final float LOAD_FACTOR = 1.1f;
  private static final long serialVersionUID = 4355961131491046577L;
  private int capacity;

  public LruCache(int capacity) {
    super(capacity + 1, LOAD_FACTOR, true); // true = access-order.
    this.capacity = capacity;
  }

  @Override
  protected boolean removeEldestEntry(Entry<K, V> eldest) {
    return size() > capacity;
  }
}

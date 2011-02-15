/* 
 * Copyright (C) 2009 Google Inc.
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

package com.google.flightmap.parsing.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides access to an array with non-integer indexes.
 * <p>
 * The {@code fields} array is indexed by retrieving a position corresponding to a given 
 * {@code header}.
 */
public class IndexedArray<K, V> {
  public static class UnknownHeaderException extends RuntimeException {
    static final long serialVersionUID = -6472728155140574293L;

    public UnknownHeaderException(final Object header) {
      super("Unknown header: " + header.toString());
    }
  }

  private final Map<K, Integer> headerPosition;
  private V[] fields;

  /**
   * Initializes indexed array with given {@code headers}.
   */
  public IndexedArray(final K[] headers) {
    headerPosition = new HashMap<K, Integer>();
    for (int i = 0; i < headers.length; ++i) {
      headerPosition.put(headers[i], i);
    }
  }


  /**
   * Set the {@code fields} array which subsequent {@link #get} operations will use.
   */
  public synchronized void setFields(final V[] fields) {
    this.fields = fields;
  }

  /**
   * Returns field at index corresponding to {@code header}.
   * <p>
   * In the simplest case, the returned value is equivalent to:
   * <pre>
   * fields[headerPosition.get(header)]
   * </pre>
   *
   * @throws ArrayIndexOutOfBoundsException No entry corresponding to {@code header} in {@code
   * fields}
   * @throws UnknownHeaderException {@code header} is not defined in {@link #IndexedArray headers}
   *
   * @see #tryGet
   */
  public synchronized V get(final K header) {
    final Integer index = headerPosition.get(header);
    if (index == null) {
      throw new UnknownHeaderException(header);
    }
    return fields[index];
  }

  /**
   * Returns field at index corresponding to {@code header} if available.
   * <p>
   * This is equivalent to {@link #get} but without any exception being thrown if no entry
   * corresponding to {@code header} is found in {@code fields}.
   *
   * @throws UnknownHeaderException {@code header} is not defined in {@link #IndexedArray headers}
   * fields}
   *
   * @see #get
   */
  public synchronized V tryGet(final K header) {
    try {
      return get(header);
    } catch (ArrayIndexOutOfBoundsException ex) {
      return null;
    }
  }
}

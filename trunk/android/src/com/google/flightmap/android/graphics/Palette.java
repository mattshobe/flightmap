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
package com.google.flightmap.android.graphics;

import android.graphics.Paint;

public interface Palette<V> {
  /**
   * Returns appropriate {@link Paint} for {@code item}.
   * <p>
   * Note: no assumption shall be made on the values returned by successive calls.  In particular,
   * the same Paint object may be returned multiple times (to avoid allocation costs).
   */
  public Paint getPaint(V item);
}

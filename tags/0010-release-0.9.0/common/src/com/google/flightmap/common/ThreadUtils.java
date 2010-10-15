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

/**
 * Convenience methods related to threading.
 */
public class ThreadUtils {
  /**
   *  Utility class: default and only constructor is private.
   */
  private ThreadUtils() { }

  /**
   * Checks if current thread was interrupted.  Does not affect the value of the interrupted flag.
   *
   * @throws InterruptedException Current thread was interrupted.
   */
  public static void checkIfInterrupted() throws InterruptedException {
    if (Thread.currentThread().interrupted()) {
      // DEBUG(aristidis)
      System.err.println("Thread " + Thread.currentThread().getName() + " was interrupted...");
      throw new InterruptedException();
    } 
  }
}


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

package com.google.flightmap.common.io;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.File;

import com.google.flightmap.common.ProgressListener;

/**
 * <p>Utility class for typical operations involving streams, readers, etc.</p>
 *
 * <p>I/O objects are never closed by the methods of this class.</p>
 */
public class StreamUtils {
  private final static int DEFAULT_BUFFER_SIZE = 10240;

  /**
   *  Utility class: default and only constructor is private.
   */
  private StreamUtils() { }

  /**
   * Pipe data from {@code in} to {@code out} using {@code buf} as a data buffer.
   */
  public static void pipe(final InputStream in, final OutputStream out, final byte[] buf)
      throws IOException {
    pipe(in, out, buf, 0, null);
  }

  /**
   * Pipe data from {@code in} to {@code out}.
   * 
   * @param buf Data buffer
   * @param contentLength Bytes to transfer.  Ignored if {@code listener} is {@code null}.
   * @param listener Optional progress listener.
   */
  public static void pipe(final InputStream in, final OutputStream out,
                          final byte[] buf, final int contentLength, 
                          final ProgressListener listener) throws IOException {
    boolean success = false;

    try {
      int totalCount = 0;
      int lastNotifiedPercent = -1;
      int count;

      while ((count = in.read(buf)) != -1) {
        out.write(buf, 0, count);
        if (listener != null) {
          totalCount += count;
          final int percent = (int)(100.0 * totalCount / contentLength + 0.5);
          if (percent != lastNotifiedPercent) {
            listener.hasProgressed(percent);
          }
        }
      }
      success = true;
    } finally {
      if (listener != null) {
        listener.hasCompleted(success);
      }
    }
  }

  /**
   * Gets all bytes from {@code file}.
   */
  public static byte[] getBytes(final File file) throws IOException {
    final int length = (int)file.length();
    final InputStream in = new FileInputStream(file);
    final ByteArrayOutputStream out = new ByteArrayOutputStream(length);
    final byte[] buf = new byte[DEFAULT_BUFFER_SIZE];
    pipe(in, out, buf);
    return out.toByteArray();
  }

  /**
   * Reads contents of {@code file} as a string.
   */
  public static String read(final File file) throws IOException {
    final Reader in = new BufferedReader(new FileReader(file));
    final int fileSize = (int) file.length();
    final StringBuilder out = new StringBuilder(fileSize);

    read(in, out);
    return out.toString();
  }

  /**
   * Reads data from {@code in} to {@code out} using a default size data buffer.
   */
  public static void read(final Reader in, final StringBuilder out) throws IOException {
    read(in, out, new char[DEFAULT_BUFFER_SIZE]);
  }

  /**
   * Read data from {@code in} into {@code out} using {@code buf} as a data buffer.
   */
  public static void read(final Reader in, final StringBuilder out, final char[] buf)
      throws IOException {
    final int bufSize = buf.length;
    int count;
    while ((count = in.read(buf, 0, bufSize)) != -1) {
      out.append(buf, 0, count);
    }
  }

  /**
   * Write data from {@code in} to {@code file} using {@code buf} as a data buffer.
   *
   * @param buf Data buffer
   * @param contentLength Bytes to transfer.  Ignored if {@code listener} is {@code null}.
   * @param listener Optional progress listener.
   */
  public static void write(final InputStream in, final File file, final byte[] buf,
      final int contentLength, final ProgressListener listener) throws IOException {
    final int bufSize = buf.length;
    final OutputStream out = new BufferedOutputStream(new FileOutputStream(file), bufSize);
    try {
      pipe(in, out, buf, contentLength, listener);
    } finally {
      tryClose(out);
    }
  }

  private final static void tryClose(final OutputStream out) {
    try {
      out.close();
    } catch (IOException ioEx) {
      ioEx.printStackTrace();
    }
  }
}

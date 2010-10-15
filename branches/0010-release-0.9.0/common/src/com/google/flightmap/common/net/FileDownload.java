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

package com.google.flightmap.common.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import com.google.flightmap.common.ProgressListener;
import com.google.flightmap.common.io.StreamUtils;

/**
 * Utility class for downloading files.
 */
public class FileDownload {
  static final int BUFFER_SIZE = 10240;

  private FileDownload() {
  }

  /**
   * <p>Downloads {@code url} to {@code destination} without altering the data.</p>
   *
   * <p>Deletes {@code destination} on failure.</p>
   * 
   * @param listener Optional progress listener.
   */
  public static void download(final URL url, final File destination, final ProgressListener listener)
      throws IOException {
    final URLConnection urlConnection = url.openConnection();
    final InputStream in = new BufferedInputStream(urlConnection.getInputStream(), BUFFER_SIZE);
    final int contentLength = urlConnection.getContentLength();

    doDownload(in, destination, contentLength, listener);
  }

  /**
   * <p>Downloads {@code url} to {@code destination} without altering the data.</p>
   *
   * <p>Deletes {@code destination} on failure.</p>
   * 
   * @param listener Optional progress listener.
   * @return true/false on success/failure.
   */
  public static boolean tryDownload(final URL url, final File destination,
      final ProgressListener listener) {
    try {
      download(url, destination, listener);
      return true;
    } catch (IOException ioEx) {
      ioEx.printStackTrace();
      return false;
    }
  }

  /**
   * <p>Downloads {@code url} to {@code destination} and gunzips data on the fly.</p>
   *
   * <p>Deletes {@code destination} on failure.</p>
   * 
   * @param contentLength Length of decompressed data, in bytes.
   * @param listener Optional progress listener.
   */
  public static void downloadGunzip(final URL url, final File destination, final int contentLength,
      final ProgressListener listener) throws IOException {
    final URLConnection urlConnection = url.openConnection();
    final InputStream in = new GZIPInputStream(urlConnection.getInputStream(), BUFFER_SIZE);
    doDownload(in, destination, contentLength, listener);
  }

  /**
   * <p>Downloads {@code url} to {@code destination} and gunzips data on the fly.</p>
   *
   * <p>Deletes {@code destination} on failure.</p>
   * 
   * @param contentLength Length of decompressed data, in bytes.
   * @param listener Optional progress listener.
   */
  public static boolean tryDownloadGunzip(final URL url, final File destination,
      final int contentLength, final ProgressListener listener) {
    try {
      downloadGunzip(url, destination, contentLength, listener);
      return true;
    } catch (IOException ioEx) {
      ioEx.printStackTrace();
      return false;
    }
  }

  /**
   * Gets contents of {@code url} as {@code String}.
   */
  public static String read(final URL url) throws IOException {
    final URLConnection urlConnection = url.openConnection();

    final InputStream inStream =
        new BufferedInputStream(urlConnection.getInputStream(), BUFFER_SIZE);
    final Reader in = new BufferedReader(new InputStreamReader(inStream));

    final int contentLength = urlConnection.getContentLength();
    final StringBuilder out = new StringBuilder(contentLength);

    final char buf[] = new char[BUFFER_SIZE];
    try {
      StreamUtils.read(in, out, buf);
      return out.toString();
    } finally {
      in.close();
    }
  }

  /**
   * <p>Provides a generic way to download content from {@code in} to {@code out}.</p>
   *
   * <p>Deletes {@code destination} on failure.</p>
   */
  private static void doDownload(final InputStream in, final File destination,
      final int contentLength, final ProgressListener listener) throws IOException {
    final byte buf[] = new byte[BUFFER_SIZE];
    try {
      StreamUtils.write(in, destination, buf, contentLength, listener);
    } catch (IOException ioEx) {
      destination.delete();
      throw ioEx;
    } finally {
      in.close();
    }
  }
}

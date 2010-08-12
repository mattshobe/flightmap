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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;

/**
 * Utility class for downloading files.
 * TODO: Add support for progress listeners, file splitting, etc.
 */
public class FileDownload {
  private static final int BUFFER_SIZE = 1024;
  private final URL url;

  /**
   * Initialized downloader for {@code url}
   */
  public FileDownload(final URL url) {
    this.url = url;
  }

  /**
   * Downloads file to {@code destination}
   */
  public void download(final File destination) throws IOException {
    final URLConnection urlConnection = url.openConnection();

    final InputStream in = new BufferedInputStream(urlConnection.getInputStream());
    final OutputStream out = new FileOutputStream(destination);
    try {
      final byte data[] = new byte[BUFFER_SIZE];
      int count;
      while ((count = in.read(data)) != -1) {
        out.write(data, 0, count);
      }
    } finally {
      in.close();
      out.close();
    }
  }

  /**
   * Returns content of file as string
   */
  public String getContentString() throws IOException {
    final URLConnection urlConnection = url.openConnection();

    final InputStream inStream = new BufferedInputStream(urlConnection.getInputStream());
    final Reader in = new BufferedReader(new InputStreamReader(inStream));

    final int contentLength = urlConnection.getContentLength();
    final StringBuilder out = new StringBuilder(contentLength);

    try {
      getContentString(in, out);
      return out.toString();
    } finally {
      in.close();
    }
  }

  /**
   * Downloads {@code url} to {@code destination}
   */
  public static void download(final URL url, final File destination) throws IOException{
    new FileDownload(url).download(destination);
  }

  /**
   * Gets contents of {@code url} as string
   */
  public static String getContentString(final URL url) throws IOException {
    return new FileDownload(url).getContentString();
  }

  /**
   * Reads content from {@code in} as string and appends it to {@code out}
   */
  static void getContentString(final Reader in, final StringBuilder out) throws IOException {
    final char data[] = new char[BUFFER_SIZE];
    int count;
    while ((count = in.read(data, 0, BUFFER_SIZE)) != -1) {
      out.append(data);
    }
  }
}

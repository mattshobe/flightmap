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
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;

/**
 * Utility class for downloading files.
 * TODO: Add support for progress listeners, file splitting, etc.
 */
public class FileDownload {
  private final static int BUFFER_SIZE = 1024;
  private final URL url;

  /**
   * Prepares downloader for {@code url}
   */
  public FileDownload(final String url) throws MalformedURLException {
    this.url = new java.net.URL(url);
  }

  /**
   * Downloads file to {@code destination}
   */
  synchronized public void download(final String destination) throws IOException {
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
   * Downloads {@code url} to {@code destination}
   */
  public static void download(final String url, final String destination) throws IOException {
    new FileDownload(url).download(destination);
  }
}


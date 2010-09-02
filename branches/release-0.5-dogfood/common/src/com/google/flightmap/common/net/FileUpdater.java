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
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.flightmap.common.ProgressListener;
import com.google.flightmap.common.io.StreamUtils;

/**
 * <p>Provides a way to update a local file with a remote file. The local file is a
 * possibly outdated copy of the remote file. For each file, there must exist a
 * metadata file with the same name and an ".info" suffix. The local file is
 * considered outdated if the local info file and the remote info file do not
 * match. A successful update will always update the local info file
 * accordingly.</p>
 *
 * <p>The remote file can also be split into multiple files.  The latter must be
 * listed with their size (in bytes) first in a file with a ".parts" suffix.
 * Each part file can (but doesn't need to) be gzipped, in which case it must
 * have ".gz" appended to its name.</p>
 *
 * <p>Besides compression, splitting files has the advantage of reducing unecessary
 * retransmissions: if a transmission failure occurs, the following attempts
 * will not download part files that were succesfully downloaded previously.</p>
 */
public class FileUpdater {
  /** Suffix of info files for both local and remote files. */
  private static final String INFO_SUFFIX = ".info";
  private static final String PARTS_SUFFIX = ".parts";

  private final File localFile;
  private final File localInfoFile;
  private final URL remoteUrl;
  private final URL remoteInfoUrl;
  private final URL remotePartsUrl;
  private final File workingDir;

  private LinkedHashMap<String, Integer> parts;
  private int totalBytes;
  private int bytesDownloaded;
  private int lastPercentNotified;

  private List<ProgressListener> listeners = new LinkedList<ProgressListener>();

  /**
   * Initializes updater with local resource {@code localFile} and remote
   * resource {@code remoteUrl}.
   *
   * @param localFile Path of local file.
   * @param remoteUrl URL of remote file that will be used to update local
   *        {@code localFile}
   * @param workingDir Path of local working directory.
   */
  public FileUpdater(final File localFile, final URL remoteUrl, final File workingDir)
      throws IOException {
    this.localFile = localFile;
    localInfoFile = new File(localFile.getPath() + INFO_SUFFIX);
    this.remoteUrl = remoteUrl;
    remoteInfoUrl = new URL(remoteUrl.toString() + INFO_SUFFIX);
    remotePartsUrl = new URL(remoteUrl.toString() + PARTS_SUFFIX);
    this.workingDir = workingDir;
  }

  /**
   * <p>Checks if the local file needs to be updated. This is determined based on
   * the info file contents.</p>
   *
   * <p>See class description.</p>
   */
  public synchronized boolean isUpdateNeeded() throws IOException {
    if (!localFile.exists() || !localInfoFile.exists()) {
      return true;
    }

    return !contentsMatch(localInfoFile, remoteInfoUrl);
  }

  /**
   * Checks if info {@code file} matches the latest info from remote URL.
   */
  private static boolean contentsMatch(final File file, final URL url) throws IOException {
    final String local = StreamUtils.read(file);
    final String remote = FileDownload.read(url);
    return local.equals(remote);
  }

  /**
   * Updates the local file iff needed.
   *
   * @return {@code true} if an update was needed.
   */
  public synchronized boolean update() throws IOException {
    boolean success = false;
    try {
      final boolean updateNeeded = isUpdateNeeded();
      if (updateNeeded) {
        doUpdate();
      }
      success = true;
      return updateNeeded;
    } finally {
      notifyUpdateCompleted(success);
    }
  }

  /**
   * Adds a listener to notify of progress.
   */
  public synchronized void addProgressListener(ProgressListener listener) {
    listeners.add(listener);
  }

  /**
   * Forces an update of the local file (regardless of whether it is needed).
   */
  private synchronized void doUpdate() throws IOException {
    final File workingInfoFile = new File(workingDir, localInfoFile.getName());
    final File workingFile = new File(workingDir, localFile.getName());

    // Check if existing working info file matches latest info file, clean working dir if not.
    if (workingInfoFile.exists() && !contentsMatch(workingInfoFile, remoteInfoUrl)) {
      cleanWorkingDir();
    }

    // Create working dir if needed.
    checkWorkingDir();

    // Download latest info file to working directory if needed.
    if (!workingInfoFile.exists()) {
      FileDownload.download(remoteInfoUrl, workingInfoFile, null);
    }

    // See if parts file is available.
    final File workingPartsFile = new File(workingDir, localFile.getName() + PARTS_SUFFIX);
    if (FileDownload.tryDownload(remotePartsUrl, workingPartsFile, null)) {
      parsePartsFile(workingPartsFile);
      downloadInParts(workingFile);
    } else {
      FileDownload.download(remoteUrl, workingFile, new ProgressListener() {
        @Override
        public void hasProgressed(int percent) {
          notifyUpdateProgress(percent);
        }

        @Override
        public void hasCompleted(boolean success) {
          // Completion is notified by update()
        }
      });
    }

    try {
      renameFile(workingFile, localFile);
      renameFile(workingInfoFile, localInfoFile);
      cleanWorkingDir();
    } catch (IOException e) {
      workingFile.delete();
      workingInfoFile.delete();
      throw e;
    }
  }

  /**
   * Removes files relaed to the download of parts files.
   */
  private void cleanWorkingDir() {
    for (File file: workingDir.listFiles()) {
      file.delete();
    }
    workingDir.delete();
  }

  /**
   * <p>Download parts and combine them in {@code file}.</p>
   * <p>Deletes the later on failure but leaves succesfully downloaded partial files intact.</p>
   */
  private void downloadInParts(final File file) throws IOException {
    final byte[] buf = new byte[FileDownload.BUFFER_SIZE];
    final OutputStream out =
        new BufferedOutputStream(new FileOutputStream(file), FileDownload.BUFFER_SIZE);

    try {
      for (Map.Entry<String, Integer> partEntry: parts.entrySet()) {
        final String partFilename = partEntry.getKey();
        final int partFileSize = partEntry.getValue().intValue();

        final URL remotePartUrl = new URL(remoteUrl, partFilename);
        final File localPartFile = new File(workingDir, partFilename);

        downloadPart(remotePartUrl, localPartFile, partFileSize);

        final InputStream in =
            new BufferedInputStream(new FileInputStream(localPartFile), FileDownload.BUFFER_SIZE);

        try {
          StreamUtils.pipe(in, out, buf);
        } finally {
          try {
            in.close();
          } catch (IOException ioEx) {
            ioEx.printStackTrace();
          }
        }
      }
    } catch (IOException ioEx) {
      file.delete();
      throw ioEx;
    }
  }

  /**
   * <p>Downloads partial file from {@code url} to {@code file}.</p>
   *
   * <p>If {@code file} already exists and has {@code size} bytes, download is skipped.
   * Otherwise, an attempt to download a gzipped file will be made.  If it fails, the plain file
   * will be downloaded.</p>
   */
  private void downloadPart(final URL url, final File file, final int size) throws IOException {
    if (file.exists() && file.length() == size) {
      addDownloadedBytes(size);
      return;
    }

    try {
      downloadGzippedPart(url, file, size);
    } catch (FileNotFoundException fnfEx) {
      downloadPlainPart(url, file, size);
    }
  }

  /**
   * Appends .gz to {@code url} and downloads with on-the-fly decompression to {@code file}.
   */
  private void downloadGzippedPart(final URL url, final File file,
                                   final int size) throws IOException {
    final URL gzUrl = new URL(url, url.getPath() + ".gz");
    FileDownload.downloadGunzip(gzUrl, file, size, new PartDownloadProgressListener(size));
  }

  /**
   * Download plain partial file from {@code url} to {@code file}.
   */
  private void downloadPlainPart(final URL url, final File file,
                                 final int size) throws IOException {
    FileDownload.download(url, file, new PartDownloadProgressListener(size));
  }
  

  private class PartDownloadProgressListener implements ProgressListener {
    final int size;
    int bytesDownloaded;

    PartDownloadProgressListener(final int size) {
      this.size = size;
    }

    @Override
    public synchronized void hasProgressed(int percent) {
      final int newBytesDownloaded = (int)(percent / 100.0 * size);
      final int downloadedDiff = newBytesDownloaded - bytesDownloaded;
      if (downloadedDiff > 0) {
        addDownloadedBytes(downloadedDiff);
        bytesDownloaded = newBytesDownloaded;
      }
    }

    @Override
    public synchronized void hasCompleted(boolean success) {
      if (bytesDownloaded != size) {
        addDownloadedBytes(size - bytesDownloaded);
      }
    }
  }

  /**
   * Adds {@code bytes} to the total number of bytes downloaded and notifies listeners.
   */
  private synchronized void addDownloadedBytes(final int bytes) {
    bytesDownloaded += bytes;
    final int percent = (int)(100.0 * bytesDownloaded / totalBytes + 0.5);
    notifyUpdateProgress(percent);
  }

  /**
   * Parses parts data from {@code file}.
   */
  private void parsePartsFile(final File file) throws IOException {
    parts = new LinkedHashMap<String, Integer>();
    final String partsContent = StreamUtils.read(file);
    final Pattern pattern = Pattern.compile("^(\\d+)\\s+(\\S+)$");

    final BufferedReader in = new BufferedReader(new StringReader(partsContent));
    String line;
    while ((line = in.readLine()) != null) {
      final Matcher matcher = pattern.matcher(line);
      if (!matcher.matches()) {
        continue;
      }
      final Integer bytes = Integer.valueOf(matcher.group(1));
      final String filename = matcher.group(2);

      addPart(filename, bytes);
    }
  }

  /**
   * Checks that the working directory exists or creates one.
   *
   * @throws IOException  Creation of the working directory failed.
   */
  private void checkWorkingDir() throws IOException {
    if (workingDir.exists()) {
      if (!workingDir.isDirectory()) {
        throw new IllegalArgumentException(
            "Working directory conflicts with existing file: " + workingDir.getPath());
      }
      return;
    }

    // Can only get here if the working directory does not exist.
    if (!workingDir.mkdirs()) {
      throw new IOException("Could not create working directory: " + workingDir.getPath());
    }
  }

  /**
   * Registers a new part.
   *
   * @param filename  Relative path of this part.
   * @param bytes  Size of part.
   */
  private synchronized void addPart(final String filename, final Integer bytes) {
    parts.put(filename, bytes);
    totalBytes += bytes;
  }

  private synchronized void notifyUpdateCompleted(boolean success) {
    notifyUpdateProgress(100);
    for (ProgressListener listener : listeners) {
      listener.hasCompleted(success);
    }
  }

  private synchronized void notifyUpdateProgress(int percent) {
    if (lastPercentNotified == percent) {
      return;
    }
    for (ProgressListener listener: listeners) {
      listener.hasProgressed(percent);
    }
    lastPercentNotified = percent;
  }

  /**
   * Attempts to rename {@code source} to {@code dest}.
   *
   * @throws IOException File rename failed.
   */
  private static void renameFile(final File source, final File dest) throws IOException {
    if (!source.renameTo(dest)) {
      throw new IOException("Could not rename file: " + source.getPath() + " -> " + dest.getPath());
    }
  }
}

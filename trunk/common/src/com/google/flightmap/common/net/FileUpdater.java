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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;

/**
 * Provides a way to update a local file with a remote file.
 * The local file is a possibly outdated copy of the remote file.  For each file, there
 * must exist a metadata file with the same name and an ".info" suffix.  The local file is
 * considered outdated if the local info file and the remote info file do not match.
 * A successful update will always update the local info file accordingly.
 */
public class FileUpdater {
  /** Suffix of info files for both local and remote files. */
  private static final String INFO_SUFFIX = ".info";

  private final File localFile;
  private final File localInfoFile;
  private final URL remoteFile;
  private final URL remoteInfoFile;

  /**
   * Initializes updater with local resource {@code localFile} and remote
   * resource {@code remoteFile}.
   * @param localFile Path of local file.
   * @param remoteFile URL of remote file that will be used to update local {@code localFile}
   */
  public FileUpdater(final File localFile, final URL remoteFile) throws IOException {
    this.localFile = localFile;
    localInfoFile = new File(localFile.getPath() + INFO_SUFFIX);
    this.remoteFile = remoteFile;
    remoteInfoFile = new URL(remoteFile.toString() + INFO_SUFFIX);
  }

  /**
   * Reads the content of the local info file.
   */
  private String getLocalInfoContentString() throws IOException {
    final Reader in = new BufferedReader(new FileReader(localInfoFile));
    final StringBuilder out = new StringBuilder();
    FileDownload.getContentString(in, out);
    return out.toString();
  }

  /**
   * Checks if the local file needs to be updated.
   * This is determined based on the info file contents.
   * @see FileUpdater
   */
  public boolean isUpdateNeeded() throws IOException {
    if (!localFile.exists() || !localInfoFile.exists()) {
      return true;
    }

    final String remoteInfo = FileDownload.getContentString(remoteInfoFile);
    try {
      final String localInfo = getLocalInfoContentString();
      return !remoteInfo.equals(localInfo);
    } catch (IOException ioEx) {
      return true;
    }
  }

  /**
   * Forces an update of the local file (regardless of whether it is needed).
   */
  public void forceUpdate() throws IOException {
    final File newInfoFile = File.createTempFile(localInfoFile.getName(), "new");
    final File newFile = File.createTempFile(localFile.getName(), "new");
    FileDownload.download(remoteInfoFile, newInfoFile);
    FileDownload.download(remoteFile, newFile);
    renameFile(newFile, localFile);
    renameFile(newInfoFile, localInfoFile);
  }

  /**
   * Updates the local file iff needed.
   */
  public boolean update() throws IOException {
    final boolean updateNeeded = isUpdateNeeded();
    if (updateNeeded) {
      forceUpdate();
    }
    return updateNeeded;
  }

  /**
   * Attempts to rename {@code source} to {@code dest}.
   * @throws RuntimeException File rename failed.
   */
  private static void renameFile(final File source, final File dest) {
    if (!source.renameTo(dest)) {
      throw new RuntimeException(
          "Could not rename file: " + source.getPath() + " -> " + dest.getPath());
    } 
  }
}

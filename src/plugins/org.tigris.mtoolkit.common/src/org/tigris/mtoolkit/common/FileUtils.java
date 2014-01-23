/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class FileUtils {

  private FileUtils() {
  }

  /**
   * Extracts zip file.
   *
   * @param zipFile
   *          the zip file to extract
   * @param destinationDir
   *          the destination directory where to extract the contents of zip
   *          file
   * @throws IOException
   */
  public static void extractZip(File zipFile, File destinationDir) throws IOException {
    ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
    try {
      ZipEntry zipEntry = zis.getNextEntry();
      destinationDir.mkdirs();

      while (zipEntry != null) {
        String entryName = zipEntry.getName();
        File file = new File(destinationDir.getAbsolutePath() + "/" + entryName);
        if (zipEntry.isDirectory()) {
          file.mkdirs();
        } else {
          file.getParentFile().mkdirs();
          byte[] buf = new byte[1024];
          FileOutputStream outputStream = new FileOutputStream(file);
          try {
            int len;
            while ((len = zis.read(buf)) > 0) {
              outputStream.write(buf, 0, len);
            }
          } finally {
            close(outputStream);
          }
        }
        zis.closeEntry();
        zipEntry = zis.getNextEntry();
      }
    } finally {
      close(zis);
    }
  }

  /**
   * Makes zip file.
   *
   * @param src
   *          the source file or directory
   * @param zipFile
   *          the result file
   * @throws IOException
   */
  public static void makeZip(File src, File zipFile) throws IOException { // NO_UCD
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
    try {
      addToZip(src, src.isDirectory() ? "" : src.getName(), zos);
    } finally {
      close(zos);
    }
  }

  public static void archiveDirectory(File archive, File directory) throws IOException { // NO_UCD
    archiveDirectory(archive, directory, null);
  }

  public static void archiveDirectory(File archive, File directory, String prefix) throws IOException {
    FileOutputStream out = new FileOutputStream(archive);
    try {
      ZipOutputStream zip = new ZipOutputStream(out);
      addFileToArchive(directory, directory, zip, getPrefix(prefix));
      zip.close();
    } finally {
      close(out);
    }
  }

  private static String getPrefix(String originalPrefix) {
    // check for empty prefix
    if (originalPrefix == null || "".equals(originalPrefix)) {
      return "";
    }
    // if there is a leading slash, remove it
    if (originalPrefix.startsWith("/")) {
      if (originalPrefix.length() == 1) {
        return "";
      }
      originalPrefix = originalPrefix.substring(1);
    }
    if (!originalPrefix.endsWith("/")) {
      originalPrefix = originalPrefix.concat("/");
    }
    return originalPrefix;
  }

  private static void addFileToArchive(File archiveBase, File file, ZipOutputStream zip, String prefix)
      throws IOException {
    if (file.isFile()) {
      String name = getEntryName(archiveBase, file, prefix);
      FileInputStream input = new FileInputStream(file);
      try {
        addStreamToArchive(name, input, zip);
      } finally {
        close(input);
      }
    } else {
      File[] files = file.listFiles();
      if (files == null || files.length == 0) {
        return;
      }
      for (int i = 0; i < files.length; i++) {
        addFileToArchive(archiveBase, files[i], zip, prefix);
      }
    }
  }

  private static void addStreamToArchive(String name, InputStream in, ZipOutputStream zip) throws IOException {
    ZipEntry newEntry = new ZipEntry(name);
    zip.putNextEntry(newEntry);
    byte[] buf = new byte[4096];
    int read;
    while ((read = in.read(buf)) != -1) {
      zip.write(buf, 0, read);
    }
  }

  private static String getEntryName(File archiveBase, File file, String prefix) throws IOException {
    String relativePath = getRelativePath(archiveBase, file);
    return prefix.concat(relativePath);
  }

  private static String getRelativePath(File base, File file) throws IOException {
    String basePath = base.getAbsolutePath();
    String filePath = file.getAbsolutePath();
    if (!filePath.startsWith(basePath)) {
      throw new IOException("Cannot add file to archive from different directory: " + filePath);
    }
    String relativePath = filePath.substring(basePath.length());
    relativePath = relativePath.replace(File.separatorChar, '/');
    if (relativePath.startsWith("/")) {
      relativePath = relativePath.substring(1);
    }
    return relativePath;
  }

  private static void addToZip(File file, String name, ZipOutputStream zos) throws IOException {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null) {
        for (int i = 0; i < files.length; i++) {
          String childName = (name.length() == 0) ? files[i].getName() : name + "/" + files[i].getName();
          addToZip(files[i], childName, zos);
        }
      }
    } else {
      FileInputStream in = new FileInputStream(file);
      try {
        zos.putNextEntry(new ZipEntry(name));
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
          zos.write(buf, 0, len);
        }
        zos.closeEntry();
      } finally {
        close(in);
      }
    }
  }

  /**
   * Deletes all files and subdirectories under dir. Returns true if all
   * deletions were successful. If a deletion fails, the method stops attempting
   * to delete and returns false.
   *
   * @param file
   * @return
   */
  public static boolean delete(File file) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children == null) {
        return false;
      }
      for (int i = 0; i < children.length; i++) {
        boolean success = delete(children[i]);
        if (!success) {
          return false;
        }
      }
    }
    return file.delete();
  }

  /**
   * Returns file extension in lower case.
   *
   * @param file
   * @return
   */
  public static String getFileExtension(File file) {
    String name = file.getName();
    return name.substring(name.lastIndexOf('.') + 1, name.length()).toLowerCase();
  }

  public static File saveFile(File file, InputStream input) throws IOException {
    if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
      throw new IOException("Failed to create " + file);
    }
    FileOutputStream stream = null;
    try {
      stream = new FileOutputStream(file);
      byte[] buf = new byte[4096];
      int read;
      while ((read = input.read(buf)) != -1) {
        stream.write(buf, 0, read);
      }
    } finally {
      close(stream);
    }
    return file;
  }

  public static void close(Closeable... streams) {
    for (Closeable c : streams) {
      if (c == null) {
        continue;
      }
      try {
        c.close();
      } catch (IOException e) {
        //This exception is to be ignored
      }
    }
  }

  public static void close(ZipFile... zips) {
    for (ZipFile zip : zips) {
      if (zip == null) {
        continue;
      }
      try {
        zip.close();
      } catch (IOException e) {
        //This exception is to be ignored
      }
    }
  }

  public static void copy(InputStream in, OutputStream out) throws IOException {
    byte[] buf = new byte[2048];
    int read = -1;
    while ((read = in.read(buf)) != -1) {
      out.write(buf, 0, read);
    }
  }
}

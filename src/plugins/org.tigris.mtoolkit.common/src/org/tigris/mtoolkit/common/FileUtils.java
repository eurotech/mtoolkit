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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtils {

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
            outputStream.close();
          }
        }
        zis.closeEntry();
        zipEntry = zis.getNextEntry();
      }
    } finally {
      zis.close();
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
  public static void makeZip(File src, File zipFile) throws IOException {
    ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
    try {
      addToZip(src, src.isDirectory() ? "" : src.getName(), zos);
    } finally {
      zos.close();
    }
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
        in.close();
      }
    }
  }

  /**
   * Deletes all files and subdirectories under dir. Returns true if all
   * deletions were successful. If a deletion fails, the method stops attempting
   * to delete and returns false.
   * 
   * @param dir
   * @return
   */
  public static boolean deleteDir(File dir) {
    if (dir.isDirectory()) {
      File[] children = dir.listFiles();
      for (int i = 0; i < children.length; i++) {
        boolean success = deleteDir(children[i]);
        if (!success) {
          return false;
        }
      }
    }
    return dir.delete();
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
}

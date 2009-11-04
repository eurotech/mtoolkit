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
package org.tigris.mtoolkit.common.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Constants;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.ProcessOutputReader;
import org.tigris.mtoolkit.common.UtilitiesPlugin;

public class AndroidUtils {

  /**
   * Converts jar file to dex format.
   * 
   * @param file
   *          the file to be converted.
   * @param outputFile
   *          the result file. The file extension of outputFile must be one of
   *          supported by dx tool.
   * @param monitor
   *          the progress monitor.
   * @throws IOException
   *           in case of error
   */
  public static void convertToDex(File file, File outputFile, IProgressMonitor monitor) throws IOException {
    String androidSDK = getAndroidSdkLocation();
    String androidVersion = getAndroidVersion();

    String dxTool = MessageFormat.format("{0}/platforms/{1}/tools/lib/dx.jar", new String[] { androidSDK,
        androidVersion });
    File dxToolFile = new File(dxTool);
    if (!dxToolFile.exists()) {
      throw new IOException("Unable to find dx tool at " + dxToolFile.getAbsolutePath());
    }

    List command = new ArrayList();
    command.add("java");
    command.add("-jar");
    command.add(dxToolFile.getAbsolutePath());
    command.add("--dex");
    command.add("--output=" + outputFile.getAbsolutePath());
    command.add(file.getAbsolutePath());

    Process process;
    try {
      process = Runtime.getRuntime().exec((String[]) command.toArray(new String[command.size()]));
    } catch (IOException ioe) {
      IOException e = new IOException("Cannot convert to dex format. Error occured while executing dx tool.");
      e.initCause(ioe);
      throw e;
    }
    ProcessOutputReader outputReader = new ProcessOutputReader(process.getInputStream(), "[dx tool] Output Reader");
    outputReader.start();
    ProcessOutputReader errorReader = new ProcessOutputReader(process.getErrorStream(), "[dx tool] Error Reader");
    errorReader.start();

    int retries = 150;
    int result = -1;
    while (!monitor.isCanceled()) {
      try {
        Thread.sleep(200);
        result = process.exitValue();
        break;
      } catch (IllegalThreadStateException itse) {
        if (--retries == 0) {
          break;
        }
      } catch (InterruptedException e) {
        retries = 0;
        break;
      }
    }
    outputReader.stopReader();
    errorReader.stopReader();

    // check whether process is canceled by user or just has finished
    if (monitor.isCanceled()) {
      process.destroy();
      if (result != 0) {
        throw new IOException("Converting operation was cancelled.");
      }
    }
    if (result != 0 || !outputFile.exists()) {
      if (retries == 0) {
        throw new IOException("Cannot convert to dex format. Operation timed out.");
      } else {
        throw new IOException("Cannot convert to dex format. \ndx tool output: '" + outputReader.getOutput()
            + "'\ndx tool error: '" + errorReader.getOutput() + "'");
      }
    }
  }

  /**
   * Converts dp file to android dex format
   * 
   * @param dpFile
   * @param outputFile
   * @param monitor
   * @throws IOException
   */
  public static void convertDpToDex(File dpFile, File outputFile, IProgressMonitor monitor) throws IOException {
    File tmpDir = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/tmp.extracted");
    FileUtils.deleteDir(tmpDir);
    File dexDir = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/tmp.dex");
    FileUtils.deleteDir(dexDir);

    JarInputStream jis = null;
    JarOutputStream jos = null;
    try {
      jis = new JarInputStream(new FileInputStream(dpFile));
      Manifest manifest = jis.getManifest();
      if (manifest == null) {
        throw new IOException("DP file has no manifest.");
      }
      jos = new JarOutputStream(new FileOutputStream(outputFile), manifest);

      byte[] buf = new byte[1024];
      int len;
      JarEntry jarEntry;
      while ((jarEntry = jis.getNextJarEntry()) != null) {
        JarEntry newEntry = new JarEntry(jarEntry.getName());
        newEntry.setTime(jarEntry.getTime());
        newEntry.setExtra(jarEntry.getExtra());
        newEntry.setComment(jarEntry.getComment());
        jos.putNextEntry(newEntry);

        Attributes attributes = jarEntry.getAttributes();
        if (attributes != null && attributes.getValue(Constants.BUNDLE_SYMBOLICNAME) != null) {
          // this entry is bundle - convert it to dex format
          String entryName = jarEntry.getName();
          File file = new File(tmpDir.getAbsolutePath() + "/" + entryName);
          file.getParentFile().mkdirs();
          FileOutputStream outputStream = new FileOutputStream(file);
          try {
            while ((len = jis.read(buf)) > 0) {
              outputStream.write(buf, 0, len);
            }
          } finally {
            try {
              outputStream.close();
            } catch (IOException e) {
            }
          }
          File dexFile = file;
          if (!isConvertedToDex(file)) {
            dexFile = new File(dexDir.getAbsolutePath() + "/" + entryName);
            dexFile.getParentFile().mkdirs();
            convertToDex(file, dexFile, monitor);
          }

          FileInputStream dexIn = new FileInputStream(dexFile);
          try {
            while ((len = dexIn.read(buf)) > 0) {
              jos.write(buf, 0, len);
            }
          } finally {
            try {
              dexIn.close();
            } catch (IOException e) {
            }
          }
        } else {
          // entry is not a bundle - put it unchanged
          while ((len = jis.read(buf)) > 0) {
            jos.write(buf, 0, len);
          }
        }
        jis.closeEntry();
        jos.closeEntry();
      }
    } finally {
      if (jis != null) {
        try {
          jis.close();
        } catch (IOException e) {
        }
      }
      if (jos != null) {
        try {
          jos.close();
        } catch (IOException e) {
        }
      }
    }
  }

  private static String getAndroidSdkLocation() {
    ScopedPreferenceStore preferenceStore = new ScopedPreferenceStore(new InstanceScope(), "com.prosyst.tools.android");
    return preferenceStore.getString("android.sdk.location");
  }

  private static String getAndroidVersion() {
    return "android-1.5"; // TODO read from preferences?
  }

  /**
   * Checks if file is in android dex format.
   * @param file
   * @return
   */
  public static boolean isConvertedToDex(File file) {
    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(file);
      Enumeration zipEntries = zipFile.entries();
      while (zipEntries.hasMoreElements()) {
        if ("classes.dex".equalsIgnoreCase(((ZipEntry) zipEntries.nextElement()).getName())) {
          return true;
        }
      }
    } catch (IOException ex) {
      return false;
    } finally {
      if (zipFile != null) {
        try {
          zipFile.close();
        } catch (IOException e) {
        }
      }
    }
    return false;
  }
}

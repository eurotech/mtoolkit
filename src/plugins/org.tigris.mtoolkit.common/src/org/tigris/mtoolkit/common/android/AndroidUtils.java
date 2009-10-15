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
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
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
    FileUtils.extractZip(dpFile, tmpDir);
    processDpToDexDir(tmpDir, monitor);
    FileUtils.makeZip(tmpDir, outputFile);
  }

  private static void processDpToDexDir(File dir, IProgressMonitor monitor) throws IOException {
    File[] children = dir.listFiles();
    for (int i = 0; i < children.length; i++) {
      if (children[i].isDirectory()) {
        processDpToDexDir(children[i], monitor);
      } else {
        String ext = FileUtils.getFileExtension(children[i]);
        if (ext.equals("zip") || ext.equals("jar")) {
          AndroidUtils.convertToDex(children[i], children[i], monitor);
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
}

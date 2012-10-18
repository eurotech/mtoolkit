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
import java.util.Arrays;
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

  private static final String[] DEX_COMPATIBLE_EXTENSIONS = new String[] { ".jar", ".zip" };

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
    File dxToolFile = getDxToolLocation();

    if (dxToolFile == null)
      throw new IOException("Unable to find DEX tool. Check whether Android SDK location is set in preferences.");

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
    FileUtils.delete(tmpDir);
    File dexDir = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/tmp.dex");
    FileUtils.delete(dexDir);

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
          // TODO: No need to use the jarEntry.getName() as a name for
          // the temporary file
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

  /**
   * Returns Android SDK location or null if location is not set in preferences.
   * 
   * @return
   * @since 6.0
   */
  public static String getAndroidSdkLocation() {
    @SuppressWarnings("deprecation")
	ScopedPreferenceStore preferenceStore = new ScopedPreferenceStore(new InstanceScope(),
        "com.android.ide.eclipse.adt");
    return preferenceStore.getString("com.android.ide.eclipse.adt.sdk");
  }

  /**
   * Returns platforms for the specified android sdk location in ascending order
   * of versions.
   * 
   * @param sdkLocation
   * @return array with platforms or empty array
   * @since 5.0
   */
  public static String[] getAndroidPlatforms(String sdkLocation) {
    if (sdkLocation == null) {
      return new String[0];
    }
    File platformsDir = new File(sdkLocation, "platforms");
    if (!platformsDir.exists() || !platformsDir.isDirectory()) {
      return new String[0];
    }
    File[] children = platformsDir.listFiles();
    String[] platforms = new String[children.length];
    for (int i = 0; i < children.length; i++) {
      platforms[i] = children[i].getName();
    }
    Arrays.sort(platforms);
    return platforms;
  }

  /**
   * Checks if file is in android dex format.
   * 
   * @param file
   * @return
   */
  public static boolean isConvertedToDex(File file) {
    ZipFile zipFile = null;
    try {
      zipFile = new ZipFile(file);
      // TODO: ZipFile.getEntry() can be used directly for this
      // functionality
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

  /**
   * Checks if dp file is in android dex format.
   * 
   * @param dpFile
   * @since 5.0
   */
  public static boolean isDpConvertedToDex(File dpFile) {
    File tmpDir = new File(UtilitiesPlugin.getDefault().getStateLocation().toFile(), "tmp.extracted");
    FileUtils.delete(tmpDir);

    JarInputStream jis = null;
    try {
      jis = new JarInputStream(new FileInputStream(dpFile));
      byte[] buf = new byte[1024];
      JarEntry jarEntry;
      while ((jarEntry = jis.getNextJarEntry()) != null) {
        Attributes attributes = jarEntry.getAttributes();
        if (attributes != null && attributes.getValue(Constants.BUNDLE_SYMBOLICNAME) != null) {
          // this entry is bundle - check it for dex format
          String entryName = jarEntry.getName();
          File file = new File(tmpDir, entryName);
          file.getParentFile().mkdirs();
          FileOutputStream os = new FileOutputStream(file);
          try {
            int len;
            while ((len = jis.read(buf)) > 0) {
              os.write(buf, 0, len);
            }
          } finally {
            try {
              os.close();
            } catch (IOException e) {
            }
          }
          if (!isConvertedToDex(file)) {
            return false;
          }
        }
        jis.closeEntry();
      }
    } catch (IOException ioe) {
      return false;
    } finally {
      if (jis != null) {
        try {
          jis.close();
        } catch (IOException e) {
        }
      }
    }
    return true;
  }

  /**
   * Checks whether the file is compatible with Android's dex format.
   * 
   * <p>
   * Android 'dx' tool has limitations on the output format of the files: only
   * <em>zip</em> and <em>jar</em> files are supported as output. This method
   * returns whether the file can be converted to dex format without special
   * handling or options.
   * </p>
   * 
   * @param file
   *          the file to be checked for compatibility
   * @return whether the file can be converted to dex format without additional
   *         processing or options
   */
  public static boolean isDexCompatible(File file) {
    for (int i = 0; i < DEX_COMPATIBLE_EXTENSIONS.length; i++) {
      if (file.getName().endsWith(DEX_COMPATIBLE_EXTENSIONS[i]))
        return true;
    }
    return false;
  }

  /**
   * Returns location of dx tool or null if this tool cannot be found.
   * 
   * @return
   */
  private static File getDxToolLocation() {
    String androidSDK = getAndroidSdkLocation();

    File dxToolFile = null;
    String dxTool = null;
    dxTool = MessageFormat.format("{0}/platform-tools/lib/dx.jar", new Object[] { androidSDK });
    dxToolFile = new File(dxTool);
    if (dxToolFile.exists()) {
      return dxToolFile;
    }
    String[] androidPlatforms = getAndroidPlatforms(androidSDK);
    for (int i = 0; i < androidPlatforms.length; i++) {
      String androidPlatform = androidPlatforms[i];
      dxTool = MessageFormat.format("{0}/platforms/{1}/tools/lib/dx.jar", new Object[] { androidSDK, androidPlatform });
      dxToolFile = new File(dxTool);
      if (dxToolFile.exists()) {
        return dxToolFile;
      }
    }
    return null;
  }
}

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
package org.tigris.mtoolkit.common.certificates;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.ProcessOutputReader;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.gui.PasswordDialog;
import org.tigris.mtoolkit.common.installation.InstallationConstants;

public class CertUtils {
  private static ServiceTracker certProviderTracker;

  private static final String DT = "."; //$NON-NLS-1$

  /**
   * A convenient method which delegates to
   * ICertificateProvider.getCertificates() method if ICertificateProvider
   * service is registered. Otherwise returns null.
   * 
   * @return the certificates provided or null
   * @see ICertificateProvider
   */
  public static ICertificateDescriptor[] getCertificates() {
    ICertificateProvider provider = getCertProvider();
    if (provider != null) {
      return provider.getCertificates();
    }
    return null;
  }

  /**
   * A convenient method which delegates to
   * ICertificateProvider.getCertificate() method if ICertificateProvider
   * service is registered. Otherwise returns null.
   * 
   * @param uid
   *          the uid of certificate to find
   * @return the certificate or null
   * @see ICertificateProvider
   */
  public static ICertificateDescriptor getCertificate(String uid) {
    ICertificateProvider provider = getCertProvider();
    if (provider != null) {
      return provider.getCertificate(uid);
    }
    return null;
  }

  private static ICertificateProvider getCertProvider() {
    if (certProviderTracker == null) {
      certProviderTracker = new ServiceTracker(UtilitiesPlugin.getDefault().getBundleContext(),
          ICertificateProvider.class.getName(), null);
      certProviderTracker.open();
    }
    return (ICertificateProvider) certProviderTracker.getService();
  }

  /**
   * Adds certificate data for signing content to provided properties.
   * 
   * @param properties
   *          the Map to push in certificate data
   * @param cert
   *          the certificate to push
   * @param id
   *          the id to append to each key that is inserted to properties
   */
  public static void pushCertificate(Map properties, ICertificateDescriptor cert, int id) {
    if (cert.getAlias() != null) {
      properties.put(InstallationConstants.CERT_ALIAS + DT + id, cert.getAlias());
    }
    if (cert.getStoreLocation() != null) {
      properties.put(InstallationConstants.CERT_STORE_LOCATION + DT + id, cert.getStoreLocation());
    }
    if (cert.getStoreType() != null) {
      properties.put(InstallationConstants.CERT_STORE_TYPE + DT + id, cert.getStoreType());
    }
    if (cert.getStorePass() != null) {
      properties.put(InstallationConstants.CERT_STORE_PASS + DT + id, cert.getStorePass());
    }
    if (cert.getKeyPass() != null) {
      properties.put(InstallationConstants.CERT_KEY_PASS + DT + id, cert.getKeyPass());
    }
  }

  public static int getCertificatesCount(Map properties) {
    Set keys = properties.keySet();
    Iterator iterator = keys.iterator();
    int count = 0;
    while (iterator.hasNext()) {
      String key = (String) iterator.next();
      if (key.startsWith(InstallationConstants.CERT_ALIAS + DT)) {
        count++;
      }
    }
    return count;
  }

  public static String getCertificateAlias(Map properties, int id) {
    return (String) properties.get(InstallationConstants.CERT_ALIAS + DT + id);
  }

  public static String getCertificateStoreLocation(Map properties, int id) {
    return (String) properties.get(InstallationConstants.CERT_STORE_LOCATION + DT + id);
  }

  public static String getCertificateStoreType(Map properties, int id) {
    return (String) properties.get(InstallationConstants.CERT_STORE_TYPE + DT + id);
  }

  public static String getCertificateStorePass(Map properties, int id) {
    return (String) properties.get(InstallationConstants.CERT_STORE_PASS + DT + id);
  }

  public static String getCertificateKeyPass(Map properties, int id) {
    return (String) properties.get(InstallationConstants.CERT_KEY_PASS + DT + id);
  }

  /**
   * Signs provided file with passed information for signing.
   * 
   * @param jarName
   *          the file to sign
   * @param signedJar
   *          the path to the new signed file or null. If this is null changes
   *          will be made directly to 'jarName' file.
   * @param monitor
   *          the progress monitor. Cannot be null.
   * @param alias
   *          the alias. Cannot be null.
   * @param storeLocation
   *          the key store location. Cannot be null.
   * @param storeType
   *          the key store type. Cannot be null.
   * @param storePass
   *          the key store pass. Cannot be null.
   * @param keyPass
   *          the key store pass. If null then the same pass as store pass will
   *          be used.
   * @throws IOException
   */
  public static void signJar(String jarName, String signedJar, IProgressMonitor monitor, String alias,
      String storeLocation, String storeType, String storePass, String keyPass) throws IOException {
    String jarSigner = getJarsignerLocation();
    if (jarSigner == null) {
      throw new IOException("The location of jarsigner tool was not correctly specified in Preferences.");
    }
    File f = new File(jarSigner);
    if (!f.exists()) {
      throw new IOException("The jarsigner tool was not found at \"" + jarSigner + "\"");
    }
    List list = new ArrayList();
    list.add(jarSigner);
    addOption(list, "-keystore", storeLocation, false);
    addOption(list, "-storepass", storePass, true);
    addOption(list, "-keypass", keyPass, false);
    addOption(list, "-storetype", storeType, false);
    addOption(list, "-signedjar", signedJar, false);
    list.add(jarName);
    list.add(alias);

    Process ps;
    try {
      ps = Runtime.getRuntime().exec((String[]) list.toArray(new String[list.size()]));
    } catch (IOException ioe) {
      IOException e = new IOException("Cannot sign provided content.");
      e.initCause(ioe);
      throw e;
    }
    ProcessOutputReader outputReader = new ProcessOutputReader(ps.getInputStream(), "[Jar Signer] Output Reader");
    outputReader.start();

    int retries = 150;
    int result = -1;
    while (!monitor.isCanceled()) {
      try {
        Thread.sleep(200);
        result = ps.exitValue();
        break;
      } catch (IllegalThreadStateException itse) {
        if (--retries == 0)
          break;
      } catch (InterruptedException e) {
        retries = 0;
        break;
      }
    }
    outputReader.stopReader();
    //check whether process is canceled by user or just has finished signing.
    if (monitor.isCanceled()) {
      ps.destroy();
      if (result != 0) {
        throw new IOException("Signing operation was cancelled.");
      }
    }
    if (result != 0) {
      try {
        ps.getOutputStream().write("a\na\na\n".getBytes());
        ps.getOutputStream().flush();
      } catch (IOException e) {
        // ignore, most probably the pipe will be closed
      }
      if (retries == 0) {
        throw new IOException("Cannot sign provided content. Operation timed out.");
      } else {
        throw new IOException("Cannot sign provided content. Jarsigner return code: " + result + ". Jarsigner output: "
            + outputReader.getOutput());
      }
    }
  }

  private static void addOption(List list, String option, String value, boolean emptyAllowed) {
    if (value == null) {
      return;
    }
    if (!emptyAllowed && value.trim().length() == 0) {
      return;
    }
    list.add(option.trim());
    list.add(value.trim());
  }

  /**
   * Convenient method for signing jar file with information provided in passed
   * properties. If no signing information is provided then this function does
   * nothing. Multiple signing is allowed. If no password is provided for given
   * certificate, a dialog for entering password is displayed.
   * 
   * @param file
   *          the file to be signed.
   * @param signedFile
   *          the output file.
   * @param monitor
   *          the progress monitor.
   * @param properties
   * @throws IOException
   *           in case of signing error
   */
  public static void signJar(File file, File signedFile, IProgressMonitor monitor, Map properties) throws IOException {
    if (properties == null) {
      return;
    }
    int count = getCertificatesCount(properties);
    if (count <= 0) {
      return;
    }
    signJar0(file, signedFile, properties, new ArrayList(), new ArrayList(), monitor);
  }

  /**
   * Convenient method for signing jar files with information provided in passed
   * properties. If no signing information is provided then this function does
   * nothing. Multiple signing is allowed. If no password is provided for given
   * certificate, a dialog for entering password is displayed.
   * 
   * @param files
   *          the files to be signed.
   * @param signedFiles
   *          the output files.
   * @param monitor
   *          the progress monitor.
   * @param properties
   * @throws IOException
   *           in case of signing error
   * @since 6.1
   */
  public static void signJars(File files[], File signedFiles[], IProgressMonitor monitor, Map properties)
      throws IOException {
    if (properties == null || files == null || signedFiles == null || files.length != signedFiles.length) {
      return;
    }
    int count = getCertificatesCount(properties);
    if (count <= 0) {
      return;
    }
    List storePasswords = new ArrayList();
    List keyPasswords = new ArrayList();
    for (int i = 0; i < files.length; i++) {
      boolean shouldContinue = signJar0(files[i], signedFiles[i], properties, storePasswords, keyPasswords, monitor);
      if (!shouldContinue)
        return;
    }
  }

  /**
   * Convenient method for signing DP file with information provided in passed
   * properties. If no signing information is provided then this function does
   * nothing. Multiple signing is allowed. If no password is provided for given
   * certificate, a dialog for entering password is displayed.
   * 
   * @param dpFile
   *          the file to be signed.
   * @param signedFile
   *          the output file.
   * @param monitor
   *          the progress monitor.
   * @param properties
   * @throws IOException
   *           in case of signing error
   * @since 5.1
   */
  public static void signDp(File dpFile, File signedFile, IProgressMonitor monitor, Map properties) throws IOException {
    if (properties == null) {
      return;
    }
    int count = getCertificatesCount(properties);
    if (count <= 0) {
      return;
    }

    SubMonitor subMonitor = SubMonitor.convert(monitor);
    File tmpDir = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/tmp.extracted");
    FileUtils.deleteDir(tmpDir);

    JarInputStream jis = null;
    JarOutputStream jos = null;
    try {
      jis = new JarInputStream(new FileInputStream(dpFile));
      Manifest manifest = jis.getManifest();
      if (manifest == null) {
        throw new IOException("DP file has no manifest.");
      }
      jos = new JarOutputStream(new FileOutputStream(signedFile), manifest);

      List storePasswords = new ArrayList();
      List keyPasswords = new ArrayList();
      JarEntry jarEntry;
      while ((jarEntry = jis.getNextJarEntry()) != null && !monitor.isCanceled()) {
        JarEntry newEntry = new JarEntry(jarEntry.getName());
        newEntry.setTime(jarEntry.getTime());
        newEntry.setExtra(jarEntry.getExtra());
        newEntry.setComment(jarEntry.getComment());
        jos.putNextEntry(newEntry);

        Attributes attributes = jarEntry.getAttributes();
        if (attributes != null && attributes.getValue(Constants.BUNDLE_SYMBOLICNAME) != null) {
          // this entry is bundle - sign it
          String entryName = jarEntry.getName();
          File file = new File(tmpDir.getAbsolutePath() + "/" + entryName);
          file.getParentFile().mkdirs();

          copyBytes(jis, new FileOutputStream(file), false, true);
          if (!signJar0(file, null, properties, storePasswords, keyPasswords, subMonitor.newChild(1))) {
            // operation is cancelled
            return;
          }
          copyBytes(new FileInputStream(file), jos, true, false);
        } else {
          // entry is not a bundle - put it unchanged
          copyBytes(jis, jos, false, false);
        }
        jis.closeEntry();
        jos.closeEntry();
      }
      jis.close();
      jos.close();
      signJar0(signedFile, null, properties, storePasswords, keyPasswords, subMonitor.newChild(1));
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
      FileUtils.deleteDir(tmpDir);
    }
  }

  private static void copyBytes(InputStream in, OutputStream out, boolean closeIn, boolean closeOut) throws IOException {
    byte[] buf = new byte[1024];
    int len;
    try {
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
    } finally {
      if (closeIn)
        try {
          in.close();
        } catch (IOException e) {
        }
      if (closeOut)
        try {
          out.close();
        } catch (IOException e) {
        }
    }
  }

  private static boolean signJar0(File file, File signedFile, Map properties, List storePasswords, List keyPasswords,
      IProgressMonitor monitor) throws IOException {
    int count = getCertificatesCount(properties);
    for (int i = 0; i < count && !monitor.isCanceled(); i++) {
      String alias = getCertificateAlias(properties, i);
      String location = getCertificateStoreLocation(properties, i);
      String type = getCertificateStoreType(properties, i);
      String storePass = getCertificateStorePass(properties, i);
      String keyPass = getCertificateKeyPass(properties, i);
      if (alias == null || location == null || type == null) {
        throw new IOException("Not enough information is specified for signing content.");
      }
      if (storePass == null || storePass.length() == 0) {
        if (storePasswords.size() > i) {
          storePass = (String) storePasswords.get(i);
        } else {
          storePass = getKeystorePassword(location);
          if (storePass == null) {
            return false;
          }
        }
      }
      if (storePasswords.size() <= i) {
        storePasswords.add(storePass);
      }
      if (keyPass == null || keyPass.length() == 0) {
        if (keyPasswords.size() > i) {
          keyPass = (String) keyPasswords.get(i);
        } else {
          keyPass = getPrivateKeyPassword(location, alias);
          if (keyPass == null) {
            return false;
          }
        }
      }
      if (keyPasswords.size() <= i) {
        keyPasswords.add(keyPass);
      }
      String inFileName = (signedFile != null && i > 0) ? signedFile.getAbsolutePath() : file.getAbsolutePath();
      String outFileName = (signedFile != null && i == 0) ? signedFile.getAbsolutePath() : null;
      signJar(inFileName, outFileName, monitor, alias, location, type, storePass, keyPass);
    }
    return !monitor.isCanceled();
  }

  /**
   * Returns location of jarsigner tool or null if the location cannot be
   * determined.
   * 
   * @return the jarsigner location
   */
  public static String getJarsignerLocation() {
    ScopedPreferenceStore preferenceStore = new ScopedPreferenceStore(new InstanceScope(),
        "org.tigris.mtoolkit.certmanager");
    String location = preferenceStore.getString("jarsigner.location");
    if (location == null || location.length() == 0) {
      location = getDefaultJarsignerLocation();
    }
    return location;
  }

  public static String getDefaultJarsignerLocation() {
    String location = "";
    String javaHome = System.getProperty("java.home");
    if (javaHome != null) {
      String relativePath = "bin" + File.separator + "jarsigner.exe";
      File signerFile = new File(javaHome, relativePath);
      if (signerFile.exists()) {
        location = signerFile.getAbsolutePath();
      } else {
        File parentPath = new File(javaHome).getParentFile();
        signerFile = new File(parentPath, relativePath);
        if (signerFile.exists()) {
          location = signerFile.getAbsolutePath();
        }
      }
    }
    return location;
  }

  /**
   * Opens "Continue without signing" dialog. Returns true if user selects Yes.
   * 
   * @param error
   *          error to be displayed
   * @return
   */
  public static boolean continueWithoutSigning(final String error) {
    final Display display = PlatformUI.getWorkbench().getDisplay();
    final Boolean result[] = new Boolean[1];
    display.syncExec(new Runnable() {
      public void run() {
        Shell shell = display.getActiveShell();
        String nl = System.getProperty("line.separator");
        MessageDialog dialog = new MessageDialog(shell, "Warning", null, "The signing operation failed. Reason:" + nl
            + error + nl + "Continue without signing?", MessageDialog.WARNING, new String[] { "Yes", "No" }, 0);
        if (dialog.open() == Window.OK) {
          result[0] = new Boolean(true);
        }
      }
    });
    return result[0] != null;
  }

  /**
   * Opens password dialog for getting keystore password.
   * 
   * @param keystoreLocation
   * @return the password entered or null if dialog is canceled
   */
  public static String getKeystorePassword(final String keystoreLocation) {
    final Display display = PlatformUI.getWorkbench().getDisplay();
    final String result[] = new String[1];
    display.syncExec(new Runnable() {
      public void run() {
        Shell shell = display.getActiveShell();
        PasswordDialog dialog = new PasswordDialog(shell, "Enter Password");
        dialog.setMessage("Enter password for keystore: \"" + keystoreLocation + "\"");
        if (dialog.open() == Window.OK) {
          result[0] = dialog.getPassword();
        }
      }
    });
    return result[0];
  }

  /**
   * Opens password dialog for getting private key password.
   * 
   * @param keystoreLocation
   * @return the password entered or null if dialog is canceled
   */
  public static String getPrivateKeyPassword(final String keystoreLocation, final String alias) {
    final Display display = PlatformUI.getWorkbench().getDisplay();
    final String result[] = new String[1];
    display.syncExec(new Runnable() {
      public void run() {
        Shell shell = display.getActiveShell();
        PasswordDialog dialog = new PasswordDialog(shell, "Enter Password");
        dialog.setMessage("Enter private key password for alias \"" + alias + "\" in keystore \"" + keystoreLocation
            + "\" or leave the field empty to use the keystore password.");
        if (dialog.open() == Window.OK) {
          result[0] = dialog.getPassword();
        }
      }
    });
    return result[0];
  }
}

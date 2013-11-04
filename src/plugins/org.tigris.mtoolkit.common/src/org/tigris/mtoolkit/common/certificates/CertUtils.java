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
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.Messages;
import org.tigris.mtoolkit.common.PlatformUtils;
import org.tigris.mtoolkit.common.ProcessOutputReader;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.gui.PasswordDialog;
import org.tigris.mtoolkit.common.installation.InstallationConstants;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;

public final class CertUtils {
  private static final String   DT = ".";           //$NON-NLS-1$

  private static ServiceTracker certProviderTracker;

  private CertUtils() {
  }

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

  public static Map getPreparationProperties(List signCertificateUids) {
    Map preparationProps = new Hashtable();
    if (signCertificateUids != null) {
      int certId = 0;
      for (Iterator it = signCertificateUids.iterator(); it.hasNext();) {
        ICertificateDescriptor cert = getCertificate((String) it.next());
        if (cert != null) {
          pushCertificate(preparationProps, cert, certId++);
        }
      }
    }
    return preparationProps;
  }

  public static boolean validateCertificateUids(List signCertificateUids) {
    if (signCertificateUids != null) {
      for (Iterator it = signCertificateUids.iterator(); it.hasNext();) {
        ICertificateDescriptor cert = getCertificate((String) it.next());
        if (cert == null) {
          return false;
        }
      }
    }
    return true;
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
   * Method for signing InstallationItem instances as they must contain a
   * java.io.File instance that will be signed. Files can be JAR and DP files.
   * It is common this file instance to be set at implementation of
   * InstallationItem interface method prepare() in each provided item. In that
   * case prepare() method should be invoked before invoking signItems() method.
   * If key store and/or private key passwords for given certificate are not
   * provided then this function opens password dialogs for getting them from
   * the user.
   *
   * @param InstallationItem
   *          [] items - items that will be signed
   * @param IProgressMonitor
   *          monitor
   * @param Map
   *          preparationProps - properties for signing.
   * @return IStatus that can be with severity ERROR / CANCEL or OK
   */

  public static IStatus signItems(InstallationItem[] items, IProgressMonitor monitor, Map preparationProps) {
    boolean hasError = false;
    try {
      if (preparationProps == null) {
        throw new CoreException(new Status(IStatus.ERROR, UtilitiesPlugin.PLUGIN_ID,
            "Signing properties are not initialized!"));
      }
      int count = getCertificatesCount(preparationProps);
      if (count <= 0) {
        return Status.OK_STATUS;
      }
      List preparedJarFiles = new ArrayList();
      List preparedDpFiles = new ArrayList();

      Map preparedItems = initializePreparedItemsMap(items, preparedJarFiles, preparedDpFiles);
      //if items types are not supporting signing
      if (preparedItems == null || preparedItems.size() == 0) {
        return Status.OK_STATUS;
      }
      IStatus status = setSigningProperties(monitor, preparationProps);
      if (status.matches(IStatus.ERROR)) {
        if (!CertUtils.continueWithoutSigning(status.getMessage())) {
          monitor.setCanceled(true);
        }
      } else {
        List signedJarFilesList = CertUtils.signJars0(preparedJarFiles, monitor, preparationProps);
        List signedDpFilesList = CertUtils.signDps0(preparedDpFiles, monitor, preparationProps);
        setItemsLocation(preparedJarFiles, preparedDpFiles, preparedItems, signedJarFilesList, signedDpFilesList);
      }
    } catch (IOException ioe) {
      //this exception is thrown by setSigningProperties() if the user has not been entered full information needed for sign
      if (!monitor.isCanceled()) {
        if (!CertUtils.continueWithoutSigning(ioe.getMessage())) {
          monitor.setCanceled(true);
        }
      }
    } catch (CoreException ex) {
      hasError = true;
      return ex.getStatus();
    } finally {
      //if installation process is cancelled by the user or there is error
      if (monitor.isCanceled() || hasError) {
        for (int i = 0; i < items.length; i++) {
          items[i].dispose();
        }
      }
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  public static IStatus signJar(File file, File signedFile, IProgressMonitor monitor, Map properties) {
    if (properties == null) {
      return UtilitiesPlugin.newStatus(IStatus.ERROR, "Signing properties are not initialized");
    }
    int count = getCertificatesCount(properties);
    if (count == 0) {
      int code = monitor.isCanceled() ? IStatus.CANCEL : IStatus.OK;
      return UtilitiesPlugin.newStatus(code, "No certificates set");
    }
    IStatus status = Status.OK_STATUS;
    Map propertiesDupl = new HashMap(properties);
    status = setSigningProperties(monitor, propertiesDupl);

    if (status.isOK()) {
      for (int i = 0; i < count && !monitor.isCanceled(); i++) {
        String alias = getCertificateAlias(propertiesDupl, i);
        String location = getCertificateStoreLocation(propertiesDupl, i);
        String type = getCertificateStoreType(propertiesDupl, i);
        String storePass = getCertificateStorePass(propertiesDupl, i);
        String keyPass = getCertificateKeyPass(propertiesDupl, i);
        String inFileName = (signedFile != null && i > 0) ? signedFile.getAbsolutePath() : file.getAbsolutePath();
        String outFileName = (signedFile != null && i == 0) ? signedFile.getAbsolutePath() : null;
        status = signJar(inFileName, outFileName, monitor, alias, location, type, storePass, keyPass);
      }
    }
    if (monitor.isCanceled()) {
      return UtilitiesPlugin.newStatus(IStatus.CANCEL, "The signing process is canceled");
    }
    return status;
  }

  /**
   * Returns location of jarsigner tool or null if the location cannot be
   * determined.
   *
   * @return the jarsigner location
   */
  public static String getJarsignerLocation() {
    @SuppressWarnings("deprecation")
    ScopedPreferenceStore preferenceStore = new ScopedPreferenceStore(new InstanceScope(), "org.tigris.mtoolkit.common");
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
      String jarSignerExecutable = (PlatformUtils.isWindows()) ? "jarsigner.exe" : "jarsigner";
      String relativePath = ((PlatformUtils.isMacOSX()) ? "Commands" : "bin") + File.separator + jarSignerExecutable;
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
            + error + nl + "Continue without signing?", MessageDialog.WARNING, new String[] {
            "Yes", "No"
        }, 0);
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

  private static void setCertificateStorePass(Map properties, int id, String storePass) {
    properties.put(InstallationConstants.CERT_STORE_PASS + DT + id, storePass);
  }

  private static void setCertificateKeyPass(Map properties, int id, String keyPass) {
    properties.put(InstallationConstants.CERT_KEY_PASS + DT + id, keyPass);
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
   */
  private static IStatus signJar(String jarName, String signedJar, IProgressMonitor monitor, String alias,
      String storeLocation, String storeType, String storePass, String keyPass) {
    String jarSigner = getJarsignerLocation();
    if (jarSigner == null) {
      return UtilitiesPlugin.newStatus(IStatus.ERROR,
          "The location of jarsigner tool was not correctly specified in Preferences.");
    }
    File f = new File(jarSigner);
    if (!f.exists()) {
      return UtilitiesPlugin.newStatus(IStatus.ERROR, "The jarsigner tool was not found at \"" + jarSigner + "\"");
    }
    List list = new ArrayList();
    list.add(jarSigner);
    //    list.add("-strict");//added in java7
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
      return UtilitiesPlugin.newStatus(IStatus.ERROR, "Cannot sign provided content.", ioe);
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
        if (--retries == 0) {
          break;
        }
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
        return UtilitiesPlugin.newStatus(IStatus.CANCEL, "Signing operation was cancelled.");
      }
    }

    try {
      ps.getOutputStream().write("a\na\na\n".getBytes());
      ps.getOutputStream().flush();
    } catch (IOException e) {
      // ignore, most probably the pipe will be closed
    }
    if (result != 0) {
      if (retries == 0) {
        return UtilitiesPlugin.newStatus(IStatus.ERROR, "Cannot sign provided content. Operation timed out. Alias: "
            + alias);
      }
      return UtilitiesPlugin.newStatus(IStatus.ERROR, "Cannot sign provided content. Alias: " + alias
          + ". Jarsigner return code: " + result + ". Jarsigner output: " + outputReader.getOutput());
    }
    String outputMsg = outputReader.getOutput();
    //return the warnings
    if (outputMsg.indexOf("Warning") != -1 && outputMsg.indexOf("will expire") == -1) {//skip the unnecessary warning for the '6 months'
      return UtilitiesPlugin.newStatus(IStatus.WARNING, "Signing warning found. Alias: " + alias
          + ". Jarsigner return code: " + result + ". Jarsigner output: " + outputMsg.replaceAll("\r\n", " "));
    }
    return Status.OK_STATUS;
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
  private static IStatus signJars(File files[], File signedFiles[], IProgressMonitor monitor, Map properties)
      throws IOException {
    if (files == null || signedFiles == null || files.length != signedFiles.length) {
      return new Status(IStatus.ERROR, UtilitiesPlugin.PLUGIN_ID, "Invalid or missing sinning information.");
    }
    MultiStatus operationStatus = new MultiStatus(UtilitiesPlugin.PLUGIN_ID, IStatus.WARNING,
        "Signing warnings found.", null);
    for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
      IStatus status = signJar(files[i], signedFiles[i], monitor, properties);
      if (status.matches(IStatus.ERROR) || status.matches(IStatus.CANCEL)) {
        return status;
      }
      if (status.matches(IStatus.WARNING)) {
        operationStatus.add(status);
      }
      monitor.worked(1);
    }
    if (operationStatus.getChildren().length == 0) {
      return Status.OK_STATUS;
    }
    return operationStatus;
  }

  /**
   * Convenient method for signing DP file with information provided in passed
   * properties. If no signing information is provided then this function does
   * nothing. Multiple signing is allowed. If no password is provided for given
   * certificate, a dialog for entering password is displayed.
   *
   * @param dpFiles
   *          the file to be signed.
   * @param signedFiles
   *          the output file.
   * @param monitor
   *          the progress monitor.
   * @param properties
   * @throws IOException
   *           in case of signing error
   * @since 5.1
   */

  private static void signDpFiles(File[] dpFiles, File[] signedFiles, IProgressMonitor monitor, Map properties)
      throws IOException {
    if (dpFiles == null || signedFiles == null || dpFiles.length != signedFiles.length) {
      throw new IOException("Invalid or missing sinning information.");
    }
    for (int i = 0; i < dpFiles.length && !monitor.isCanceled(); i++) {
      signDp(dpFiles[i], signedFiles[i], monitor, properties);
      monitor.worked(1);
    }
  }

  private static IStatus signDp(File dpFile, File signedFile, IProgressMonitor monitor, Map properties)
      throws IOException {
    SubMonitor subMonitor = SubMonitor.convert(monitor);
    File tmpDir = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/tmp.extracted");
    FileUtils.delete(tmpDir);

    JarInputStream jis = null;
    JarOutputStream jos = null;
    try {
      jis = new JarInputStream(new FileInputStream(dpFile));
      Manifest manifest = jis.getManifest();
      if (manifest == null) {
        return UtilitiesPlugin.newStatus(IStatus.ERROR, "DP file has no manifest.");
      }
      jos = new JarOutputStream(new FileOutputStream(signedFile), manifest);
      JarEntry jarEntry;

      MultiStatus operationStatus = new MultiStatus(UtilitiesPlugin.PLUGIN_ID, IStatus.OK, "", null);
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
          IStatus status = signJar(file, null, subMonitor.newChild(1), properties);
          if (status.matches(IStatus.CANCEL) || status.matches(IStatus.ERROR)) {
            return status;
          }
          if (status.matches(IStatus.WARNING)) {
            operationStatus.add(status);
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
      IStatus status = signJar(signedFile, null, subMonitor.newChild(1), properties);
      if (status.matches(IStatus.CANCEL) || status.getCode() == IStatus.ERROR) {
        return status;
      }
      if (status.matches(IStatus.WARNING)) {
        operationStatus.add(status);
      }
      return operationStatus;
    } finally {
      FileUtils.close(jis);
      FileUtils.close(jos);
      FileUtils.delete(tmpDir);
    }
  }

  private static IStatus setSigningProperties(IProgressMonitor monitor, Map properties) {
    int count = getCertificatesCount(properties);
    for (int i = 0; i < count && !monitor.isCanceled(); i++) {
      String alias = getCertificateAlias(properties, i);
      String location = getCertificateStoreLocation(properties, i);
      String type = getCertificateStoreType(properties, i);
      if (alias == null || location == null || type == null) {
        return UtilitiesPlugin.newStatus(IStatus.ERROR, "Not enough information is specified for signing content.");
      }
      File keyStoreLocation = new File(location);
      if (!keyStoreLocation.isFile() || !keyStoreLocation.exists()) {
        return UtilitiesPlugin.newStatus(IStatus.ERROR,
            NLS.bind("Keystore {0} is invalid or does not exists.", location));
      }
    }
    for (int i = 0; i < count && !monitor.isCanceled(); i++) {
      String storePass = getCertificateStorePass(properties, i);
      if (storePass == null || storePass.length() == 0) {
        storePass = getKeystorePassword(getCertificateStoreLocation(properties, i));
        if (storePass == null || storePass.length() == 0) {
          return UtilitiesPlugin.newStatus(IStatus.ERROR, "Keystore password is not provided");
        }
        setCertificateStorePass(properties, i, storePass);
      }

      String keyPass = getCertificateKeyPass(properties, i);
      if (keyPass == null || keyPass.length() == 0) {
        String alias = getCertificateAlias(properties, i);
        keyPass = getPrivateKeyPassword(getCertificateStoreLocation(properties, i), alias);
        if (keyPass == null) {
          return UtilitiesPlugin.newStatus(IStatus.ERROR, "Private key password is missing");
        }
        if (keyPass.length() == 0) {
          keyPass = storePass;
        }
        setCertificateKeyPass(properties, i, keyPass);
      }
    }
    return Status.OK_STATUS;
  }

  private static void copyBytes(InputStream in, OutputStream out, boolean closeIn, boolean closeOut) throws IOException {
    byte[] buf = new byte[1024];
    int len;
    try {
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
    } finally {
      if (closeIn) {
        FileUtils.close(in);
      }
      if (closeOut) {
        FileUtils.close(out);
      }
    }
  }

  /**
   * Signs a List of java.io.File items with provided in parameter properties
   * information. If there is not information for certificates at provided
   * properties or an error occurs during the signing and the user chooses to
   * continue without sign or the user has canceled operation - signFiles method
   * returns empty List.
   *
   * @param files
   *          - An List of java.io.File items that will be signed. Files can be
   *          JAR or DP.
   * @param monitor
   * @param properties
   *          - Properties used for signing
   * @return List of java.io. File items - signed files
   * @throws IOException
   * @throws CoreException
   *           if provided parameters are not correct
   *
   */

  private static List signDps0(List files, IProgressMonitor monitor, Map properties) throws IOException {
    if (files.isEmpty()) {
      return files;
    }
    List allSignedFilesList = new ArrayList();
    boolean hasError = false;
    try {

      List dpFilesToSign = new ArrayList();
      List dpSignedFiles = new ArrayList();

      for (int i = 0; i < files.size() && !monitor.isCanceled(); i++) {
        File fileToSign = (File) files.get(i);
        if (fileToSign == null || !fileToSign.exists()) {
          break;
        }
        File signedFile = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/signed/" + fileToSign.getName());
        allSignedFilesList.add(signedFile);
        signedFile.getParentFile().mkdirs();
        if (signedFile.exists()) {
          signedFile.delete();
        }
        dpFilesToSign.add(fileToSign);
        dpSignedFiles.add(signedFile);
      }
      CertUtils.signDpFiles((File[]) dpFilesToSign.toArray(new File[] {}),
          (File[]) dpSignedFiles.toArray(new File[] {}), monitor, properties);
    } catch (IOException ioe) {
      hasError = true;
      throw ioe;
    } finally {
      if (monitor.isCanceled() || hasError) {
        deleteSignedFiles(allSignedFilesList);
        allSignedFilesList = Collections.EMPTY_LIST;
      }
    }
    return allSignedFilesList;
  }

  private static List signJars0(List files, IProgressMonitor monitor, Map properties) throws IOException {
    if (files.isEmpty()) {
      return files;
    }
    List jarFilesToSign = new ArrayList();
    List jarSignedFiles = new ArrayList();
    List allSignedFilesList = new ArrayList();
    boolean hasError = false;
    try {
      for (int i = 0; i < files.size() && !monitor.isCanceled(); i++) {
        File fileToSign = (File) files.get(i);
        if (fileToSign == null || !fileToSign.exists()) {
          break;
        }
        File signedFile = new File(UtilitiesPlugin.getDefault().getStateLocation() + "/signed/" + fileToSign.getName());
        allSignedFilesList.add(signedFile);
        signedFile.getParentFile().mkdirs();
        if (signedFile.exists()) {
          signedFile.delete();
        }
        jarFilesToSign.add(fileToSign);
        jarSignedFiles.add(signedFile);
      }
      IStatus status = CertUtils.signJars((File[]) jarFilesToSign.toArray(new File[] {}),
          (File[]) jarSignedFiles.toArray(new File[] {}), monitor, properties);
      if (status.matches(IStatus.WARNING)) {
        UtilitiesPlugin.log(status);
      } else if (status.matches(IStatus.ERROR)) {
        throw new IOException(status.getMessage());
      }
    } catch (IOException ioe) {
      hasError = true;
      throw ioe;
    } finally {
      if (monitor.isCanceled() || hasError) {
        deleteSignedFiles(allSignedFilesList);
        allSignedFilesList = Collections.EMPTY_LIST;
      }
    }
    return allSignedFilesList;
  }

  private static void deleteSignedFiles(List signedFiles) {
    File fileToDelete = null;
    for (int i = 0; i < signedFiles.size(); i++) {
      fileToDelete = (File) signedFiles.get(i);
      if (fileToDelete != null) {
        fileToDelete.delete();
      }
    }
  }

  private static void setItemsLocation(List preparedJarFiles, List preparedDpFiles, Map preparedItems,
      List signedJarFilesList, List signedDpFilesList) {
    int size = signedJarFilesList.size();
    InstallationItem itemToSet;
    for (int i = 0; i < size; i++) {
      itemToSet = (InstallationItem) preparedItems.get(preparedJarFiles.get(i));
      itemToSet.setLocation((File) signedJarFilesList.get(i));
    }
    size = signedDpFilesList.size();
    for (int i = 0; i < size; i++) {
      itemToSet = (InstallationItem) preparedItems.get(preparedDpFiles.get(i));
      itemToSet.setLocation((File) signedDpFilesList.get(i));
    }
  }

  private static Map initializePreparedItemsMap(InstallationItem[] items, List preparedJarFiles, List preparedDpFiles) {
    InstallationItem item;
    Map preparedItems = new HashMap();
    for (int i = 0; i < items.length; i++) {
      item = items[i];
      File preparedFile = new File(item.getLocation());

      if (item.getMimeType().equals(InstallationItemProcessor.MIME_DP)) {
        preparedItems.put(preparedFile, item);
        preparedDpFiles.add(preparedFile);
      } else {
        preparedJarFiles.add(preparedFile);
        preparedItems.put(preparedFile, item);
      }
    }
    return preparedItems;
  }

  public static Certificate readCertificate(String location, String alias) throws IOException,
      GeneralSecurityException, NullPointerException {
    if (location == null || alias == null) {
      String message = Messages.CertUtils_NotNullAlias;
      if (location == null) {
        message = Messages.CertUtils_NotNullLocation;
      }
      throw new NullPointerException(message);
    }

    FileInputStream fis = null;
    try {
      KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
      fis = new FileInputStream(location);
      ks.load(fis, null);
      if (ks.isKeyEntry(alias)) {
        Certificate certs[] = ks.getCertificateChain(alias);
        return certs[0];
      } else if (ks.isCertificateEntry(alias)) {
        return ks.getCertificate(alias);
      } else {
        throw new GeneralSecurityException(NLS.bind(Messages.dlgCertMan_verifyUnknownAlias, new Object[] {
          alias
        }));
      }
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
        }
      }
    }
  }
}

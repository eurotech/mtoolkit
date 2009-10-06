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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.util.tracker.ServiceTracker;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.gui.PasswordDialog;
import org.tigris.mtoolkit.common.installation.InstallationConstants;

public class CertUtils {
  private static ServiceTracker certProviderTracker;

  private static final String   DT = ".";           //$NON-NLS-1$

  /**
   * A convenient method which delegates to ICertificateProvider.getCertificates() 
   * method if ICertificateProvider service is registered. Otherwise returns null. 
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
   * A convenient method which delegates to ICertificateProvider.getCertificate() 
   * method if ICertificateProvider service is registered. Otherwise returns null. 
   * @param uid the uid of certificate to find
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
   * @param properties the Map to push in certificate data
   * @param cert the certificate to push
   * @param id the id to append to each key that is inserted to properties
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

  /**
   * Signs provided file with passed information for signing.
   * @param jarName the file to sign
   * @param signedJar the path to the new signed file or null. If this is null changes will be made directly to 'jarName' file.
   * @param monitor the progress monitor. Cannot be null.
   * @param alias the alias. Cannot be null.
   * @param storeLocation the key store location. Cannot be null.
   * @param storeType the key store type. Cannot be null.
   * @param storePass the key store pass. Cannot be null.
   * @throws IOException
   */
  public static void signJar(String jarName, String signedJar, IProgressMonitor monitor, String alias,
      String storeLocation, String storeType, String storePass) throws IOException {
    String jarSigner = System.getProperty("dpeditor.jarsigner");
    if (jarSigner == null) {
      jarSigner = System.getProperty("java.home") + File.separator + "bin" + File.separator + "jarsigner";
    }
    File f = new File(jarSigner);
    if (!f.exists()) {
      throw new IOException("The jarsigner tool was not found at \"" + jarSigner + "\"");
    }
    List list = new ArrayList();
    list.add(jarSigner);
    addOption(list, "-keystore", storeLocation, false);
    addOption(list, "-storepass", storePass, true);
    //addOption(list, "-keypass", keypass, false);
    addOption(list, "-storetype", storeType, false);
    addOption(list, "-signedjar", signedJar, false);
    list.add(jarName);
    list.add(alias);

    Process ps;
    try {
      ps = Runtime.getRuntime().exec((String[]) list.toArray(new String[list.size()]));
    } catch (IOException ioe) {
      throw new IOException("Cannot sign provided content.", ioe);
    }
    OutputReader outputReader = new OutputReader(ps.getInputStream(), "[Jar Signer] Output Reader");
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
        throw new IOException("Cannot sign provided content. Jarsigner return code: " + result
            + ". Jarsigner output: " + outputReader.output);
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
   * Convenient method for signing jar file with information provided in passed properties. 
   * If no signing information is contained in passed properties null is returned.
   * Multiple signing is allowed. If no password is provided for given certificate, 
   * a dialog for entering password is displayed.
   * @param file the file to be signed.
   * @param monitor the progress monitor.
   * @param properties
   * @return the new signed file or null if no signing information is 
   * contained in passed properties.
   * @throws IOException in case of signing error
   */
  public static File signJar(File file, IProgressMonitor monitor, Map properties) throws IOException {
    if (properties == null) {
      return null;
    }
    int count = getCertificatesCount(properties);
    if (count <= 0) {
      return null;
    }

    String signedFilePath = UtilitiesPlugin.getDefault().getStateLocation() + "/" + file.getName();
    for (int i = 0; i < count; i++) {
      String alias = getCertificateAlias(properties, i);
      final String location = getCertificateStoreLocation(properties, i);
      String type = getCertificateStoreType(properties, i);
      String pass = getCertificateStorePass(properties, i);
      if (alias == null || location == null || type == null) {
        throw new IOException("Not enough information is specified for signing content.");
      }
      if (pass == null || pass.isEmpty()) {
        final Display display = PlatformUI.getWorkbench().getDisplay();
        final String result[] = new String[1];
        display.syncExec(new Runnable() {
          public void run() {
            Shell shell = display.getActiveShell();
            PasswordDialog dialog = new PasswordDialog(shell, "Enter Password");
            dialog.setMessage("Enter password for key store: \"" + location + "\"");
            if (dialog.open() == Dialog.OK) {
              result[0] = dialog.getPassword();
            }
          }
        });
        if (result[0] == null) {
          return null;
        }
        pass = result[0];
      }
      if (i == 0) {
        CertUtils.signJar(file.getAbsolutePath(), signedFilePath, monitor, alias, location, type, pass);
      } else {
        CertUtils.signJar(signedFilePath, null, monitor, alias, location, type, pass);
      }
    }
    return new File(signedFilePath);
  }

  private static class OutputReader extends Thread {
    private volatile boolean stopReader = false;
    private InputStream      inputStream;
    private StringBuffer     output     = new StringBuffer();

    public OutputReader(InputStream inputStream, String name) {
      super(name);
      this.inputStream = inputStream;
    }

    public void run() {
      try {
        byte[] buf = new byte[4096];
        int len = inputStream.available();
        while (len != -1 && !stopReader) {
          len = inputStream.read(buf);
          if (len > 0) {
            output.append(new String(buf, 0, len));
          }
          len = inputStream.available();
        }
      } catch (IOException io) {
      }
    }

    private void stopReader() {
      stopReader = true;
    }
  };
}

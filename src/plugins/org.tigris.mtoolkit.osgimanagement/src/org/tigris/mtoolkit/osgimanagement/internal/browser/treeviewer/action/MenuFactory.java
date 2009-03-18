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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.internal.ConsoleView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.StoreConstants;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.InstallDialog;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertiesDialog;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertySheet;


public class MenuFactory {

  public static void addFrameworkAction(TreeRoot treeRoot, TreeViewer parentView) {
    String frameworkName = generateName(treeRoot);
    FrameWork newFrameWork = new FrameWork(frameworkName, treeRoot, false);
    PropertySheet sheet = new PropertySheet(parentView, newFrameWork, true);
    sheet.open();
  }
  
  // generates unique name for the new FrameWork
  public static String generateName(TreeRoot treeRoot) {
    HashMap frameWorkMap = treeRoot.getFrameWorkMap();
    int index = 1;
    String frameWorkName;
    do {
      frameWorkName = Messages.new_framework_default_name + '_' + index;
      index++;
    } while (frameWorkMap.containsKey(frameWorkName));

    return frameWorkName;
  }
  
  public static void bundlePropertiesAction(Bundle bundle, TreeViewer parentView) {
    try {
      RemoteBundle rBundle = bundle.getRemoteBundle();
      Shell shell = parentView.getTree().getShell();
      PropertiesDialog propertiesDialog = new PropertiesDialog(shell, true);
      Dictionary headers = rBundle.getHeaders(null);
      propertiesDialog.open();
      propertiesDialog.getMainControl().setData(headers);
    } catch (IAgentException e) {
      e.printStackTrace();
      BrowserErrorHandler.processError(e, true);
      return;
    }
  }
  
  public static void dpPropertiesAction(DeploymentPackage dp, TreeViewer parentView) {
    try {
      RemoteDP rdp = dp.getRemoteDP();

      Shell shell = parentView.getTree().getShell();
      PropertiesDialog propertiesDialog = new PropertiesDialog(shell, false);
      Dictionary headers = new Hashtable();
      headers.put("DeploymentPackage-SymbolicName", rdp.getHeader("DeploymentPackage-SymbolicName")); //$NON-NLS-1$ //$NON-NLS-2$
      headers.put("DeploymentPackage-Version", rdp.getHeader("DeploymentPackage-Version")); //$NON-NLS-1$ //$NON-NLS-2$
      
      String header = "DeploymentPackage-FixPack"; //$NON-NLS-1$
      String value = rdp.getHeader(header);
      if (value != null) headers.put(header, value);
      
      header = "DeploymentPackage-Copyright"; value = rdp.getHeader(header);  //$NON-NLS-1$
      if (value != null) headers.put(header, value);
      
      header = "DeploymentPackage-ContactAddress"; value = rdp.getHeader(header);  //$NON-NLS-1$
      if (value != null) headers.put(header, value);
      
      header = "DeploymentPackage-Description"; value = rdp.getHeader(header);  //$NON-NLS-1$
      if (value != null) headers.put(header, value);
      
      header = "DeploymentPackage-DocURL"; value = rdp.getHeader(header);  //$NON-NLS-1$
      if (value != null) headers.put(header, value);
      
      header = "DeploymentPackage-Vendor"; value = rdp.getHeader(header);  //$NON-NLS-1$
      if (value != null) headers.put(header, value);
      
      header = "DeploymentPackage-License"; value = rdp.getHeader(header);  //$NON-NLS-1$
      if (value != null) headers.put(header, value);
      
      header = "DeploymentPackage-Icon"; value = rdp.getHeader(header);  //$NON-NLS-1$
      if (value != null) headers.put(header, value);
      
      propertiesDialog.open();
      propertiesDialog.getMainControl().setData(headers);
      
    } catch (IAgentException e) {
      BrowserErrorHandler.processError(e, true);
      return;
    }
  }
  
  public static void deinstallBundleAction(Bundle bundle) {
    FrameworkConnectorFactory.deinstallBundle(bundle);
    
    // Switch to console
    if (FrameWorkView.getConsoleStatus()) {
      IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
      if (store.getBoolean(StoreConstants.SWITCH_FOCUS_KEY)) {
        ConsoleView.setActiveServer(bundle.findFramework().getName());
      }
    }
  }
  
  public static void deinstallDPAction(DeploymentPackage dpNode) {
    FrameworkConnectorFactory.deinstallDP(dpNode);

    // Switch to console
    if (FrameWorkView.getConsoleStatus()) {
      IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
      if (store.getBoolean(StoreConstants.SWITCH_FOCUS_KEY)) {
        ConsoleView.setActiveServer(dpNode.findFramework().getName());
      }
    }  
  }
  
  public static void installBundleAction(FrameWork framework, TreeViewer parentView) {
    InstallDialog installDialog = new InstallDialog(parentView, InstallDialog.INSTALL_BUNDLE_TYPE);
    installDialog.open();
    final String result = installDialog.getResult();
    if ((installDialog.getReturnCode() > 0) || (result == null) || result.trim().equals("")) { //$NON-NLS-1$
      return;
    }
    
    FrameworkConnectorFactory.installBundle(result, framework, false);
    
    // Switch to console
    if (FrameWorkView.getConsoleStatus()) {
      IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
      if (store.getBoolean(StoreConstants.SWITCH_FOCUS_KEY)) {
        ConsoleView.setActiveServer(framework.getName());
      }
    }  
  }
  
  public static void installDPAction(FrameWork framework, TreeViewer parentView) {
    InstallDialog installDialog = new InstallDialog(parentView, InstallDialog.INSTALL_DP_TYPE);
    installDialog.open();
    final String result = installDialog.getResult();
    if ((installDialog.getReturnCode() > 0) || (result == null) || result.trim().equals("")) { //$NON-NLS-1$
      return;
    }

    FrameworkConnectorFactory.installDP(result, framework);
    
    // Switch to console
    if (FrameWorkView.getConsoleStatus()) {
      IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
      if (store.getBoolean(StoreConstants.SWITCH_FOCUS_KEY)) {
        ConsoleView.setActiveServer(framework.getName());
      }
    }
  }
  
  public static void frameworkPropertiesAction(FrameWork framework, TreeViewer parentView) {
    PropertySheet sheet = new PropertySheet(parentView, framework, false);
    sheet.open();
  }
  
  public static void removeFrameworkAction(FrameWork framework) {
    if (framework.isConnected()) {
      framework.disconnect();
    }

    framework.dispose();

    if (FrameWorkView.getConsoleStatus()) {
      ConsoleView.removeServerConsole(framework.getName());
    }
  }

  public static void startBundleAction(Bundle bundle) {
    FrameworkConnectorFactory.startBundle(bundle);
    // Switch to console
    if (FrameWorkView.getConsoleStatus()) {
      IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
      if (store.getBoolean(StoreConstants.SWITCH_FOCUS_KEY)) {
        ConsoleView.setActiveServer(bundle.findFramework().getName());
      }
    }
  }
  
  public static void stopBundleAction(Bundle bundle) {
    FrameworkConnectorFactory.stopBundle(bundle);
    // Switch to console
    if (FrameWorkView.getConsoleStatus()) {
      IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
      if (store.getBoolean(StoreConstants.SWITCH_FOCUS_KEY)) {
        ConsoleView.setActiveServer(bundle.findFramework().getName());
      }
    }
  }
  
  public static void updateBundleAction(final Bundle bundle, TreeViewer parentView) {
    InstallDialog installDialog = new InstallDialog(parentView, InstallDialog.UPDATE_BUNDLE_TYPE);
    installDialog.open();
    final String result = installDialog.getResult();
    if ((installDialog.getReturnCode() > 0) || (result == null) || result.trim().equals("")) { //$NON-NLS-1$
      return;
    }
    FrameworkConnectorFactory.updateBundle(result, bundle);
    
    // Switch to console
    if (FrameWorkView.getConsoleStatus()) {
      IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
      if (store.getBoolean(StoreConstants.SWITCH_FOCUS_KEY)) {
        ConsoleView.setActiveServer(bundle.findFramework().getName());
      }
    }
  }
  
  public static void disconnectFrameworkAction(FrameWork fw) {
    FrameworkConnectorFactory.disconnectFramework(fw);
  }
  
  public static void connectFrameworkAction(FrameWork framework) {
    FrameworkConnectorFactory.connectFrameWork(framework);
  }
  
  public static void refreshFrameworkAction(FrameWork fw) {
    fw.refreshAction();
  }
  
  public static void refreshBundleAction(Bundle bundle) {
    bundle.findFramework().refreshBundleAction(bundle);
  }
  
}

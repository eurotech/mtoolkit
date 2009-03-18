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
package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.logic;

import java.net.InetAddress;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.internal.ConsoleView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertySheet;


public class PropertySheetLogic implements SelectionListener, ConstantsDistributor {

  private PropertySheet target;
  private IMemento config;
  private FrameWork fw;
  private TreeViewer parentView;
  private boolean firstTime;
  private String elementOldName;

  public PropertySheetLogic (TreeViewer parentView, FrameWork element, boolean firstTime, PropertySheet obj) {
    this.parentView = parentView;
    this.fw = element;
    this.config = element.getConfig();
    this.firstTime = firstTime;
    this.target = obj;
    this.elementOldName = element.getName();
  }

  public void widgetDefaultSelected(SelectionEvent event) {
  }

  public void widgetSelected(SelectionEvent event) {
    if (event.getSource() instanceof Button) {
      Button button = (Button)event.getSource();

      //Connect
      if (button == target.connectButton) {
        if (isFrameworkInfoCorrect()) {
          changeSettings();
          FrameworkConnectorFactory.connectFrameWork(fw);
          target.close();
        }
      }
      // OK
      if (button == target.okButton) {
        if (isFrameworkInfoCorrect()) {
          changeSettings();
          target.close();
        }
      }
      // Cancel
      if (button == target.cancelButton) {
        target.close();
      }
      // Apply
      if (button == target.applyButton) {
        if (isFrameworkInfoCorrect()) {
          changeSettings();
        }
      }
    }
  }

  // Called after sheet is finished creating it's GUI
  public void sheetLoaded() {
    target.initValues(config);
    if (fw.isConnected() && fw.autoConnected) {
      target.setIPEditable(false);
    }
  }
  
  // Called when target options are changed
  public void changeSettings() {
    boolean consoleActive = FrameWorkView.getConsoleStatus();
    
    target.saveValues(config);
    fw.setName(config.getString(FRAMEWORK_ID));

    if (firstTime) {
      fw.getParent().addElement(fw);
      if (consoleActive) {
        ConsoleView.addServerConsole(fw.getName());
      }
      firstTime = false;
      elementOldName = fw.getName();
    } else {
      DeviceConnector connector = fw.getConnector();
      if (connector != null) {
        connector.getProperties().put("framework-name", fw.getName()); //$NON-NLS-1$
        String prevIP = (String) connector.getProperties().get(DeviceConnector.KEY_DEVICE_IP);
        connector.getProperties().put("framework-connection-ip", target.getNewIP()); //$NON-NLS-1$
        if (fw.isConnected() && !target.getNewIP().equals(prevIP)) {
          MessageDialog.openInformation(target.getShell(), 
              Messages.framework_ip_changed_title, 
              Messages.framework_ip_changed_message);
        }
      }
      fw.updateViewers();
      parentView.setSelection(parentView.getSelection());
      if (consoleActive) {
        ConsoleView.renameServerConsole(elementOldName, fw.getName());
      }
      elementOldName = fw.getName();
    }
  }

  // Set value of specified widget
  public boolean setValue(Text target, String key) {
    String result = config.getString(key); 

    if (result != null) {
      target.setText(result);
      return true;
    }
    else {
      return false;
    }
  }
  
  // Check for duplicate
  private boolean isFrameworkInfoCorrect() {
    String newName = target.getNewName().trim();
    if (newName.equals("")) { //$NON-NLS-1$
      
      BrowserErrorHandler.showInfoDialog(Messages.incorrect_framework_name_message);
      return false;
    }
    String ip = target.getNewIP().trim();
    boolean connectButtonState = target.connectButton.isEnabled();
    try {
      // just check if address is correct
      target.connectButton.setEnabled(false);
      target.okButton.setEnabled(false);
      target.applyButton.setEnabled(false);
      InetAddress.getByName(ip);
    } catch (Exception e) {
      e.printStackTrace();
      BrowserErrorHandler.showInfoDialog(Messages.incorrect_framework_ip_message);
      return false;
    } finally {
      target.connectButton.setEnabled(connectButtonState);
      target.okButton.setEnabled(true);
      target.applyButton.setEnabled(true);
    }
    
    Model[] frameworks = fw.getParent().getChildren();
    for (int i=0; i < frameworks.length; i++) {
      if (newName.equals(frameworks[i].getName()) &&
         !frameworks[i].equals(fw)) {
        BrowserErrorHandler.showInfoDialog(Messages.duplicate_framework_name_message);
        return false;
      }
    }
    return true;
  }
}
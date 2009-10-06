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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertySheet;

public class PropertySheetLogic implements SelectionListener, ConstantsDistributor {

	private PropertySheet target;
	private IMemento config;
	private FrameWork fw;
	private TreeViewer parentView;
	private boolean firstTime;
	private Model parent;

	public PropertySheetLogic(TreeViewer parentView, Model parent, FrameWork element, boolean firstTime,
			PropertySheet obj) {
		this.parentView = parentView;
		this.fw = element;
		this.config = element.getConfig();
		this.firstTime = firstTime;
		this.target = obj;
		this.parent = parent;
	}

	public void widgetDefaultSelected(SelectionEvent event) {
	}

	public void widgetSelected(SelectionEvent event) {
		if (event.getSource() instanceof Button) {
			Button button = (Button) event.getSource();
			// OK
			if (button == target.okButton) {
				if (isFrameworkInfoCorrect()) {
					changeSettings();
					if (target.connectButton != null && !target.connectButton.isDisposed()
							&& target.connectButton.getSelection()) {
						FrameworkConnectorFactory.connectFrameWork(fw);
					}
					target.close();
				}
			} else if (button == target.cancelButton) {
				target.close();
			} else if (button == target.chkSignContent) {
				target.tblCertificates.setEnabled(button.getSelection());
			}
		}
	}

	// Called after sheet is finished creating it's GUI
	public void sheetLoaded() {
		target.initValues(config);
		if (fw.isConnected() && fw.autoConnected) {
			target.setEditable(false);
		}
	}

	// Called when target options are changed
	public void changeSettings() {
		target.saveValues(config);
		fw.setName(config.getString(FRAMEWORK_NAME));

		if (firstTime) {
			parent.addElement(fw);
			firstTime = false;
		} else {
			DeviceConnector connector = fw.getConnector();
			if (connector != null) {
				connector.getProperties().put("framework-name", fw.getName()); //$NON-NLS-1$
				// String prevIP = (String)
				// connector.getProperties().get(DeviceConnector.KEY_DEVICE_IP);
				//				connector.getProperties().put("framework-connection-ip", target.getNewIP()); //$NON-NLS-1$
				// if (fw.isConnected() && !target.getNewIP().equals(prevIP)) {
				// MessageDialog.openInformation(target.getShell(),
				// Messages.framework_ip_changed_title,
				// Messages.framework_ip_changed_message);
				// }
			}
			fw.updateElement();
			parentView.setSelection(parentView.getSelection());
		}
	}

	// Set value of specified widget
	public boolean setValue(Control target, String key) {
		if (target instanceof Text) {
			String result = config.getString(key);

			if (result != null) {
				((Text) target).setText(result);
				return true;
			}
		} else if (target instanceof Button) {
			Boolean result = config.getBoolean(key);
			if (result != null) {
				((Button) target).setSelection(result.booleanValue());
				return true;
			}
		}
		return false;
	}

	// Check for duplicate
	private boolean isFrameworkInfoCorrect() {
		try {
			target.okButton.setEnabled(false);
			String newName = target.getNewName().trim();
			if (newName.equals("")) { //$NON-NLS-1$
				BrowserErrorHandler.showInfoDialog(Messages.incorrect_framework_name_message);
				return false;
			}

			Model[] frameworks = parent.getChildren();
			for (int i = 0; i < frameworks.length; i++) {
				if (newName.equals(frameworks[i].getName()) && !frameworks[i].equals(fw)) {
					BrowserErrorHandler.showInfoDialog(Messages.duplicate_framework_name_message);
					return false;
				}
			}

			boolean result = target.validate();
			if (!result) {
				BrowserErrorHandler.showInfoDialog(Messages.incorrect_framework_properties_message);
			}
			return result;
		} finally {
			target.okButton.setEnabled(true);
		}
	}
}
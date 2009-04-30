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
package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Vector;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;

public class ServicePropertiesPage extends PropertiesPage {

	public ServicePropertiesPage() {
		super();
	}

	protected String getGroupName() {
		return Messages.service_label;
	}

	public void setServiceName(String name) {
		propertiesGroup.setText(name);
	}

	public void setData(RemoteService service) {
		try {
			Dictionary props = service.getProperties();
			Vector data = new Vector();
			Enumeration keys = props.keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				Object value = props.get(key);
				if (value instanceof String[]) {
					String[] values = (String[]) value;
					if (values.length == 1) {
						PropertyObject object = new PropertyObject(key, values[0]);
						data.addElement(object);
					} else {
						for (int j = 0; j < values.length; j++) {
							StringBuffer buff = new StringBuffer();
							buff.append(key).append("[").append(String.valueOf(j + 1)).append("]");
							String key2 = buff.toString();
							PropertyObject object = new PropertyObject(key2, values[j]);
							data.addElement(object);
						}
					}
				} else {
					PropertyObject object = new PropertyObject(key, value.toString());
					data.addElement(object);
				}
			}
			tableViewer.setInput(data);
		} catch (IAgentException e) {
			BrowserErrorHandler.processError(e, true);
		} catch (Throwable t) {
			BrowserErrorHandler.processError(t, true);
		}
	}
}
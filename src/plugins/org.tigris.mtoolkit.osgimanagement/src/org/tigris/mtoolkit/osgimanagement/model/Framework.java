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
package org.tigris.mtoolkit.osgimanagement.model;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.internal.DeviceConnectorSWTWrapper;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;

/**
 * @since 5.0
 */
public abstract class Framework extends Model {

	public final static int BUNDLES_VIEW = 0;
	public final static int SERVICES_VIEW = 1;

	public boolean autoConnected;
	protected DeviceConnector connector;
	protected boolean connectedFlag;
	protected int viewType;
	protected List modelProviders = new ArrayList();
	// framework IP or other ID
	public static final String FRAMEWORK_ID = "framework_id_key"; //$NON-NLS-1$

	public Framework(String name, boolean autoConnected) {
		super(name);
		this.autoConnected = autoConnected;
	}

	public DeviceConnector getConnector() {
		return connector;
	}

	public boolean isConnected() {
		return connectedFlag;
	}
	
	public boolean isAutoConnected() {
		return autoConnected;
	}
	
	public int getViewType() {
		return viewType;
	}


	/**
	 * Returns map, containing information for certificates which shall be 
	 * used for signing the content, installed to this framework. If no signing
	 * is required, then empty Map is returned.
	 * @return the map with certificate properties
	 */
	public abstract Map getSigningProperties();

	public abstract Model createModel(String mimeType, String id, String version);

	public static Object getLockObject(DeviceConnector connector) {
		Object lockObj = FrameworkConnectorFactory.lockObjHash.get(connector);
		if (lockObj == null && !(connector instanceof DeviceConnectorSWTWrapper)) {
			Enumeration connectors = FrameworkConnectorFactory.lockObjHash.keys();
			while (connectors.hasMoreElements()) {
				DeviceConnector connKey = (DeviceConnector) connectors.nextElement();
				if (connKey.equals(connector)) {
					lockObj = FrameworkConnectorFactory.lockObjHash.get(connKey);
					break;
				}
			}
		}
		if (lockObj == null) {
			lockObj = new Object();
			FrameworkConnectorFactory.lockObjHash.put(connector, lockObj);
		}
		return lockObj;
	}
}

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
package org.tigris.mtoolkit.osgimanagement.browser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.tigris.mtoolkit.iagent.DeviceConnector;

public abstract class Framework extends Model {

	public final static int BUNDLES_VIEW = 0;
	public final static int SERVICES_VIEW = 1;

	protected DeviceConnector connector;
	protected boolean connectedFlag;
	protected int viewType;
	protected List modelProviders = new ArrayList();

	public Framework(String name) {
		super(name);
	}

	public DeviceConnector getConnector() {
		return connector;
	}

	public boolean isConnected() {
		return connectedFlag;
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
}

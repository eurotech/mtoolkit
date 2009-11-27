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
package org.tigris.mtoolkit.osgimanagement.dp.model;

import java.util.Dictionary;
import java.util.Enumeration;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;
import org.tigris.mtoolkit.osgimanagement.browser.model.Framework;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;

public class DeploymentPackage extends Model {

	private Framework framework;
	private RemoteDP dp;
	private final String DP_STALE_NAME = "stale"; //$NON-NLS-1$
	private final String DP_STALE_VALUE_TRUE = "true"; //$NON-NLS-1$
	private final String DP_STALE_VALUE_FALSE = "false"; //$NON-NLS-1$

	public DeploymentPackage(RemoteDP dp, Framework fw) throws IAgentException {
		super(dp.getName());
		this.dp = dp;
		this.framework = fw;
		Dictionary bundles = dp.getBundles();
		Enumeration keys = bundles.keys();
		while (keys.hasMoreElements()) {
			try {
				String name = (String) keys.nextElement();
				RemoteBundle bundle = dp.getBundle(name);

				if (bundle == null)
					continue;
				Model bundleNode = fw.createModel(ContentTypeModelProvider.MIME_TYPE_BUNDLE, Long.toString(bundle.getBundleId()), bundle.getVersion());
				if (bundleNode != null) {
					addElement(bundleNode);
				}
			} catch (IAgentException e) {
				e.printStackTrace();
			}
		}
	}

	public RemoteDP getRemoteDP() {
		return dp;
	}

	/**
	 * @return
	 * @throws IAgentException
	 */
	public boolean isStale() {
		try {
			return dp.isStale();
		} catch (IAgentException e) {
			e.printStackTrace();
			BrowserErrorHandler.processError(e, this);
		}
		return false;
	}

	// Overrides method in Model class
	public boolean testAttribute(Object target, String name, String value) {
		if (!(target instanceof DeploymentPackage)) {
			return false;
		}
		if (!framework.isConnected()) {
			return false;
		}

		if (name.equalsIgnoreCase(DP_STALE_NAME)) {
			if (value.equalsIgnoreCase(DP_STALE_VALUE_TRUE)) {
				return isStale();
			}
			if (value.equalsIgnoreCase(DP_STALE_VALUE_FALSE)) {
				return !isStale();
			}
		}

		return false;
	}

}

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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import java.util.Dictionary;
import java.util.Enumeration;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;

public class DeploymentPackage extends Model {

	private FrameworkImpl framework;
	private RemoteDP dp;

	public DeploymentPackage(RemoteDP dp, FrameworkImpl fw) throws IAgentException {
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
				Dictionary headers = bundle.getHeaders(null);
				Bundle bundleNode = new Bundle(name,
					bundle,
					bundle.getState(),
					fw.getRemoteBundleType(bundle, headers),
					(String) headers.get("Bundle-Category")); //$NON-NLS-1$
				addElement(bundleNode);
				Bundle bundleNodeInBundles = (Bundle) fw.findBundle(bundle.getBundleId());
				if (bundleNodeInBundles == null) {
					// log an error only if we are in debug mode, otherwise we
					// are not interested in it:)
					BrowserErrorHandler.debug(new IllegalStateException("Bundle " + bundle.getBundleId() + " is missing")); //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}
				Model children[] = bundleNodeInBundles.getChildren();
				if (children != null && children.length > 0) {
//					Model[] categories = FrameworkConnectorFactory.addServiceCategoriesNodes(bundleNode);
//					if (categories == null) {
//						continue;
//					}
					Model regServ[] = children[0].getChildren();
					if (regServ != null) {
						Model servCategory = fw.getServiceCategoryNode(bundleNodeInBundles, ServicesCategory.REGISTERED_SERVICES, true);
						for (int i = 0; i < regServ.length; i++) {
							ObjectClass oc = new ObjectClass(regServ[i].getName(),
								new Long(((ObjectClass) regServ[i]).getService().getServiceId()),
								((ObjectClass) regServ[i]).getService());
							servCategory.addElement(oc);
							if (fw != null &&  fw.isShownServicePropertiss()) {
								try {
									fw.addServicePropertiesNodes(oc);
								} catch (IAgentException e) {
									e.printStackTrace();
								}
							}
						}
					}

					Model usedServ[] = children[1].getChildren();
					if (usedServ != null) {
						Model servCategory = fw.getServiceCategoryNode(bundleNodeInBundles, ServicesCategory.USED_SERVICES, true);
						for (int i = 0; i < usedServ.length; i++) {
							ObjectClass oc = new ObjectClass(usedServ[i].getName(),
								new Long(((ObjectClass) usedServ[i]).getService().getServiceId()),
								((ObjectClass) usedServ[i]).getService());
							servCategory.addElement(oc);
							if (fw != null &&  fw.isShownServicePropertiss()) {
								try {
									fw.addServicePropertiesNodes(oc);
								} catch (IAgentException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
				if (framework.getViewType() == FrameworkImpl.SERVICES_VIEW) {
					framework.removeElement(bundleNode.getParent().getParent());
				}
			} catch (IllegalStateException e) {
				// bundle was uninstalled
			} catch (Throwable t) {
				t.printStackTrace();
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
		if (!(target instanceof org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage)) {
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

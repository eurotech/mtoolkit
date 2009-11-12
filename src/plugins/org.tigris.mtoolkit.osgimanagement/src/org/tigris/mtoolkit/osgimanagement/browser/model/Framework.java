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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IMemento;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;

public class Framework extends Model {

	public final static int BUNDLES_VIEW = 0;
	public final static int SERVICES_VIEW = 1;

	protected DeviceConnector connector;
	protected boolean connectedFlag;
	protected int viewType;
	protected IMemento configs;
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
	public Map getSigningProperties() {
		Map properties = new Hashtable();
		List certUids = getSignCertificateUids(getConfig());
		Iterator signIterator = certUids.iterator();
		int certId = 0;
		while (signIterator.hasNext()) {
			ICertificateDescriptor cert = CertUtils.getCertificate((String) signIterator.next());
			if (cert != null) {
				CertUtils.pushCertificate(properties, cert, certId++);
			}
		}
		return properties;
	}

	public static List getSignCertificateUids(IMemento config) {
		String keys[] = config.getAttributeKeys();
		List result = new ArrayList();
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].startsWith(FRAMEWORK_SIGN_CERTIFICATE_ID)) {
				String uid = config.getString(keys[i]);
				if (uid != null && uid.trim().length() > 0) {
					result.add(uid.trim());
				}
			}
		}
		return result;
	}

	public static void setSignCertificateUids(IMemento config, List uids) {
		String keys[] = config.getAttributeKeys();
		for (int i = 0; i < keys.length; i++) {
			if (keys[i].startsWith(FRAMEWORK_SIGN_CERTIFICATE_ID)) {
				config.putString(keys[i], ""); //$NON-NLS-1$
			}
		}
		Iterator iterator = uids.iterator();
		int num = 0;
		while (iterator.hasNext()) {
			config.putString(FRAMEWORK_SIGN_CERTIFICATE_ID + num, (String) iterator.next());
			num++;
		}
	}

	public IMemento getConfig() {
		return configs;
	}


	public List getModelProviders() {
		return modelProviders;
	}

	protected void obtainModelProviders() {
		modelProviders.clear();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry
				.getExtensionPoint("org.tigris.mtoolkit.osgimanagement.contentTypeExtensions");

		obtainModelProviderElements(extensionPoint.getConfigurationElements(), modelProviders);
	}

	private void obtainModelProviderElements(IConfigurationElement[] elements, List providers) {
		for (int i = 0; i < elements.length; i++) {
			if (!elements[i].getName().equals("model")) {
				continue;
			}
			String clazz = elements[i].getAttribute("class");
			if (clazz == null) {
				continue;
			}

			ModelProviderElement providerElement = new ModelProviderElement(elements[i]);
			if (providers.contains(providerElement))
				continue;

			try {
				Object provider = elements[i].createExecutableExtension("class");

				if (provider instanceof ContentTypeModelProvider) {
					providerElement.setProvider(((ContentTypeModelProvider) provider));
					providers.add(providerElement);
				}
			} catch (CoreException e) {
				FrameworkPlugin.error(e.getMessage(), e);
			}
		}
	}

	public class ModelProviderElement {
		private String extension;
		private String clazz;
		private ContentTypeModelProvider provider;
		private IConfigurationElement confElement;

		public ModelProviderElement(IConfigurationElement configurationElement) {
			confElement = configurationElement;
			extension = configurationElement.getAttribute("extension");
			clazz = configurationElement.getAttribute("class");
		}

		public void setProvider(ContentTypeModelProvider provider) {
			this.provider = provider;
		}

		public IConfigurationElement getConfigurationElement() {
			return confElement;
		}

		public ContentTypeModelProvider getProvider() {
			return provider;
		}

		public boolean equals(ModelProviderElement otherElement) {
			if (this.clazz.equals(otherElement.clazz) && this.extension.equals(otherElement.extension))
				return true;
			return false;
		}
	}


}

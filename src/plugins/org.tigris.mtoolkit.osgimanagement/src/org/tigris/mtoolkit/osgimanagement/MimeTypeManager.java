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
package org.tigris.mtoolkit.osgimanagement;

import java.util.List;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.browser.model.Framework;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;

public class MimeTypeManager {

	private Vector registeredTypes = new Vector();
	
	private static MimeTypeManager manager = new MimeTypeManager();
	
	static {
		manager.obtainMimeTypeProviders();
	}
	
	private void obtainMimeTypeProviders() {
		registeredTypes.clear();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry
				.getExtensionPoint("org.tigris.mtoolkit.osgimanagement.mimeTypeExtensions");

		obtainMimeTypeProviderElements(extensionPoint.getConfigurationElements(), registeredTypes);
	}

	private void obtainMimeTypeProviderElements(IConfigurationElement[] elements, List providers) {
		for (int i = 0; i < elements.length; i++) {
			if (!elements[i].getName().equals("mime_type")) {
				continue;
			}
			String clazz = elements[i].getAttribute("class");
			if (clazz == null) {
				continue;
			}

			MimeTypeProviderElement providerElement = new MimeTypeProviderElement(elements[i]);
			if (providers.contains(providerElement))
				continue;

			try {
				Object provider = elements[i].createExecutableExtension("class");

				if (provider instanceof MimeTypeContentProvider) {
					providerElement.setProvider(((MimeTypeContentProvider) provider));
					providers.add(providerElement);
				}
			} catch (CoreException e) {
				FrameworkPlugin.error(e.getMessage(), e);
			}
		}
	}
	
	public static Model createModel(Framework fw, String mimeType, String id, String version) {
		Model model = null;
		for (int i=0; i<manager.registeredTypes.size(); i++) {
			MimeTypeProviderElement providerElement = ((MimeTypeProviderElement)manager.registeredTypes.elementAt(i));
			MimeTypeContentProvider provider = providerElement.getProvider();
			String types[] = provider.getSupportedMimeTypes();
			for (int j=0; j<types.length; j++) {
				if (mimeType.equals(types[j])) {
					try {
						model = provider.getResource(id, version, fw);
					} catch (IAgentException e) {
						e.printStackTrace();
					}
					return model;
				}
			}
		}
		return model;
	}

	public class MimeTypeProviderElement {
		private String type;
		private String clazz;
		private MimeTypeContentProvider provider;
		private IConfigurationElement confElement;

		public MimeTypeProviderElement(IConfigurationElement configurationElement) {
			confElement = configurationElement;
			type = configurationElement.getAttribute("type");
			clazz = configurationElement.getAttribute("class");
		}

		public void setProvider(MimeTypeContentProvider provider) {
			this.provider = provider;
		}

		public IConfigurationElement getConfigurationElement() {
			return confElement;
		}

		public MimeTypeContentProvider getProvider() {
			return provider;
		}

		public boolean equals(MimeTypeProviderElement otherElement) {
			if (this.clazz.equals(otherElement.clazz) && this.type.equals(otherElement.type))
				return true;
			return false;
		}
	}


	
}

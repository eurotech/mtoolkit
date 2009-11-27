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

import org.eclipse.core.runtime.IConfigurationElement;
import org.tigris.mtoolkit.osgimanagement.ContentTypeModelProvider;

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

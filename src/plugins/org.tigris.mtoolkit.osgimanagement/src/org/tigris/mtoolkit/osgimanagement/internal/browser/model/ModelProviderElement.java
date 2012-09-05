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

public final class ModelProviderElement {
	private final String clazz;
	private final String extension;
	private final IConfigurationElement confElement;
	private ContentTypeModelProvider provider;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof ModelProviderElement)) {
			return false;
		}
		ModelProviderElement otherElement = (ModelProviderElement) other;
		if (!this.clazz.equals(otherElement.clazz)) {
			return false;
		}
		if (this.extension == null) {
			return otherElement.extension == null;
		}
		return this.extension.equals(otherElement.extension);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		if (extension == null) {
			return this.clazz.hashCode();
		}
		return this.clazz.hashCode() ^ this.extension.hashCode();
	}
}

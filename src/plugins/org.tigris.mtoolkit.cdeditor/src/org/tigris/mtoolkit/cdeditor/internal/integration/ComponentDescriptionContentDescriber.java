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
package org.tigris.mtoolkit.cdeditor.internal.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.ITextContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.common.ReflectionUtils;
import org.tigris.mtoolkit.common.ReflectionUtils.InvocationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is intended to determine whether contents of provided file is 
 * valid Component Description.
 */
public class ComponentDescriptionContentDescriber implements ITextContentDescriber {

	private ITextContentDescriber describer = null;
	
	public ComponentDescriptionContentDescriber() {
		String className = PluginUtilities.compareVersion("org.eclipse.core.contenttype", PluginUtilities.VERSION_3_5_0) ? "org.eclipse.core.runtime.content.XMLContentDescriber" : "org.eclipse.core.internal.content.XMLContentDescriber";
		try {
			describer = (ITextContentDescriber) ReflectionUtils.newInstance(className, null, null);
		} catch (InvocationException e) {
			e.printStackTrace();
		}
	}
	
	private int checkComponentDescription(InputSource source) throws IOException {
		ComponentDescriptionHandler handler = new ComponentDescriptionHandler();
		try {
			handler.parseContents(source);
		} catch (ParserConfigurationException e) {
			// unable to use XML parser, disable the content describer
			throw new RuntimeException("Unable to create SAX parser for component description content describing", e); //$NON-NLS-1$
		} catch (SAXException e) {
			// failed to parse the content
			return INDETERMINATE;
		}
		if (handler.isComponentDescriptionFound()) {
			return VALID;
		}
		return INDETERMINATE;
	}
	
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		if (describer.describe(contents, description) == INVALID) {
			return INVALID;
		}
		contents.reset();
		return checkComponentDescription(new InputSource(contents));
	}

	public int describe(Reader contents, IContentDescription description) throws IOException {
		if (describer.describe(contents, description) == INVALID) {
			return INDETERMINATE;
		}
		contents.reset();
		return checkComponentDescription(new InputSource(contents));
	}

	public QualifiedName[] getSupportedOptions() {
		return describer.getSupportedOptions();
	}
}
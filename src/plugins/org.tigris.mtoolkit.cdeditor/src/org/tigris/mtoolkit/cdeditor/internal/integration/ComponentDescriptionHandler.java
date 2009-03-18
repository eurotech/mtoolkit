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
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Class ComponentDescriptionHandler is SAX event handler for Component
 * Descriptions.
 */
public class ComponentDescriptionHandler extends DefaultHandler {
	
	public static final String SCR_NAMESPACE = "http://www.osgi.org/xmlns/scr/v1.0.0";
	
	private boolean foundComponentDescription; 
	
	private class StopParsingException extends SAXException {
		/**
		 * All serializable objects should have a stable serialVersionUID
		 */
		private static final long serialVersionUID = 1L;

		/**
		 * Constructs an instance of <code>StopParsingException</code> with a
		 * <code>null</code> detail message.
		 */
		public StopParsingException() {
			super((String) null);
		}
	}

	private SAXParserFactory fFactory;
	private int fLevel;

	private SAXParser createParser(SAXParserFactory parserFactory)
	throws ParserConfigurationException, SAXException,
	SAXNotRecognizedException, SAXNotSupportedException {
		// Initialize the parser.
		final SAXParser parser = parserFactory.newSAXParser();
		final XMLReader reader = parser.getXMLReader();
		// disable DTD validation
		try {
			//  be sure validation is "off" or the feature to ignore DTD's will not apply
			reader.setFeature("http://xml.org/sax/features/validation", false); //$NON-NLS-1$
			reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); //$NON-NLS-1$
		} catch (SAXNotRecognizedException e) {
			// not a big deal if the parser does not recognize the features
		} catch (SAXNotSupportedException e) {
			// not a big deal if the parser does not support the features
		}
		return parser;
	}

	private SAXParserFactory getFactory() {
		synchronized (this) {
			if (fFactory != null) {
				return fFactory;
			}
			fFactory = SAXParserFactory.newInstance();
			fFactory.setNamespaceAware(true);
		}
		return fFactory;
	}

	protected boolean parseContents(InputSource contents) throws IOException, ParserConfigurationException, SAXException {
		// Parse the file into we have what we need (or an error occurs).
		try {
			fFactory = getFactory();
			if (fFactory == null) {
				return false;
			}
			final SAXParser parser = createParser(fFactory);
			// to support external entities specified as relative URIs (see bug 63298)
			contents.setSystemId("/"); //$NON-NLS-1$
			parser.parse(contents, this);
		} catch (StopParsingException e) {
			// Abort the parsing normally. Fall through...
		}
		return true;
	}

	/**
	 * Resolve external entity definitions to an empty string.  This is to speed
	 * up processing of files with external DTDs.  Not resolving the contents 
	 * of the DTD is ok, as only the System ID of the DTD declaration is used.
	 * @see org.xml.sax.helpers.DefaultHandler#resolveEntity(java.lang.String, java.lang.String)
	 */
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

	public final void startElement(final String uri, final String elementName, final String qualifiedName, final Attributes attributes) throws SAXException {
		if (SCR_NAMESPACE.equals(uri) && "component".equals(elementName)) {
			foundComponentDescription = true;	// component element in the SCR namespace was found, stop parsing
			throw new StopParsingException();
		} else if ("component".equals(elementName) && fLevel == 0) {
			foundComponentDescription = true;	// the root element is a component description, stop parsing
			throw new StopParsingException();
		}
		fLevel++;
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		fLevel--;
	}

	public boolean isComponentDescriptionFound() {
		return foundComponentDescription;
	}

}

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
package org.tigris.mtoolkit.cdeditor.internal.model;

import org.eclipse.jface.text.IDocument;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.CDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.impl.ComponentDescriptionValidator;
import org.xml.sax.SAXException;


/**
 * CDFactory allows easy creation of new Components and CD Models.
 */
public class CDFactory {

	public static ICDComponent createComponent() {
		CDComponent component = new CDComponent();
		return component;
	}
	
	public static ICDModel createModel(String rootTagName, String namespacePrefix) {
		CDModel model = new CDModel();
		model.setXMLTagName(rootTagName);
		if (namespacePrefix == null)
			namespacePrefix = "scr";
		if (namespacePrefix.equals(""))
			model.setXMLAttribute("xmlns", ComponentDescriptionValidator.SCR_NAMESPACE);
		else
			model.setXMLAttribute("xmlns:" + namespacePrefix, ComponentDescriptionValidator.SCR_NAMESPACE);
		return model;
	}

	public static ICDModel createModel(IDocument document) throws SAXException {
		CDModel model = new CDModel();
		model.load(document);
		return model;
	}
	
/*	public ICDComponent copy(ICDComponent component) {
		ICDComponent newComponent = createComponent(component.getName(), component.getImplementationClass());
		return newComponent;
	}
	
	public ICDProperty createProperty(String name, String value) {
		
	}
	
	public ICDProperty createProperty(String name, String[] values) {
		
	}
	
	public ICDProperty copy(ICDProperty property) {
		
	}
	
	public ICDReference createReference(String name, String className) {
		
	}
	
	public ICDReference copy(ICDReference reference) {
		
	}
	
	public ICDProperties createProperties(String entry) {
		
	}
	
	public ICDProperties copy(ICDProperties properties) {
		
	}
	
	public ICDService createService(String[] interfaces) {
		
	}
	
	public ICDService createService(String singleInterface) {
		
	}
	
	public ICDService copy(ICDService service) {
		
	}
*/
}

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
package org.tigris.mtoolkit.cdeditor.internal.model.impl;

import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDService;
import org.tigris.mtoolkit.cdeditor.internal.text.PlainDocumentHandler;


public class ComponentDocumentHandler extends PlainDocumentHandler {

	private ICDModel model;

	public ComponentDocumentHandler(ICDModel model, boolean reconciling) {
		super(reconciling);
		this.model = model;
	}

	protected IDocument getDocument() {
		return ((CDModel) model).getDocument();
	}

	protected IDocumentElementNode doGetDocumentNode(String name, IDocumentElementNode parent) {
		String localName = ModelUtil.getElementLocalName(name);

		IDocumentElementNode node = null;
		if (parent == null) {
			if (localName.equals("component")) {
				model.setSingle(true);
				CDComponent component = (CDComponent) model.getSingleComponent();
				if (component == null) {
					component = new CDComponent();
					model.setSingleComponent(component);
				}
				node = component;
			} else {
				model.setSingle(false);
				node = (IDocumentElementNode) model;
			}
		} else {
			node = findExistingChildNode(name, parent);
		}

		if (node == null) {
			if (localName.endsWith("component") && !insideComponentDescription(parent)) {
				node = new CDComponent();
			} else if ("reference".equals(localName) && parent instanceof ICDComponent) {
				node = new CDReference();
			} else if ("property".equals(localName) && parent instanceof ICDComponent) {
				node = new CDProperty();
			} else if ("properties".equals(localName) && parent instanceof ICDComponent) {
				node = new CDProperties();
			} else if ("service".equals(localName) && parent instanceof ICDComponent) {
				node = new CDService();
			} else if ("provide".equals(localName) && parent instanceof ICDService) {
				node = new CDInterface();
			} else {
				node = new CDElement();
			}
		}
		return node;
	}

	private boolean insideComponentDescription(IDocumentElementNode parent) {
		if (parent instanceof ICDComponent)
			return true;
		else if (parent != null)
			return insideComponentDescription(parent.getParentNode());
		else
			return false;
	}

}

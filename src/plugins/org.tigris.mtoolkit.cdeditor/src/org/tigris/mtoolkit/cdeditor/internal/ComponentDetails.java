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
package org.tigris.mtoolkit.cdeditor.internal;

import java.util.Iterator;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IDetailsPage;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.tigris.mtoolkit.cdeditor.internal.model.CDModelEvent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModifyListener;
import org.tigris.mtoolkit.cdeditor.internal.parts.AggregatedFormPart;
import org.tigris.mtoolkit.cdeditor.internal.parts.ComponentDetailsPart;
import org.tigris.mtoolkit.cdeditor.internal.parts.GeneralDetailsPart;
import org.tigris.mtoolkit.cdeditor.internal.parts.PropertyDetailsPart;
import org.tigris.mtoolkit.cdeditor.internal.parts.ReferenceDetailsPart;
import org.tigris.mtoolkit.cdeditor.internal.parts.ServiceDetailsPart;

/**
 * The Details part of Master-Details block describing component details. 
 * This class is intended to be used with <b>ComponentsBlock</b>.
 */
public class ComponentDetails extends AggregatedFormPart implements
		IDetailsPage, ICDModifyListener {

	private ICDComponent component = null;

	private ReferenceDetailsPart referenceDetailsPart;
	private GeneralDetailsPart componentGeneralPart;
	private ServiceDetailsPart providedServicePart;
	private PropertyDetailsPart propertyDetailsPart;

	private Composite componentDetailsParent;
	private boolean ignoreReflow = false;

	public void initialize(IManagedForm form) {
		super.initialize(form);
		((ICDModel) form.getInput()).addModifyListener(this);
	}

	public void createContents(Composite parent) {
		this.componentDetailsParent = parent;

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 5;
		layout.marginWidth = 0;
		parent.setLayout(layout);

		createDetailsSection(parent);
		createReferencesSection(parent);
		createServiceSection(parent);
		createPropertiesSection(parent);
	}

	private void createDetailsSection(Composite parent) {
		componentGeneralPart = new GeneralDetailsPart();
		addPart(componentGeneralPart);
		componentGeneralPart.createContents(parent);
	}

	private void createReferencesSection(Composite parent) {
		referenceDetailsPart = new ReferenceDetailsPart();
		addPart(referenceDetailsPart);
		referenceDetailsPart.createContents(parent);
	}

	private void createServiceSection(Composite parent) {
		providedServicePart = new ServiceDetailsPart();
		addPart(providedServicePart);
		providedServicePart.createContents(parent);
	}

	private void createPropertiesSection(Composite parent) {
		propertyDetailsPart = new PropertyDetailsPart();
		addPart(propertyDetailsPart);
		propertyDetailsPart.createContents(parent);

	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.isEmpty()) {
			component = null;
		} else {
			Object selectedObject = (sel.size() > 0) ? sel.getFirstElement() : null;
			if (selectedObject instanceof ICDComponent) {
				component = (ICDComponent) selectedObject;
			}
		}

		componentGeneralPart.selectionChanged(part, selection);
		providedServicePart.selectionChanged(part, selection);
		referenceDetailsPart.selectionChanged(part, selection);
		propertyDetailsPart.selectionChanged(part, selection);

		refresh();
	}

	public ICDComponent getSelectedComponent() {
		return component;
	}

	protected void doCommit(boolean onSave) {
		// nothing to commit
	}

	protected void doRefresh() {
		// nothing to refresh
	}

	public Composite getParent() {
		return componentDetailsParent;
	}

	public void modelModified(CDModelEvent event) {
		ignoreReflow = true;
		try {
			for (Iterator it = getParts().iterator(); it.hasNext();) {
				ComponentDetailsPart part = (ComponentDetailsPart) it.next();
				part.modelModified(event);
			}
		} finally {
			ignoreReflow = false;
		}
		reflowContainer();
	}

	protected void reflowContainer() {
		// here we are causing a reflow of the container
		// another way to achieve the same, is to relayout the section parent
		// and call IManagedForm.reflow() (which will show the scrollbars)
		SharedScrolledComposite sc = null;
		Composite c = getParent();
		while (c != null) {
			if (c instanceof SharedScrolledComposite) {
				sc = (SharedScrolledComposite) c;
				break;
			}
			c = c.getParent();
		}
		if (sc != null) {
			sc.setRedraw(false);
		}
		try {
			c = getParent();
			while (c != null) {
				c.layout();
				if (c instanceof SharedScrolledComposite) {
					// call twice reflow because of bug 242258
					((SharedScrolledComposite) c).reflow(true);
					((SharedScrolledComposite) c).reflow(true);
					break;
				}
				c = c.getParent();
			}
		} finally {
			if (sc != null) {
				sc.setRedraw(true);
			}
		}
	}

	public void refresh() {
		super.refresh();
		if (!ignoreReflow)
			reflowContainer();
	}
}

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
/**
 * 
 */
package org.tigris.mtoolkit.cdeditor.internal.providers;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.widgets.UIResources;
import org.tigris.mtoolkit.cdeditor.widgets.StatusShowingIcon;


public class MasterLabelProvider extends LabelProvider {

	private StatusShowingIcon icon = new StatusShowingIcon(UIResources.getImageDescriptor(UIResources.COMPONENT_ICON));

	public String getText(Object element) {
		if (element instanceof ICDComponent) {
			return ((ICDComponent) element).getName();
		}
		return element.toString();
	}

	public Image getImage(Object element) {
		if (element instanceof ICDComponent) {
			ICDComponent comp = (ICDComponent) element;
			IStatus validationStatus = getValidationStatus(comp);
			return icon.getIcon(validationStatus.getSeverity());
		}
		return null;
	}

	public void dispose() {
		if (icon != null)
			icon.dispose();
	}

	private IStatus getValidationStatus(ICDComponent comp) {
		return comp.getModel().getAggregatedValidationStatus(comp)[0];
	}

}
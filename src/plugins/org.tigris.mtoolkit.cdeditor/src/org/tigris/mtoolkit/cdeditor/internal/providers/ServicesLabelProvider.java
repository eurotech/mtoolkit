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
package org.tigris.mtoolkit.cdeditor.internal.providers;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.ui.ISharedImages;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDInterface;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.widgets.StatusShowingIcon;


public class ServicesLabelProvider extends LabelProvider {

	private StatusShowingIcon icon = new StatusShowingIcon(JavaUI.getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_INTERFACE));

	public Image getImage(Object element) {
		if (element instanceof ICDInterface) {
			ICDInterface xface = (ICDInterface) element;
			ICDModel model = xface.getModel();
			if (model == null) {
				return null;
			}
			IStatus validationStatus = model.getAggregatedValidationStatus(xface)[0];
			return icon.getIcon(validationStatus.getSeverity());
		}
		return null;
	}

	public void dispose() {
		if (icon != null)
			icon.dispose();
	}

	public String getText(Object element) {
		if (element instanceof ICDInterface) {
			return ((ICDInterface) element).getInterface();
		}
		return super.getText(element);
	}

}

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
package org.tigris.mtoolkit.osgimanagement.internal;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;

public class MyViewerFilter extends ViewerFilter {

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (element instanceof Model) {
			boolean selected = ((Model) element).isVisible();
			return selected;
		}
		return true;
	}

}

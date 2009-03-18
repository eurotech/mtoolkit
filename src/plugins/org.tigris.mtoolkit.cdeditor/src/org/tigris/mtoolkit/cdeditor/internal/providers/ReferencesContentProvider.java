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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;


public class ReferencesContentProvider implements ITreeContentProvider {
	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ICDComponent) {
			return ((ICDComponent)inputElement).getReferences();
		}
		return new Object[0];
	}

	public Object[] getChildren(Object parentElement) {
		if(parentElement instanceof ICDComponent){
			return ((ICDComponent)parentElement).getReferences();
		}
		return new Object[0];
	}

	public Object getParent(Object element) {
		if(element instanceof ICDReference){
			return ((ICDReference)element).getParent();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}

}

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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;


public class InterfacesContentProvider extends BaseContentProvider implements
		IStructuredContentProvider {

	public Object[] getElements(Object inputElement) {
		if (inputElement instanceof ICDComponent) {
			if (((ICDComponent) inputElement).getService() == null) {
				return new Object[0];
			}
			return ((ICDComponent) inputElement).getService().getInterfaces();
		}
		return new Object[0];
	}

}

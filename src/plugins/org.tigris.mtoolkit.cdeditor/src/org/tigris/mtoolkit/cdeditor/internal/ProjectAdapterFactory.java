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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdapterFactory;
import org.tigris.mtoolkit.cdeditor.internal.integration.ComponentProjectContext;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;

/**
 * Adapter factory for adapting objects of type 
 * <b>org.eclipse.core.resources.IProject</b> to objects of type 
 * <b>org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext</b>.
 */
public class ProjectAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		if (adapterType.equals(IEclipseContext.class))
			return new ComponentProjectContext((IProject) adaptableObject);
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IEclipseContext.class };
	}

}

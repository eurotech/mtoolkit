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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.tigris.mtoolkit.cdeditor.internal.integration.ComponentProjectContext;

/**
 * Adapter factory for adapting objects of type 
 * <b>org.tigris.mtoolkit.cdeditor.internal.integration.ComponentProjectContext</b>
 * to objects of type <b>org.eclipse.core.resources.IProject</b> or objects of
 * type <b>org.eclipse.jdt.core.IJavaProject</b>.
 */
public class ContextAdapterFactory implements IAdapterFactory {

	public Object getAdapter(Object adaptableObject, Class adapterType) {
		ComponentProjectContext context = (ComponentProjectContext) adaptableObject;
		if (adapterType.equals(IProject.class))
			return context.getProject();
		if (adapterType.equals(IJavaProject.class)) {
			IJavaProject javaProject = JavaCore.create(context.getProject());
			if (javaProject.exists())
				return javaProject;
		}
		return null;
	}

	public Class[] getAdapterList() {
		return new Class[] { IJavaProject.class, IProject.class };
	}

}

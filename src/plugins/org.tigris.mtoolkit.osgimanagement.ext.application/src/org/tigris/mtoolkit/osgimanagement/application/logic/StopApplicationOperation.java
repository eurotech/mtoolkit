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
package org.tigris.mtoolkit.osgimanagement.application.logic;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.application.model.Application;


public class StopApplicationOperation extends RemoteApplicationOperation {
	
	public StopApplicationOperation(Application application) {
		super("Stop Application", application);
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		getApplication().getRemoteApplication().stop();
		return Status.OK_STATUS;
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind("Application {0} stop failed", getApplication().toString());
	}
}
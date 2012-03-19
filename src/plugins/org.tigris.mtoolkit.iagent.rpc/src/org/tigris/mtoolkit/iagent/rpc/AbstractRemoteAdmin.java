/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.rpc;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public abstract class AbstractRemoteAdmin implements RemoteServiceIDProvider, Remote {
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.tigris.mtoolkit.iagent.rpc.RemoteServiceIDProvider#getRemoteServiceID
	 * ()
	 */
	public final long getRemoteServiceID() {
		try {
			ServiceRegistration localRegistration = getServiceRegistration();
			if (localRegistration == null) {
				return SERVICE_UNAVAILABLE;
			}
			ServiceReference localRef = localRegistration.getReference();
			if (localRef == null) {
				return SERVICE_UNAVAILABLE;
			}
			return ((Long) localRef.getProperty(Constants.SERVICE_ID)).longValue();
		} catch (IllegalStateException e) {
			// catch it in case the service is unregistered mean while
			return SERVICE_UNAVAILABLE;
		}
	}

	protected abstract ServiceRegistration getServiceRegistration();
}

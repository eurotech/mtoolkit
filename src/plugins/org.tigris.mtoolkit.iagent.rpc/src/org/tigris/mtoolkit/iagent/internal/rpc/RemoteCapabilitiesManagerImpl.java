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
package org.tigris.mtoolkit.iagent.internal.rpc;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.rpc.AbstractRemoteAdmin;
import org.tigris.mtoolkit.iagent.rpc.Remote;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesProvider;

public class RemoteCapabilitiesManagerImpl extends AbstractRemoteAdmin implements Remote, RemoteCapabilitiesProvider,
		RemoteCapabilitiesManager {

	private static final String PROPERTY_EVENT = "iagent_property_event";
	private static final String EVENT_CAPABILITY_NAME = "capability.name";
	private static final String EVENT_CAPABILITY_VALUE = "capability.value";

	BundleContext bc;
	private ServiceRegistration registration;
	private Hashtable capabilities = new Hashtable();

	public Class[] remoteInterfaces() {
		return new Class[] { RemoteCapabilitiesProvider.class, RemoteCapabilitiesManager.class };
	}

	public void register(BundleContext bundleContext) {
		this.bc = bundleContext;
		initCapabilities();

		registration = bc.registerService(new String[] { RemoteCapabilitiesProvider.class.getName(),
				RemoteCapabilitiesManager.class.getName() }, this, null);
	}

	public void unregister(BundleContext bundleContext) {
		if (registration != null) {
			registration.unregister();
			registration = null;
		}

		this.bc = null;
	}

	private void initCapabilities() {
		capabilities.clear();
	}

	public Map getCapabilities() {
		return (Map) capabilities.clone();
	}

	public void setCapability(String capability, Object value) {
		capabilities.put(capability, value);

		EventSynchronizer synchronizer = Activator.getSynchronizer();
		if (synchronizer != null) {
			Dictionary pmpEventData = new Hashtable();
			pmpEventData.put(EVENT_CAPABILITY_NAME, capability);
			pmpEventData.put(EVENT_CAPABILITY_VALUE, value);
			synchronizer.enqueue(new EventData(pmpEventData, PROPERTY_EVENT));
		}
	}

	protected ServiceRegistration getServiceRegistration() {
		return registration;
	}
}

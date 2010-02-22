/*******************************************************************************
 * Copyright (c) 2005, 2010 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal;

import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.BundleSnapshot;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.rpc.RemoteBundleAdmin;

public class BundleSnapshotImpl implements BundleSnapshot {

	private long bid;
	private RemoteBundle rBundle;
	private int state;
	private Dictionary headers;
	private RemoteService[] registeredServices;
	private RemoteService[] usedServices;
	private DeploymentManagerImpl deploymentManager;

	public BundleSnapshotImpl(DeploymentManagerImpl deploymentManager, Dictionary props) throws IAgentException {
		this.deploymentManager = deploymentManager;

		// Bundle ID should always be available
		this.bid = ((Long) props.get(RemoteBundleAdmin.KEY_BUNDLE_ID)).longValue();
		this.rBundle = new RemoteBundleImpl(deploymentManager, new Long(bid));

		Integer stateVal = (Integer) props.get(RemoteBundleAdmin.KEY_BUNDLE_STATE);
		if (stateVal != null) {
			this.state = stateVal.intValue();
		}

		Dictionary headersVal = (Dictionary) props.get(RemoteBundleAdmin.KEY_BUNDLE_HEADERS);
		if (headersVal != null) {
			this.headers = headersVal;
		}

		Dictionary[] registeredSvcsProps = (Dictionary[]) props.get(RemoteBundleAdmin.KEY_REGISTERED_SERVICES);
		if (registeredSvcsProps != null) {
			this.registeredServices = getRemoteServices(registeredSvcsProps);
		}

		Dictionary[] usedSvcsProps = (Dictionary[]) props.get(RemoteBundleAdmin.KEY_USED_SERVICES);
		if (usedSvcsProps != null) {
			this.usedServices = getRemoteServices(usedSvcsProps);
		}
	}

	public RemoteBundle getRemoteBundle() {
		return rBundle;
	}

	public int getBundleState() {
		return state;
	}

	public Dictionary getBundleHeaders() {
		return headers;
	}

	public RemoteService[] getRegisteredServices() {
		return registeredServices;
	}

	public RemoteService[] getUsedServices() {
		return usedServices;
	}

	private RemoteService[] getRemoteServices(Dictionary[] servicesProps) throws IAgentException {
		RemoteService[] services = new RemoteService[servicesProps.length];
		for (int i = 0; i < servicesProps.length; i++) {
			services[i] = new RemoteServiceImpl((ServiceManagerImpl) deploymentManager.getDeviceConnector()
					.getServiceManager(), servicesProps[i]);
		}
		return services;
	}
}

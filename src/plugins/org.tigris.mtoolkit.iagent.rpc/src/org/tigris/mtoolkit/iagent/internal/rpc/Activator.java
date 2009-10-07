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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.internal.VMCommander;
import org.tigris.mtoolkit.iagent.internal.rpc.console.EquinoxRemoteConsole;
import org.tigris.mtoolkit.iagent.internal.rpc.console.ProSystRemoteConsole;
import org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;
import org.tigris.mtoolkit.iagent.pmp.PMPServerFactory;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.pmp.PMPServiceFactory;
import org.tigris.mtoolkit.iagent.rpc.RemoteDeploymentAdmin;

public class Activator implements BundleActivator, ServiceTrackerCustomizer, FrameworkListener {

	private static final String DEPLOYMENT_ADMIN_CLASS = "org.osgi.service.deploymentadmin.DeploymentAdmin";
	private static final String EVENT_ADMIN_CLASS = "org.osgi.service.event.EventAdmin";

	private static final String SERVICE_STATE = "iagent.service.state";
	public static final String CUSTOM_PROPERTY_EVENT = "iagent_property_event";

	private RemoteBundleAdminImpl bundleAdmin;
	private RemoteApplicationAdminImpl applicationAdmin;
	private RemoteDeploymentAdminImpl deploymentAdmin;
	private RemoteServiceAdminImpl serviceAdmin;
	private RemoteConsoleServiceBase console;
	private PMPServer pmpServer;
	private ServiceRegistration pmpServiceReg;
	private ServiceRegistration pmpServerReg;
	private BundleContext context;
	private static Activator instance;
	private EventSynchronizerImpl synchronizer;
	private VMCommander vmCommander;

	private ServiceTracker deploymentAdminTrack;
	private ServiceTracker eventAdminTracker;

	public void start(BundleContext context) throws Exception {
		this.context = context;
		instance = this;
		synchronizer = new EventSynchronizerImpl(context);

		synchronizer.addEventSource(CUSTOM_PROPERTY_EVENT);

		bundleAdmin = new RemoteBundleAdminImpl();
		bundleAdmin.register(context);

		applicationAdmin = new RemoteApplicationAdminImpl();
		applicationAdmin.register(context);

		deploymentAdminTrack = new ServiceTracker(context, DEPLOYMENT_ADMIN_CLASS, this);
		deploymentAdminTrack.open(true);

		eventAdminTracker = new ServiceTracker(context, EVENT_ADMIN_CLASS, this);
		eventAdminTracker.open(true);

		serviceAdmin = new RemoteServiceAdminImpl();
		serviceAdmin.register(context);

		boolean registerVMController = !"false".equals(System.getProperty("iagent.controller"));
		if (registerVMController)
			registerControllerSupport(context);

		registerConsole(context);

		pmpServiceReg = context.registerService(PMPService.class.getName(), PMPServiceFactory.getDefault(), null);
		pmpServer = PMPServerFactory.createServer(context, 1450, null);
		pmpServerReg = context.registerService(PMPServer.class.getName(), pmpServer, null);
		synchronizer.setPMPServer(pmpServer);
		synchronizer.start();
	}

	private void registerControllerSupport(BundleContext context) {
		Bundle sysBundle = context.getBundle(0);
		switch(sysBundle.getState()) {
		case Bundle.ACTIVE:
			startController(context);
			break;
		case Bundle.STARTING:
			context.addFrameworkListener(this);
			break;
		}
	}

	private void startController(BundleContext context) {
		boolean shutdownOnDisconnect = Boolean.getBoolean("iagent.shutdownOnDisconnect");
		vmCommander = new VMCommander(context, shutdownOnDisconnect);
	}

	private void registerConsole(BundleContext context) {
		// trying Equinox console
		try {
			console = new EquinoxRemoteConsole();
			console.register(context);
			return;
		} catch (Throwable t) {
			console = null;
		}
		// trying mBS Console
		try {
			console = new ProSystRemoteConsole();
			console.register(context);
			return;
		} catch (Throwable t) {
			console = null;
		}
	}

	private void unregisterConsole() {
		if (console != null) {
			console.unregister();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {

		unregisterConsole();

		pmpServerReg.unregister();
		pmpServer.close();

		pmpServiceReg.unregister();

		if (synchronizer != null) {
			synchronizer.removeEventSource(CUSTOM_PROPERTY_EVENT);
			synchronizer.stopDispatching();
			synchronizer.unregister(context);
		}

		if (bundleAdmin != null) {
			bundleAdmin.unregister(context);
			bundleAdmin = null;
		}

		if (applicationAdmin != null) {
			applicationAdmin.unregister(context);
			applicationAdmin = null;
		}

		if (deploymentAdminTrack != null) {
			deploymentAdminTrack.close();
			deploymentAdminTrack = null;
		}

		if (eventAdminTracker != null) {
			eventAdminTracker.close();
			eventAdminTracker = null;
		}

		if (serviceAdmin != null) {
			serviceAdmin.unregister(context);
			serviceAdmin = null;
		}

		if (vmCommander != null) {
			context.removeFrameworkListener(this);
			vmCommander.close();
		}

		instance = null;
		this.context = null;
	}

	private boolean registerDeploymentAdmin(DeploymentAdmin admin) {
		if (deploymentAdmin == null) {
			deploymentAdmin = new RemoteDeploymentAdminImpl();
			deploymentAdmin.register(context, admin);
			return true;
		} else {
			return false;
		}
	}

	private boolean unregisterDeploymentAdmin(DeploymentAdmin admin) {
		if (deploymentAdmin != null && deploymentAdmin.getDeploymentAdmin() == admin) {
			deploymentAdmin.unregister(context);
			deploymentAdmin = null;
			return true;
		}
		return false;
	}

	// TODO: Rework dependency support
	public Object addingService(ServiceReference arg0) {
		String[] classes = (String[]) arg0.getProperty("objectClass");
		for (int i = 0; i < classes.length; i++)
			if (classes[i].equals(DEPLOYMENT_ADMIN_CLASS)) {
				DeploymentAdmin admin = (DeploymentAdmin) context.getService(arg0);
				if (registerDeploymentAdmin(admin))
					sendEvent(RemoteDeploymentAdmin.class, true);
				return admin;
			} else if (classes[i].equals(EVENT_ADMIN_CLASS)) {
				sendEvent(EventAdmin.class, true);
				return new Object();
			}
		return null;
	}

	public void modifiedService(ServiceReference arg0, Object arg1) {
	}

	public void removedService(ServiceReference ref, Object obj) {
		String[] classes = (String[]) ref.getProperty("objectClass");
		for (int i = 0; i < classes.length; i++) {
			if (classes[i].equals(DEPLOYMENT_ADMIN_CLASS)) {
				if (unregisterDeploymentAdmin((DeploymentAdmin) obj)) {
					DeploymentAdmin admin = (DeploymentAdmin) deploymentAdminTrack.getService();
					if (admin != null)
						registerDeploymentAdmin(admin);
					else
						sendEvent(RemoteDeploymentAdmin.class, false);
				}
			} else if (classes[i].equals(EVENT_ADMIN_CLASS)) {
				if (eventAdminTracker.getService() == null)
					sendEvent(EventAdmin.class, false);
			}
		}
	}

	public static EventSynchronizer getSynchronizer() {
		return instance != null ? instance.synchronizer : null;
	}

	private void sendEvent(Class objClass, boolean state) {
		Dictionary pmpEventData = new Hashtable();

		pmpEventData.put(Constants.OBJECTCLASS, new String[] { objClass.getName() });
		pmpEventData.put(SERVICE_STATE, new Boolean(state));

		EventSynchronizer synchronizer = this.synchronizer;
		if (synchronizer != null)
			synchronizer.enqueue(new EventData(pmpEventData, CUSTOM_PROPERTY_EVENT));
	}

	
	public void frameworkEvent(FrameworkEvent event) {
		if (event.getType() == FrameworkEvent.STARTED)
			startController(context);
	}
}

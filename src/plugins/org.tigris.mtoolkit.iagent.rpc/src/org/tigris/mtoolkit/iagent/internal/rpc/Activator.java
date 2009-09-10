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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.internal.rpc.console.EquinoxRemoteConsole;
import org.tigris.mtoolkit.iagent.internal.rpc.console.ProSystRemoteConsole;
import org.tigris.mtoolkit.iagent.internal.rpc.console.RemoteConsoleServiceBase;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;
import org.tigris.mtoolkit.iagent.pmp.PMPServerFactory;
import org.tigris.mtoolkit.iagent.pmp.PMPService;
import org.tigris.mtoolkit.iagent.pmp.PMPServiceFactory;
import org.tigris.mtoolkit.iagent.rpc.RemoteDeploymentAdmin;

public class Activator implements BundleActivator, ServiceTrackerCustomizer {

	private static final String DEPLOYMENT_ADMIN_CLASS = "org.osgi.service.deploymentadmin.DeploymentAdmin";
	private static final String EVENT_ADMIN_CLASS = "org.osgi.service.event.EventAdmin";
		
	private static final String SERVICE_STATE = "iagent.service.state";
	public static final String CUSTOM_PROPERTY_EVENT = "iagent_property_event";
	
	private RemoteBundleAdminImpl bundleAdmin;
	private RemoteDeploymentAdminImpl deploymentAdmin;
	private RemoteServiceAdminImpl serviceAdmin;
	private RemoteConsoleServiceBase console;
	private PMPServer pmpServer;
	private ServiceRegistration pmpServiceReg;
	private ServiceRegistration pmpServerReg;
	private BundleContext context;
	private static Activator instance;
	private EventSynchronizerImpl synchronizer;

	private ServiceTracker deploymentAdminTrack;
	private ServiceTracker eventAdminTracker;
	
	public void start(BundleContext context) throws Exception {
		this.context = context;
		instance = this;
		synchronizer = new EventSynchronizerImpl(context);
		bundleAdmin = new RemoteBundleAdminImpl();
		bundleAdmin.register(context);

		deploymentAdminTrack = new ServiceTracker(context, DEPLOYMENT_ADMIN_CLASS, this);
		deploymentAdminTrack.open(true);

		eventAdminTracker = new ServiceTracker(context, EVENT_ADMIN_CLASS, this);
		eventAdminTracker.open(true);
		
		serviceAdmin = new RemoteServiceAdminImpl();
		serviceAdmin.register(context);

		registerConsole(context);

		pmpServiceReg = context.registerService(PMPService.class.getName(), PMPServiceFactory.getDefault(), null);
		pmpServer = PMPServerFactory.createServer(context, 1450, null);
		pmpServerReg = context.registerService(PMPServer.class.getName(), pmpServer, null);
		synchronizer.setPMPServer(pmpServer);
		synchronizer.start();
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
		instance = null;

		unregisterConsole();

		pmpServerReg.unregister();
		pmpServer.close();

		pmpServiceReg.unregister();

		if (synchronizer != null) {
			synchronizer.stopDispatching();
			synchronizer.unregister(context);
		}

		if (bundleAdmin != null) {
			bundleAdmin.unregister(context);
			bundleAdmin = null;
		}

		if (deploymentAdminTrack != null) {
			deploymentAdminTrack.close();
			deploymentAdminTrack = null;
		}

		if(eventAdminTracker != null) {
			eventAdminTracker.close();
			eventAdminTracker = null;
		}
		
		if (serviceAdmin != null) {
			serviceAdmin.unregister(context);
			serviceAdmin = null;
		}

		this.context = null;
	}

	public Object addingService(ServiceReference arg0) {
		String[] classes =  (String[]) arg0.getProperty("objectClass");
		for(int i = 0; i < classes.length;i++)
			if(classes[i].equals(DEPLOYMENT_ADMIN_CLASS)) {
				sendEvent(RemoteDeploymentAdmin.class, true);
				if (deploymentAdminTrack.getService() == null) {
					deploymentAdmin = new RemoteDeploymentAdminImpl();
					DeploymentAdmin admin = (DeploymentAdmin) context.getService(arg0);
					deploymentAdmin.register(context, admin);
					return admin;
				} else {
					return null;
				}
			} else if(classes[i].equals(EVENT_ADMIN_CLASS)) {
				sendEvent(EventAdmin.class, true);
				return new Object();
			}
		
		return null;
	}

	public void modifiedService(ServiceReference arg0, Object arg1) {
		
	}

	public void removedService(ServiceReference arg0, Object arg1) {
		
		String[] classes = (String[]) arg0.getProperty("objectClass");
		for(int i = 0; i < classes.length;i++) {
			if(classes[i].equals(DEPLOYMENT_ADMIN_CLASS)) {
				sendEvent(RemoteDeploymentAdmin.class, false);
				deploymentAdmin.unregister(context);
				deploymentAdmin = null;
				break;
			} else if(classes[i].equals(EVENT_ADMIN_CLASS)) {
				sendEvent(EventAdmin.class, false);
			}
		}
	}

//	public static EventSynchronizerImpl getSynchronizer() {
//		return instance != null ? instance.synchronizer : null;
//	}

	private void initSynchronizer() {
	}

	private void sendEvent(Class objClass, boolean state) {
		Dictionary pmpEventData = new Hashtable();
		
		pmpEventData.put(Constants.OBJECTCLASS, new String[]{objClass.getName()});
		pmpEventData.put(SERVICE_STATE, new Boolean(state));
		
		synchronizer.enqueue(new EventData(pmpEventData, CUSTOM_PROPERTY_EVENT));
	}
}

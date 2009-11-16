package org.tigris.mtoolkit.iagent.internal.rpc;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;

public class DeploymentEventListener implements EventHandler {

	public static final String DEPLOYMENT_EVENT = "d_event";

	private static final String EVENT_TYPE_KEY = "type";
	private static final String EVENT_DEPLOYMENT_PACKAGE_KEY = "deployment.package";

	private static final int DP_INSTALLED = 1 << 0;
	private static final int DP_UNINSTALLED = 1 << 1;

	private static final String INSTALL_EVENT_TOPIC = "org/osgi/service/deployment/INSTALL";
	private static final String UNINSTALL_EVENT_TOPIC = "org/osgi/service/deployment/UNINSTALL";
	private static final String COMPLETE_EVENT_TOPIC = "org/osgi/service/deployment/COMPLETE";

	private static final String PROP_DEPLOYMENT_PACKAGE = "deploymentpackage.name";
	private static final String PROP_SUCCESSFUL = "successful";

	private ServiceRegistration registration;

	private String dpSymbolicName;
	private String dpVersion;
	
	private DeploymentAdmin deploymentAdmin;

	public void register(BundleContext context, DeploymentAdmin admin) {
		this.deploymentAdmin = admin;
		
		debug("[register] Registering Event handler for deployment package events...");
		Dictionary eventProps = new Hashtable();
		eventProps.put(EventConstants.EVENT_TOPIC, new String[] { INSTALL_EVENT_TOPIC, UNINSTALL_EVENT_TOPIC,
				COMPLETE_EVENT_TOPIC, });
		registration = context.registerService(EventHandler.class.getName(), this, eventProps);
		debug("[register] Event handler registered.");
		
		Activator.getSynchronizer().addEventSource(DEPLOYMENT_EVENT);
	}

	public void unregister() {
		Activator.getSynchronizer().removeEventSource(DEPLOYMENT_EVENT);
		
		if (registration != null) {
			debug("[unregister] Unregistering event handler for deployment packages...");
			registration.unregister();
			registration = null;
			debug("[unregister] Event handler unregistered.");
		} else {
		}

	}

	public void handleEvent(Event event) {
		String symbolicName = (String) event.getProperty(PROP_DEPLOYMENT_PACKAGE);
		DebugUtils.debug(this, "[handleEvent] >>> name: " + symbolicName + "; topic: " + event.getTopic());
		if (INSTALL_EVENT_TOPIC.equals(event.getTopic())) {
			handleInstall(symbolicName);
		} else if (UNINSTALL_EVENT_TOPIC.equals(event.getTopic())) {
			handleUninstall(symbolicName);
		} else if (COMPLETE_EVENT_TOPIC.equals(event.getTopic())) {
			boolean successful = ((Boolean) event.getProperty(PROP_SUCCESSFUL)).booleanValue();
			handleComplete(symbolicName, successful);
		} // else ignore
	}

	private void handleInstall(String symbolicName) {
		debug("[handleInstall] newSymbolicName: " + symbolicName + "; oldSymbolicName: "
				+ dpSymbolicName);
		if (dpSymbolicName != null)
			return;
		dpSymbolicName = symbolicName;
	}

	private void handleUninstall(String symbolicName) {
		DeploymentAdmin deploymentAdmin = this.deploymentAdmin;
		if (deploymentAdmin == null) {
			info("[handleUninstall] DeploymentAdmin is unavailable");
			return;
		}
		DeploymentPackage dp = deploymentAdmin.getDeploymentPackage(symbolicName);
		if (dp == null) {
			info("[handleUninstall] No such deployment package with symbolic name: " + symbolicName);
			return;
		}
		if (dpSymbolicName != null) {
			debug("[handleUninstall] Uninstallation already in progress for: " + dpSymbolicName
					+ " Check for bug in deployment admin.");
			return; // don't handle uninstall events for bundles
		}
		dpSymbolicName = symbolicName;
		dpVersion = dp.getVersion().toString();
		debug("[handleUninstall] Uninstallation in progress for: " + dpSymbolicName + "_" + dpVersion);
	}

	private void handleComplete(String symbolicName, boolean successful) {
		if (dpSymbolicName == null) {
			info("[handleComplete] There is no information for started session");
			return;
		}
		if (dpSymbolicName != null && dpSymbolicName.equals(symbolicName)) {
			try {
				if (successful) {
					Dictionary event;
					EventSynchronizer synchronizer = Activator.getSynchronizer();
					if (synchronizer != null) {
						if (dpVersion != null) { // uninstall event
							debug("[handleComplete] Uninstall event completed for: " + dpSymbolicName
									+ "_" + dpVersion);
							event = convertDeploymentEvent(dpSymbolicName, dpVersion, DP_UNINSTALLED);
						} else { // install event
							DeploymentAdmin deploymentAdmin = this.deploymentAdmin; // save
							// the
							// reference
							// before
							// someone
							// stills
							// it:)
							if (deploymentAdmin == null)
								return;
							DeploymentPackage dp = deploymentAdmin.getDeploymentPackage(dpSymbolicName);
							dpVersion = dp.getVersion().toString();
							debug("[handleComplete] Install event completed for: " + dpSymbolicName
									+ "_" + dpVersion);
							event = convertDeploymentEvent(dpSymbolicName, dpVersion, DP_INSTALLED);
						}
						synchronizer.enqueue(new EventData(event, DEPLOYMENT_EVENT));
					} else {
						info("[handleComplete] Event synchronizer was disabled.");
					}
				} else {
					info("[handleComplete] Deployment package event is not successful");
				}
			} finally {
				dpSymbolicName = null;
				dpVersion = null;
			}
		}
	}

	private Dictionary convertDeploymentEvent(String symbolicName, String version, int type) {
		Dictionary event = new Hashtable();
		event.put(EVENT_TYPE_KEY, new Integer(type));
		event.put(EVENT_DEPLOYMENT_PACKAGE_KEY, new String[] { symbolicName, version });
		return event;
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}
}

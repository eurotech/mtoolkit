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
package org.tigris.mtoolkit.osgimanagement.installation;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.iagent.DeviceConnectionListener;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.DeviceConnectorSpi;
import org.tigris.mtoolkit.osgimanagement.internal.DeviceConnectorSWTWrapper;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConnectFrameworkJob;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.PMPConnectionListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ActionsManager;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class FrameworkConnectorFactory implements DeviceConnectionListener {

	/**
	 * Job listener, which deletes given file, when the associated job has
	 * finished.
	 * <p>
	 * Use this class, whenever you create temporary file, which is passed to a
	 * job and you want to remove the file, when the job has finished. The file
	 * is removed independently from the exact result of the job execution.
	 * 
	 */
	private static FrameworkConnectorFactory factory = new FrameworkConnectorFactory();

	public static boolean isAutoConnectEnabled = FrameworkPreferencesPage.autoConnectDefault;
	public static boolean isAutoStartBundlesEnabled = FrameworkPreferencesPage.autoStartAfterInstall;
	public static boolean isActivationPolicyEnabled = FrameworkPreferencesPage.useActivationPolicy;
	public static boolean isBundlesCategoriesShown = FrameworkPreferencesPage.showBundleCategories;
	public static boolean isAutoUpdateBundlesOnInstallEnabled = FrameworkPreferencesPage.autoUpdateBundlesOnInstallDefault;

	public static final int CONNECT_PROGRESS = 1000;

	public static final int CONNECT_PROGRESS_CONNECTING = (int) (CONNECT_PROGRESS * 0.1);
	public static final int CONNECT_PROGRESS_BUNDLES = (int) (CONNECT_PROGRESS * 0.3);
	public static final int CONNECT_PROGRESS_SERVICES = (int) (CONNECT_PROGRESS * 0.2);
	public static final int CONNECT_PROGRESS_ADDITIONAL = (int) (CONNECT_PROGRESS * 0.4);

	public static void init() {
		DeviceConnector.addDeviceConnectionListener(factory);
	}

	public static void deinit() {
		DeviceConnector.removeDeviceConnectionListener(factory);
	}

	public static void connectFrameWork(final Framework fw) {
		ConnectFrameworkJob job = new ConnectFrameworkJob(fw);
		job.schedule();
	}

	public void connected(final DeviceConnector connector) {
	}

	public static void connectFramework(final DeviceConnector connector, String frameworkName) {
		// wrap the connector
		final DeviceConnector fConnector = new DeviceConnectorSWTWrapper(connector, PlatformUI.getWorkbench()
				.getDisplay());
		Dictionary connProps = fConnector.getProperties();
		Boolean temporary = (Boolean) connProps.get("framework-connection-temporary");
		if (temporary != null && temporary.booleanValue())
			// the connection is only temporary and will be closed shortly
			return;
		boolean autoConnected = true;
		FrameworkImpl fw = null;
		FrameworkImpl fws[] = FrameWorkView.getFrameworks();
		if (fws != null) {
			for (int i = 0; i < fws.length; i++) {
				if (fws[i].getName().equals(frameworkName)) {
					fw = fws[i];
					autoConnected = false;
					break;
				}
			}
		}

		// generate framework name
		if (fw == null) {
			if (!isAutoConnectEnabled)
				return;
			frameworkName = generateFrameworkName(connProps);
		}

		if (FrameWorkView.getTreeRoot() != null && fw == null) {
			fw = new FrameworkImpl(frameworkName, true);
			FrameWorkView.getTreeRoot().addElement(fw);
		}

		if (!fConnector.equals(fw.getConnector())) {
			fw.setConnector(fConnector);
		}
		// fw.connect(connector, SubMonitor.convert(new NullProgressMonitor()));
		BrowserErrorHandler.debug("FrameworkPlugin: " + fw.getName() + " was connected with connector: " + fConnector); //$NON-NLS-1$ //$NON-NLS-2$
		createPMPConnection(fConnector, fw, frameworkName, autoConnected);
	}

	/**
	 * Creates PMP connection.
	 * 
	 * @param connector
	 * @param fw
	 * @param frameworkName
	 * @param autoConnected
	 */
	public static void createPMPConnection(final DeviceConnector connector, final FrameworkImpl fw,
			String frameworkName, boolean autoConnected) {
		boolean pmp = false;
		try {
			pmp = ((DeviceConnectorSpi) connector).getConnectionManager().getActiveConnection(
					ConnectionManager.PMP_CONNECTION) != null;
		} catch (IAgentException e1) {
			e1.printStackTrace();
		}
		final boolean pmpConnected = pmp;

		// create and add pmp connection listener to fw
		PMPConnectionListener pmpListener = fw.getPMPConnectionListener();
		if (pmpListener == null || !connector.equals(pmpListener.getConnector())) {
			pmpListener = new PMPConnectionListener(fw, frameworkName, connector, autoConnected);
			fw.setPMPConnectionListener(pmpListener);
		}

		final PMPConnectionListener listener = pmpListener;

		// force creating of pmp connection
		Job job = new Job(NLS.bind(Messages.connect_framework, fw.getName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("", 2); //$NON-NLS-1$
				// if pmp connection is available do not force creation but
				// directly connect
				if (pmpConnected) {
					listener.connected();
				} else {
					try {
						connector.getVMManager().isVMActive();
					} catch (IAgentException e) {
						BrowserErrorHandler.processError(e, NLS.bind(Messages.pmp_connect_error_message, fw.getName()),
								true); //$NON-NLS-1$
						e.printStackTrace();
					}
				}
				monitor.done();
				return monitor.isCanceled() ? Status.CANCEL_STATUS : Status.OK_STATUS;
			}
		};
		// connectJobs.put(connector, job);
		job.schedule();
	}

	public void disconnected(DeviceConnector connector) {
		FrameworkImpl fwArr[] = FrameWorkView.findFramework(connector);

		if (fwArr == null /* || !fw.isConnected() */)
			return;

		for (int j = 0; j < fwArr.length; j++) {
			FrameworkImpl fw = fwArr[j];
			BrowserErrorHandler
					.debug("FrameworkPlugin: " + fw.getName() + " was disconnected with connector: " + connector); //$NON-NLS-1$ //$NON-NLS-2$
			synchronized (Framework.getLockObject(connector)) {
				ActionsManager.disconnectConsole(fw); //$NON-NLS-1$
				FrameworkImpl fws[] = FrameWorkView.getFrameworks();
				if (fws != null) {
					for (int i = 0; i < fws.length; i++) {
						fw = fws[i];
						if (fw.getConnector() != null && fw.getConnector().equals(connector)) {
							fw.disconnect();
							fw.setPMPConnectionListener(null);
							if (fw.autoConnected) {
								FrameWorkView.getTreeRoot().removeElement(fw);
							}
							break;
						}
					}
				}
			}
		}
	}

	public static String generateFrameworkName(Dictionary connProps) {
		Hashtable frameWorkMap = new Hashtable();
		FrameworkImpl fws[] = FrameWorkView.getFrameworks();
		if (fws != null) {
			for (int i = 0; i < fws.length; i++) {
				frameWorkMap.put(fws[i].getName(), ""); //$NON-NLS-1$
			}
		}

		Object ip = connProps.get(DeviceConnector.KEY_DEVICE_IP);
		String defaultFWName = Messages.new_framework_default_name + " ("
				+ connProps.get(DeviceConnector.TRANSPORT_TYPE) + "=" + connProps.get(DeviceConnector.TRANSPORT_ID)
				+ ")";
		String frameWorkName = defaultFWName;
		String suffix = " ";
		if (ip != null) {
			suffix += ip;
		}
		int index = 1;
		while (frameWorkMap.containsKey(frameWorkName)) {
			frameWorkName = defaultFWName + suffix + "(" + index + ")";
			index++;
		}
		return frameWorkName;
	}
}
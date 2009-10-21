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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.io.File;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.installation.BaseFileItem;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.InstallDialog;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertiesDialog;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertySheet;
import org.tigris.mtoolkit.osgimanagement.internal.console.ConsoleManager;
import org.tigris.mtoolkit.osgimanagement.internal.installation.FrameworkProcessor;
import org.tigris.mtoolkit.osgimanagement.internal.installation.FrameworkTarget;

public class MenuFactory {
	private static final String MIME_JAR = "application/java-archive"; //$NON-NLS-1$
	private static final String MIME_DP = "application/vnd.osgi.dp"; //$NON-NLS-1$

	public static void addFrameworkAction(TreeRoot treeRoot, TreeViewer parentView) {
		String frameworkName = generateName(treeRoot);
		FrameWork newFrameWork = new FrameWork(frameworkName, false);
		PropertySheet sheet = new PropertySheet(parentView, treeRoot, newFrameWork, true);
		sheet.open();
	}

	// generates unique name for the new FrameWork
	public static String generateName(TreeRoot treeRoot) {
		HashMap frameWorkMap = treeRoot.getFrameWorkMap();
		int index = 1;
		String frameWorkName;
		do {
			frameWorkName = Messages.new_framework_default_name + " (" + index+")";
			index++;
		} while (frameWorkMap.containsKey(frameWorkName));

		return frameWorkName;
	}

	public static void bundlePropertiesAction(Bundle bundle, TreeViewer parentView) {
		try {
			RemoteBundle rBundle = bundle.getRemoteBundle();
			Shell shell = parentView.getTree().getShell();
			PropertiesDialog propertiesDialog = new PropertiesDialog(shell, Messages.bundle_properties_title) {
				protected void attachHelp(Composite container) {
					PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
							IHelpContextIds.PROPERTY_BUNDLE);
				}
				
			};
			Dictionary headers = rBundle.getHeaders(null);
			propertiesDialog.open();
			propertiesDialog.getMainControl().setData(headers);
		} catch (IAgentException e) {
			e.printStackTrace();
			BrowserErrorHandler.processError(e, true);
			return;
		}
	}

	public static void dpPropertiesAction(DeploymentPackage dp, TreeViewer parentView) {
		try {
			RemoteDP rdp = dp.getRemoteDP();

			Shell shell = parentView.getTree().getShell();
			PropertiesDialog propertiesDialog = new PropertiesDialog(shell, Messages.dp_properties_title) {
				protected void attachHelp(Composite container) {
					PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
							IHelpContextIds.PROPERTY_PACKAGE);				}
			};
			Dictionary headers = new Hashtable();
			headers.put("DeploymentPackage-SymbolicName", rdp.getHeader("DeploymentPackage-SymbolicName")); //$NON-NLS-1$ //$NON-NLS-2$
			headers.put("DeploymentPackage-Version", rdp.getHeader("DeploymentPackage-Version")); //$NON-NLS-1$ //$NON-NLS-2$

			String header = "DeploymentPackage-FixPack"; //$NON-NLS-1$
			String value = rdp.getHeader(header);
			if (value != null)
				headers.put(header, value);

			header = "DeploymentPackage-Copyright";value = rdp.getHeader(header); //$NON-NLS-1$
			if (value != null)
				headers.put(header, value);

			header = "DeploymentPackage-ContactAddress";value = rdp.getHeader(header); //$NON-NLS-1$
			if (value != null)
				headers.put(header, value);

			header = "DeploymentPackage-Description";value = rdp.getHeader(header); //$NON-NLS-1$
			if (value != null)
				headers.put(header, value);

			header = "DeploymentPackage-DocURL";value = rdp.getHeader(header); //$NON-NLS-1$
			if (value != null)
				headers.put(header, value);

			header = "DeploymentPackage-Vendor";value = rdp.getHeader(header); //$NON-NLS-1$
			if (value != null)
				headers.put(header, value);

			header = "DeploymentPackage-License";value = rdp.getHeader(header); //$NON-NLS-1$
			if (value != null)
				headers.put(header, value);

			header = "DeploymentPackage-Icon";value = rdp.getHeader(header); //$NON-NLS-1$
			if (value != null)
				headers.put(header, value);

			propertiesDialog.open();
			propertiesDialog.getMainControl().setData(headers);

		} catch (IAgentException e) {
			BrowserErrorHandler.processError(e, true);
			return;
		}
	}

	public static void deinstallBundleAction(Bundle bundle) {
		FrameworkConnectorFactory.deinstallBundle(bundle);
		ConsoleManager.showConsole(bundle.findFramework());
	}

	public static void deinstallDPAction(DeploymentPackage dpNode) {
		FrameworkConnectorFactory.deinstallDP(dpNode);
		ConsoleManager.showConsoleIfCreated(dpNode.findFramework());
	}

	public static void installBundleAction(final FrameWork framework, TreeViewer parentView) {
		InstallDialog installDialog = new InstallDialog(parentView, InstallDialog.INSTALL_BUNDLE_TYPE);
		installDialog.open();
		final String result = installDialog.getResult();
		if ((installDialog.getReturnCode() > 0) || (result == null) || result.trim().equals("")) { //$NON-NLS-1$
			return;
		}

		Job job = new Job("Installing to " + framework.getName()) {
			public IStatus run(IProgressMonitor monitor) {
				InstallationItem item = new BaseFileItem(new File(result), MIME_JAR);
				FrameworkProcessor processor = new FrameworkProcessor();
				processor.setUseAdditionalProcessors(false);
				IStatus status = processor.processInstallationItem(item, new FrameworkTarget(framework), monitor);
				monitor.done();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return status;
			}
		};
		job.schedule();

		ConsoleManager.showConsoleIfCreated(framework);
	}

	public static void installDPAction(final FrameWork framework, TreeViewer parentView) {
		InstallDialog installDialog = new InstallDialog(parentView, InstallDialog.INSTALL_DP_TYPE);
		installDialog.open();
		final String result = installDialog.getResult();
		if ((installDialog.getReturnCode() > 0) || (result == null) || result.trim().equals("")) { //$NON-NLS-1$
			return;
		}

		Job job = new Job("Installing to " + framework.getName()) {
			public IStatus run(IProgressMonitor monitor) {
				InstallationItem item = new BaseFileItem(new File(result), MIME_DP);
				FrameworkProcessor processor = new FrameworkProcessor();
				processor.setUseAdditionalProcessors(false);
				IStatus status = processor.processInstallationItem(item, new FrameworkTarget(framework), monitor);
				monitor.done();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return status;
			}
		};
		job.schedule();

		ConsoleManager.showConsoleIfCreated(framework);
	}

	public static void frameworkPropertiesAction(FrameWork framework, TreeViewer parentView) {
		PropertySheet sheet = new PropertySheet(parentView, framework.getParent(), framework, false);
		sheet.open();
	}

	public static void removeFrameworkAction(FrameWork framework) {
//		if (framework.isConnected()) {
//			framework.disconnect();
//		}

		framework.dispose();
		ConsoleManager.disconnectConsole(framework);
	}

	public static void startBundleAction(Bundle bundle) {
		FrameworkConnectorFactory.startBundle(bundle);
		ConsoleManager.showConsoleIfCreated(bundle.findFramework());
	}

	public static void stopBundleAction(Bundle bundle) {
		FrameworkConnectorFactory.stopBundle(bundle);
		ConsoleManager.showConsoleIfCreated(bundle.findFramework());
	}

	public static void updateBundleAction(final Bundle bundle, TreeViewer parentView) {
		InstallDialog installDialog = new InstallDialog(parentView, InstallDialog.UPDATE_BUNDLE_TYPE);
		installDialog.open();
		final String result = installDialog.getResult();
		if ((installDialog.getReturnCode() > 0) || (result == null) || result.trim().equals("")) { //$NON-NLS-1$
			return;
		}
		FrameworkConnectorFactory.updateBundle(result, bundle);
		ConsoleManager.showConsoleIfCreated(bundle.findFramework());
	}

	public static void disconnectFrameworkAction(FrameWork fw) {
		FrameworkConnectorFactory.disconnectFramework(fw);
	}

	public static void connectFrameworkAction(FrameWork framework) {
		FrameworkConnectorFactory.connectFrameWork(framework);
	}

	public static void refreshFrameworkAction(FrameWork fw) {
		fw.refreshAction();
	}

	public static void refreshBundleAction(Bundle bundle) {
		bundle.findFramework().refreshBundleAction(bundle);
	}

}

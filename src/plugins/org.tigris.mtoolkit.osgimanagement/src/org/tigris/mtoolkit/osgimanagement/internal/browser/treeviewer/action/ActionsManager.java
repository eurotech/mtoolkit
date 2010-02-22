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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.gui.PropertiesDialog;
import org.tigris.mtoolkit.common.installation.BaseFileItem;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkTarget;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.UIHelper;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.RemoteBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.StartBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.StopBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.UninstallBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.UpdateBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.InstallDialog;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertySheet;
import org.tigris.mtoolkit.osgimanagement.internal.console.ConsoleManager;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class ActionsManager {
	private static final String MIME_JAR = "application/java-archive"; //$NON-NLS-1$

	public static void addFrameworkAction(TreeRoot treeRoot, TreeViewer parentView) {
		String frameworkName = generateName(treeRoot);
		FrameworkImpl newFrameWork = new FrameworkImpl(frameworkName, false);
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
			propertiesDialog.create();
			propertiesDialog.getMainControl().setData(headers);
			propertiesDialog.open();
		} catch (IAgentException e) {
			e.printStackTrace();
			BrowserErrorHandler.processError(e, true);
			return;
		}
	}

	public static void deinstallBundleAction(Bundle bundle) {
		RemoteBundleOperation job = new UninstallBundleOperation(bundle);
		job.schedule();
	}

	public static void installBundleAction(final FrameworkImpl framework, TreeViewer parentView) {
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
	}

	public static void frameworkPropertiesAction(FrameworkImpl framework, TreeViewer parentView) {
		PropertySheet sheet = new PropertySheet(parentView, framework.getParent(), framework, false);
		sheet.open();
	}

	public static void removeFrameworkAction(final FrameworkImpl framework) {
		Job removeJob = new Job("Remove device") {
			protected IStatus run(IProgressMonitor monitor) {
				framework.dispose();
				return Status.OK_STATUS;
			}
		};
		// When disconnect action is scheduled before this action, we need 
		// to be sure that it is completed before we dispose the framework
		removeJob.setRule(new FwMutexRule(framework));
		removeJob.schedule();
		ConsoleManager.disconnectConsole(framework);
	}

	public static void startBundleAction(Bundle bundle) {
		String name = bundle.getName();
		try {
			name = name + " ("+bundle.getVersion()+")";
		} catch (IAgentException e) {
		}
		RemoteBundleOperation job = new StartBundleOperation(name, bundle);
		job.schedule();
	}

	public static void stopBundleAction(Bundle bundle) {
		if (bundle.getID() == 0) {
			MessageDialog dialog = new MessageDialog(FrameWorkView.getShell(),
					"Stop bundle",
					null,
					NLS.bind(Messages.stop_system_bundle, bundle.getName()),
					MessageDialog.QUESTION,
					new String[] { "Continue", "Cancel" },
					0);
			int statusCode = UIHelper.openWindow(dialog);
			if (statusCode == 1)
				return;
		}

		RemoteBundleOperation job = new StopBundleOperation(bundle);
		job.schedule();
	}

	public static void updateBundleAction(final Bundle bundle, TreeViewer parentView) {
		InstallDialog installDialog = new InstallDialog(parentView, InstallDialog.UPDATE_BUNDLE_TYPE);
		installDialog.open();
		final String bundleFileName = installDialog.getResult();
		if ((installDialog.getReturnCode() > 0) || (bundleFileName == null) || bundleFileName.trim().equals("")) { //$NON-NLS-1$
			return;
		}
		RemoteBundleOperation job = new UpdateBundleOperation(bundle, new File(bundleFileName));
		job.schedule();
	}

	public static void disconnectFrameworkAction(final FrameworkImpl fw) {
		Job disconnectJob = new Job("Disconnect device") {
			protected IStatus run(IProgressMonitor monitor) {
				try {
					disconnectFramework0(fw);
					return Status.OK_STATUS;
				} catch (Throwable t) {
					return new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, t.getMessage(), t);
				}
			}
		};
		disconnectJob.setRule(new FwMutexRule(fw));
		disconnectJob.schedule();
		disconnectConsole(fw);
	}
	
	public static void disconnectConsole(FrameworkImpl fw) {
		ConsoleManager.disconnectConsole(fw);
	}


	private static void disconnectFramework0(FrameworkImpl fw) {
		try {
			fw.userDisconnect = true;
			if (fw.autoConnected) {
				fw.disconnect();
			} else {
//				if (fw.monitor != null) {
//					fw.monitor.setCanceled(true);
//				}
				// wait if connect operation is still active
				DeviceConnector connector;
				if ((connector = fw.getConnector()) != null) {
					// framework connects synchronously, while holding a lock
					// wait until the lock is released to know when the connect op has finished
					synchronized (Framework.getLockObject(connector)) {
					}
				}
				// if the connection fails, connector will be null, so we need to recheck the condition
				if ((connector = fw.getConnector()) != null) {
					connector.closeConnection();
				}
			}
		} catch (IAgentException e) {
			BrowserErrorHandler.processError(e, true);
			e.printStackTrace();
		}
	}


	public static void connectFrameworkAction(FrameworkImpl framework) {
		FrameworkConnectorFactory.connectFrameWork(framework);
	}

	public static void refreshFrameworkAction(FrameworkImpl fw) {
		fw.refreshAction();
	}

	public static void refreshBundleAction(Bundle bundle) {
		((FrameworkImpl) bundle.findFramework()).refreshBundleAction(bundle);
	}

	public static void setViewTypeAction(FrameworkImpl fw, int viewType) {
		if (fw.getViewType() != viewType) {
			fw.setViewType(viewType);
		}
	}

	private static class FwMutexRule implements ISchedulingRule {
		private Framework fw;

		public FwMutexRule(Framework fw) {
			this.fw = fw;
		}

		public boolean isConflicting(ISchedulingRule rule) {
			return (rule instanceof FwMutexRule) && (((FwMutexRule) rule).fw == fw);
		}

		public boolean contains(ISchedulingRule rule) {
			return (rule instanceof FwMutexRule) && (((FwMutexRule) rule).fw == fw);
		}
	}

}

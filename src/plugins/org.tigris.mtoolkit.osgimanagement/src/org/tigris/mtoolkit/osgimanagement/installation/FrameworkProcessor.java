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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.installation.PluginProvider.PluginItem;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConnectFrameworkJob;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.InstallBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

/**
 * @since 5.0
 */
public class FrameworkProcessor implements InstallationItemProcessor {

	private static FrameworkProcessor defaultinstance;
	private static final String MIME_JAR = "application/java-archive";
	private static final String MIME_ZIP = "application/zip";
	private static final String PROP_JVM_NAME = "jvm.name";

	private static Vector additionalProcessors = new Vector();
	
	private boolean useAdditionalProcessors = true;

	public static FrameworkProcessor getDefault() {
		if (defaultinstance == null) {
			defaultinstance = new FrameworkProcessor();
		}

		return defaultinstance;
	}

	public static void addAdditionalProcessor(InstallationItemProcessor processor) {
		additionalProcessors.addElement(processor);
	}

	public static void removeAdditionalProcessor(InstallationItemProcessor processor) {
		additionalProcessors.removeElement(processor);
	}

	public InstallationTarget[] getInstallationTargets() {
		FrameworkImpl[] fws = FrameWorkView.getFrameworks();
		if (fws == null || fws.length == 0) {
			return new InstallationTarget[0];
		}

		List targets = new ArrayList();
		for (int i = 0; i < fws.length; i++) {
			targets.add(new FrameworkTarget(fws[i]));
		}
		return (InstallationTarget[]) targets.toArray(new InstallationTarget[targets.size()]);
	}

	public String getGeneralTargetName() {
		return "OSGi Framework";
	}

	public String[] getSupportedMimeTypes() {
		Vector mimeTypes = new Vector();
		mimeTypes.addElement(MIME_JAR);
		mimeTypes.addElement(MIME_ZIP);
		for (int i = 0; useAdditionalProcessors && i < additionalProcessors.size(); i++) {
			String additionalMT[] = ((InstallationItemProcessor) additionalProcessors.get(i)).getSupportedMimeTypes();
			for (int j = 0; j < additionalMT.length; j++) {
				if (mimeTypes.indexOf(additionalMT[j]) == -1) {
					mimeTypes.addElement(additionalMT[j]);
				}
			}
		}
		String result[] = new String[mimeTypes.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = (String) mimeTypes.elementAt(i);
		}
		return result;
	}

	public IStatus processInstallationItem(final InstallationItem item, InstallationTarget target,
			final IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		Framework framework = ((FrameworkTarget) target).getFramework();

		// TODO: Connecting to framework should report the connection progress
		// to the current monitor
		if (!framework.isConnected()) {
			Job connectJob = new ConnectFrameworkJob(framework);
			connectJob.schedule();
			try {
				connectJob.join();
			} catch (InterruptedException e1) {
				return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), e1.getMessage(), e1);
			}
			if (!connectJob.getResult().isOK()) {
				return connectJob.getResult();
			}
			int counter = 0;
			while (!framework.isConnected() && counter++ < 100) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		Map preparationProps = new Hashtable();

		// Signing properties
		preparationProps.putAll(framework.getSigningProperties());

		// Platform properties
		try {
			DeviceConnector connector = framework.getConnector();
			if (connector == null) return Util.newStatus(IStatus.ERROR, "Connection lost", null);
			Map platformProps = connector.getVMManager().getPlatformProperties();
			preparationProps.putAll(platformProps);
		} catch (IAgentException iae) {
			// Cannot get platform properties - continuing.
		}

		if (framework.getConnector() == null) {
			return new Status(Status.ERROR, FrameworkPlugin.getDefault().getId(), "Could not establish connection to " + framework);
		}

		if (!preparationProps.containsKey(PROP_JVM_NAME)) {
			String transportType = (String) framework.getConnector().getProperties().get(DeviceConnector.TRANSPORT_TYPE);
			if ("android".equals(transportType)) {
				preparationProps.put(PROP_JVM_NAME, "Dalvik");
			}
		}

		IStatus preparationStatus = item.prepare(subMonitor.newChild(50), preparationProps);

		if (preparationStatus.getSeverity() == IStatus.ERROR || preparationStatus.getSeverity() == IStatus.CANCEL) {
			return preparationStatus;
		}
			
		if (item instanceof PluginItem) {
			IStatus status = ((PluginItem) item).checkAdditionalBundles((FrameworkImpl) framework, monitor);
			if (status.getSeverity() == IStatus.CANCEL) {
				monitor.setCanceled(true);
				return status;
			}
		}

		InputStream input = null;
		try {
			String mimeType = item.getMimeType();
			Vector processors = new Vector();
			if (mimeType.equals(MIME_JAR) || mimeType.equals(MIME_ZIP)) {
				processors.addElement(this);
			}
			for (int i = 0; useAdditionalProcessors && i < additionalProcessors.size(); i++) {
				String procMimeTypes[] = ((InstallationItemProcessor) additionalProcessors.elementAt(i))
						.getSupportedMimeTypes();
				for (int j = 0; j < procMimeTypes.length; j++) {
					if (mimeType.equals(procMimeTypes[j])) {
						processors.addElement(additionalProcessors.elementAt(i));
						break;
					}
				}
			}

			final FrameworkProcessor processor[] = new FrameworkProcessor[] { (FrameworkProcessor) processors
					.elementAt(0) };
			if (processors.size() > 1) {
				final FrameworkProcessor prArr[] = new FrameworkProcessor[processors.size()];
				for (int i = 0; i < prArr.length; i++) {
					FrameworkProcessor pr = (FrameworkProcessor) processors.elementAt(i);
					prArr[i] = pr;
				}
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						ListDialog dialog = new ListDialog(FrameWorkView.getShell());

						dialog.setTitle("Select processor");
						dialog.setLabelProvider(new LabelProvider());
						dialog.setMessage("Select installation processor for " + item.getName());
						dialog.setContentProvider(new ArrayContentProvider());
						dialog.setInput(Arrays.asList(prArr));
						dialog.setInitialSelections(new Object[] { prArr[0] });

						int result = dialog.open();
						if (result == Window.CANCEL) {
							monitor.setCanceled(true);
						} else {
							processor[0] = (FrameworkProcessor) dialog.getResult()[0];
						}
					}
				});
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
			}

			input = item.getInputStream();
			processor[0].install(input, item, framework, monitor);
		} catch (Exception e) {
			return Util.newStatus(IStatus.ERROR, "Remote content installation failed", e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			item.dispose();
		}

		monitor.done();
		if (monitor.isCanceled()) {
			return Status.CANCEL_STATUS;
		}
		return Status.OK_STATUS;
	}

	public void install(InputStream input, InstallationItem item, Framework framework, IProgressMonitor monitor)
			throws Exception {
		// TODO: Make methods, which are called from inside jobs to do
		// the real job
		installBundle(input, item.getName(), (FrameworkImpl) framework);
	}

	private void installBundle(InputStream input, String name, FrameworkImpl framework) {
		try {
			final File bundle = saveFile(input, name);
			Job installBundleJob = new InstallBundleOperation(bundle, framework);
			installBundleJob.addJobChangeListener(new DeleteWhenDoneListener(bundle));
			installBundleJob.schedule();
		} catch (IOException e) {
			StatusManager.getManager().handle(Util.newStatus(IStatus.ERROR, "Unable to install bundle", e),
				StatusManager.SHOW | StatusManager.LOG);
		}
	}

	protected File saveFile(InputStream input, String name) throws IOException {
		// TODO: Make saving stream to file done in a job
		IPath statePath = Platform.getStateLocation(FrameworkPlugin.getDefault().getBundle());
		File file = new File(statePath.toFile(), name);
		// make the directory hierarchy
		if (!file.getParentFile().exists() && !file.getParentFile().mkdirs())
			throw new IOException("Failed to create bundle state folder");
		FileOutputStream stream = new FileOutputStream(file);
		try {
			byte[] buf = new byte[8192];
			int read;
			while ((read = input.read(buf)) != -1) {
				stream.write(buf, 0, read);
			}
		} finally {
			stream.close();
		}
		return file;
	}


	
	public String getName() {
		return "Bundles processor";
	}

	public String toString() {
		return getName();
	}

	public ImageDescriptor getGeneralTargetImageDescriptor() {
		return ImageHolder.getImageDescriptor(ConstantsDistributor.SERVER_ICON_CONNECTED);
	}

	public boolean getUseAdditionalProcessors() {
		return useAdditionalProcessors;
	}

	public void setUseAdditionalProcessors(boolean enable) {
		this.useAdditionalProcessors = enable;
	}
	
	protected static class DeleteWhenDoneListener extends JobChangeAdapter {
		private final File packageFile;

		public DeleteWhenDoneListener(File packageFile) {
			this.packageFile = packageFile;
		}

		public void done(IJobChangeEvent event) {
			packageFile.delete();
		}
	}


}

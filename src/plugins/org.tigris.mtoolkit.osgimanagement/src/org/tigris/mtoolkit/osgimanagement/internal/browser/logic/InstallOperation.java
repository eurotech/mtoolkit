package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor;
import org.tigris.mtoolkit.osgimanagement.installation.InstallationPair;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

public class InstallOperation extends Job {

	private FrameworkImpl framework;
	private List<InstallationPair> installationPairs;

	public InstallOperation(FrameworkImpl framework, List<InstallationPair> installationPairs) {
		super(Messages.install_operation_title);
		this.framework = framework;
		this.installationPairs = installationPairs;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		// monitor.beginTask(getName(), 1);
		SubMonitor subMonitor = SubMonitor.convert(monitor, installationPairs.size() * 2);
		IStatus operationResult = Status.OK_STATUS;
		Map<Object, FrameworkProcessor> installedItems = new HashMap<Object, FrameworkProcessor>();
		for (InstallationPair item : installationPairs) {
			try {
				monitor.beginTask(getName(), 1);
				operationResult = doOperation(monitor, item, installedItems, subMonitor);
			} catch (IAgentException e) {
				operationResult = handleException(e);
				// } finally {
				// monitor.done();
			}
			if (monitor.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
		}
		installationPairs = null;
		startInstalledItems(installedItems, monitor);
		return Util.newStatus(getMessage(operationResult), operationResult);
	}

	protected IStatus doOperation(IProgressMonitor monitor, InstallationPair installationPair, Map installedItems,
			SubMonitor subMonitor) throws IAgentException {
		IStatus result;
		FrameworkProcessor processor = installationPair.processor();
		if (processor != null) {
			InstallationItem item = installationPair.item();
			InputStream input = null;
			try {
				input = item.getInputStream();
				Object installedItem = processor.install(input, item, framework, subMonitor.newChild(1));
				if (installedItem != null) {
					installedItems.put(installedItem, processor);
				}
				result = Status.OK_STATUS;
			} catch (Exception e) {
				result = new Status(Status.ERROR, FrameworkPlugin.PLUGIN_ID, e.getMessage());
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
					}
				}
				item.dispose();
			}
		} else {
			result = Status.CANCEL_STATUS;
		}
		return result;
	}

	private IStatus startInstalledItems(Map<Object, FrameworkProcessor> itemsToStart, IProgressMonitor monitor) {
		for (Entry<Object, FrameworkProcessor> entry : itemsToStart.entrySet()) {
			try {
				entry.getValue().start(entry.getKey(), monitor);
			} catch (Exception e) {
				StatusManager.getManager().handle(Util.newStatus(IStatus.ERROR, e.getMessage(), e), StatusManager.LOG);
			}
		}
		return Status.OK_STATUS;
	}

	protected IStatus handleException(IAgentException e) {
		return Util.handleIAgentException(e);
	}

	protected String getMessage(IStatus operationStatus) {
		return "Failed to install item(s) on remote framework";
	}
}

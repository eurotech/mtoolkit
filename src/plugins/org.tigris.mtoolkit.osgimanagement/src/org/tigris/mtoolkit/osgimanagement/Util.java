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
package org.tigris.mtoolkit.osgimanagement;

import java.io.File;
import java.util.Dictionary;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

/**
 * @since 5.0
 */
public class Util {

	public static IStatus handleIAgentException(IAgentException e) {
		Throwable cause = e.getCauseException() != null ? e.getCauseException() : e;
		return Util.newStatus(IStatus.ERROR, getErrorMessage(e), cause);
	}

	public static IStatus newStatus(String message, IStatus e) {
		return new MultiStatus(FrameworkPlugin.PLUGIN_ID, 0, new IStatus[] { e }, message, null);
	}

	public static IStatus newStatus(int severity, String message, Throwable t) {
		return new Status(severity, FrameworkPlugin.PLUGIN_ID, message, t);
	}

	private static String getErrorMessage(IAgentException e) {
		String msg = e.getMessage();
		if (msg == null) {
			msg = Messages.operation_failed;
			Throwable cause = e.getCauseException();
			if (cause != null && cause.getMessage() != null) {
				msg += " " + cause.getMessage(); //$NON-NLS-1$
			}
		}
		return msg;
	}

	/**
	 * @since 6.0
	 */
	public static void throwException(int severity, String message, Throwable t) throws CoreException {
		throw newException(severity, message, t);
	}

	/**
	 * @since 6.0
	 */
	public static CoreException newException(int severity, String message, Throwable t) {
		return new CoreException(newStatus(severity, message, t));
	}

	/**
	 * @since 6.0
	 */
	public static Framework addFramework(DeviceConnector connector, IProgressMonitor monitor) throws CoreException {
		if (!FrameworkPreferencesPage.isAutoConnectEnabled()) {
			return null;
		}
		Dictionary connProps = connector.getProperties();
		String frameWorkName = FrameworkConnectorFactory.generateFrameworkName(connProps);
		return addFramework(connector, frameWorkName, monitor);
	}

	public static Framework addFramework(DeviceConnector connector, String name, IProgressMonitor monitor)
			throws CoreException {
		if (!FrameworkPreferencesPage.isAutoConnectEnabled()) {
			return null;
		}
		FrameworkImpl fw = null;
		boolean success = false;
		try {
			fw = new FrameworkImpl(name, true);
			if (!connector.getVMManager().isVMActive()) {
				String message = Messages.connection_failed;
				throw newException(IStatus.ERROR, message, null);
			}
			FrameWorkView.getTreeRoot().addElement(fw);
			fw.connect(connector, SubMonitor.convert(monitor));
			success = true;
			return fw;
		} catch (IAgentException e) {
			throw newException(IStatus.ERROR, Messages.connection_failed, e);
		} finally {
			if (!success && fw != null) {
				FrameWorkView.getTreeRoot().removeElement(fw);
			}
		}
	}

	public static Set<String> getSystemBundles() {
		return FrameWorkView.getSystemBundles();
	}

	/**
	 * @since 6.0
	 */
	public static File[] openFileSelectionDialog(Shell shell, String title, String filter, String filterLabel,
			boolean multiple) {
		FileDialog dialog = new FileDialog(shell, SWT.OPEN | (multiple ? SWT.MULTI : SWT.SINGLE));
		String[] filterArr = { filter, "*.*" }; //$NON-NLS-1$
		String[] namesArr = { filterLabel, Messages.all_files_filter_label };
		dialog.setFilterExtensions(filterArr);
		dialog.setFilterNames(namesArr);
		if (FrameworkPlugin.fileDialogLastSelection != null) {
			dialog.setFileName(null);
			dialog.setFilterPath(FrameworkPlugin.fileDialogLastSelection);
		}
		dialog.setText(title);
		String res = dialog.open();
		if (res != null) {
			FrameworkPlugin.fileDialogLastSelection = res;
			// getFileNames returns relative names!
			String[] names = dialog.getFileNames();
			String path = dialog.getFilterPath();
			File[] files = new File[names.length];
			for (int i = 0; i < names.length; i++) {
				files[i] = new File(path, names[i]);
			}
			return files;
		}
		return null;
	}

	public static Framework findFramework(DeviceConnector connector) {
		FrameworkImpl[] fws = FrameWorkView.findFramework(connector);
		if (fws != null && fws.length > 0) {
			return fws[0];
		}
		return null;
	}

	/**
	 * @since 6.1
	 */
	public static void connectFramework(Framework fw) {
		FrameworkConnectorFactory.connectFrameWork(fw);
	}
}

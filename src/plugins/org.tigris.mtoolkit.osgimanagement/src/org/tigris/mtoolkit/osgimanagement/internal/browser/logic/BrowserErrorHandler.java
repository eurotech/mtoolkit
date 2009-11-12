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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.browser.model.Framework;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;

public class BrowserErrorHandler {

	private static boolean internalShell;
	private static Shell shell;
	public static boolean isInfoLogEnabled = FrameworkPreferencesPage.infoLogDefault;

	private static final boolean debug = Boolean.getBoolean("mtoolkit.osgimanagement.debug"); //$NON-NLS-1$

	// Process given error as a String message
	public static void processError(String message) {
		processError(message, false);
	}

	public static void processError(final String message, boolean showDialog) {
		if (showDialog) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					shell = getShell();
					if (shell != null) {
						if (!shell.isDisposed()) {
							MessageDialog.openError(shell, Messages.standard_error_title, message);
						}
						manageShell(shell);
					}
				}
			});
		}
		dumpToLog(IStatus.ERROR, message, null);
	}

	public static void processError(Throwable t, Model unknown) {
		if (unknown instanceof Model) {
			processError(t, findConnector((Model) unknown), ((FrameworkImpl)unknown.findFramework()).userDisconnect);
		}
	}

	private static DeviceConnector findConnector(Model node) {
		DeviceConnector connector = null;
		Framework fw = node.findFramework();
		if (fw != null)
			connector = fw.getConnector();
		return connector;
	}

	// Process given exception with no reason given
	public static void processError(Throwable t, DeviceConnector connector, boolean user) {
		if (!user) {
			processError(t, connector, Messages.no_reason_message);
		}
	}

	public static void processError(Throwable t, DeviceConnector connector, String reason) {
		boolean display = true;
		Boolean autoConnected = connector == null	? new Boolean(false)
													: (Boolean) connector.getProperties().get("framework-connection-immediate"); //$NON-NLS-1$
		if (autoConnected == null || autoConnected.booleanValue()) {
			display = false;
		}
		processError(t, reason, display);
	}

	// Process given exception with no reason given
	public static void processError(Throwable t, boolean showDialog) {
		processError(t, Messages.no_reason_message, showDialog);
	}

	public static void processError(Throwable t, String reason) {
		processError(t, reason, true);
	}

	// Process given exception with reason
	public static void processError(final Throwable t, String info, boolean display) {

		// Subsitute missing exception message
		final String reason[] = new String[1];
		if (t.getMessage() == null) {
			reason[0] = Messages.no_exception_message;
		} else {
			reason[0] = t.getMessage();
		}

		if (display) {
			if (t instanceof IAgentException || t instanceof IllegalStateException) {
				String infoCode = ""; //$NON-NLS-1$
				if (t instanceof IAgentException) {
					int errorCode = ((IAgentException) t).getErrorCode();
					if (errorCode != -1 && errorCode != 0) {
						infoCode = Messages.get(String.valueOf(errorCode).replace('-', '_'));
					}
					if (info == null || Messages.no_reason_message.equals(info)) {
						info = infoCode;
					}
				}
				final String trueMessage = info;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						shell = getShell();
						if (shell != null) {
							if (!shell.isDisposed()) {
								PluginUtilities.showErrorDialog(shell,
										Messages.standard_error_title,
										trueMessage,
										reason[0],
										t);
							}
							manageShell(shell);
						}
					}
				});
			} else {
				processError(info, display);
			}
		}

		if (t instanceof IAgentException && ((IAgentException) t).getCauseException() != null) {
			dumpToLog(IStatus.ERROR, reason[0], ((IAgentException) t).getCauseException());
		} else {
			dumpToLog(IStatus.ERROR, reason[0], t);
		}
	}

	public static void processWarning(final String info, boolean display) {
		if (display) {
			Display.getDefault().asyncExec(new Runnable() {
				public void run() {
					shell = getShell();
					if (shell != null) {
						if (!shell.isDisposed()) {
							PluginUtilities.showWarningDialog(shell, Messages.standard_error_title, info, null);
						}
						manageShell(shell);
					}
				}
			});
		}

		dumpToLog(IStatus.WARNING, info, null);
	}

	public static void processWarning(final Throwable t, String info, boolean display) {
		// Subsitute missing exception message
		final String reason;
		if (t.getMessage() == null) {
			reason = Messages.no_exception_message;
		} else {
			reason = t.getMessage();
		}

		if (display) {
			if (t instanceof IAgentException) {
				int errorCode = ((IAgentException) t).getErrorCode();
				if (errorCode != -1 && errorCode != 0) {
					info = Messages.get(String.valueOf(errorCode).replace('-', '_'));
				}
				final String trueMessage = info;
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						shell = getShell();
						if (shell != null) {
							if (!shell.isDisposed()) {
								PluginUtilities.showWarningDialog(shell,
										Messages.standard_error_title,
										trueMessage,
										reason);
							}
							manageShell(shell);
						}
					}
				});
			}
		}

		if (t instanceof IAgentException && ((IAgentException) t).getCauseException() != null) {
			dumpToLog(IStatus.WARNING, reason, ((IAgentException) t).getCauseException());
		} else {
			dumpToLog(IStatus.WARNING, reason, t);
		}
	}

	public static void processInfo(String text) {
		processInfo(text, true);
	}

	public static void showInfoDialog(final String text) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				shell = getShell();
				if (shell != null) {
					if (!shell.isDisposed()) {
						MessageDialog.openInformation(shell, Messages.standard_info_title, text);
					}
					manageShell(shell);
				}
			}
		});
	}

	public static void processInfo(final String text, boolean display) {
		if (display) {
			showInfoDialog(text);
		}
		if (isInfoLogEnabled) {
			dumpToLog(IStatus.INFO, text, null);
		}
	}

	// Dump to eclipse system log
	private static void dumpToLog(int severity, String text, Throwable t) {
		final FrameworkPlugin plugin = FrameworkPlugin.getDefault();
		if (plugin == null)
			return;
		final ILog log = plugin.getLog();

		if (text == null) {
			text = ""; //$NON-NLS-1$
		}

		final IStatus status = new Status(severity, plugin.getId(), 0, text, t);
		log.log(status);
	}

	// Get active shell
	public static Shell getShell() {
		Display display = Display.getCurrent();
		if (display == null)
			display = Display.getDefault();

		if (display == null) {
			return null;
		}
		final Display displ = display;
		displ.syncExec(new Runnable() {
			public void run() {
				Shell shell = displ.getActiveShell();
				if (shell == null) {
					shell = new Shell(displ);
					internalShell = true;
				}
				BrowserErrorHandler.shell = shell;
			}
		});

		return shell;
	}

	private static void manageShell(Shell shell) {
		if (internalShell) {
			shell.dispose();
			internalShell = false;
		}
	}

	public static void debug(String msg) {
		if (debug) {
			System.out.println("[OSGiManagement][debug] " + msg); //$NON-NLS-1$
		}

	}

	public static void debug(Throwable t) {
		if (debug) {
			t.printStackTrace(System.out);
		}
	}
}
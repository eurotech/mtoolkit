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
package org.tigris.mtoolkit.dpeditor.util;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.common.PluginUtilities;

/**
 * This class provides a static methods to shows and logs the error as warning,
 * information or errors message.
 */
public class DPPErrorHandler {

	public static String ERROR_MSG = "DPPEditor.Error";
	private static Shell shell;

	/**
	 * Process given exception with no reason given.
	 * 
	 * @param e
	 *            the occurred error
	 */
	public static void processError(final Throwable e) {
		processError(e, false);
	}

	/**
	 * Process given exception with no reason given and logs this error into the
	 * OSGi Log. Shows the error message dialog, depending on the given
	 * <code>boolean</code> flag.
	 * 
	 * @param e
	 *            the occurred error
	 * @param showDialog
	 *            if <code>true</code> shows error message dialog, otherwise
	 *            does not shows the dialog
	 */
	public static void processError(Throwable e, boolean showDialog) {
		processError(e, e.getMessage(), ResourceManager.getString("DPPErrorHandler.no_reason"), showDialog, true);
	}

	/**
	 * Process given exception with reason, logs this error into the OSGi Log
	 * and shows the error message dialog.
	 * 
	 * @param e
	 *            the occurred error
	 * @param reason
	 *            the reason
	 */
	public static void processError(Throwable e, String reason) {
		processError(e, e.getMessage(), reason, true);
	}

	/**
	 * Process given exception with reason and logs this error into the OSGi
	 * Log.
	 * 
	 * @param e
	 *            the occurred error
	 * @param reason
	 *            the reason
	 * @param showDialog
	 *            if <code>true</code> shows error message dialog, otherwise
	 *            does not shows the dialog
	 */
	public static void processError(Throwable e, String reason, boolean showDialog) {
		processError(e, e.getMessage(), reason, showDialog, true);
	}

	/**
	 * Process given exception with reason and shows the error message dialog.
	 * 
	 * @param e
	 *            the occurred error
	 * @param message
	 *            a human-readable message
	 * @param reason
	 *            the reason
	 * @param dumpOSGiLog
	 *            if is <code>true</code> logs this error in OSGi Log
	 */
	public static void processError(Throwable e, String message, String reason, boolean dumpOSGiLog) {
		processError(e, message, reason, true, dumpOSGiLog);
	}

	/**
	 * Process given exception with reason.
	 * 
	 * @param e
	 *            the occurred error
	 * @param message
	 *            a human-readable message
	 * @param reason
	 *            the reason
	 * @param showDialog
	 *            if <code>true</code> shows error message dialog, otherwise
	 *            does not shows the dialog
	 * @param dumpOSGiLog
	 *            if is <code>true</code> logs this error in OSGi Log
	 */
	public static void processError(final Throwable e, final String message, final String reason, final boolean showDialog, final boolean dumpOSGiLog) {
		// Display.getDefault().syncExec(new Runnable() {
		// public void run() {
		// Shell shell = getAnyShell();
		// if (shell != null && showDialog) {
		// if (!shell.isDisposed()) {
		// PluginUtilities.showErrorDialog(shell,
		// ResourceManager.getString("DPPErrorHandler.error"),
		// message, reason, e);
		// }
		// }
		// dumpToLog(IStatus.ERROR, message, e);
		// //Log to OSGi set
		// if (dumpOSGiLog) {
		// LogView logview = LogView.getLogView();
		// if (logview == null ? false : logview.isCreated()) {
		// logview.logError(message, e);
		// }
		// }
		// }
		// });

	}

	/**
	 * Logs the message as an information message. If given <code>boolean</code>
	 * flag for shows dialog is <code>true</code> open a standard information
	 * dialog.
	 * 
	 * @param text
	 *            a human-readable message
	 * @param showDialog
	 *            if <code>true</code> shows information message dialog,
	 *            otherwise does not shows the dialog
	 */
	public static void processInfo(final String text, final boolean showDialog) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Shell shell = getAnyShell();
				if (shell != null && showDialog) {
					if (!shell.isDisposed()) {
						MessageDialog.openInformation(shell, ResourceManager.getString("DPPErrorHandler.information"), text);
					}
				}
				dumpToLog(IStatus.INFO, text, null);
				processInfo(text);
			}
		});
	}

	/**
	 * Logs the message as an information message.
	 * 
	 * @param text
	 *            a human-readable message
	 */
	public static void processInfo(final String text) {
		// // Log to OSGi set
		// Display.getDefault().syncExec(new Runnable() {
		// public void run() {
		// LogView logview = LogView.getLogView();
		// if (logview == null ? false : logview.isCreated()) {
		// logview.logInfo(text);
		// }
		// }
		// });
		// dumpToLog(IStatus.INFO, text, null);
	}

	/**
	 * Process given exception with reason.
	 * 
	 * @param message
	 *            a human-readable message
	 * @param showDialog
	 *            if <code>true</code> shows error message dialog, otherwise
	 *            does not shows the dialog
	 */
	public static void processError(final String message, final boolean showDialog) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Shell shell = getAnyShell();
				if (shell != null && showDialog) {
					if (!shell.isDisposed()) {
						MessageDialog.openError(shell, ResourceManager.getString("DPPErrorHandler.error"), message);
					}
				}
				dumpToLog(IStatus.ERROR, message, null);
			}
		});
	}

	public static void processError(final String message, final String details) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Shell shell = getAnyShell();
				if (shell != null) {
					if (!shell.isDisposed()) {
						PluginUtilities.showDetailsErrorDialog(shell, ResourceManager.getString("DPPErrorHandler.error"), message, details);
					}
				}
				dumpToLog(IStatus.ERROR, message, null);
			}
		});
	}

	/**
	 * Logs the message as a warning message. If given <code>boolean</code> flag
	 * for shows dialog is <code>true</code> open a standard information dialog.
	 * 
	 * @param text
	 *            a human-readable message
	 * @param showDialog
	 *            if <code>true</code> shows information message dialog,
	 *            otherwise does not shows the dialog
	 */
	public static void processWarning(final String text, final boolean showDialog) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Shell shell = getAnyShell();
				if (shell != null && showDialog) {
					if (!shell.isDisposed()) {
						MessageDialog.openInformation(shell, ResourceManager.getString("DPPErrorHandler.warning"), text);
					}
				}
				dumpToLog(IStatus.INFO, text, null);
				processWarning(text);
			}
		});
	}

	/**
	 * Logs the message as a warning message.
	 * 
	 * @param text
	 *            a human-readable message
	 */
	public static void processWarning(final String text) {
		// Display.getDefault().syncExec(new Runnable() {
		// public void run() {
		// // Log to OSGi set
		// LogView logview = LogView.getLogView();
		// if (logview == null ? false : logview.isCreated()) {
		// logview.logWarning(text);
		// }
		// }
		// });
	}

	/**
	 * Opens a Yes/No question dialog for the given message text.
	 * 
	 * @param text
	 *            a human-readable message of the Yes/No question dialog
	 * @return <code>true</code> if the user presses the OK button of the
	 *         question dialog, <code>false</code> otherwise
	 */
	public static boolean processQuestion(String text) {
		return processQuestion(text, true);
	}

	/**
	 * Opens a Yes/No question dialog for the given message text.
	 * 
	 * @param text
	 *            a human-readable message of the Yes/No question dialog
	 * @param showDialog
	 *            if this parameter is <code>true</code> the question dialog
	 *            will be shown, otherwise the dialog will not appear
	 * @return <code>true</code> if the user presses the OK button of the
	 *         question dialog, <code>false</code> otherwise
	 */
	public static boolean processQuestion(final String text, final boolean showDialog) {
		final boolean result[] = new boolean[1];
		result[0] = false;

		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				Shell shell = getAnyShell();
				if (shell != null && showDialog) {
					if (!shell.isDisposed()) {
						result[0] = MessageDialog.openQuestion(shell, ResourceManager.getString("DPPErrorHandler.question"), text);
					}
				}
			}
		});

		return result[0];
	}

	/**
	 * Dump to eclipse system log.
	 * 
	 * @param severity
	 *            the severity of created status
	 * @param text
	 *            a human-readable message, localized to the created status
	 * @param t
	 *            a low-level exception, or <code>null</code> if not applicable
	 */
	private static void dumpToLog(int severity, String text, Throwable t) {
		// if (text == null) {
		// text = "";
		// }
		// Status status = new Status(severity, BundlesPlugin.PLUGIN_ID, 0,
		// text,
		// t);
		// BundlesPlugin.getDefault().getLog().log(status);
	}

	/**
	 * Get active shell.
	 * 
	 * @return the active display shell
	 */
	public static Shell getShell() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}

		if (display == null) {
			return null;
		}
		final Display displ = display;
		displ.syncExec(new Runnable() {
			public void run() {
				Shell shell = displ.getActiveShell();
				if (shell == null) {
					shell = new Shell(displ);
				}
				DPPErrorHandler.shell = shell;
			}
		});

		return shell;
	}

	/**
	 * Gets active shell.
	 * 
	 * @return the active shell
	 */
	public static Shell getAnyShell() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		Shell shell = display.getActiveShell();
		if (shell == null) {
			Shell shells[] = display.getShells();
			if (shells.length > 0)
				shell = shells[0];
		}
		return shell;
	}

	/**
	 * Opens a standard error dialog with the given message.
	 * 
	 * @param message
	 *            the message of the error dialog
	 */
	public static void showErrorTableDialog(final String message) {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				MessageDialog.openError(DPPErrorHandler.getAnyShell(), ResourceManager.getString(ERROR_MSG, ""), message);
			}
		});
	}
}

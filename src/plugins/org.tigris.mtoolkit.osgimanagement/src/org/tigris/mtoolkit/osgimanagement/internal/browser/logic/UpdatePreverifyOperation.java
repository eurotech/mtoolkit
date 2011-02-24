package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.BundleException;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;

public class UpdatePreverifyOperation extends RemoteBundleOperation {
	private final File bundleFile;

	public UpdatePreverifyOperation(Bundle bundle, File bundleFile) {
		super(Messages.update_bundle, bundle);
		this.bundleFile = bundleFile;
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		try {
			RemoteBundle rBundle = getBundle().getRemoteBundle();

			boolean nameDiff = true;
			boolean versionsDiff = true;
			ZipFile zip = new ZipFile(bundleFile);
			ZipEntry mf = zip.getEntry("META-INF/MANIFEST.MF");
			final String symbNames[] = new String[] { "", rBundle.getSymbolicName() };
			final String versions[] = new String[] { "", rBundle.getVersion() };
			if (mf != null) {
				Map headers = getManifestHeaders(zip.getInputStream(mf));
				if (headers != null) {
					symbNames[0] = (String) headers.get("Bundle-SymbolicName");
					versions[0] = (String) headers.get("Bundle-Version");
				}
				nameDiff = symbNames[1] != null && !symbNames[1].equals(symbNames[0]);
				versionsDiff = versions[1] != null && !versions[1].equals(versions[0]);
			}
			correctNullElements(symbNames, "unknown");
			correctNullElements(versions, "unknown");
			if (nameDiff) {
				final int confirm[] = new int[SWT.CANCEL];
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					public void run() {
						String message = "The new and old versions of the bundle have different symbolic names:\n"
								+ "Existing: " + symbNames[1] + " (" + versions[1] + ")\n"
								+ "New: " + symbNames[0] + " (" + versions[0] + ")\n"
								+ "Are you sure you want to do this?";
						confirm[0] = PluginUtilities.showConfirmationDialog(FrameWorkView.getShell(), "Update bundle",
								message);
					}
				});
				if (confirm[0] == SWT.CANCEL) {
					return Status.CANCEL_STATUS;
				}
			} else if (versionsDiff) {
				final int confirm[] = new int[SWT.CANCEL];
				PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
					public void run() {
						String message = "The new and old bundles have different versions:\n"
								+ "Existing: " + versions[1] + "\n"
								+ "New: " + versions[0] + "\n"
								+ "Are you sure you want to do this?";
						confirm[0] = PluginUtilities.showConfirmationDialog(FrameWorkView.getShell(), "Update bundle",
								message);
					}
				});
				if (confirm[0] == SWT.CANCEL) {
					return Status.CANCEL_STATUS;
				}
			}
		} catch (IOException ioe) {
			return Util.newStatus(IStatus.ERROR, "Failed to verify bundle", ioe);
		}
		return Status.OK_STATUS;
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind(Messages.bundle_update_failure, operationStatus);
	}

	private static Map getManifestHeaders(InputStream stream) throws IOException {
		try {
			return ManifestElement.parseBundleManifest(stream, null);
		} catch (BundleException e) {
			IOException ioe = new IOException("JAR Manifest is invalid: " + e.toString());
			ioe.initCause(e);
			throw ioe;
		}
	}

	private static void correctNullElements(String[] arr, String def) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == null) {
				arr[i] = def;
			}
		}
	}
}

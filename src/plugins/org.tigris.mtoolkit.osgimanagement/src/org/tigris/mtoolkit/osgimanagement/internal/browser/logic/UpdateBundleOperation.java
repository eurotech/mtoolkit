/**
 * 
 */
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.BundleException;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.common.android.AndroidUtils;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.installation.ProgressInputStream;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class UpdateBundleOperation extends RemoteBundleOperation {
	private final File bundleFile;

	public UpdateBundleOperation(Bundle bundle, File bundleFile) {
		super(Messages.update_bundle, bundle);
		this.bundleFile = bundleFile;
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 100);

		File preparedFile = null;
		InputStream pis = null;
		try {
			Framework framework = getBundle().findFramework();
			String transportType = (String) framework.getConnector().getProperties()
					.get(DeviceConnector.TRANSPORT_TYPE);

			// converting to dex
			if ("android".equals(transportType) && !AndroidUtils.isConvertedToDex(bundleFile)) {
				File convertedFile = new File(FrameworkPlugin.getDefault().getStateLocation() + "/dex/"
						+ bundleFile.getName());
				convertedFile.getParentFile().mkdirs();
				AndroidUtils.convertToDex(bundleFile, convertedFile, subMonitor.newChild(10));
				preparedFile = convertedFile;
			}
			// signing
			File signedFile = new File(FrameworkPlugin.getDefault().getStateLocation() + "/signed/"
					+ bundleFile.getName());
			signedFile.getParentFile().mkdirs();
			if (signedFile.exists()) {
				signedFile.delete();
			}
			try {
				CertUtils.signJar(preparedFile != null ? preparedFile : bundleFile, signedFile,
						subMonitor.newChild(10), framework.getSigningProperties());
			} catch (IOException ioe) {
				if (CertUtils.continueWithoutSigning(ioe.getMessage())) {
					signedFile.delete();
				} else {
					return Status.CANCEL_STATUS;
				}
			}
			if (signedFile.exists()) {
				if (preparedFile != null) {
					preparedFile.delete();
				}
				preparedFile = signedFile;
			}

			// updating
			File updateFile = preparedFile != null ? preparedFile : bundleFile;
			RemoteBundle rBundle = getBundle().getRemoteBundle();
			
			boolean warning = true;
			boolean versionsDiff = true;
			ZipFile zip = new ZipFile(updateFile);
			ZipEntry mf = zip.getEntry("META-INF/MANIFEST.MF");
			final String symbNames[] = new String[] {"", rBundle.getSymbolicName()};
			final String versions[] = new String[] {"", rBundle.getVersion()};
			if (mf != null) {
				Map headers = getManifestHeaders(zip.getInputStream(mf));
				if (headers != null) {
					symbNames[0] = (String) headers.get("Bundle-SymbolicName");
					versions[0] = (String) headers.get("Bundle-Version");
				}
				warning = symbNames[1] != null && !symbNames[1].equals(symbNames[0]);
				versionsDiff = versions[1] != null && !versions[1].equals(versions[0]);
			}
			if (warning) {
				final int confirm[] = new int[SWT.CANCEL];
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						String message = "The new and old versions of the bundle have different symbolic names:\n"+
							"Existing: "+symbNames[1]+"\n" +
							"New: "+symbNames[0]+"\n" +
							"Are you sure you want to do this?";
						confirm[0] = PluginUtilities.showConfirmationDialog(FrameWorkView.getShell(), "Update bundle", message);
					}
				});
				if (confirm[0] == SWT.CANCEL) {
					return Status.OK_STATUS;
				}
			}
			if (versionsDiff) {
				final int confirm[] = new int[SWT.CANCEL];
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						String message = "The new and old bundles have different versions:\n"+
							"Existing: "+versions[1]+"\n" +
							"New: "+versions[0]+"\n" +
							"Are you sure you want to do this?";
						confirm[0] = PluginUtilities.showConfirmationDialog(FrameWorkView.getShell(), "Update bundle", message);
					}
				});
				if (confirm[0] == SWT.CANCEL) {
					return Status.OK_STATUS;
				}
			}

			SubMonitor mon = subMonitor.newChild(80);
			mon.beginTask(Messages.update_bundle, (int) updateFile.length());
			
			pis = new ProgressInputStream(new FileInputStream(updateFile), mon);
			rBundle.update(pis);
			getBundle().refreshTypeFromRemote();
		} catch (IOException ioe) {
			return Util.newStatus(IStatus.ERROR, "Failed to update bundle", ioe);
		} finally {
			if (pis != null) {
				try {
					pis.close();
				} catch (IOException e) {
				}
			}
			if (preparedFile != null) {
				preparedFile.delete();
			}
		}
		return Status.OK_STATUS;
	}

	protected String getMessage(IStatus operationStatus) {
		return NLS.bind(Messages.bundle_update_failure, operationStatus);
	}
	
	public static Map getManifestHeaders(InputStream stream) throws IOException {
		try {
			return ManifestElement.parseBundleManifest(stream, null);
		} catch (BundleException e) {
			IOException ioe = new IOException("JAR Manifest is invalid: " + e.toString());
			ioe.initCause(e);
			throw ioe;
		}
	}

}
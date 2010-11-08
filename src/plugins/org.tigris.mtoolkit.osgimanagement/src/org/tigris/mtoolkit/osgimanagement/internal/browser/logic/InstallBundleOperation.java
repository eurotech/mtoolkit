package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.List;
import org.eclipse.ui.statushandlers.StatusManager;
import org.tigris.mtoolkit.common.installation.ProgressInputStream;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

public class InstallBundleOperation extends RemoteBundleOperation {
	private final File bundle;
	private final FrameworkImpl framework;
	private String symbolicName;
	private String version;

	public InstallBundleOperation(File bundle, FrameworkImpl framework) {
		super(Messages.install_bundle, null);
		this.bundle = bundle;
		this.framework = framework;
	}

	protected IStatus doOperation(IProgressMonitor monitor) throws IAgentException {
		InputStream input = null;
		RemoteBundle rBundle[] = null;
		ZipFile zip = null;
		InputStream zis = null;
		try {
			int work = (int) bundle.length();
			monitor.beginTask(getName(), work);
			input = new ProgressInputStream(new FileInputStream(bundle), monitor);
			DeviceConnector connector = framework.getConnector();
			if (connector == null)
				return Util.newStatus(IStatus.ERROR, "Connection lost", null);

			zip = new ZipFile(bundle);
			ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
			if (entry == null) {
				return Util.newStatus(IStatus.ERROR, "Invalid bundle content", null);
			}
			zis = zip.getInputStream(entry);
			Manifest mf = new Manifest(zis);
			symbolicName = getBundleName(mf);
			version = (String) mf.getMainAttributes().getValue("Bundle-Version");

			// check if already installd
			final boolean update[] = new boolean[] { false };
			final boolean install[] = new boolean[] { false };
			if (symbolicName != null) {
				rBundle = connector.getDeploymentManager().getBundles(symbolicName, "[" + version + "," + version + "]");
				if (rBundle != null) {
					update[0] = true;
				} else {
					rBundle = connector.getDeploymentManager().getBundles(symbolicName, null);
					if (rBundle != null) {
						install[0] = true;
					}
				}
			}

			// install if missing
			if (!update[0] && !install[0]) {
				Set bundleIds = new HashSet();
				bundleIds.addAll(framework.getBundlesKeys());
				rBundle = new RemoteBundle[1];
				rBundle[0] = connector.getDeploymentManager().installBundle(
						"remote:" + bundle.getName() + "." + getTimestamp(), input);
				// check again if already installed
				if (bundleIds.contains(new Long(rBundle[0].getBundleId()))) {
					update[0] = true;
				}
			}

			// bundle already exists, in which case, we need to update it
			if (update[0] || install[0]) {
				try {
					// close the old input stream and try again
					input.close();
				} catch (IOException e) {
				}
				final Object rBundles[] = rBundle;
				final int selected[] = new int[] { 0 };
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						if (!install[0]) {
							// if bundle with same symbolic name and version is
							// installed - ask user for update
							MessageDialog updateDialog = new MessageDialog(FrameWorkView.getShell(),
									Messages.update_dialog_title, null, "Bundle \"" + symbolicName + " (" + version
											+ ")\" is already installed.\nDo you want to update bundle?",
									MessageDialog.QUESTION, new String[] { "Update", "Cancel" }, 0);
							int updateResult = updateDialog.open();
							if (updateResult == 0) {
								update[0] = true;
							} else {
								update[0] = false;
							}
						} else {
							// if bundle(s) with same symbolic name is installed
							// ask user to install this version or to update
							// an existing bundle
							TitleAreaDialog updateDialog = new TitleAreaDialog(FrameWorkView.getShell()) {
								private Button updateButton;
								private List list;

								protected Control createDialogArea(Composite parent) {
									Control main = super.createDialogArea(parent);
									list = new List((Composite) main, SWT.BORDER);
									try {
										for (int i = 0; i < rBundles.length; i++) {
											list.add(symbolicName + " (" + ((RemoteBundle) rBundles[i]).getVersion() + ")");
										}
									} catch (IAgentException e) {
									}
									if (list.getItemCount() > 0)
										list.setSelection(0);
									list.addSelectionListener(new SelectionListener() {
										public void widgetSelected(SelectionEvent e) {
											updateButton.setEnabled(list.getSelectionIndex() != -1);
										}

										public void widgetDefaultSelected(SelectionEvent e) {
										}
									});
									list.setLayoutData(new GridData(GridData.FILL_BOTH));
									setTitle("Bundle \"" + symbolicName + "\" is already installed!\nInstall version "
											+ version + ", or select bundle to update.");
									getShell().setText("Update bundle");
									return main;
								}

								protected void createButtonsForButtonBar(Composite parent) {
									createButton(parent, IDialogConstants.CLIENT_ID + 1, "Install", false);
									updateButton = createButton(parent, IDialogConstants.CLIENT_ID + 2, "Update", false);
									updateButton.setEnabled(list.getSelectionIndex() != -1);
									createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL,
											true);
								}

								protected void buttonPressed(int buttonId) {
									selected[0] = list.getSelectionIndex();
									setReturnCode(buttonId);
									close();
								}

							};
							int updateResult = updateDialog.open();
							if (updateResult == IDialogConstants.CLIENT_ID + 1) {
								install[0] = true;
								update[0] = false;
							} else if (updateResult == IDialogConstants.CLIENT_ID + 2) {
								update[0] = true;
								install[0] = false;
							} else {
								install[0] = false;
								update[0] = false;
							}

						}
					}
				});

				if (install[0]) {
					monitor.beginTask(Messages.install_bundle, work);
					rBundle[0] = connector.getDeploymentManager().installBundle(
							"remote:" + bundle.getName() + "." + getTimestamp(),
							new ProgressInputStream(new FileInputStream(bundle), monitor));
				} else if (update[0]) {
					monitor.beginTask(Messages.update_bundle, work);
					input = new ProgressInputStream(new FileInputStream(bundle), monitor);
					rBundle[selected[0]].update(input);
				}
			}
		} catch (IOException e) {
			return Util.newStatus(IStatus.ERROR, NLS.bind(Messages.update_file_not_found, bundle.getName()),
					e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			if (zis != null) {
				try {
					zis.close();
				} catch (IOException e) {
				}
			}
			if (zip != null) {
				try {
					zip.close();
				} catch (IOException e) {
				}
			}
		}
		if (FrameworkConnectorFactory.isAutoStartBundlesEnabled && rBundle != null && rBundle[0].getType() != RemoteBundle.BUNDLE_TYPE_FRAGMENT) {
			try {
				monitor.setTaskName("Starting bundle "+symbolicName+" ("+version+")");
				rBundle[0].start(0);
			} catch (IAgentException e) {
				// only log this exception, because the user requested install
				// bundle, which succeeded
				StatusManager.getManager().handle(Util.handleIAgentException(e), StatusManager.LOG);
			}
		}
		return Status.OK_STATUS;
	}
	
	private String getBundleName(Manifest mf) {
		String symbolicName;
		symbolicName = (String) mf.getMainAttributes().getValue("Bundle-SymbolicName");
		int index = symbolicName.indexOf(';');
		if (index > 0) {
			symbolicName = symbolicName.substring(0, index);
		}
		return symbolicName;
	}

	private static final DateFormat df = new SimpleDateFormat("yyyyMMdd-hhmmssSSS");
	
	private String getTimestamp() {
		return df.format(new Date());
	}

	protected String getMessage(IStatus operationStatus) {
		return "Failed to install bundle on remote framework";
	}
}
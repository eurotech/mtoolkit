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
package org.tigris.mtoolkit.common.gui;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.images.UIResources;
import org.tigris.mtoolkit.common.security.UserInteraction;

public class SWTUserInteraction implements UserInteraction {

	public boolean confirmConnectionTrust(final int validationResult, final String message, final String host,
					final X509Certificate[] certChain) {
		final boolean result[] = new boolean[1];
		Display display = PlatformUI.getWorkbench().getDisplay();
		display.syncExec(new Runnable() {
			public void run() {
				SSLConfirmationDialog dialog = new SSLConfirmationDialog(null,
					host,
					getProblemDescription(validationResult),
					message,
					certChain);
				int returnCode = dialog.open();
				result[0] = returnCode == Window.OK;
			}
		});
		return result[0];
	}

	private String[] getProblemDescription(int validationResult) {
		List problems = new ArrayList(2);
		if ((validationResult & CERTIFICATE_NOT_TRUSTED) != 0)
			problems.add("Server provided certificate, which is not issued by a trusted authority.");
		if ((validationResult & CERTIFICATE_EXPIRED) != 0)
			problems.add("Server provided certificate, which is no longer valid.");
		if ((validationResult & CERTIFICATE_NOT_VALID_YET) != 0)
			problems.add("Server provided certificate, which valid period is in the future.");
		if ((validationResult & HOSTNAME_MISMATCH) != 0)
			problems.add("Server provided certificate, whose owner is not the server itself.");
		if ((validationResult & VERIFICATION_FAILED) != 0)
			problems.add("The verification of the server certificate failed for unknown reason.");
		return (String[]) problems.toArray(new String[problems.size()]);
	}

}

class SSLConfirmationDialog extends TitleAreaDialog {

	private class ResolveHostnameJob extends Job implements Runnable {
		private String ip;
		private volatile String result;
		private Display display;

		private ResolveHostnameJob(String ip, Display display) {
			super("Resolve hostname " + ip);
			setSystem(true);
			this.ip = ip;
			this.display = display;
		}

		protected IStatus run(IProgressMonitor monitor) {
			InetAddress address;
			try {
				address = InetAddress.getByName(ip);
				result = address.getHostName();
			} catch (UnknownHostException e) {
				return new Status(IStatus.ERROR, UtilitiesPlugin.PLUGIN_ID, "Failed to resolve " + ip, e);
			}
			updateHostnameLabel();
			return Status.OK_STATUS;
		}

		public String getHostname() {
			return result != null ? result + " (" + ip + ")" : ip;
		}

		public void updateHostnameLabel() {
			display.syncExec(this); // update the label in UI thread
		}

		public void run() {
			if (hostNameLbl != null)
				hostNameLbl.setText(getHostname());
		}
	}

	private Label hostNameLbl;
	private X509CertificateViewer certViewer;
	private X509Certificate[] certChain;
	private String[] explanations;
	private String technicalDetails;
	private ResolveHostnameJob resolveJob;

	private final class ExpansionResizer implements IExpansionListener {
		private Point oldSize;

		public void expansionStateChanging(ExpansionEvent e) {
			oldSize = getDialogArea().getSize();
		}

		public void expansionStateChanged(ExpansionEvent e) {
			Point newSize = getDialogArea().computeSize(oldSize.x, SWT.DEFAULT);
			Point shellSize = getShell().getSize();
			Point candidateShellSize = new Point(shellSize.x, shellSize.y + (newSize.y - oldSize.y));
			getShell().setSize(shellSize.x, Math.max(getInitialSize().y, candidateShellSize.y));
		}
	}

	public SSLConfirmationDialog(	Shell parentShell,
									String host,
									String[] problemDescriptions,
									String technicalMessage,
									X509Certificate[] chain) {
		super(parentShell);
		this.explanations = problemDescriptions;
		this.technicalDetails = technicalMessage;
		this.certChain = chain;
		resolveJob = new ResolveHostnameJob(host, PlatformUI.getWorkbench().getDisplay());
		resolveJob.schedule();
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Warning - Security");
	}

	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	protected Control createContents(Composite parent) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.SSL_CONFIRM_DIALOG);
		Control contents = super.createContents(parent);

		setTitle("The server digital signature cannot be verified.\nDo you still want to connect?");
		setTitleImage(UIResources.getImage(UIResources.SSL_INTERACTION_WIZBAN_ICON));

		return contents;
	}

	private String joinStrings(String[] strings) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < strings.length; i++) {
			buf.append(strings[i]).append('\n');
		}
		buf.deleteCharAt(buf.length() - 1);
		return buf.toString();
	}

	protected Control createDialogArea(Composite parent) {
		Composite parentDialogArea = (Composite) super.createDialogArea(parent);
		Composite dialogArea = new Composite(parentDialogArea, SWT.NONE);
		dialogArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 10;
		layout.verticalSpacing = 10;
		dialogArea.setLayout(layout);

		hostNameLbl = createPanelEntry(dialogArea, "Host:");
		resolveJob.updateHostnameLabel();

		Label problemView = createPanelEntry(dialogArea, "Problem:");
		problemView.setText(joinStrings(explanations));

		if (certChain != null) {
			// certificate information in expandable composite
			ExpandableComposite certificateExpand = new ExpandableComposite(dialogArea,
				ExpandableComposite.CLIENT_INDENT | ExpandableComposite.FOCUS_TITLE | ExpandableComposite.TWISTIE);
			certificateExpand.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			certificateExpand.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
			certificateExpand.setText("Certificate");
			certificateExpand.addExpansionListener(new ExpansionResizer());

			// the certificate panel itself
			certViewer = new X509CertificateViewer(certificateExpand, SWT.NONE);
			certViewer.setCertificate(certChain[0]);
			certificateExpand.setClient(certViewer.getControl());
		}

		if (technicalDetails != null && technicalDetails.length() > 0) {
			ExpandableComposite technicalDetailsExpand = new ExpandableComposite(dialogArea,
				ExpandableComposite.CLIENT_INDENT | ExpandableComposite.FOCUS_TITLE | ExpandableComposite.TWISTIE);
			technicalDetailsExpand.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			technicalDetailsExpand.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
			technicalDetailsExpand.setText("Technical Details");
			technicalDetailsExpand.addExpansionListener(new ExpansionResizer());

			Label technicalDetailsLbl = new Label(technicalDetailsExpand, SWT.WRAP);
			technicalDetailsLbl.setText(technicalDetails);
			technicalDetailsExpand.setClient(technicalDetailsLbl);
		}

		return parentDialogArea;
	}

	private Label createPanelEntry(Composite parent, String label) {
		Label entryNameLbl = new Label(parent, SWT.NONE);
		entryNameLbl.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		entryNameLbl.setText(label);
		entryNameLbl.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));

		Label entryValueLbl = new Label(parent, SWT.NONE);
		entryValueLbl.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		return entryValueLbl;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, "Confirm", false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

}

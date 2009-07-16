package org.tigris.mtoolkit.common.gui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

// IPLOG: Parts of this class were got from the original StatusDialog 
public class StatusLineDialog extends TrayDialog {

	private String dialogTitle;

	private StatusLine statusLine;
	private IStatus fLastStatus;
	private Button fOkButton;

	private boolean shellInitialized = false;

	public StatusLineDialog(Shell shell, String title) {
		super(shell);
		Assert.isNotNull(title);
		this.dialogTitle = title;
	}

	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(dialogTitle);
	}
	
	protected int getShellStyle() {
		return super.getShellStyle() | SWT.RESIZE;
	}

	protected Control createButtonBar(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginLeft = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		statusLine = new StatusLine(composite, SWT.NONE);
		GridData statusData = new GridData(SWT.FILL, SWT.CENTER, true, false);

		if (isHelpAvailable()) {
			statusData.horizontalSpan = 2;
			createHelpControl(composite);
		}
		statusLine.setLayoutData(statusData);
		applyDialogFont(composite);

		/*
		 * Create the rest of the button bar, but tell it not to create a help
		 * button (we've already created it).
		 */
		boolean helpAvailable = isHelpAvailable();
		setHelpAvailable(false);
		super.createButtonBar(composite);
		setHelpAvailable(helpAvailable);
		return composite;
	}

	public IStatus getStatus() {
		return fLastStatus;
	}

	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		refresh();
		if (fLastStatus != null) {
			IStatus safeStatus = new Status(fLastStatus.getSeverity(), fLastStatus.getPlugin(), fLastStatus.getCode(), "", fLastStatus.getException()); //$NON-NLS-1$
			updateStatus(safeStatus);
		}
		return control;
	}

	public void updateStatus(IStatus status) {
		if (fLastStatus != null && fLastStatus.equals(status))
			return;
		fLastStatus = status;
		updateButtonsEnableState(status);
		if (statusLine != null) {
			Point oldSize = statusLine.getSize();

			statusLine.updateStatus(status);

			if (shellInitialized) { // only resize the shell if we have already
				// showed it, otherwise do nothing, it will
				// display the shell with correct size
				Point newSize = statusLine.computeSize(oldSize.x, SWT.DEFAULT);
				Point shellSize = getShell().getSize();
				getShell().setSize(shellSize.x, shellSize.y + (newSize.y - oldSize.y));
				getShell().layout(true, true);
			}
		}
	}

	protected void initializeBounds() {
		super.initializeBounds();
		shellInitialized = true;
	}

	protected void updateButtonsEnableState(IStatus status) {
		if (fOkButton != null && !fOkButton.isDisposed()) {
			fOkButton.setEnabled(!status.matches(IStatus.ERROR));
			getShell().setDefaultButton(fOkButton);
		}
	}

	protected void createButtonsForButtonBar(Composite parent) {
		fOkButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		getShell().setDefaultButton(fOkButton);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		if (fLastStatus != null)
			updateButtonsEnableState(fLastStatus);
	}

	protected void commit() {

	}

	protected void refresh() {

	}

	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			if ((getStatus().getSeverity() == IStatus.OK) || (getStatus().getSeverity() == IStatus.WARNING)) {
				commit();
			}
		}
		super.buttonPressed(buttonId);
	}

	protected void setTextField(Text field, String value) {
		field.setText(value == null ? "" : value); //$NON-NLS-1$
	}
}

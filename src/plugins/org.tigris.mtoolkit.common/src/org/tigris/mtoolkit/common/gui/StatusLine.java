/**
 * 
 */
package org.tigris.mtoolkit.common.gui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.tigris.mtoolkit.common.images.UIResources;

public final class StatusLine extends Composite {
	
	private Label fStatusIcon;
	private Label fStatusMessage;
	private IStatus fLastStatus;
	
	public StatusLine(Composite parent, int style) {
		super(parent, style);
		createContents();
	}
	
	public void createContents() {
		setLayout(new GridLayout(2, false));
		
		fStatusIcon = new Label(this, SWT.LEFT);
		fStatusIcon.setLayoutData(new GridData());
		
		fStatusMessage = new Label(this, SWT.LEFT | SWT.WRAP);
		fStatusMessage.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	}
	
	public void updateStatus(IStatus status) {
		fLastStatus = status;
		updateStatusLine();
	}
	
	public IStatus getStatus() {
		return fLastStatus;
	}
	
	private void updateStatusLine() {
		setRedraw(false);
		try {
			if (fLastStatus == null || fLastStatus.isOK() || fLastStatus.getMessage().length() < 1) {
				fStatusIcon.setImage(null);
				fStatusMessage.setText("");
			} else {
				fStatusIcon.setImage(findImage(fLastStatus));
				fStatusMessage.setText(fLastStatus.getMessage());
			}
		} finally {
			setRedraw(true);
		}
	}
	
	private Image findImage(IStatus status) {
		if (status.matches(IStatus.ERROR)) {
			return UIResources.getImage(UIResources.SMALL_ERROR_ICON);
		} else if (status.matches(IStatus.WARNING)) {
			return UIResources.getImage(UIResources.SMALL_WARNING_ICON);
		} else if (status.matches(IStatus.INFO)) {
			return UIResources.getImage(UIResources.SMALL_INFO_ICON);
		}
		return null;
	}

}
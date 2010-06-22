package org.tigris.mtoolkit.common.gui;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @since 5.1
 */
public class ErrorPanel extends Composite {

	public static final int NONE = 0;
	
	public static final int EXPANDED = 1 << 0;
	
	public static final int NO_DETAILS = 1 << 1;
	
	public static final int SMALL_ICONS = 1 << 2;
	
	private final int panelStyle;
	private IStatus status;
	private Label statusIcon;
	private Text statusText;
	private Text detailsText;
	private Control detailsArea;
	private FontMetrics fontMetrics;
	private boolean detailsAreaShowed;

	public ErrorPanel(Composite parent, int panelStyle, int style) {
		super(parent, style);
		this.panelStyle = panelStyle;
		if ((panelStyle & NO_DETAILS) != 0)
			super.setLayout(new GridLayout(2, false));
		else
			super.setLayout(new GridLayout(3, false));
		initializeDialogUnits(this);
		createContents();
	}
	
	protected void initializeDialogUnits(Control control) {
		// Compute and store a font metric
		GC gc = new GC(control);
		gc.setFont(JFaceResources.getDialogFont());
		fontMetrics = gc.getFontMetrics();
		gc.dispose();
	}
	
	private void createContents() {
		statusIcon = new Label(this, SWT.NONE);
		statusIcon.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		statusText = new Text(this, SWT.WRAP | SWT.READ_ONLY);
		statusText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
//		statusText.setEnabled(false);
		
		if ((panelStyle & NO_DETAILS) == 0) { 
			Button btnDetails = new Button(this, SWT.PUSH);
			btnDetails.setText("Details >>");
			GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
			gd.widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);
			btnDetails.setLayoutData(gd);
			btnDetails.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					toggleDetailsArea();
				}
			});
			
			createDetailsArea();
		}
	}
	
	private void createDetailsArea() {
		Composite detailsArea = new Composite(this, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
		gd.heightHint = Dialog.convertVerticalDLUsToPixels(fontMetrics, 100);
		detailsArea.setLayoutData(gd);
		detailsArea.setLayout(new FillLayout());
		detailsArea.setVisible(false);
		
		detailsText = new Text(detailsArea, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		
		this.detailsArea = detailsArea;
	}

	public void toggleDetailsArea() {
		detailsArea.setVisible(!detailsAreaShowed);
		detailsAreaShowed = !detailsAreaShowed;
		layout(true);
	}
	
	public boolean isDetailsAreaVisible() {
		return detailsAreaShowed;
	}

	public IStatus getStatus() {
		return status;
	}
	
	public void setStatus(IStatus status) {
		this.status = status;
		showStatus();
	}

	private void showStatus() {
		IStatus status = getStatus();
		Image severityIcon;
		if ((panelStyle & SMALL_ICONS) != 0)
			severityIcon = getSeverityIconSmall(status);
		else
			severityIcon = getSeverityIcon(status);
		statusIcon.setImage(severityIcon);
		statusText.setText(status.getMessage());
		if (detailsText != null)
			detailsText.setText(getDetails(status));
		layout(true);
	}
	
	private Image getSeverityIcon(IStatus status) {
		switch (status.getSeverity()) {
		case IStatus.ERROR:
			return getDisplay().getSystemImage(SWT.ICON_ERROR);
		case IStatus.WARNING:
			return getDisplay().getSystemImage(SWT.ICON_WARNING);
		default:
			return getDisplay().getSystemImage(SWT.ICON_INFORMATION);
		}
	}
	
	private Image getSeverityIconSmall(IStatus status) {
		ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
		switch (status.getSeverity()) {
		case IStatus.ERROR:
			return images.getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
		case IStatus.WARNING:
			return images.getImage(ISharedImages.IMG_OBJS_WARN_TSK);
		default:
			return images.getImage(ISharedImages.IMG_OBJS_INFO_TSK);
		}
	}
	
	private String getDetails(IStatus status) {
		if (status.getException() != null) {
			return getStackTrace(status.getException()); 
		}
		return "No details are available";
	}
	
	// IPLOG: This method is taken from org.eclipse.ui.internal.part.StatusPart
    private String getStackTrace(Throwable throwable) {
        StringWriter swriter = new StringWriter();
        PrintWriter pwriter = new PrintWriter(swriter);
        throwable.printStackTrace(pwriter);
        pwriter.flush();
        pwriter.close();
        return swriter.toString();
    }
}

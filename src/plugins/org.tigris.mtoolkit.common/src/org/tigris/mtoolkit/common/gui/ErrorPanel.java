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
 * Simple panel, which takes a single {@link IStatus} and display it in simple
 * form: a single line with an icon (determined by the severity of the status),
 * a text (with the message of the status) and optional button to show details
 * regarding the status (determined by the contained exception).
 * <p>
 * Multistatuses are not supported at the moment.
 * </p>
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @since 6.0
 */
public class ErrorPanel extends Composite {

	/**
	 * Default style for the error panel.
	 */
	public static final int NONE = 0;

	/**
	 * Style constant for initially expanded state. When this constant is set,
	 * the error panel will show details area by default.
	 */
	public static final int EXPANDED = 1 << 0;

	/**
	 * Style constant for disabling the details area in the error panel.
	 */
	public static final int NO_DETAILS = 1 << 1;

	/**
	 * Style constant for using small icon. Usable, when the error panel is
	 * embedded among other dialog controls.
	 */
	public static final int SMALL_ICONS = 1 << 2;

	private final int panelStyle;
	private IStatus status;
	private Label statusIcon;
	private Text statusText;
	private Text detailsText;
	private Control detailsArea;
	private FontMetrics fontMetrics;
	private boolean detailsAreaShowed;

	/**
	 * Constructs new {@link ErrorPanel} with given parent, panel style and
	 * style for the underlying Composite.
	 * <p>
	 * Panel style:
	 * 
	 * </p>
	 * @param parent
	 *            the parent of the new error panel
	 * @param panelStyle
	 * 			the 
	 * @param style
	 */
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

	private void initializeDialogUnits(Control control) {
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

	/**
	 * Toggles details area on and off
	 */
	public void toggleDetailsArea() {
		detailsArea.setVisible(!detailsAreaShowed);
		detailsAreaShowed = !detailsAreaShowed;
		layout(true);
	}

	/**
	 * Returns whether details area is visible or not
	 * 
	 * @return true if the details area is currently visible
	 */
	public boolean isDetailsAreaVisible() {
		return detailsAreaShowed;
	}

	/**
	 * Returns the current {@link IStatus} shown
	 * 
	 * @return current {@link IStatus}
	 */
	public IStatus getStatus() {
		return status;
	}

	/**
	 * Sets {@link IStatus} object to be shown
	 * 
	 * @param status
	 */
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

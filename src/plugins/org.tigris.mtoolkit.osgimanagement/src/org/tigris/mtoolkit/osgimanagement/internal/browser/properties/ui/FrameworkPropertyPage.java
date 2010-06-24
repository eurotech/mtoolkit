package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.dialogs.PropertyPage;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

public class FrameworkPropertyPage extends PropertyPage implements ConstantsDistributor, FrameworkPanel.ErrorMonitor {

	private FrameworkPanel fwPanel;
	private FrameworkImpl fw;

	public FrameworkPropertyPage() {
		super();
	}

	/**
	 * @see PreferencePage#createContents(Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData gridData = new GridData(GridData.FILL);
		gridData.grabExcessHorizontalSpace = true;
		composite.setLayoutData(gridData);

		fw = (FrameworkImpl) getElement();

		fwPanel = new FrameworkPanel(composite, fw, fw.getParent());
		fwPanel.setErrorMonitor(this);
		fwPanel.initialize(fw.getConfig());

		return composite;
	}

	protected void performDefaults() {

	}

	public boolean performOk() {
		boolean correct = fwPanel.validate();
		if (correct) {
			String oldLabel = fw.getLabel();

			boolean connChanged = fwPanel.save(fw.getConfig());
			fw.setName(fw.getConfig().getString(FRAMEWORK_NAME));

			updateTitle(oldLabel, fw.getLabel());

			DeviceConnector connector = fw.getConnector();
			if (connector != null) {
				if (fw.isConnected() && connChanged) {
					MessageDialog.openInformation(getShell(), Messages.framework_ip_changed_title,
							Messages.framework_ip_changed_message);
				}
			}
			fw.updateElement();
		}

		return correct;
	}

	private void updateTitle(String oldName, String newName) {
		// there is no other way to update PropertyDialog's title
		String title = getShell().getText();
		title = title.replace(oldName, newName);
		getShell().setText(title);
	}
}

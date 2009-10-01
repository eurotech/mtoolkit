package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import java.net.InetAddress;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProvider;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;

public class SocketTypeProvider implements DeviceTypeProvider, ConstantsDistributor {

	private final String TRANSPORT_TYPE = "socket";

	private Text idText;

	// Initialize ui values from storage
	public void setProperties(IMemento config) {
		String idString = config.getString(FRAMEWORK_ID);
		if (idString == null) {
			idString = "127.0.0.1";
		}
		idText.setText(idString);
//		aConProps.put("framework-connection-immediate", new Boolean(false)); //$NON-NLS-1$
	}

	public Control createPanel(Composite parent) {
		Composite contentPanel = new Composite(parent, SWT.NULL);
		GridLayout groupLayout = new GridLayout(2, false);
		contentPanel.setLayout(groupLayout);

		// create IP label:
		Label ipLabel = new Label(contentPanel, SWT.NONE);
		ipLabel.setText("Address:");
		ipLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		idText = new Text(contentPanel, SWT.SINGLE | SWT.BORDER);
		idText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		return contentPanel;
	}

	public boolean validate() {
		String ip = getTransportID();
		try {
			// just check if address is correct
			InetAddress.getByName(ip);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	public String getTransportID() {
		return idText.getText().trim();
	}

	public String getTransportType() {
		return TRANSPORT_TYPE;
	}

	public Dictionary load(IMemento config) {
		Dictionary aConnProps = new Hashtable();
		aConnProps.put("framework-connection-immediate", Boolean.FALSE);
		aConnProps.put(ConstantsDistributor.FRAMEWORK_ID, config.getString(ConstantsDistributor.FRAMEWORK_ID));
		return aConnProps;
	}

	public void save(IMemento config) {
		config.putString(ConstantsDistributor.FRAMEWORK_ID, getTransportID());
	}

	public void setEditable(boolean editable) {
		idText.setEditable(editable);
	}

}

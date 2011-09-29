package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import java.net.InetAddress;
import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProvider;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProviderValidator;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class SocketTypeProvider implements DeviceTypeProvider, ConstantsDistributor {

	private final String TRANSPORT_TYPE = "socket";

	private Text idText;
	private Text portText;

	private DeviceTypeProviderValidator validator;

	// Initialize ui values from storage
	public void setProperties(IMemento config) {
		if (config == null) return;
		String idString = config.getString(Framework.FRAMEWORK_ID);
		if (idString == null || idString.length() == 0) {
			idString = "127.0.0.1";
		}
		idText.setText(idString);
		
		Integer port = config.getInteger(DeviceConnector.PROP_PMP_PORT);
		if (port == null) {
			port = new Integer(1450);
		}
		portText.setText(port.toString());
	}

	public Control createPanel(Composite parent, DeviceTypeProviderValidator validator) {
		this.validator = validator;
		Composite contentPanel = new Composite(parent, SWT.NULL);
		GridLayout groupLayout = new GridLayout(2, false);
		contentPanel.setLayout(groupLayout);

		// create IP label:
		Label ipLabel = new Label(contentPanel, SWT.NONE);
		ipLabel.setText("Address:");
		ipLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		idText = new Text(contentPanel, SWT.SINGLE | SWT.BORDER);
		idText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (validator != null) {
			idText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validateJob = new Job("Validate address") {
						protected IStatus run(IProgressMonitor monitor) {
							final String ip[] = new String[1];
							PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
								public void run() {
									ip[0] = getTransportID();
								}
							});
							final String result = validate(ip[0]);
							if (validateJob == this) {
								PlatformUI.getWorkbench().getDisplay().syncExec(new Runnable() {
									public void run() {
										SocketTypeProvider.this.validator.setValidState(result);
									}
								});
							}
							return Status.OK_STATUS;
						}
					};
					validateJob.schedule();
				}
			});
		}
		
		Label portLabel = new Label(contentPanel, SWT.NONE);
		portLabel.setText("Port:");
		portLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

		portText = new Text(contentPanel, SWT.SINGLE | SWT.BORDER);
		portText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (validator != null) {
			portText.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					String result = portText.getText();
					try {
						Integer.parseInt(result);
						result = null;
					} catch (NumberFormatException ex) {
						result = "Invalid port: " + result;
					}
					SocketTypeProvider.this.validator.setValidState(result);
				}
			});
		}
		
		return contentPanel;
	}

	private Job validateJob;
	

	public String validate() {
		String ip = getTransportID();
		String result = validate(ip);
		return result == null ? validatePort(portText.getText()) : result;
	}
	
	public String validate(String ip) {
		if (ip.trim().equals("")) {
			return "Type framework address";
		}
		try {
			// just check if address is correct
			InetAddress.getByName(ip);
		} catch (Exception e) {
			return "Invalid address: " + e.getMessage();
		}
		return null;
	}

	public String validatePort(String port) {
		if (port.trim().equals("")) {
			return "Type port number";
		}
		try {
			int intPort = Integer.parseInt(port);
		} catch (NumberFormatException ex) {
			return "Invalid port: " + port;
		}
		return null;
	}
	
	
	public String getTransportID() {
		return idText.getText().trim();
	}

	public String getTransportType() {
		return TRANSPORT_TYPE;
	}
	
	private Integer getTransportPort() {
		return Integer.decode(portText.getText().trim());
	}

	public Dictionary load(IMemento config) {
		Dictionary aConnProps = new Hashtable();
		aConnProps.put("framework-connection-immediate", Boolean.FALSE);
		aConnProps.put(Framework.FRAMEWORK_ID, config.getString(Framework.FRAMEWORK_ID));
		Integer port = config.getInteger(DeviceConnector.PROP_PMP_PORT);
		if (port == null) port = new Integer(1450);
		aConnProps.put(DeviceConnector.PROP_PMP_PORT, port);
		return aConnProps;
	}

	public void save(IMemento config) {
		config.putString(Framework.FRAMEWORK_ID, getTransportID());
		config.putInteger(DeviceConnector.PROP_PMP_PORT, getTransportPort().intValue());
	}

	public void setEditable(boolean editable) {
		idText.setEditable(editable);
		portText.setEditable(editable);
	}
}

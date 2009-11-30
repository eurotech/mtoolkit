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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProvider;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProviderValidator;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class SocketTypeProvider implements DeviceTypeProvider, ConstantsDistributor {

	private final String TRANSPORT_TYPE = "socket";

	private Text idText;
	
	private DeviceTypeProviderValidator validator;

	// Initialize ui values from storage
	public void setProperties(IMemento config) {
		String idString = config.getString(Framework.FRAMEWORK_ID);
		if (idString == null) {
			idString = "127.0.0.1";
		}
		idText.setText(idString);
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
							Display.getDefault().syncExec(new Runnable() {
								public void run() {
									ip[0] = getTransportID();
								}
							});
							final String result = validate(ip[0]);
							if (validateJob == this) {
								Display.getDefault().syncExec(new Runnable() {
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
		return contentPanel;
	}

	private Job validateJob;
	
	public String validate() {
		String ip = getTransportID();
		return validate(ip);
	}
	
	public String validate(String ip) {
		try {
			// just check if address is correct
			InetAddress.getByName(ip);
		} catch (Exception e) {
			return "Invalid address\n"+e.getMessage();
		}
		return null;
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
		aConnProps.put(Framework.FRAMEWORK_ID, config.getString(Framework.FRAMEWORK_ID));
		return aConnProps;
	}

	public void save(IMemento config) {
		config.putString(Framework.FRAMEWORK_ID, getTransportID());
	}

	public void setEditable(boolean editable) {
		idText.setEditable(editable);
	}

}

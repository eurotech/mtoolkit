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
package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.tigris.mtoolkit.common.certificates.CertificatesPanel;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProvider;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProviderValidator;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class PropertySheet extends TitleAreaDialog implements /*ControlListener, */ConstantsDistributor, SelectionListener, DeviceTypeProviderValidator {
	
	private static final String DEFAULT_DEVICE_TYPE = "DEFAULT_DEVICE_TYPE";

	private Text textServer;

	public Button connectButton;

	private CertificatesPanel certificatesPanel;

	private FrameworkImpl fw;

	private List deviceTypesProviders;

	private Combo deviceTypeCombo;

	private DeviceTypeProviderElement selectedProvider;

	private PageBook pageBook;

	private Composite mainContent;

	private boolean addFramework;

	private Model parent;

	private TreeViewer parentView;

	// Constructor
	public PropertySheet(TreeViewer parentView, Model parent, FrameworkImpl element, boolean newFramework) {
		super(parentView.getControl().getShell());
		this.addFramework = newFramework;
		this.parent = parent;
		this.parentView = parentView;
		this.setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
		fw = element;
	}

	// Create page contents
	protected Control createDialogArea(Composite parent) {
		Control main = super.createDialogArea(parent);
		setTitle("Framework details");
		setMessage("Edit framework details");
		
		parent.getShell().setText(Messages.framework_properties_title);

		mainContent = new Composite((Composite) main, SWT.NONE);
		mainContent.setLayout(new GridLayout());
		GridData mainGD = new GridData(GridData.FILL_BOTH);
		mainGD.minimumWidth = 300;
		mainContent.setLayoutData(mainGD);

		Group deviceGroup = new Group(mainContent, SWT.NONE);
		deviceGroup.setText(Messages.framework_name_label);
		deviceGroup.setLayout(new GridLayout());
		deviceGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		textServer = createText(1, deviceGroup);
		textServer.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				checkFrameworkInfo();
			}
		});

		deviceTypesProviders = obtainDeviceTypeProviders(this);
		
		createDeviceTypeCombo(mainContent);

		// Connect properties group
		Group connectPropertiesGroup = new Group(mainContent, SWT.NONE);
		connectPropertiesGroup.setText(Messages.connect_properties_group_label);
		connectPropertiesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		connectPropertiesGroup.setLayout(new GridLayout());

		pageBook = new PageBook(connectPropertiesGroup, SWT.NONE);
		pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		String type = FrameworkPlugin.getDefault().getPreferenceStore().getString(DEFAULT_DEVICE_TYPE);
		if ("".equals(type)) {
			type = "plainSocket";
		}
		
		int index = 0;
		for (int i=0; i<deviceTypesProviders.size(); i++) {
			DeviceTypeProviderElement provider = (DeviceTypeProviderElement) deviceTypesProviders.get(i);
			if (type.equals(provider.getTypeId())) {
				index = i;
				break;
			}
		}
		deviceTypeCombo.select(index);
		
		selectedProvider = (DeviceTypeProviderElement) deviceTypesProviders.get(0);
		showDeviceTypePanel(selectedProvider);

		// Signing Certificates
		certificatesPanel = new CertificatesPanel(mainContent, 2, 1);

		// Autoconnect checkbox
		if (!fw.autoConnected && fw.getParent() == null) {
			connectButton = createCheckboxButton(Messages.connect_button_label, mainContent);
			connectButton.setEnabled(!fw.isConnected());
		}

		init();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(mainContent, IHelpContextIds.PROPERTY_FRAMEWORK);

		return mainContent;
	}

	private void createDeviceTypeCombo(Composite parent) {
		Group typeGroup = new Group(parent, SWT.NONE);
		typeGroup.setText("Connection Type:");
		FillLayout layout = new FillLayout();
		layout.marginHeight = 5;
		layout.marginWidth = 5;
		typeGroup.setLayout(layout);
		typeGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		deviceTypeCombo = new Combo(typeGroup, SWT.READ_ONLY);
		deviceTypeCombo.addSelectionListener(this);

		for (Iterator it = deviceTypesProviders.iterator(); it.hasNext();) {
			DeviceTypeProviderElement element = (DeviceTypeProviderElement) it.next();
			deviceTypeCombo.add(element.getDeviceTypeName());
		}

		if (selectedProvider != null)
			selectType(selectedProvider);
	}

	private void selectType(DeviceTypeProviderElement element) {
		selectedProvider = element;
		if (deviceTypeCombo != null) {
			int indexOf = deviceTypesProviders.indexOf(element);
			if (indexOf != -1) {
				deviceTypeCombo.select(indexOf);
				showDeviceTypePanel(element);
			}
		}
	}

	private void showDeviceTypePanel(DeviceTypeProviderElement element) {
		Control panel = element.createPanel(pageBook);
		pageBook.showPage(panel);
		mainContent.layout(true);
	}

	// Save ui values to storage and update target element
	public void saveConfig(IMemento config) {
		config.putString(FRAMEWORK_NAME, textServer.getText());
		try {
			selectedProvider.getProvider().save(config);
			config.putString(TRANSPORT_PROVIDER_ID, selectedProvider.getTypeId());
		} catch (CoreException e) {
			e.printStackTrace();
		}

		if (connectButton != null) {
			config.putBoolean(CONNECT_TO_FRAMEWORK, connectButton.getSelection());
		}
			
		// Signing Certificates
		fw.setSignCertificateUids(config, certificatesPanel.getSignCertificateUids());

		fw.setConfig(config);
	}

	private Button createCheckboxButton(String label, Composite parent) {
		Button resultButton = new Button(parent, SWT.CHECK);
		GridData grid = new GridData(GridData.FILL_HORIZONTAL);
		resultButton.setText(label);
		resultButton.setLayoutData(grid);
		resultButton.setSelection(false);
		return resultButton;
	}

	private Text createText(int horizSpan, Composite parent) {
		Text resultText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData grid = new GridData(GridData.FILL_HORIZONTAL);

		grid.horizontalSpan = horizSpan;
		resultText.setLayoutData(grid);

		return resultText;
	}

	public static List obtainDeviceTypeProviders(DeviceTypeProviderValidator validator) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint = registry
					.getExtensionPoint("org.tigris.mtoolkit.osgimanagement.osgiDeviceTypes");

		return obtainProviders(extensionPoint.getConfigurationElements(), validator);
	}

	private static List obtainProviders(IConfigurationElement[] elements, DeviceTypeProviderValidator validator) {
		List providers = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (!element.getName().equals("osgiDeviceTypeProvider")) {
				FrameworkPlugin.getDefault().getLog().log(
						new Status(IStatus.WARNING, FrameworkPlugin.PLUGIN_ID, NLS.bind(
								"Unrecognized element in extension point: {0}", element.getName(), null)));
				continue;
			}
			try {
				DeviceTypeProviderElement providerElement = new DeviceTypeProviderElement(element, validator);
				providers.add(providerElement);
			} catch (CoreException e) {
				FrameworkPlugin.error("Exception while initializing device type providers", e);
			}
		}
		return providers;
	}

	public static class DeviceTypeProviderElement {
		private String typeId;
		private String deviceTypeName;
		private String clazz;
		private DeviceTypeProvider provider;
		private IConfigurationElement confElement;
		private CoreException initFailure;
		private Control panel;
		private DeviceTypeProviderValidator validator;

		public DeviceTypeProviderElement(IConfigurationElement configurationElement, DeviceTypeProviderValidator validator) throws CoreException {
			// TODO: Change to not throw exception, but rather display a an
			// error panel
			this.validator = validator;
			confElement = configurationElement;
			typeId = configurationElement.getAttribute("id");
			if (typeId == null)
				throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
						"Device type provider must specify 'id' attribute", null));
			deviceTypeName = configurationElement.getAttribute("name");
			if (deviceTypeName == null)
				throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
						"Device type provider must specify 'name' attribute", null));
			clazz = configurationElement.getAttribute("class");
			if (clazz == null)
				throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
						"Device type provider must specify 'class' attribute", null));
		}

		public String getTypeId() {
			return typeId;
		}

		public synchronized DeviceTypeProvider getProvider() throws CoreException {
			if (initFailure != null)
				throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
						"Device type provider failed to initialize", initFailure));
			if (provider == null) {
				try {
					provider = (DeviceTypeProvider) confElement.createExecutableExtension("class");
				} catch (Throwable t) {
					initFailure = new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
							"Failed to initialize device type provider", t));
					throw initFailure;
				}
			}
			return provider;
		}

		public String getDeviceTypeName() {
			return deviceTypeName;
		}

		public Control createPanel(Composite parent) {
			try {
				panel = getProvider().createPanel(parent, validator);
				if (panel != null)
					panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			} catch (CoreException e) {
				// TODO: Display the error directly in the UI
				FrameworkPlugin.error("Unable to create device type panel", e);
			}
			return panel;
		}

		public String validate() {
			try {
				return getProvider().validate();
			} catch (CoreException e) {
				FrameworkPlugin.error("Unable to validate dialog", e);
			}
			return "Internal error: unable to validate dialog";
		}
	}

	public void widgetDefaultSelected(SelectionEvent e) {
	}

	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == deviceTypeCombo) {
			int selectedIdx = deviceTypeCombo.getSelectionIndex();
			if (selectedIdx != -1) {
				DeviceTypeProviderElement newProvider = (DeviceTypeProviderElement) deviceTypesProviders.get(selectedIdx);
				if (newProvider != selectedProvider) {
					selectedProvider = newProvider;
					showDeviceTypePanel(selectedProvider);
				}
			}
		}
	}

	private String validate() {
		return selectedProvider.validate();
	}

	protected void okPressed() {
		boolean correct = checkFrameworkInfo();
		if (correct) {
			setFWSettings();
			if (connectButton != null && !connectButton.isDisposed()
					&& connectButton.getSelection()) {
				FrameworkConnectorFactory.connectFrameWork(fw);
			}
			FrameworkPlugin.getDefault().getPreferenceStore().setValue(DEFAULT_DEVICE_TYPE, selectedProvider.getTypeId());
			super.okPressed();
		}
	}
	
	private void init() {
		IMemento config = fw.getConfig();
		String providerId = config.getString(TRANSPORT_PROVIDER_ID);
		boolean providerFound = false;
		if (providerId != null) {
			for (Iterator it = deviceTypesProviders.iterator(); it.hasNext();) {
				DeviceTypeProviderElement provider = (DeviceTypeProviderElement) it.next();
				if (providerId.equals(provider.getTypeId())) {
					selectType(provider);
					providerFound = true;
					break;
				}
			}
		}
		String name = fw.getConfig().getString(FRAMEWORK_NAME);
		if (name != null) {
			textServer.setText(name);
		}

		if (providerId != null && !providerFound) {
			warningProviderNotFound();
		}

		try {
			selectedProvider.getProvider().setProperties(config);
		} catch (CoreException e) {
			FrameworkPlugin.error("Failed to initialize device type provider", e);
		}

		if (connectButton != null) {
			Boolean connect = fw.getConfig().getBoolean(CONNECT_TO_FRAMEWORK);
			if (connect != null) {
				connectButton.setSelection(connect.booleanValue());
			}
		}

		// Signing Certificates
		certificatesPanel.initialize(fw.getSignCertificateUids(config));
		
		if (fw.isConnected() && fw.autoConnected) {
			try {
				selectedProvider.getProvider().setEditable(false);
			} catch (CoreException e) {
				FrameworkPlugin.error("Failed to switch device type provider to read-only mode", e);
			}
		}
	}

	private static void warningProviderNotFound() {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		MessageDialog.openWarning(shell, "Warning", "The selected connection type provider is no more " +
				"available. Default connection type will be set.");
	}

	// Called when target options are changed
	public void setFWSettings() {
		saveConfig(fw.getConfig());
		fw.setName(fw.getConfig().getString(FRAMEWORK_NAME));

		if (addFramework) {
			parent.addElement(fw);
			addFramework = false;
		} else {
			DeviceConnector connector = fw.getConnector();
			if (connector != null) {
				connector.getProperties().put("framework-name", fw.getName()); //$NON-NLS-1$
				// String prevIP = (String)
				// connector.getProperties().get(DeviceConnector.KEY_DEVICE_IP);
				//				connector.getProperties().put("framework-connection-ip", target.getNewIP()); //$NON-NLS-1$
				// if (fw.isConnected() && !target.getNewIP().equals(prevIP)) {
				// MessageDialog.openInformation(target.getShell(),
				// Messages.framework_ip_changed_title,
				// Messages.framework_ip_changed_message);
				// }
			}
			fw.updateElement();
			parentView.setSelection(parentView.getSelection());
		}
	}

	// Check for duplicate
	private boolean checkFrameworkInfo() {
		String newName = textServer.getText().trim();
		if (newName.equals("")) { //$NON-NLS-1$
			setErrorMessage(Messages.incorrect_framework_name_message);
			return false;
		}

		Model[] frameworks = parent.getChildren();
		for (int i = 0; i < frameworks.length; i++) {
			if (newName.equals(frameworks[i].getName()) && !frameworks[i].equals(fw)) {
				setErrorMessage(Messages.duplicate_framework_name_message);
				return false;
			}
		}

		String result = validate();
		setErrorMessage(result);
		return result == null;
	}
	
	public void setErrorMessage(String newErrorMessage) {
		super.setErrorMessage(newErrorMessage);
		Button ok = getButton(OK);
		if (ok != null) {
			ok.setEnabled(newErrorMessage == null);
		}
	}

	public void setValidState(String error) {
		setErrorMessage(error);
	}

}

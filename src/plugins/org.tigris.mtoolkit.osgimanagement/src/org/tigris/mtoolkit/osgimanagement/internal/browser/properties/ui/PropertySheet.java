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
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.PageBook;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProvider;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.logic.PropertySheetLogic;

public class PropertySheet extends Window implements ControlListener, ConstantsDistributor, SelectionListener {

	private PropertySheetLogic logic;

	private Text textServer;

	public Button connectButton;
	public Button okButton;
	public Button cancelButton;

	private Composite bottomButtonsHolder;
	private FrameWork fw;

	private static List deviceTypesProviders;

	private Combo deviceTypeCombo;

	private DeviceTypeProviderElement selectedProvider;

	private PageBook pageBook;

	private Composite mainContent;

	// Constructor
	public PropertySheet(TreeViewer parentView, Model parent, FrameWork element, boolean firstTime) {
		super(parentView.getControl().getShell());
		logic = new PropertySheetLogic(parentView, parent, element, firstTime, this);
		this.setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
		fw = element;
	}

	// Create page contents
	protected Control createContents(Composite parent) {

		parent.getShell().setText(Messages.framework_properties_title);
		getShell().addControlListener(this);

		mainContent = new Composite(parent, SWT.NONE);
		mainContent.setLayout(new GridLayout());
		GridData mainGD = new GridData(GridData.FILL_BOTH);
		mainGD.minimumWidth = 300;
		mainContent.setLayoutData(mainGD);

		Group deviceGroup = new Group(mainContent, SWT.NONE);
		deviceGroup.setText(Messages.framework_name_label);
		deviceGroup.setLayout(new GridLayout());
		deviceGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		textServer = createText(1, deviceGroup);
		
		createDeviceTypeCombo(mainContent);
		obtainDeviceTypeProviders();

		// Connect properties group
		Group connectPropertiesGroup = new Group(mainContent, SWT.NONE);
		connectPropertiesGroup.setText(Messages.connect_properties_group_label);
		connectPropertiesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		connectPropertiesGroup.setLayout(new GridLayout());
		
		pageBook = new PageBook(connectPropertiesGroup, SWT.NONE);
		pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		
		deviceTypeCombo.select(0);
		selectedProvider = (DeviceTypeProviderElement) deviceTypesProviders.get(0);
		showDeviceTypePanel(selectedProvider);

		connectButton = createCheckboxButton(Messages.connect_button_label, mainContent);
		connectButton.setEnabled(!fw.isConnected());

		// Bottom buttons group
		bottomButtonsHolder = new Composite(mainContent, SWT.NONE);
		bottomButtonsHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout bottomButtonsGrid = new GridLayout();
		bottomButtonsGrid.numColumns = 4;
		bottomButtonsHolder.setLayout(bottomButtonsGrid);

		okButton = createButton(Messages.ok_button_label, bottomButtonsHolder);
		cancelButton = createButton(Messages.cancel_button_label, bottomButtonsHolder);
		getShell().setDefaultButton(okButton);

		logic.sheetLoaded();

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

		obtainDeviceTypeProviders();
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

	// Initialize ui values from storage
	public void initValues(IMemento config) {
		logic.setValue(textServer, FRAMEWORK_NAME);
		try {
			selectedProvider.getProvider().setProperties(config);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (logic.setValue(connectButton, CONNECT_TO_FRAMEWORK)) {
			// connectButton.setVisible(false);
			Composite parent = connectButton.getParent();
			Control[] children = parent.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i].equals(connectButton)) {
					children[i].dispose();
					break;
					// children[i] = null;
				}
			}
			parent.pack();
		}
	}

	// Save ui values to storage and update target element
	public void saveValues(IMemento config) {
		config.putString(FRAMEWORK_NAME, textServer.getText());
		try {
			selectedProvider.getProvider().save(config);
			config.putString(TRANSPORT_PROVIDER_ID, selectedProvider.getTypeId());
		} catch (CoreException e) {
			e.printStackTrace();
		}
		
		if (!connectButton.isDisposed())
			config.putBoolean(CONNECT_TO_FRAMEWORK, connectButton.getSelection());
		fw.setConfig(config);
	}

	// Get currently entered server name
	public String getNewName() {
		return textServer.getText();
	}

	private Button createCheckboxButton(String label, Composite parent) {
		Button resultButton = new Button(parent, SWT.CHECK);
		GridData grid = new GridData(GridData.FILL_HORIZONTAL);

		resultButton.setText(label);
		resultButton.setLayoutData(grid);
		resultButton.addSelectionListener(logic);
		resultButton.setSelection(false);

		return resultButton;
	}

	// Create Button
	private Button createButton(String label, Composite parent) {
		Button resultButton = new Button(parent, SWT.PUSH);
		GridData grid = new GridData(GridData.FILL_HORIZONTAL);

		resultButton.setText(label);
		resultButton.setLayoutData(grid);
		resultButton.addSelectionListener(logic);

		return resultButton;
	}

	private Text createText(int horizSpan, Composite parent) {
		Text resultText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData grid = new GridData(GridData.FILL_HORIZONTAL);

		grid.horizontalSpan = horizSpan;
		resultText.setLayoutData(grid);

		return resultText;
	}

	// Override to give the window correct size
	protected Point getInitialSize() {
		Point preferedSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		return (preferedSize);
	}

	public void controlMoved(ControlEvent e) {
		// do nothing
	}

	public void controlResized(ControlEvent e) {
		bottomButtonsHolder.layout();
	}
	
	public static List obtainDeviceTypeProviders() {
		if (deviceTypesProviders == null) {
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IExtensionPoint extensionPoint = registry.getExtensionPoint("org.tigris.mtoolkit.osgimanagement.osgiDeviceTypes");

			deviceTypesProviders = obtainProviders(extensionPoint.getConfigurationElements());
		}
		return deviceTypesProviders;
	}

	private static List obtainProviders(IConfigurationElement[] elements) {
		List providers = new ArrayList();
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement element = elements[i];
			if (!element.getName().equals("osgiDeviceTypeProvider")) {
				FrameworkPlugin.getDefault().getLog().log(new Status(IStatus.WARNING, FrameworkPlugin.PLUGIN_ID, NLS.bind("Unrecognized element in extension point: {0}", element.getName(), null)));
				continue;
			}
			try {
				DeviceTypeProviderElement providerElement = new DeviceTypeProviderElement(element);
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

		public DeviceTypeProviderElement(IConfigurationElement configurationElement) throws CoreException {
			// TODO: Change to not throw exception, but rather display a an error panel
			confElement = configurationElement;
			typeId = configurationElement.getAttribute("id");
			if (typeId == null)
				throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Device type provider must specify 'id' attribute", null));
			deviceTypeName = configurationElement.getAttribute("name");
			if (deviceTypeName == null)
				throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Device type provider must specify 'name' attribute", null));
			clazz = configurationElement.getAttribute("class");
			if (clazz == null)
				throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Device type provider must specify 'class' attribute", null));
		}

		public String getTypeId() {
			return typeId;
		}

		public synchronized DeviceTypeProvider getProvider() throws CoreException {
			if (initFailure != null)
				throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Device type provider failed to initialize", initFailure));
			if (provider == null) {
				try {
					provider = (DeviceTypeProvider) confElement.createExecutableExtension("class");
				} catch (Throwable t) {
					initFailure = new CoreException(
							new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, "Failed to initialize device type provider", t));
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
					panel = getProvider().createPanel(parent);
					if (panel != null)
						panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
				} catch (CoreException e) {
					// TODO: Display the error directly in the UI
					FrameworkPlugin.error("Unable to create device type panel", e);
				}
			return panel;
		}

		public boolean validate() {
			return provider.validate();
		}
	}


	public void widgetDefaultSelected(SelectionEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() == deviceTypeCombo) {
			int selectedIdx = deviceTypeCombo.getSelectionIndex();
			if (selectedIdx != -1) {
				selectedProvider = (DeviceTypeProviderElement) deviceTypesProviders.get(selectedIdx);
				showDeviceTypePanel(selectedProvider);
			}
		}
	}

	public boolean validate() {
		return selectedProvider.validate();
	}

	public void setEditable(boolean editable) {
		try {
			selectedProvider.getProvider().setEditable(editable);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

}
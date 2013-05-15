/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
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
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.PageBook;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProvider;
import org.tigris.mtoolkit.osgimanagement.DeviceTypeProviderValidator;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class FrameworkPanel implements ConstantsDistributor, SelectionListener, DeviceTypeProviderValidator {

  private static final String       DEFAULT_DEVICE_TYPE = "DEFAULT_DEVICE_TYPE";

  private Composite                 composite;

  private Text                      textServer;
  private Combo                     deviceTypeCombo;
  private PageBook                  pageBook;

  private List                      deviceTypesProviders;
  private DeviceTypeProviderElement selectedProvider;

  private FrameworkImpl             fw;
  private Model                     parent;
  private ErrorMonitor              errorMonitor;
  private IMemento                  defaultConfig;

  public FrameworkPanel(Composite composite, FrameworkImpl fw, Model parent) {
    this(composite, fw, parent, GridData.FILL_BOTH);
  }

  public FrameworkPanel(Composite composite, FrameworkImpl fw, Model parent, int style) {
    this.composite = composite;
    this.fw = fw;
    this.parent = parent;

    Group deviceGroup = new Group(composite, SWT.NONE);
    deviceGroup.setText(Messages.framework_name_label);
    deviceGroup.setLayout(new GridLayout());
    deviceGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    textServer = createText(1, deviceGroup);
    textServer.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        validate();
      }
    });

    deviceTypesProviders = obtainDeviceTypeProviders(this);

    createDeviceTypeCombo(composite);

    // Connect properties group
    Group connectPropertiesGroup = new Group(composite, SWT.NONE);
    connectPropertiesGroup.setText(Messages.connect_properties_group_label);
    connectPropertiesGroup.setLayoutData(new GridData(style));
    connectPropertiesGroup.setLayout(new GridLayout());

    pageBook = new PageBook(connectPropertiesGroup, SWT.NONE);
    pageBook.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

    String type = FrameworkPlugin.getDefault().getPreferenceStore().getString(DEFAULT_DEVICE_TYPE);
    if ("".equals(type)) {
      type = "plainSocket";
    }

    int index = 0;
    for (int i = 0; i < deviceTypesProviders.size(); i++) {
      DeviceTypeProviderElement provider = (DeviceTypeProviderElement) deviceTypesProviders.get(i);
      if (type.equals(provider.getTypeId())) {
        index = i;
        break;
      }
    }
    deviceTypeCombo.select(index);

    selectedProvider = (DeviceTypeProviderElement) deviceTypesProviders.get(index);
    showDeviceTypePanel(selectedProvider);

    DeviceConnector connector = fw.getConnector();
    if (connector != null) {
      String transportType = (String) connector.getProperties().get(DeviceConnector.TRANSPORT_TYPE);
      if (transportType != null) {
        List providers = FrameworkPanel.obtainDeviceTypeProviders(null);
        for (int i = 0; i < providers.size(); i++) {
          DeviceTypeProviderElement provider = (DeviceTypeProviderElement) providers.get(i);
          try {
            String pTransportType = provider.getProvider().getTransportType();
            if (transportType.equals(pTransportType)) {
              String id = provider.getTypeId();
              fw.getConfig().putString(TRANSPORT_PROVIDER_ID, id);
              break;
            }
          } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
      }
    }
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

    if (selectedProvider != null) {
      selectType(selectedProvider);
    }
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
    composite.layout(true);
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
    IExtensionPoint extensionPoint = registry.getExtensionPoint("org.tigris.mtoolkit.osgimanagement.osgiDeviceTypes");

    return obtainProviders(extensionPoint.getConfigurationElements(), validator);
  }

  private static List obtainProviders(IConfigurationElement[] elements, DeviceTypeProviderValidator validator) {
    List providers = new ArrayList();
    for (int i = 0; i < elements.length; i++) {
      IConfigurationElement element = elements[i];
      if (!element.getName().equals("osgiDeviceTypeProvider")) {
        FrameworkPlugin
            .getDefault()
            .getLog()
            .log(
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
    private String                      typeId;
    private String                      deviceTypeName;
    private String                      clazz;
    private DeviceTypeProvider          provider;
    private IConfigurationElement       confElement;
    private CoreException               initFailure;
    private Control                     panel;
    private DeviceTypeProviderValidator validator;
    private IMemento                    props;

    public DeviceTypeProviderElement(IConfigurationElement configurationElement, DeviceTypeProviderValidator validator) throws CoreException {
      // TODO: Change to not throw exception, but rather display a an
      // error panel
      this.validator = validator;
      confElement = configurationElement;
      typeId = configurationElement.getAttribute("id");
      if (typeId == null) {
        throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
            "Device type provider must specify 'id' attribute", null));
      }
      deviceTypeName = configurationElement.getAttribute("name");
      if (deviceTypeName == null) {
        throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
            "Device type provider must specify 'name' attribute", null));
      }
      clazz = configurationElement.getAttribute("class");
      if (clazz == null) {
        throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
            "Device type provider must specify 'class' attribute", null));
      }
    }

    public String getTypeId() {
      return typeId;
    }

    public synchronized DeviceTypeProvider getProvider() throws CoreException {
      if (initFailure != null) {
        throw new CoreException(new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID,
            "Device type provider failed to initialize", initFailure));
      }
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
        if (panel != null) {
          panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        }
        if (props != null) {
          getProvider().setProperties(props);
        }
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
          // remember settings of the old provider
          try {
            if (selectedProvider.props == null) {
              selectedProvider.props = cloneMemento(fw.getConfig());
            }
            if (selectedProvider.getProvider().validate() == null) {
              selectedProvider.getProvider().save(selectedProvider.props);
            } else {
              selectedProvider.props = cloneMemento(defaultConfig);
            }
          } catch (CoreException ex) {
            FrameworkPlugin.error("Failed to initialize device type provider", ex);
          }
          if (newProvider.props == null) {
            newProvider.props = cloneMemento(defaultConfig);
          }
          // switching to the new provider
          selectedProvider = newProvider;
          showDeviceTypePanel(selectedProvider);
        }
      }
    }
  }

  public void initialize(IMemento config) {
    defaultConfig = cloneMemento(config);
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
    String name = config.getString(FRAMEWORK_NAME);
    if (name != null) {
      textServer.setText(name);
    }

    if (providerId != null && !providerFound) {
      warningProviderNotFound();
    }
    try {
      selectedProvider.props = cloneMemento(config);
      selectedProvider.getProvider().setProperties(selectedProvider.props);
    } catch (CoreException e) {
      FrameworkPlugin.error("Failed to initialize device type provider", e);
    }

    if (fw.isAutoConnected()) {
      deviceTypeCombo.setEnabled(false);
      try {
        selectedProvider.getProvider().setEditable(false);
      } catch (CoreException e) {
        FrameworkPlugin.error("Failed to switch device type provider to read-only mode", e);
      }
    }
  }

  private static void warningProviderNotFound() {
    Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    MessageDialog.openWarning(shell, "Warning", "The selected connection type provider is no more "
        + "available. Default connection type will be set.");
  }

  /**
   * Returns true if content is valid.
   *
   * @return
   */
  public boolean validate() {
    String newName = textServer.getText().trim();
    if (newName.equals("")) { //$NON-NLS-1$
      setValidState(Messages.incorrect_framework_name_message);
      return false;
    }

    if (parent != null) {
      Model[] frameworks = parent.getChildren();
      for (int i = 0; i < frameworks.length; i++) {
        if (newName.equals(frameworks[i].getName()) && !frameworks[i].equals(fw)) {
          setValidState(Messages.duplicate_framework_name_message);
          return false;
        }
      }
    }

    String providerErr = selectedProvider.validate();
    if (providerErr != null) {
      setValidState(providerErr);
      return false;
    }

    setValidState(null);
    return true;
  }

  public void setValidState(String error) {
    if (errorMonitor != null) {
      if (!composite.isDisposed()) {
        errorMonitor.setErrorMessage(error);
      }
    }
  }

  /**
   * Save ui values to provided storage.
   *
   * @param config
   * @return true if connection properties have changed
   */
  public boolean save(IMemento config) {
    config.putString(FRAMEWORK_NAME, textServer.getText());
    boolean connChanged = false;
    try {
      IMemento initialConfig = cloneMemento(config);
      if (selectedProvider.getProvider().validate() != null) {
        config = cloneMemento(defaultConfig);
      }
      selectedProvider.getProvider().save(config);
      config.putString(TRANSPORT_PROVIDER_ID, selectedProvider.getTypeId());
      connChanged = !mementoEquals(initialConfig, config);
    } catch (CoreException e) {
      e.printStackTrace();
    }
    FrameworkPlugin.getDefault().getPreferenceStore().setValue(DEFAULT_DEVICE_TYPE, selectedProvider.getTypeId());
    return connChanged;
  }

  private static boolean mementoEquals(IMemento m1, IMemento m2) {
    if (m1 == null || m2 == null) {
      return m1 != m2;
    }
    String keys[] = m1.getAttributeKeys();
    if (keys.length != m2.getAttributeKeys().length) {
      return false;
    }
    for (int i = 0; i < keys.length; i++) {
      String val = m1.getString(keys[i]);
      if (!val.equals(m2.getString(keys[i]))) {
        return false;
      }
    }
    // Note: we can't compare children because we can't get them (if we
    // don't know their types).
    return true;
  }

  private static IMemento cloneMemento(IMemento memento) {
    IMemento result = XMLMemento.createWriteRoot("temp");
    result.putMemento(memento);
    return result;
  }

  public void setErrorMonitor(ErrorMonitor errorMonitor) {
    this.errorMonitor = errorMonitor;
  }

  public interface ErrorMonitor {
    public void setErrorMessage(String error);
  }

}

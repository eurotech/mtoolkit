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
package org.tigris.mtoolkit.osgimanagement.internal.preferences;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.Preferences;
import org.tigris.mtoolkit.common.preferences.IMToolkitPreferencePage;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

// TODO: Change this class to change only the preferences, the settings holder
// should get the changes from the events
public class FrameworkPreferencesPage extends PreferencePage

implements IWorkbenchPreferencePage, IMToolkitPreferencePage {

	private Button enableAutoConnectButton;
	private Button enableInfoLogButton;
	private Button enableAutoStartButton;
	private Button enableBundleCategoriesButton;

	// TODO: Move these defaults to PreferencesInitializer, which is more
	// suitable for them
	public static final boolean autoConnectDefault = true;
	public static final boolean infoLogDefault = true;
	public static final boolean autoStartAfterInstall = true;
	public static final boolean showBundleCategories = true;

	public Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		setPreferenceStore(FrameworkPlugin.getDefault().getPreferenceStore());
		loadSettings();

		enableAutoConnectButton = new Button(composite, SWT.CHECK);
		enableAutoConnectButton.setText(Messages.enable_frameworks_autoconnect);
		enableAutoConnectButton.setSelection(FrameworkConnectorFactory.isAutoConnectEnabled);

		enableInfoLogButton = new Button(composite, SWT.CHECK);
		enableInfoLogButton.setText(Messages.enable_info_log);
		enableInfoLogButton.setSelection(BrowserErrorHandler.isInfoLogEnabled);

		enableAutoStartButton = new Button(composite, SWT.CHECK);
		enableAutoStartButton.setText(Messages.autostart_bundles_on_install);
		enableAutoStartButton.setSelection(FrameworkConnectorFactory.isAutoStartBundlesEnabled);

		enableBundleCategoriesButton = new Button(composite, SWT.CHECK);
		enableBundleCategoriesButton.setText(Messages.show_bundle_categories);
		enableBundleCategoriesButton.setSelection(FrameworkConnectorFactory.isBundlesCategoriesShown);

		return composite;
	}

	public void init(IWorkbench workbench) {
	}

	public boolean isAutoConnectEnabled() {
		return !enableAutoConnectButton.getSelection();
	}

	public void performDefaults() {
		super.performDefaults();
		enableAutoConnectButton.setSelection(autoConnectDefault);
		enableInfoLogButton.setSelection(infoLogDefault);
		enableAutoStartButton.setSelection(autoStartAfterInstall);
		enableBundleCategoriesButton.setSelection(showBundleCategories);
	}

	public boolean performOk() {
		// TODO: This should change the preferences, no the settings holder
		FrameworkConnectorFactory.isAutoConnectEnabled = enableAutoConnectButton.getSelection();
		FrameworkConnectorFactory.isAutoStartBundlesEnabled = enableAutoStartButton.getSelection();
		BrowserErrorHandler.isInfoLogEnabled = enableInfoLogButton.getSelection();

		boolean shouldUpdateFrameworks = false;
		if (FrameworkConnectorFactory.isBundlesCategoriesShown != enableBundleCategoriesButton.getSelection())
			shouldUpdateFrameworks = true;

		FrameworkConnectorFactory.isBundlesCategoriesShown = enableBundleCategoriesButton.getSelection();

		if (shouldUpdateFrameworks) {
			HashMap existingFrameworks = FrameWorkView.getTreeRoot().getFrameWorkMap();
			Collection frameworks = existingFrameworks.values();
			Iterator iterator = frameworks.iterator();

			while (iterator.hasNext()) {
				Object framework = iterator.next();
				if (((FrameworkImpl) framework).isConnected())
					((FrameworkImpl) framework).refreshAction();
			}
		}

		saveSettings();
		return true;
	}

	public boolean performCancel() {
		return true;
	}

	public Preferences getPrefs() {
		IPreferencesService service = Platform.getPreferencesService();
		return service.getRootNode().node("org.tigris.mtoolkit.osgimanagement.internal"); //$NON-NLS-1$
	}

	private void saveSettings() {
		FrameworkPlugin framework = FrameworkPlugin.getDefault();
		if (framework != null) {
			framework.getPreferenceStore().setValue(ConstantsDistributor.MEMENTO_AUTOCONNECT,
				FrameworkConnectorFactory.isAutoConnectEnabled);
			framework.getPreferenceStore().setValue(ConstantsDistributor.MEMENTO_AUTOSTART_AFTER_INSTALL,
				FrameworkConnectorFactory.isAutoStartBundlesEnabled);
			framework.getPreferenceStore().setValue(ConstantsDistributor.MEMENTO_INFO_LOG,
				BrowserErrorHandler.isInfoLogEnabled);
			framework.getPreferenceStore().setValue(ConstantsDistributor.MEMENTO_SHOW_BUNDLE_CATEGORY,
				FrameworkConnectorFactory.isBundlesCategoriesShown);
		}
	}

	private void loadSettings() {
		FrameworkConnectorFactory.isAutoConnectEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_AUTOCONNECT);
		FrameworkConnectorFactory.isAutoStartBundlesEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_AUTOSTART_AFTER_INSTALL);
		BrowserErrorHandler.isInfoLogEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_INFO_LOG);
		FrameworkConnectorFactory.isBundlesCategoriesShown = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_SHOW_BUNDLE_CATEGORY);
	}

}

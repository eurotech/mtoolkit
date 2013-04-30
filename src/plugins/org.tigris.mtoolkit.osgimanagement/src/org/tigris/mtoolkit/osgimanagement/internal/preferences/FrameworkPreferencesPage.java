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

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.tigris.mtoolkit.common.preferences.IMToolkitPreferencePage;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworksView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

public final class FrameworkPreferencesPage extends PreferencePage implements IWorkbenchPreferencePage,
		IMToolkitPreferencePage {
	private static final String MEMENTO_AUTOCONNECT = "framework_autoconnect"; //$NON-NLS-1$
	private static final String MEMENTO_INFO_LOG = "info_log"; //$NON-NLS-1$
	private static final String MEMENTO_AUTOSTART_AFTER_INSTALL = "autostart_after_install"; //$NON-NLS-1$
	private static final String MEMENTO_USE_ACTIVATION_POLICY = "use_activation_policy"; //$NON-NLS-1$
	private static final String MEMENTO_SHOW_BUNDLE_CATEGORY = "show_bundle_categories"; //$NON-NLS-1$
	private static final String MEMENTO_SHOW_SKIPPED_SYSTEM_BUNDLES = "show_skipped_system_bundles"; //$NON-NLS-1$
	private static final String MEMENTO_AUTO_UPDATE_BUNDLES_ON_INSTALL = "auto_update_bundles_on_install";//$NON-NLS-1$

	private static final boolean AUTO_CONNECT_DEFAULT = true;
	private static final boolean LOG_INFO_DEFAULT = false;
	private static final boolean AUTO_START_AFTER_INSTALL_DEFAULT = true;
	private static final boolean USE_ACTIVATION_POLICY_DEFAULT = true;
	private static final boolean SHOW_BUNDLE_CATEGORIES_DEFAULT = true;
	private static final boolean SHOW_SKIPPED_SYSTEM_BUNDLES_DEFAULT = true;
	private static final boolean AUTO_UPDATE_BUNDLES_DEFAULT = false;

	private Button enableAutoConnectButton;
	private Button enableInfoLogButton;
	private Button enableAutoStartButton;
	private Button enableActivationPolicyButton;
	private Button enableBundleCategoriesButton;
	private Button showSkippedSystemBundlesButton;
	private Button autoUpdateBundlesOnInstallButton;

	private IPreferenceStore store;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse
	 * .swt.widgets.Composite)
	 */
	@Override
	public Control createContents(Composite parent) {
		store = FrameworkPlugin.getDefault().getPreferenceStore();

		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		enableAutoConnectButton = new Button(composite, SWT.CHECK);
		enableAutoConnectButton.setText(Messages.enable_frameworks_autoconnect);
		enableAutoConnectButton.setSelection(isAutoConnectEnabled());

		enableInfoLogButton = new Button(composite, SWT.CHECK);
		enableInfoLogButton.setText(Messages.enable_info_log);
		enableInfoLogButton.setSelection(isLogInfoEnabled());

		enableAutoStartButton = new Button(composite, SWT.CHECK);
		enableAutoStartButton.setText(Messages.autostart_bundles_on_install);
		enableAutoStartButton.setSelection(isAutoStartBundlesEnabled());

		enableActivationPolicyButton = new Button(composite, SWT.CHECK);
		enableActivationPolicyButton.setText(Messages.use_activation_policy);
		enableActivationPolicyButton.setSelection(isActivationPolicyEnabled());
		enableActivationPolicyButton.setToolTipText(Messages.use_activation_policy_tooltip);

		enableBundleCategoriesButton = new Button(composite, SWT.CHECK);
		enableBundleCategoriesButton.setText(Messages.show_bundle_categories);
		enableBundleCategoriesButton.setSelection(isBundlesCategoriesShown());

		showSkippedSystemBundlesButton = new Button(composite, SWT.CHECK);
		showSkippedSystemBundlesButton.setText(Messages.show_skipped_system_bundles);
		showSkippedSystemBundlesButton.setSelection(isShowSkippedSystemBundles());
		showSkippedSystemBundlesButton.setToolTipText(Messages.show_skipped_system_bundles_tooltip);

		autoUpdateBundlesOnInstallButton = new Button(composite, SWT.CHECK);
		autoUpdateBundlesOnInstallButton.setText(Messages.auto_update_bundles_on_install);
		autoUpdateBundlesOnInstallButton.setSelection(isAutoUpdateBundlesOnInstallEnabled());
		autoUpdateBundlesOnInstallButton.setToolTipText(Messages.auto_update_bundles_on_install_tooltip);

		return composite;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
	 */
	@Override
	public void performDefaults() {
		super.performDefaults();
		enableAutoConnectButton.setSelection(AUTO_CONNECT_DEFAULT);
		enableInfoLogButton.setSelection(LOG_INFO_DEFAULT);
		enableAutoStartButton.setSelection(AUTO_START_AFTER_INSTALL_DEFAULT);
		enableActivationPolicyButton.setSelection(USE_ACTIVATION_POLICY_DEFAULT);
		enableBundleCategoriesButton.setSelection(SHOW_BUNDLE_CATEGORIES_DEFAULT);
		showSkippedSystemBundlesButton.setSelection(SHOW_SKIPPED_SYSTEM_BUNDLES_DEFAULT);
		autoUpdateBundlesOnInstallButton.setSelection(AUTO_UPDATE_BUNDLES_DEFAULT);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	@Override
	public boolean performOk() {
		boolean showBundleCategories = isBundlesCategoriesShown();
		store.setValue(MEMENTO_AUTOCONNECT, enableAutoConnectButton.getSelection());
		store.setValue(MEMENTO_INFO_LOG, enableInfoLogButton.getSelection());
		store.setValue(MEMENTO_AUTOSTART_AFTER_INSTALL, enableAutoStartButton.getSelection());
		store.setValue(MEMENTO_USE_ACTIVATION_POLICY, enableActivationPolicyButton.getSelection());
		store.setValue(MEMENTO_SHOW_BUNDLE_CATEGORY, enableBundleCategoriesButton.getSelection());
		store.setValue(MEMENTO_SHOW_SKIPPED_SYSTEM_BUNDLES, showSkippedSystemBundlesButton.getSelection());
		store.setValue(MEMENTO_AUTO_UPDATE_BUNDLES_ON_INSTALL, autoUpdateBundlesOnInstallButton.getSelection());
		// FIXME This is NOT proper way for tracking preferences change
		if (showBundleCategories != enableBundleCategoriesButton.getSelection()) {
			HashMap existingFrameworks = FrameworksView.getTreeRoot().getFrameWorkMap();
			Collection frameworks = existingFrameworks.values();
			Iterator iterator = frameworks.iterator();

			while (iterator.hasNext()) {
				Object framework = iterator.next();
				if (((FrameworkImpl) framework).isConnected())
					((FrameworkImpl) framework).refreshAction();
			}
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	public static boolean isAutoConnectEnabled() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		return getBoolean(store, MEMENTO_AUTOCONNECT, AUTO_CONNECT_DEFAULT);
	}

	public static boolean isLogInfoEnabled() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		return getBoolean(store, MEMENTO_INFO_LOG, LOG_INFO_DEFAULT);
	}

	public static boolean isAutoStartBundlesEnabled() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		return getBoolean(store, MEMENTO_AUTOSTART_AFTER_INSTALL, AUTO_START_AFTER_INSTALL_DEFAULT);
	}

	public static boolean isActivationPolicyEnabled() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		return getBoolean(store, MEMENTO_USE_ACTIVATION_POLICY, USE_ACTIVATION_POLICY_DEFAULT);
	}

	public static boolean isBundlesCategoriesShown() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		return getBoolean(store, MEMENTO_SHOW_BUNDLE_CATEGORY, SHOW_BUNDLE_CATEGORIES_DEFAULT);
	}

	public static boolean isAutoUpdateBundlesOnInstallEnabled() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		return getBoolean(store, MEMENTO_AUTO_UPDATE_BUNDLES_ON_INSTALL, AUTO_UPDATE_BUNDLES_DEFAULT);
	}

	static void initDefaults() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		store.setDefault(MEMENTO_AUTOCONNECT, AUTO_CONNECT_DEFAULT);
		store.setDefault(MEMENTO_INFO_LOG, LOG_INFO_DEFAULT);
		store.setDefault(MEMENTO_AUTOSTART_AFTER_INSTALL, AUTO_START_AFTER_INSTALL_DEFAULT);
		store.setDefault(MEMENTO_USE_ACTIVATION_POLICY, USE_ACTIVATION_POLICY_DEFAULT);
		store.setDefault(MEMENTO_SHOW_BUNDLE_CATEGORY, SHOW_BUNDLE_CATEGORIES_DEFAULT);
		store.setDefault(MEMENTO_SHOW_SKIPPED_SYSTEM_BUNDLES, SHOW_SKIPPED_SYSTEM_BUNDLES_DEFAULT);
		store.setDefault(MEMENTO_AUTO_UPDATE_BUNDLES_ON_INSTALL, AUTO_UPDATE_BUNDLES_DEFAULT);
	}

	private static boolean isShowSkippedSystemBundles() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		return getBoolean(store, MEMENTO_SHOW_SKIPPED_SYSTEM_BUNDLES, SHOW_SKIPPED_SYSTEM_BUNDLES_DEFAULT);
	}

	private static boolean getBoolean(IPreferenceStore aStore, String aName, boolean aDefault) {
		boolean b = aStore.getDefaultBoolean(aName);
		if (b != aDefault) {
			aStore.setDefault(aName, aDefault);
		}
		return aStore.getBoolean(aName);
	}
}

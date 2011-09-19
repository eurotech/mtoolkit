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

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;

public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public void initializeDefaultPreferences() {
		IPreferenceStore store = FrameworkPlugin.getDefault().getPreferenceStore();
		store.setDefault(ConstantsDistributor.MEMENTO_AUTOCONNECT, FrameworkPreferencesPage.autoConnectDefault);
		store.setDefault(ConstantsDistributor.MEMENTO_INFO_LOG, FrameworkPreferencesPage.infoLogDefault);
		store.setDefault(ConstantsDistributor.MEMENTO_AUTOSTART_AFTER_INSTALL,
			FrameworkPreferencesPage.autoStartAfterInstall);
		store.setDefault(ConstantsDistributor.MEMENTO_USE_ACTIVATION_POLICY,
				FrameworkPreferencesPage.useActivationPolicy);
		store.setDefault(ConstantsDistributor.MEMENTO_SHOW_BUNDLE_CATEGORY,
				FrameworkPreferencesPage.showBundleCategories);
		store.setDefault(ConstantsDistributor.MEMENTO_SHOW_SKIPPED_SYSTEM_BUNDLES,
				FrameworkPreferencesPage.showSkippedSystemBundlesDefault);
		store.setDefault(ConstantsDistributor.MEMENTO_AUTO_UPDATE_BUNDLES_ON_INSTALL,
				FrameworkPreferencesPage.autoUpdateBundlesOnInstallDefault);
	}

}

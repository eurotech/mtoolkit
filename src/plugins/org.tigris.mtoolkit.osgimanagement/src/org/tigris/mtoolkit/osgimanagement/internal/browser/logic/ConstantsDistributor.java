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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

public interface ConstantsDistributor {

	static final String DEFAULT_IP = "127.0.0.1"; //$NON-NLS-1$

	static final int LIST_DIALOG_WIDTH = 350;
	static final int LIST_DIALOG_HEIGHT = 200;

	static final String FRAMEWORK_NAME = "framework_name_key"; //$NON-NLS-1$
	static final String TRANSPORT_PROVIDER_ID = "transport_type_key"; //$NON-NLS-1$
	
	static final String FRAMEWORK_SIGN_CERTIFICATE_ID = "framework_sign_certificate_uid"; //$NON-NLS-1$
	static final String CONNECT_TO_FRAMEWORK = "framework_connect_key";

	static final String MEMENTO_TYPE = "browser_model"; //$NON-NLS-1$
	static final String MEMENTO_ROOT_TYPE = "browser_root_model"; //$NON-NLS-1$

	static final String MEMENTO_AUTOCONNECT = "framework_autoconnect"; //$NON-NLS-1$
	static final String MEMENTO_INFO_LOG = "info_log"; //$NON-NLS-1$
	static final String MEMENTO_AUTOSTART_AFTER_INSTALL = "autostart_after_install"; //$NON-NLS-1$
	static final String MEMENTO_USE_ACTIVATION_POLICY = "use_activation_policy"; //$NON-NLS-1$
	static final String MEMENTO_SHOW_BUNDLE_CATEGORY = "show_bundle_categories"; //$NON-NLS-1$
	static final String MEMENTO_SHOW_SKIPPED_SYSTEM_BUNDLES = "show_skipped_system_bundles"; //$NON-NLS-1$

	static final String STORAGE_FILE_NAME = "ModelStorage.xml"; //$NON-NLS-1$

	static final String SERVER_ICON_DISCONNECTED = "servernc.gif"; //$NON-NLS-1$
	static final String SERVER_ICON_CONNECTED = "server.gif"; //$NON-NLS-1$

	static final String FRAMEWORK_STATUS_NAME = "status"; //$NON-NLS-1$
	static final String FRAMEWORK_CONNECT_VALUE = "connected"; //$NON-NLS-1$

	static final String BUNDLE_STATE_NAME = "state"; //$NON-NLS-1$
	static final String BUNDLE_UNINSTALLED_VALUE = "uninstalled"; //$NON-NLS-1$
	static final String BUNDLE_INSTALLED_VALUE = "installed"; //$NON-NLS-1$
	static final String BUNDLE_RESOLVED_VALUE = "resolved"; //$NON-NLS-1$
	static final String BUNDLE_STARTING_VALUE = "starting"; //$NON-NLS-1$
	static final String BUNDLE_STOPPING_VALUE = "stopping"; //$NON-NLS-1$
	static final String BUNDLE_ACTIVE_VALUE = "active"; //$NON-NLS-1$

	static final String NODE_NAME = "node_name"; //$NON-NLS-1$

	static final String INSTALL_TO_DLG_ICON = "selection_dlg.gif"; //$NON-NLS-1$
}
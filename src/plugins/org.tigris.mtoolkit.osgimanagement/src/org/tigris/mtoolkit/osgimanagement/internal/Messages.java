/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM - initial API and implementation
 *******************************************************************************/

package org.tigris.mtoolkit.osgimanagement.internal;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.tigris.mtoolkit.osgimanagement.internal.osgimanagement";//$NON-NLS-1$

	public static String Not_Connected;
	public static String Connected_To;
	public static String Remote_Console;
	public static String edit_action_N;
	public static String edit_action_mark_N;
	public static String edit_action_cut_N;
	public static String edit_action_copy_N;
	public static String edit_action_paste_N;
	public static String edit_action_delete_N;
	public static String clear_action_N;
	public static String file_action_saveas_N;
	public static String file_action_print_N;
	public static String ConsoleM_N;
	public static String ViewM_NoFrameworksMI_N;
	public static String add_action_label;

	public static String all_files_filter_label;
	public static String remove_action_label;
	public static String property_action_label;
	public static String connect_action_label;
	public static String disconnect_action_label;
	public static String install_action_label;
	public static String install_dp_action_label;
	public static String deinstall_action_label;
	public static String deinstall_dp_action_label;

	public static String deployment_package_filter_label;
	public static String start_action_label;
	public static String stop_action_label;
	public static String update_action_label;
	public static String service_properties_action_label;
	public static String goto_service_action_label;
	public static String bundles_node_label;
	public static String bundles_view_action_label;
	public static String services_view_action_label;
	public static String show_bundle_id_action_label;
	public static String show_bundle_version_action_label;
	public static String show_dp_properties_action_label;
	public static String show_bundle_properties_action_label;
	public static String find_action_label;
	public static String find_action_run_string;
	public static String in_frameworks_view;
	public static String find_in_frameworks_view;
	public static String refresh_framework_action_label;
	public static String refresh_bundle_action_label;
	public static String show_framework_console;
	public static String framework_properties_title;
	public static String service_properties_title;
	public static String dp_properties_title;
	public static String bundle_properties_title;
	public static String service_label;
	public static String headers_label;
	public static String install_dialog_title;
	public static String update_dialog_title;
	public static String install_dp_dialog_title;
	public static String install_confirm_title;
	public static String package_analyze_title;

	public static String plugin_exporter_not_compatible;
	public static String name_column_label;

	public static String new_framework_default_name;
	public static String root_element_name;
	public static String connect_properties_group_label;
	public static String framework_name_label;
	public static String framework_ip_label;
	public static String install_label;
	public static String update_label;
	public static String install_dp_label;
	public static String standard_error_title;
	public static String standard_info_title;
	public static String registered_services;
	public static String services_in_use;
	public static String registered_in;
	public static String used_by;
	public static String missing_version;
	public static String ok_button_label;

	public static String overwriting_file_confirmation_text;
	public static String cancel_button_label;
	public static String apply_button_label;
	public static String browse_button_label;
	public static String connect_button_label;
	public static String enable_frameworks_autoconnect;
	public static String enable_info_log;
	public static String autostart_bundles_on_install;
	public static String connect_error_message;
	public static String pmp_connect_error_message;
	public static String duplicate_framework_name_message;
	public static String incorrect_framework_name_message;
	public static String incorrect_framework_ip_message;
	public static String duplicate_bundle_confirm_message;
	public static String duplicate_dp_confirm_message;
	public static String file_not_exists;
	public static String no_reason_message;
	public static String no_exception_message;

	public static String no_plugin_exporter_available;
	public static String bundle_deinstalled_message;

	public static String bundle_filter_label;
	public static String bundle_installation_failure;

	public static String bundle_installed_message;
	public static String bundle_updated_message;
	public static String dp_deinstalled_message;
	public static String dp_installed_message;
	public static String dpackages_node_label;
	public static String uninstall_failed;
	public static String uninstall_forced;
	public static String cant_get_bundle_version;
	public static String framework_ip_changed_title;
	public static String framework_ip_changed_message;
	public static String missing_manifest;
	public static String find_button_label;
	public static String close_button_label;
	public static String find_text;
	public static String BundlesAction_ToolTip;
	public static String DPAction_ToolTip;
	public static String retrieve_framework_info;
	public static String retrieve_bundles_info;
	public static String retrieve_services_info;
	public static String retrieve_dps_info;
	public static String stop_bundle;
	public static String start_bundle;
	public static String update_bundle;
	public static String install_dp;
	public static String install_bundle;
	public static String uninstall_dp;
	public static String uninstall_bundle;

	public static String unknown_category_label;
	public static String refresh_framework_info;
	public static String connect_framework;
	public static String install_to_framework;
	public static String _1;
	public static String _8401;
	public static String _8404;
	public static String _8450;
	public static String _8451;
	public static String _8452;
	public static String _8453;
	public static String _8454;
	public static String _8455;
	public static String _8456;
	public static String _8457;
	public static String _8458;
	public static String _8460;
	public static String _8461;
	public static String _8462;
	public static String _8463;
	public static String _8464;
	public static String _8465;
	public static String _3001;
	public static String _3002;
	public static String _3003;
	public static String _3004;
	public static String _3005;
	public static String _3006;
	public static String _3007;
	public static String _3008;
	public static String _3009;
	public static String _4001;
	public static String _4002;
	public static String _4003;
	public static String _4004;
	public static String _4005;
	public static String _4006;
	public static String _4007;
	public static String _4008;
	public static String _4009;
	public static String _4010;
	public static String _4011;
	public static String _4012;
	public static String _4013;
	public static String _4014;
	public static String _5001;
	public static String _5003;
	public static String _5004;
	public static String _5005;
	public static String _5006;
	public static String _5007;
	public static String _6000;
	public static String _7000;

	public static String question_dialog_title;

	public static String text_files_filter_label;

	public static String Toolbar_Bundles_Label;

	public static String Toolbar_DP_Label;

	public static String value_column_label;
	
	public static String install_to_menu_label;
	public static String no_frameworks_connected_label;


	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	
	public static String get(String fieldName) {
		try {
			Field f = Messages.class.getField(fieldName);
			if (f != null)
				return (String) f.get(null);
			else
				return null;
		} catch (IllegalAccessException e) {
			return null;
		} catch (NoSuchFieldException e) {
			return null;
		}
	}
}
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

package org.tigris.mtoolkit.osgimanagement.internal;

import java.lang.reflect.Field;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.tigris.mtoolkit.osgimanagement.internal.osgimanagement"; //$NON-NLS-1$

  public static String        add_action_label;

  public static String        all_files_filter_label;
  public static String        remove_action_label;
  public static String        property_action_label;
  public static String        connect_action_label;
  public static String        disconnect_action_label;
  public static String        install_action_label;
  public static String        deinstall_action_label;

  public static String        start_action_label;
  public static String        stop_action_label;
  public static String        update_action_label;
  public static String        goto_service_action_label;
  public static String        bundles_node_label;
  public static String        bundles_view_action_label;
  public static String        services_view_action_label;
  public static String        show_bundle_id_action_label;
  public static String        show_bundle_version_action_label;
  public static String        find_action_label;
  public static String        refresh_action_label;
  public static String        show_framework_console;

  public static String        add_framework_title;
  public static String        framework_properties_title;
  public static String        service_properties_title;
  public static String        bundle_properties_title;
  public static String        connecting_operation_title;
  public static String        preparing_operation_title;
  public static String        confirm_replace_title;
  public static String        install_bundle_operation_title;
  public static String        start_bundle_operation_title;

  public static String        new_framework_default_name;
  public static String        root_element_name;
  public static String        connect_properties_group_label;
  public static String        framework_name_label;

  public static String        install_bundle_title;
  public static String        update_bundle_title;

  public static String        standard_error_title;
  public static String        standard_info_title;
  public static String        registered_services;
  public static String        services_in_use;
  public static String        registered_in;
  public static String        used_by;
  public static String        missing_version;

  public static String        connect_button_label;
  public static String        enable_frameworks_autoconnect;
  public static String        show_bundle_categories;
  public static String        show_skipped_system_bundles;
  public static String        auto_update_bundles_on_install;
  public static String        auto_update_bundles_on_install_tooltip;
  public static String        enable_info_log;
  public static String        autostart_bundles_on_install;
  public static String        use_activation_policy;
  public static String        pmp_connect_error_message;
  public static String        duplicate_framework_name_message;
  public static String        incorrect_framework_name_message;
  public static String        no_reason_message;
  public static String        no_exception_message;

  public static String        bundle_startup_failure;

  public static String        bundle_update_failure;

  public static String        bundle_filter_label;
  public static String        bundle_start_failure;

  public static String        bundle_stop_failure;

  public static String        cant_get_bundle_version;
  public static String        framework_ip_changed_title;
  public static String        framework_ip_changed_message;
  public static String        close_button_label;
  public static String        BundlesAction_ToolTip;
  public static String        retrieve_bundles_info;
  public static String        retrieve_services_info;
  public static String        stop_bundle;
  public static String        start_bundle;
  public static String        update_bundle;

  public static String        update_file_not_found;
  public static String        install_bundle;
  public static String        uninstall_bundle;

  public static String        rcp_bundle_missing_title;
  public static String        rcp_bundle_missing_message;
  public static String        get_missing_bundle_message;
  public static String        get_iagent_button_label;
  public static String        save_as_dialog_title;

  public static String        framework_not_instrumented;
  public static String        framework_not_instrumented_msg;

  public static String        unknown_category_label;
  public static String        refresh_framework_info;
  public static String        refresh_bundles_info;
  public static String        connect_framework;
  public static String        connection_failed;

  public static String        show_service_properties_in_tree;

  public static String        stop_system_bundle;

  public static String        operation_failed;

  public static String        use_activation_policy_tooltip;

  public static String        show_skipped_system_bundles_tooltip;

  public static String        error_file_already_exist;

  /**
   * These are messages which fit to IAgentException error codes used though
   * reflection.
   */
  public static String        _1;                                                                        // NO_UCD
  public static String        _8401;                                                                     // NO_UCD
  public static String        _8404;                                                                     // NO_UCD
  public static String        _8450;                                                                     // NO_UCD
  public static String        _8451;                                                                     // NO_UCD
  public static String        _8452;                                                                     // NO_UCD
  public static String        _8453;                                                                     // NO_UCD
  public static String        _8454;                                                                     // NO_UCD
  public static String        _8455;                                                                     // NO_UCD
  public static String        _8456;                                                                     // NO_UCD
  public static String        _8457;                                                                     // NO_UCD
  public static String        _8458;                                                                     // NO_UCD
  public static String        _8460;                                                                     // NO_UCD
  public static String        _8461;                                                                     // NO_UCD
  public static String        _8462;                                                                     // NO_UCD
  public static String        _8463;                                                                     // NO_UCD
  public static String        _8464;                                                                     // NO_UCD
  public static String        _8465;                                                                     // NO_UCD
  public static String        _3001;                                                                     // NO_UCD
  public static String        _3002;                                                                     // NO_UCD
  public static String        _3003;                                                                     // NO_UCD
  public static String        _3004;                                                                     // NO_UCD
  public static String        _3005;                                                                     // NO_UCD
  public static String        _3006;                                                                     // NO_UCD
  public static String        _3007;                                                                     // NO_UCD
  public static String        _3008;                                                                     // NO_UCD
  public static String        _3009;                                                                     // NO_UCD
  public static String        _4001;                                                                     // NO_UCD
  public static String        _4002;                                                                     // NO_UCD
  public static String        _4003;                                                                     // NO_UCD
  public static String        _4004;                                                                     // NO_UCD
  public static String        _4005;                                                                     // NO_UCD
  public static String        _4006;                                                                     // NO_UCD
  public static String        _4007;                                                                     // NO_UCD
  public static String        _4008;                                                                     // NO_UCD
  public static String        _4009;                                                                     // NO_UCD
  public static String        _4010;                                                                     // NO_UCD
  public static String        _4011;                                                                     // NO_UCD
  public static String        _4012;                                                                     // NO_UCD
  public static String        _4013;                                                                     // NO_UCD
  public static String        _4014;                                                                     // NO_UCD
  public static String        _5001;                                                                     // NO_UCD
  public static String        _5003;                                                                     // NO_UCD
  public static String        _5004;                                                                     // NO_UCD
  public static String        _5005;                                                                     // NO_UCD
  public static String        _5006;                                                                     // NO_UCD
  public static String        _5007;                                                                     // NO_UCD
  public static String        _5008;                                                                     // NO_UCD
  public static String        _5009;                                                                     // NO_UCD
  public static String        _5010;                                                                     // NO_UCD
  public static String        _6000;                                                                     // NO_UCD
  public static String        _6901;                                                                     // NO_UCD
  public static String        _6999;                                                                     // NO_UCD
  public static String        _8000;                                                                     // NO_UCD
  public static String        _8999;                                                                     // NO_UCD
  public static String        _9000;                                                                     // NO_UCD
  public static String        _9999;                                                                     // NO_UCD

  static {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }

  public static String get(String fieldName) {
    try {
      Field f = Messages.class.getField(fieldName);
      if (f != null) {
        f.setAccessible(true);
        return (String) f.get(null);
      }
    } catch (IllegalAccessException e) {
    } catch (NoSuchFieldException e) {
    }
    return null;
  }
}

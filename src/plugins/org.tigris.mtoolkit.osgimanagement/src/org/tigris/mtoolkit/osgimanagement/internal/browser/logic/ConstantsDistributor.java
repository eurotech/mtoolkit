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
  static final String DEFAULT_IP                    = "localhost";                     //$NON-NLS-1$

  static final String FRAMEWORK_NAME                = "framework_name_key";            //$NON-NLS-1$
  static final String TRANSPORT_PROVIDER_ID         = "transport_type_key";            //$NON-NLS-1$

  static final String FRAMEWORK_SIGN_CERTIFICATE_ID = "framework_sign_certificate_uid"; //$NON-NLS-1$
  static final String CONNECT_TO_FRAMEWORK          = "framework_connect_key";

  static final String MEMENTO_TYPE                  = "browser_model";                 //$NON-NLS-1$
  static final String MEMENTO_ROOT_TYPE             = "browser_root_model";            //$NON-NLS-1$

  static final String FRAMEWORK_STATUS_NAME         = "status";                        //$NON-NLS-1$
  static final String FRAMEWORK_CONNECT_VALUE       = "connected";                     //$NON-NLS-1$

  static final String BUNDLE_STATE_NAME             = "state";                         //$NON-NLS-1$
  static final String BUNDLE_UNINSTALLED_VALUE      = "uninstalled";                   //$NON-NLS-1$
  static final String BUNDLE_INSTALLED_VALUE        = "installed";                     //$NON-NLS-1$
  static final String BUNDLE_RESOLVED_VALUE         = "resolved";                      //$NON-NLS-1$
  static final String BUNDLE_STARTING_VALUE         = "starting";                      //$NON-NLS-1$
  static final String BUNDLE_STOPPING_VALUE         = "stopping";                      //$NON-NLS-1$
  static final String BUNDLE_ACTIVE_VALUE           = "active";                        //$NON-NLS-1$

  static final String NODE_NAME                     = "node_name";                     //$NON-NLS-1$
}

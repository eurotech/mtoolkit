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

import java.util.Hashtable;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;


public class FrameworkPlugin extends AbstractUIPlugin { 

  private static FrameworkPlugin instance = null;
  private static Hashtable storage = new Hashtable();
  
  public static final String PLUGIN_ID = "org.tigris.mtoolkit.osgimanagement"; //$NON-NLS-1$
  public static String fileDialogLastSelection;
  
  public FrameworkPlugin() {
    super();
    if (instance == null) instance = this;
  }
  // Returns default instance
  public static FrameworkPlugin getDefault() {
    return instance;
  }

  public void stop(BundleContext context) throws Exception {
    super.stop(context);
    FrameworkConnectorFactory.deinit();
    instance = null;
  }

  // Initialize perespectives 
  public void start(BundleContext context) throws Exception {
    super.start(context);
    getPreferenceStore().setDefault(ConstantsDistributor.MEMENTO_AUTOCONNECT, FrameworkConnectorFactory.isAutoConnectEnabled);
    getPreferenceStore().setDefault(ConstantsDistributor.MEMENTO_AUTOSTART_AFTER_INSTALL, FrameworkConnectorFactory.isAutoStartBundlesEnabled);
    getPreferenceStore().setDefault(ConstantsDistributor.MEMENTO_INFO_LOG, BrowserErrorHandler.isInfoLogEnabled);
    FrameworkConnectorFactory.isAutoConnectEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_AUTOCONNECT) ;
    FrameworkConnectorFactory.isAutoStartBundlesEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_AUTOSTART_AFTER_INSTALL) ;
    BrowserErrorHandler.isInfoLogEnabled = getPreferenceStore().getBoolean(ConstantsDistributor.MEMENTO_INFO_LOG) ;

    FrameworkConnectorFactory.init();
    FrameWorkView.restoreModel();
    fileDialogLastSelection = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
  }
  
  public String getId() {
    return PLUGIN_ID;
  }
  
  public static void putInStorage(Object key, Object value) {
  	if (value == null) {
			storage.remove(key);
  	} else {
			storage.put(key, value);
  	}
  }
  
	public static Object getFromStorage(Object key) {
		return storage.get(key);
	}
	
	public static void error(String message, Throwable t) {
		getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, t));
	}
	
	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
	}
	
	
	
}
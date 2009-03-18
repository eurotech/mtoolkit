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
package org.tigris.mtoolkit.common;

import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.plugin.AbstractUIPlugin;

public class UtilitiesPlugin extends AbstractUIPlugin {
 
  public static final String PLUGIN_ID = "org.tigris.mtoolkit.common"; //$NON-NLS-1$
    
  private static UtilitiesPlugin inst;
   
  /**
   * Creates the Utilities plugin and caches its default instance
   *
   * @param descriptor  the plugin descriptor which the receiver is made from
   */  
  public UtilitiesPlugin() {
    super();
    if (inst == null) inst = this;
  }
    
  public String getId() {
    return PLUGIN_ID;
  }
    
  /**
   * Gets the plugin singleton.
   *
   * @return the default UtilitiesPlugin instance
   */  
  public static UtilitiesPlugin getDefault() {
    return inst;
  }
    
  public IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = getWorkbench().getActiveWorkbenchWindow();
		if (window == null)
			return null;
		return getWorkbench().getActiveWorkbenchWindow().getActivePage();
	}
  
 

}
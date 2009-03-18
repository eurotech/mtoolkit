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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;


public class Bundle extends Model {

	private long id;
  private FrameWork framework;
  private boolean needsUpdate;
  private int state;
  private String version = null;

  // bundle types
  public static final int BUNDLE_TYPE_FRAGMENT = RemoteBundle.BUNDLE_TYPE_FRAGMENT;
  public static final int BUNDLE_TYPE_EXTENSION = BUNDLE_TYPE_FRAGMENT+1;
  // 0 for regular bundles
  private int type = -1;
  private RemoteBundle rBundle;
  private String category;

  public Bundle(String name, Model parent, RemoteBundle rBundle, int state, int type, String category) throws IAgentException {
    super(name, parent);
    this.rBundle = rBundle;
    this.id = rBundle.getBundleId();
    this.framework = parent.findFramework();
    // state is not get from rBundle to avoid unnecessary remote method calls
    this.state = state;
    
    this.type = type;
    
    this.category = category;
    needsUpdate = true;
  }
  

  public RemoteBundle getRemoteBundle() throws IAgentException {
    return rBundle;
  }


  public long getID() {
    return id;
  }
  
  
  // Overrides method in Model class
  public boolean testAttribute(Object target, String name, String value) {
    if (!(target instanceof org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle)) {
      return false;
    }
    if (!framework.isConnected()) {
      return false;
    }
    
    if (name.equalsIgnoreCase(BUNDLE_STATE_NAME)) {
      if (value.equalsIgnoreCase(BUNDLE_UNINSTALLED_VALUE)) {
        return state == org.osgi.framework.Bundle.UNINSTALLED;
      }
      if (value.equalsIgnoreCase(BUNDLE_INSTALLED_VALUE)) {
        return state == org.osgi.framework.Bundle.INSTALLED;
      }
      if (value.equalsIgnoreCase(BUNDLE_RESOLVED_VALUE)) {
        return state == org.osgi.framework.Bundle.RESOLVED;
      }
      if (value.equalsIgnoreCase(BUNDLE_STARTING_VALUE)) {
        
        return state == org.osgi.framework.Bundle.STARTING;
      }
      if (value.equalsIgnoreCase(BUNDLE_STOPPING_VALUE)) {
        return state == org.osgi.framework.Bundle.STOPPING;
      }
      if (value.equalsIgnoreCase(BUNDLE_ACTIVE_VALUE)) {
        return state == org.osgi.framework.Bundle.ACTIVE;
      }
    }
    
    return false;
  }

  public void removeAll() {
    Model[] categories = getChildren();
    if (categories != null) {
      for (int i=0; i < categories.length; i++) {
        removeElement(categories[i]);
      }
    }
  }

	public boolean isShowID() {
		return framework.isShowBundlesID();
	}
  
  
  public void setShowID(boolean show) {
    framework.setShowBundlesID(show);
  }
  
  public boolean isShowVersion() {
    return framework.isShowBundlesVersion();
  }

  public void setShowVersion(boolean show) {
    framework.setShowBundlesVersion(show);
  }


	public void update() {
    if (framework.getConnector() != null) {
			try {
        RemoteBundle rBundle = getRemoteBundle(); 
        if (rBundle == null) {
          state = 0;
        } else {
          state = rBundle.getState();
        }
        updateViewers();
			} catch (IAgentException e) {
        BrowserErrorHandler.processError(e, this);
			}
    }
	}

  public boolean isNeedUpdate() {
    return needsUpdate;
  }
  
  
	public FrameWork getFramework() {
		return framework;
	}


	public int getState() {
		return state;
	}

	public void setState(int i) {
		state = i;
	}
	
	public String getVersion() throws IAgentException {
	  if (version == null) {
		 try {
			 version = rBundle.getVersion();
		 } catch (IllegalStateException e) {
			 // ignore uninstalled bundles
		 }
	  }
	  version = version == null ? Messages.missing_version : version;
	  return version;
	}
	
	public int getType() {
	  return type;
	}
	
	public String getCategory() {
	  return category;
	}
	
	public String getString() {
		StringBuffer buff = new StringBuffer();
		buff.append("ID: ").append(getID()).append(" Bundle name: ").append(getName()); //$NON-NLS-1$ //$NON-NLS-2$
		return buff.toString();
	}
}
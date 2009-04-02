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

import java.util.Dictionary;
import java.util.Enumeration;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteDP;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;


public class DeploymentPackage extends Model {

  private FrameWork framework;
  private RemoteDP dp;

  public DeploymentPackage(RemoteDP dp, Model parent, FrameWork fw) throws IAgentException {
    super(dp.getName(), parent);
    this.dp = dp;
    this.framework = fw;
    Dictionary bundles = dp.getBundles();
    Enumeration keys = bundles.keys();
    while (keys.hasMoreElements()) {
      try {
        String name = (String) keys.nextElement();
        String version = (String) bundles.get(name);
        RemoteBundle bundlesList[] = framework.getConnector().getDeploymentManager().getBundles(name, version);
        if (bundlesList != null && bundlesList.length > 0) {
          RemoteBundle bundle = bundlesList[0];
          Dictionary headers = bundle.getHeaders(null);
          Bundle bundleNode = new Bundle(name, this, bundle, bundle.getState(), FrameworkConnectorFactory.getRemoteBundleType(bundle, headers), (String)headers.get("Bundle-Category")); //$NON-NLS-1$
          addElement(bundleNode);
          Bundle bundleNodeInBundles = (Bundle) fw.bundleHash.get(new Long(bundle.getBundleId()));
          if (bundleNodeInBundles == null) {
        	  // log an error only if we are in debug mode, otherwise we are not interested in it:)
        	BrowserErrorHandler.debug(new IllegalStateException("Bundle "+bundle.getBundleId()+" is missing")); //$NON-NLS-1$ //$NON-NLS-2$
            continue;
          }
          Model children[] = bundleNodeInBundles.getChildren();
          if (children != null && children.length > 0) {
            FrameworkConnectorFactory.addServiceCategoriesNodes(bundleNode);
            Model servNodes[] = bundleNode.getChildren();
            if (servNodes.length == 0) continue;
            Model regServ[] = children[0].getChildren();
            if (regServ != null) {
              for (int i=0; i<regServ.length; i++) {
                ObjectClass oc = new ObjectClass(servNodes[0], regServ[i].getName(), new Long(((ObjectClass)regServ[i]).getService().getServiceId()),  ((ObjectClass)regServ[i]).getService());
                servNodes[0].addElement(oc);
              }
            }
            
            Model usedServ[] = children[1].getChildren();
            if (usedServ != null) {
              for (int i=0; i<usedServ.length; i++) {
                ObjectClass oc = new ObjectClass(servNodes[1], usedServ[i].getName(), new Long(((ObjectClass)usedServ[i]).getService().getServiceId()),  ((ObjectClass)usedServ[i]).getService());
                servNodes[1].addElement(oc);
              }
            }
          }
          if(framework.getViewType() == FrameWork.SERVICES_VIEW) {
        	  framework.removeElement(bundleNode.getParent().getParent());
          }
        }
      } catch (IllegalStateException e) {
        // bundle was uninstalled
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

  public RemoteDP getRemoteDP() {
    return dp;
  }
  
  /**
   * @return
   * @throws IAgentException 
   */
  public boolean isStale() {
    try {
      return dp.isStale();
    } catch (IAgentException e) {
      e.printStackTrace();
      BrowserErrorHandler.processError(e, this);
    }
    return false;
  }
  
  // Overrides method in Model class
  public boolean testAttribute(Object target, String name, String value) {
    if (!(target instanceof org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage)) {
      return false;
    }
    if (!framework.isConnected()) {
      return false;
    }
    
    if (name.equalsIgnoreCase(DP_STALE_NAME)) {
      if (value.equalsIgnoreCase(DP_STALE_VALUE_TRUE)) {
        return isStale();
      }
      if (value.equalsIgnoreCase(DP_STALE_VALUE_FALSE)) {
        return !isStale();
      }
    }
    
    return false;
  }



}

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
package org.tigris.mtoolkit.osgimanagement.model;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.IMemento;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;

/**
 * @since 5.0
 */
public abstract class Framework extends Model {
  // framework IP or other ID
  public static final String FRAMEWORK_ID     = "framework_id_key"; //$NON-NLS-1$

  public final static int    BUNDLES_VIEW     = 0;
  public final static int    SERVICES_VIEW    = 1;

  protected DeviceConnector  connector;
  protected boolean          connectedFlag;
  protected int              viewType;
  protected List             modelProviders   = new ArrayList();
  protected final List       listeners        = new ArrayList();

  /**
   * @since 6.0
   */
  private final boolean      autoConnected;

  private long               timeStamp;
  private Dictionary         remoteProperties = null;

  /**
   * @since 6.0
   */
  public Framework(String name, boolean autoConnected) {
    super(name);
    this.autoConnected = autoConnected;
  }

  public DeviceConnector getConnector() {
    return connector;
  }

  public synchronized Dictionary getRemoteDeviceProperties() {
    if (connector == null) {
      return null;
    }
    if (remoteProperties == null || System.currentTimeMillis() - timeStamp > 30 * 1000) {
      try {
        remoteProperties = connector.getRemoteProperties();
      } catch (IAgentException e) {
        return null;
      }
      timeStamp = System.currentTimeMillis();
    }
    return remoteProperties;
  }

  public boolean isConnected() {
    return connectedFlag;
  }

  public boolean isAutoConnected() {
    return autoConnected;
  }

  public int getViewType() {
    return viewType;
  }

  public void addConnectionListener(FrameworkConnectionListener l) {
    if (!listeners.contains(l)) {
      listeners.add(l);
    }
  }

  public void removeConnectionListener(FrameworkConnectionListener l) {
    listeners.remove(l);
  }

  public static Object getLockObject(DeviceConnector connector) {
    return connector.lockObj;
  }

  /**
   * Returns map, containing information for certificates which shall be used
   * for signing the content, installed to this framework. If no signing is
   * required, then empty Map is returned.
   *
   * @return the map with certificate properties
   */
  public abstract Map getSigningProperties();

  public abstract Model createModel(String mimeType, String id, String version);

  public abstract IMemento getConfig();

  public abstract List getSignCertificateUids();

  public abstract void setSignCertificateUids(List uids);
}

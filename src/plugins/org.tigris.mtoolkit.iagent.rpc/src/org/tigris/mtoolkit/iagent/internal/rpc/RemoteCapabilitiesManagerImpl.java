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
package org.tigris.mtoolkit.iagent.internal.rpc;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.rpc.AbstractRemoteAdmin;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesProvider;

public final class RemoteCapabilitiesManagerImpl extends AbstractRemoteAdmin implements RemoteCapabilitiesManager {
  private static final String  PROPERTY_EVENT         = "iagent_property_event"; //$NON-NLS-1$
  private static final String  EVENT_CAPABILITY_NAME  = "capability.name"; //$NON-NLS-1$
  private static final String  EVENT_CAPABILITY_VALUE = "capability.value"; //$NON-NLS-1$

  private static final Class[] CLASSES                = new Class[] {
      RemoteCapabilitiesProvider.class, RemoteCapabilitiesManager.class
                                                      };

  private BundleContext        bc;
  private ServiceRegistration  registration;
  private final Hashtable      capabilities           = new Hashtable(10, 1f);

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.Remote#remoteInterfaces()
   */
  public Class[] remoteInterfaces() {
    return CLASSES;
  }

  public void register(BundleContext bundleContext) {
    this.bc = bundleContext;
    initCapabilities();

    registration = bc.registerService(new String[] {
        RemoteCapabilitiesProvider.class.getName(), RemoteCapabilitiesManager.class.getName()
    }, this, null);
  }

  public void unregister(BundleContext bundleContext) {
    if (registration != null) {
      registration.unregister();
      registration = null;
    }
    this.bc = null;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesProvider#getCapabilities()
   */
  public Map getCapabilities() {
    return (Map) capabilities.clone();
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager#setCapability(java.lang.String, java.lang.Object)
   */
  public void setCapability(String capability, Object value) {
    capabilities.put(capability, value);
    EventSynchronizer synchronizer = Activator.getSynchronizer();
    if (synchronizer != null) {
      Dictionary pmpEventData = new Hashtable(2, 1f);
      pmpEventData.put(EVENT_CAPABILITY_NAME, capability);
      pmpEventData.put(EVENT_CAPABILITY_VALUE, value);
      synchronizer.enqueue(new EventData(pmpEventData, PROPERTY_EVENT));
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.AbstractRemoteAdmin#getServiceRegistration()
   */
  protected ServiceRegistration getServiceRegistration() {
    return registration;
  }

  private void initCapabilities() {
    capabilities.clear();
  }
}

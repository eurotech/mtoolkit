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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.AllServiceListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.tigris.mtoolkit.iagent.event.EventData;
import org.tigris.mtoolkit.iagent.event.EventSynchronizer;
import org.tigris.mtoolkit.iagent.rpc.AbstractRemoteAdmin;
import org.tigris.mtoolkit.iagent.rpc.Capabilities;
import org.tigris.mtoolkit.iagent.rpc.RemoteCapabilitiesManager;
import org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public final class RemoteServiceAdminImpl extends AbstractRemoteAdmin implements RemoteServiceAdmin, AllServiceListener {
  private static final String  EVENT_TYPE_KEY         = "type";
  private static final Integer SERVICE_REGISTERED     = new Integer(1 << 0);
  private static final Integer SERVICE_MODIFIED       = new Integer(1 << 1);
  private static final Integer SERVICE_UNREGISTERED   = new Integer(1 << 2);

  private static final boolean TRACK_SERVICES_DEBUG   = Boolean.getBoolean("iagent.debug.services");

  private Bundle               systemBundle;

  private Class[]              filterSupportedClasses = new Class[] {
      int.class, long.class, float.class, double.class, byte.class, short.class, char.class, boolean.class,
      Integer.class, Long.class, Float.class, Double.class, Byte.class, Short.class, Character.class, Boolean.class,
      String.class
                                                      };
  private BundleContext        bc;
  private ServiceRegistration  registration;

  private Map                  services               = new Hashtable();

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.Remote#remoteInterfaces()
   */
  public Class[] remoteInterfaces() {
    return new Class[] {
      RemoteServiceAdmin.class
    };
  }

  public void register(BundleContext context) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[register] Registering remote service admin...");
    }
    this.bc = context;

    registration = context.registerService(RemoteServiceAdmin.class.getName(), this, null);

    synchronized (services) {
      context.addServiceListener(this);
      try {
        ServiceReference[] refs = context.getAllServiceReferences(null, null);
        fillServicesMap(refs);
      } catch (InvalidSyntaxException e) {
        // ignore
      }
    }

    RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
    if (capMan != null) {
      capMan.setCapability(Capabilities.SERVICE_SUPPORT, new Boolean(true));
    }

    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[register] Remote Service Admin Registered.");
    }
  }

  public void unregister(BundleContext context) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[unregister] Unregistering...");
    }
    context.removeServiceListener(this);
    services = null;
    if (registration != null) {
      registration.unregister();
      registration = null;
    }

    RemoteCapabilitiesManager capMan = Activator.getCapabilitiesManager();
    if (capMan != null) {
      capMan.setCapability(Capabilities.SERVICE_SUPPORT, new Boolean(false));
    }

    this.bc = null;

    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[unregister] Unregistered.");
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin#checkFilter(java.lang.String)
   */
  public String checkFilter(String filter) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[checkFilter] >>> filter: " + filter);
    }
    try {
      bc.createFilter(filter);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[checkFilter] Filter check is successful");
      }
      return null;
    } catch (InvalidSyntaxException e) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[checkFilter] Unable to create filter", e);
      }
      return e.toString();
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin#getAllRemoteServices(java.lang.String, java.lang.String)
   */
  public Dictionary[] getAllRemoteServices(String clazz, String filter) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getAllRemoteServices] >>> clazz: " + clazz + "; filter: " + filter);
    }
    ServiceReference[] refs;
    try {
      refs = bc.getAllServiceReferences(clazz, filter);
    } catch (InvalidSyntaxException e) {
      return null;
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getAllRemoteServices] " + (refs != null ? refs.length : 0) + " services found.");
    }
    return convertReferences(refs);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin#getUsingBundles(long)
   */
  public long[] getUsingBundles(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getUsingBundles] >>> id: " + id);
    }
    ServiceReference ref = getServiceReference(id);
    if (ref == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getUsingBundles] No such service");
      }
      return null;
    }
    Bundle[] bundles = ref.getUsingBundles();
    long[] bids = RemoteBundleAdminImpl.convertBundlesToIds(bundles);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getUsingBundles] bundles: " + DebugUtils.convertForDebug(bids));
    }
    return bids;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin#isServiceStale(long)
   */
  public boolean isServiceStale(long id) {
    boolean stale = services.get(new Long(id)) == null;
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[isServiceStale] id: " + id + "; stale: " + stale);
    }
    return stale;
  }

  /* (non-Javadoc)
   * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
   */
  public void serviceChanged(ServiceEvent event) {
    switch (event.getType()) {
    case ServiceEvent.REGISTERED: {
      addService(event.getServiceReference());
      break;
    }
    case ServiceEvent.UNREGISTERING: {
      removeService(event.getServiceReference());
      break;
    }
    }
    postRemoteEvent(event);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin#getBundle(long)
   */
  public long getBundle(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundle] >>> id: " + id);
    }
    ServiceReference ref = getServiceReference(id);
    if (ref == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getBundle] No such service");
      }
      return -1;
    }
    long bundleID = ref.getBundle().getBundleId();
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getBundle] bundle id: " + bundleID);
    }
    return bundleID;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.RemoteServiceAdmin#getProperties(long)
   */
  public Dictionary getProperties(long id) {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getProperties] >>> id: " + id);
    }

    ServiceReference ref = getServiceReference(id);
    if (ref == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[getProperties] No such service");
      }
      return null;
    }

    Dictionary props = new Hashtable();
    String[] keys = ref.getPropertyKeys();
    for (int i = 0; i < keys.length; i++) {
      Object prop = ref.getProperty(keys[i]);
      props.put(keys[i], convertProperty(prop));
    }

    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "[getProperties] service properties: " + DebugUtils.convertForDebug(props));
    }

    return props;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.rpc.AbstractRemoteAdmin#getServiceRegistration()
   */
  protected ServiceRegistration getServiceRegistration() {
    return registration;
  }

  private Object convertProperty(Object value) {
    if (value == null) {
      return null;
    } else if (isFilterSupportedClass(value.getClass())) {
      return value;
    } else if (value.getClass().isArray()) {
      Class componentType = value.getClass().getComponentType();
      if (isFilterSupportedClass(componentType)) {
        return value;
      } else {
        return convertCollection(value);
      }
    } else if (value instanceof Collection) {
      boolean supported = true;
      for (Iterator it = ((Collection) value).iterator(); it.hasNext();) {
        Object next = it.next();
        if (!isFilterSupportedClass(next.getClass())) {
          supported = false;
          break;
        }
      }
      if (supported) {
        return value;
      } else {
        return convertCollection(value);
      }
    } else {
      return value.toString();
    }
  }

  private String[] convertCollection(Object convertible) {
    String[] result;
    if (convertible.getClass().isArray()) {
      Object[] array = (Object[]) convertible;
      result = new String[array.length];
      for (int i = 0; i < array.length; i++) {
        result[i] = (array[i] != null ? array[i].toString() : null);
      }
    } else if (convertible instanceof Collection) {
      Collection c = (Collection) convertible;
      result = new String[c.size()];
      int i = 0;
      for (Iterator it = c.iterator(); it.hasNext() && i < result.length; i++) {
        Object next = it.next();
        result[i] = (next != null ? next.toString() : null);
      }
    } else {
      throw new IllegalArgumentException("The passsed convertible is neither array or Collection");
    }
    return result;
  }

  private void postRemoteEvent(ServiceEvent event) {
    if (systemBundle == null) {
      systemBundle = bc.getBundle(0);
    }
    if (systemBundle.getState() == Bundle.STOPPING) {
      return;
    }
    if (bc.getBundle().getState() == Bundle.STOPPING) {
      return;
    }
    EventSynchronizer synchronizer = Activator.getSynchronizer();
    if (synchronizer != null) {
      Dictionary convertedServiceEvent = convertServiceEvent(event);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this,
            "[postRemoteEvent] Posting remote event: " + DebugUtils.convertForDebug(convertedServiceEvent) + "; type: "
                + RemoteServiceAdmin.CUSTOM_SERVICE_EVENT);
      }
      synchronizer.enqueue(new EventData(convertedServiceEvent, RemoteServiceAdmin.CUSTOM_SERVICE_EVENT));
    } else {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[postRemoteEvent] Event synchronizer was disabled");
      }
    }
  }

  private void addService(ServiceReference ref) {
    Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
    if (TRACK_SERVICES_DEBUG) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[addService] Track service: " + ref + "; id: " + serviceId);
      }
    }
    services.put(serviceId, ref);
  }

  private void removeService(ServiceReference ref) {
    Long serviceId = (Long) ref.getProperty(Constants.SERVICE_ID);
    if (TRACK_SERVICES_DEBUG) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "[addService] Stop tracking service: " + ref + "; id: " + serviceId);
      }
    }
    services.remove(serviceId);
  }

  private Dictionary convertServiceEvent(ServiceEvent event) {
    Dictionary props = new Hashtable();
    switch (event.getType()) {
    case ServiceEvent.REGISTERED:
      props.put(EVENT_TYPE_KEY, SERVICE_REGISTERED);
      break;
    case ServiceEvent.MODIFIED:
      props.put(EVENT_TYPE_KEY, (SERVICE_MODIFIED));
      break;
    case ServiceEvent.UNREGISTERING:
      props.put(EVENT_TYPE_KEY, SERVICE_UNREGISTERED);
      break;
    }
    props.put(Constants.SERVICE_ID, event.getServiceReference().getProperty(Constants.SERVICE_ID));
    props.put(Constants.OBJECTCLASS, event.getServiceReference().getProperty(Constants.OBJECTCLASS));
    return props;
  }

  static Dictionary[] convertReferences(ServiceReference[] refs) {
    if (refs == null) {
      return new Dictionary[0];
    }
    Dictionary[] refsProps = new Dictionary[refs.length];
    for (int i = 0; i < refs.length; i++) {
      refsProps[i] = new Hashtable();
      refsProps[i].put(Constants.SERVICE_ID, refs[i].getProperty(Constants.SERVICE_ID));
      refsProps[i].put(Constants.OBJECTCLASS, refs[i].getProperty(Constants.OBJECTCLASS));
    }
    return refsProps;
  }

  private boolean isFilterSupportedClass(Class clazz) {
    for (int i = 0; i < filterSupportedClasses.length; i++) {
      if (filterSupportedClasses[i].equals(clazz)) {
        return true;
      }
    }
    return false;
  }

  private void fillServicesMap(ServiceReference[] references) {
    if (references == null || references.length == 0) {
      return;
    }
    for (int i = 0; i < references.length; i++) {
      ServiceReference ref = references[i];
      Long sid = (Long) ref.getProperty(Constants.SERVICE_ID);
      services.put(sid, ref);
    }
  }

  private ServiceReference getServiceReference(long id) {
    ServiceReference ref = (ServiceReference) services.get(new Long(id));
    return ref;
  }
}

/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.transport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tigris.mtoolkit.iagent.util.LightServiceRegistry;

public class TransportsHub {
  private static final String         TRANSPORTS_REGISTRY_FILE = "transports.properties";
  private static LightServiceRegistry transportsRegistry;

  public static Transport openTransport(String type, String id) throws IOException {
    TransportType transportType = getType(type);
    return transportType.openTransport(id);
  }

  public static List listTypes() {
    Object[] services = getServiceRegistry().getAllServices();
    List result = new ArrayList();
    for (int i = 0; i < services.length; i++) {
      if (services[i] instanceof TransportType) {
        result.add(services[i]);
      }
    }
    return result;
  }

  public static TransportType getType(String type) {
    Object extender = getServiceRegistry().get(type);
    if (extender instanceof TransportType) {
      return (TransportType) extender;
    }
    throw new IllegalArgumentException("unable to find transport type " + type);
  }

  private static LightServiceRegistry getServiceRegistry() {
    if (transportsRegistry == null) {
      transportsRegistry = new LightServiceRegistry(TRANSPORTS_REGISTRY_FILE, TransportsHub.class.getClassLoader());
    }
    return transportsRegistry;
  }

}

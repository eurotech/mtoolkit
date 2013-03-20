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
package org.tigris.mtoolkit.iagent.pmp;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.iagent.internal.pmp.Server;

public final class PMPServerFactory {
  /** constant used for the pmp configuration */

  public static final String MAX_STRING = "maxstringlength";
  /** constant used for the pmp configuration */

  public static final String MAX_ARRAY  = "maxarraylength";
  /** constant used for the pmp configuration */

  public static final String TIMEOUT    = "timeout";

  private PMPServerFactory() {
  }

  public static PMPServer createServer(BundleContext context, int port, Dictionary props) throws PMPException {
    if (props == null) {
      props = new Hashtable();
    }
    final String uri = "tcp://:" + String.valueOf(port);
    props.put(Server.URI, uri);
    props.put(Server.PORT, new Integer(port));
    if (props.get(PMPServerFactory.TIMEOUT) == null) {
      props.put(PMPServerFactory.TIMEOUT, new Integer(10000));
    }
    if (props.get(PMPServerFactory.MAX_STRING) == null) {
      props.put(PMPServerFactory.MAX_STRING, new Integer(255));
    }
    if (props.get(PMPServerFactory.MAX_ARRAY) == null) {
      props.put(PMPServerFactory.MAX_ARRAY, new Integer(300000));
    }
    try {
      return new Server(context, props);
    } catch (IOException exc) {
      String msg = "Error creating transport configuration for " + uri;
      throw new PMPException(msg, exc);
    }
  }

}

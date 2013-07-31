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
  public static final String   MAX_STRING_PROP = "iagent.pmp.server.maxstringlength";

  /** constant used for the pmp configuration */
  public static final String   MAX_ARRAY_PROP  = "iagent.pmp.server.maxarraylength";

  /** constant used for the pmp configuration */
  public static final String   TIMEOUT_PROP    = "iagent.pmp.server.timeout";

  private static final Integer MAX_STRING      = Integer.getInteger(MAX_STRING_PROP, 255);
  private static final Integer MAX_ARRAY       = Integer.getInteger(MAX_ARRAY_PROP, 300000);
  private static final Integer TIMEOUT         = Integer.getInteger(MAX_ARRAY_PROP, 10000);

  private PMPServerFactory() {
  }

  public static PMPServer createServer(BundleContext context, int port, Dictionary props) throws PMPException {
    if (props == null) {
      props = new Hashtable();
    }
    final String uri = "tcp://:" + String.valueOf(port);
    props.put(Server.URI, uri);
    props.put(Server.PORT, new Integer(port));
    if (props.get(PMPServerFactory.TIMEOUT_PROP) == null) {
      props.put(PMPServerFactory.TIMEOUT_PROP, TIMEOUT);
    }
    if (props.get(PMPServerFactory.MAX_STRING_PROP) == null) {
      props.put(PMPServerFactory.MAX_STRING_PROP, MAX_STRING);
    }
    if (props.get(PMPServerFactory.MAX_ARRAY_PROP) == null) {
      props.put(PMPServerFactory.MAX_ARRAY_PROP, MAX_ARRAY);
    }
    try {
      return new Server(context, props);
    } catch (IOException exc) {
      String msg = "Error creating transport configuration for " + uri;
      throw new PMPException(msg, exc);
    }
  }

}

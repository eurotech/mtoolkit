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

import org.tigris.mtoolkit.iagent.internal.pmp.PMPServiceImpl;

public final class PMPServiceFactory {
  private static PMPService defaultInstance;

  private PMPServiceFactory() {
  }

  public static synchronized PMPService getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new PMPServiceImpl();
    }
    return defaultInstance;
  }

  public static synchronized void dispose() {
    if (defaultInstance != null) {
      ((PMPServiceImpl) defaultInstance).destroy();
    }
    defaultInstance = null;
  }
}

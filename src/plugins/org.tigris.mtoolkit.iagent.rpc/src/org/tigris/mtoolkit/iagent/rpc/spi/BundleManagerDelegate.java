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
package org.tigris.mtoolkit.iagent.rpc.spi;

import java.io.InputStream;

import org.osgi.framework.Bundle;

public interface BundleManagerDelegate {

  public Object installBundle(String location, InputStream in);
  
  public Object uninstallBundle(Bundle bundle);
  
  public Object updateBundle(Bundle bundle, InputStream in);
  
}

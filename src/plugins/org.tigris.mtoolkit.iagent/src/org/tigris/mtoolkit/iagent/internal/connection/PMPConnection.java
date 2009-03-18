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
package org.tigris.mtoolkit.iagent.internal.connection;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;


public interface PMPConnection extends AbstractConnection {

  public RemoteObject getRemoteBundleAdmin() throws IAgentException;
  
  public RemoteObject getRemoteDeploymentAdmin() throws IAgentException;
  
  public RemoteObject getRemoteParserService() throws IAgentException;
  
  public void releaseRemoteParserService() throws IAgentException;
  
  public RemoteObject getRemoteServiceAdmin() throws IAgentException;
  
  public void addEventListener(EventListener listener, String[] eventTypes) throws IAgentException;

  public void removeEventListener(EventListener listener, String[] eventTypes) throws IAgentException;
}

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
import org.tigris.mtoolkit.iagent.internal.tcp.ConnectionManagerImpl;

public interface AbstractConnection {
  public int getType();
  
  /**
   * Closes this connection.<br>
   * 
   * Implementors: This method should flag the connection as closed and do the
   * closing itself only once. However the method must call
   * {@link ConnectionManagerImpl#connectionClosed(AbstractConnection)} every
   * time, this method is called.
   * 
   * @throws IAgentException
   */
  public void closeConnection() throws IAgentException;
  
  /**
   * Returns true if this connection is still usable.
   * @return
   */
  public boolean isConnected();  
}

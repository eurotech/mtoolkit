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
package org.tigris.mtoolkit.iagent.mbsa;

public class MBSAResult extends MBSAReader {
  private int msgId;
  private int status;

  public MBSAResult(int msgId, int status, byte[] data) {
    super(data);
    this.status = status;
    this.msgId = msgId;
  }

  public int getId() {
    return msgId;
  }

  public int getStatus() {
    return status;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "MBSAResult[msgId=" + msgId + ";status=" + status + ";data.length=" + getData().length + "]";
  }
}

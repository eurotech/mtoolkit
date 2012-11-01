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

import java.io.IOException;
import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.internal.mbsa.DataFormater;

public class MBSAResponse extends MBSAWriter {
  private final int msgId;
  private final int status;

  public MBSAResponse(int msgId, int status) {
    this.msgId = msgId;
    this.status = status;
  }

  public MBSAResponse(int msgId, int status, byte[] data) {
    super(data);
    this.msgId = msgId;
    this.status = status;
  }

  public int getId() {
    return msgId;
  }

  public int getStatus() {
    return status;
  }

  public MBSAResponse done() {
    flush();
    return this;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.mbsa.MBSAWriter#writeTo(java.io.OutputStream)
   */
  public void writeTo(OutputStream os) throws IOException {
    validate();
    DataFormater.writeInt(os, msgId);
    DataFormater.writeInt(os, status);
    super.writeTo(os);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    String dataLength = Integer.toString(getData().length);
    return "MBSAResponse[msgId=" + msgId + ";status=" + status + ";data.length=" + dataLength + "]";
  }
}

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

public class MBSACommand extends MBSAWriter {
  private final int cmdId;

  public MBSACommand(int cmdId) {
    this.cmdId = cmdId;
  }

  public MBSACommand(int cmdId, byte[] data) {
    super(data);
    this.cmdId = cmdId;
  }

  public int getCommand() {
    return cmdId;
  }

  public MBSACommand done() {
    flush();
    return this;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.mbsa.MBSAWriter#writeTo(java.io.OutputStream)
   */
  public void writeTo(OutputStream os) throws IOException {
    validate();
    DataFormater.writeInt(os, cmdId);
    super.writeTo(os);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    String dataLength;
    try {
      dataLength = Integer.toString(getData().length);
    } catch (IllegalStateException e) {
      dataLength = "(not flushed)";
    }
    return "MBSACommand[cmdId=" + cmdId + ";data.length=" + dataLength + "]";
  }

}

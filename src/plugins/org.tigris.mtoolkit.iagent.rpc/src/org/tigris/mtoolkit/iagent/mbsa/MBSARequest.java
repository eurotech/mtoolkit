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

public class MBSARequest extends MBSAReader {
  private int msgId;
  private int cmdId;

  public MBSARequest(int msgId, int cmdId, byte[] data) {
    super(data);
    this.msgId = msgId;
    this.cmdId = cmdId;
  }

  public int getCommand() {
    return cmdId;
  }

  public int getId() {
    return msgId;
  }

  public MBSAResponse respond() {
    return respond(MBSAConstants.IAGENT_RES_OK, null);
  }

  public MBSAResponse respond(int status) {
    return respond(status, null);
  }

  public MBSAResponse respond(int status, byte[] data) {
    return new MBSAResponse(msgId, status, data);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "MBSARequest[msgId=" + msgId + ";cmdId=" + cmdId + ";data.length=" + getData().length + "]";
  }
}

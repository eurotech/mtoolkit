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
package org.tigris.mtoolkit.iagent.internal.rpc.console;

class CircularBuffer {
  private byte[]       buffer   = new byte[4096];
  private int          cbOffset = 0;
  private volatile int cbLength = 0;

  protected int getCapacity() {
    return buffer.length;
  }

  public int available() {
    return cbLength;
  }

  public void write(byte[] buf, int aOffset, int aLength) {
    synchronized (this) {
      if (buffer.length - cbLength < aLength) {
        byte[] newBuffer = new byte[(int) ((cbLength + aLength) * 1.5)];
        if (cbOffset + cbLength < buffer.length) {
          System.arraycopy(buffer, cbOffset, newBuffer, 0, cbLength);
        } else {
          System.arraycopy(buffer, cbOffset, newBuffer, 0, buffer.length - cbOffset);
          System.arraycopy(buffer, 0, newBuffer, buffer.length - cbOffset, cbLength - buffer.length + cbOffset);
        }
        buffer = newBuffer;
        cbOffset = 0;
      }
      int writeOffset = cbOffset + cbLength;
      if (writeOffset >= buffer.length) {
        writeOffset -= buffer.length;
      }
      int availableSpaceToEnd = buffer.length - writeOffset;
      if (aLength < availableSpaceToEnd) {
        System.arraycopy(buf, aOffset, buffer, writeOffset, aLength);
      } else {
        System.arraycopy(buf, aOffset, buffer, writeOffset, availableSpaceToEnd);
        System.arraycopy(buf, aOffset + availableSpaceToEnd, buffer, 0, aLength - availableSpaceToEnd);
      }
      cbLength += aLength;
      notifyAll();
    }
  }

  public int read(byte[] var0, int var1, int var2) {
    synchronized (this) {
      while (cbLength == 0) {
        try {
          wait();
        } catch (InterruptedException e) {
          return -1;
        }
        if (cbLength == 0) {
          return -1;
        }
      }
      int symbolsToRead = Math.min(cbLength, var2);
      int availableSymbolsToEnd = buffer.length - cbOffset;
      if (symbolsToRead < availableSymbolsToEnd) {
        System.arraycopy(buffer, cbOffset, var0, var1, symbolsToRead);
      } else {
        System.arraycopy(buffer, cbOffset, var0, var1, availableSymbolsToEnd);
        System.arraycopy(buffer, 0, var0, var1 + availableSymbolsToEnd, symbolsToRead - availableSymbolsToEnd);
      }
      cbLength -= symbolsToRead;
      cbOffset += symbolsToRead;
      if (cbOffset > buffer.length) {
        cbOffset -= buffer.length;
      }
      return symbolsToRead;
    }

  }

  public void release() {
    synchronized (this) {
      cbLength = 0;
      notifyAll();
    }
  }

  public int read(byte[] buf) {
    return read(buf, 0, buf.length);
  }

  public void write(byte[] buf) {
    write(buf, 0, buf.length);
  }
}

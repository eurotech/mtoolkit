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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.tigris.mtoolkit.iagent.internal.mbsa.DataFormater;

public abstract class MBSAReader {
  private byte[]      data;
  private InputStream input;

  public MBSAReader(byte[] data) {
    this.data = data != null ? data : new byte[0];
    input = new ByteArrayInputStream(this.data);
  }

  public byte[] getData() {
    byte[] copy = new byte[data.length];
    System.arraycopy(data, 0, copy, 0, data.length);
    return copy;
  }

  public byte readByte() throws IOException {
    return DataFormater.readByte(input);
  }

  public byte[] readByteArray() throws IOException {
    return DataFormater.readByteArray(input);
  }

  public short readShort() throws IOException {
    return DataFormater.readShort(input);
  }

  public short[] readShortArray() throws IOException {
    return DataFormater.readShortArray(input);
  }

  public int readInt() throws IOException {
    return DataFormater.readInt(input);
  }

  public int[] readIntArray() throws IOException {
    return DataFormater.readIntArray(input);
  }

  public long readLong() throws IOException {
    return DataFormater.readLong(input);
  }

  public long[] readLongArray() throws IOException {
    return DataFormater.readLongArray(input);
  }

  public short readWord() throws IOException {
    return DataFormater.readWord(input);
  }

  public short[] readWordArray() throws IOException {
    return DataFormater.readWordArray(input);
  }

  public int readDWord() throws IOException {
    return DataFormater.readDWord(input);
  }

  public int[] readDWordArray() throws IOException {
    return DataFormater.readDWordArray(input);
  }

  public long readQWord() throws IOException {
    return DataFormater.readQWord(input);
  }

  public long[] readQWordArray() throws IOException {
    return DataFormater.readQWordArray(input);
  }

  public String readString() throws IOException {
    return DataFormater.readString(input);
  }

  public String[] readStringArray() throws IOException {
    return DataFormater.readStringArray(input);
  }

  public byte readChar() throws IOException {
    return DataFormater.readChar(input);
  }

  public byte[] readCharArray() throws IOException {
    return DataFormater.readCharArray(input);
  }

  public int available() throws IOException {
    return input.available();
  }

  public void reset() {
    try {
      input.reset();
    } catch (IOException e) {
      // not expected, log it
    }
  }
}

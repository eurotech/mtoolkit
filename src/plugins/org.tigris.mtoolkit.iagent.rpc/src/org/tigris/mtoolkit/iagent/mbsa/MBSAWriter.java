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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.internal.mbsa.DataFormater;

public abstract class MBSAWriter {
  private byte[]                data;

  private ByteArrayOutputStream output = new ByteArrayOutputStream(64);

  public MBSAWriter() {
    this(null);
  }

  public MBSAWriter(byte[] data) {
    this.data = data;
  }

  public MBSAWriter writeByte(byte b) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeByte(output, b);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeByteArray(byte[] ba) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeByteArray(output, ba);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeShort(short sh) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeShort(output, sh);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeShortArray(short[] sha) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeShortArray(output, sha);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeInt(int i) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeInt(output, i);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeIntArray(int[] ia) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeIntArray(output, ia);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeLong(long l) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeLong(output, l);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeLongArray(long[] la) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeLongArray(output, la);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeWord(short w) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeWord(output, w);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeWordArray(short[] wa) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeWordArray(output, wa);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeDWord(int dw) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeDWord(output, dw);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeDWordArray(int[] dwa) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeDWordArray(output, dwa);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeQWord(long qw) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeQWord(output, qw);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeQWordArray(long[] qwa) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeQWordArray(output, qwa);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeString(String s) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeString(output, s);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeStringArray(String[] sa) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeStringArray(output, sa);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeChar(byte c) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeChar(output, c);
    } catch (IOException e) {
    }
    return this;
  }

  public MBSAWriter writeCharArray(byte[] ca) {
    if (data != null) {
      throw new IllegalStateException();
    }
    try {
      DataFormater.writeCharArray(output, ca);
    } catch (IOException e) {
    }
    return this;
  }

  protected void flush() {
    if (data == null) {
      data = output.toByteArray();
    }
  }

  public void validate() {
    if (data == null) {
      throw new IllegalStateException("Cannot write response to output, until it is flushed.");
    }
  }

  public void writeTo(OutputStream os) throws IOException {
    validate();
    DataFormater.writeInt(os, data.length);
    os.write(data);
  }

  public byte[] getData() {
    validate();
    byte[] copy = new byte[data.length];
    System.arraycopy(data, 0, copy, 0, data.length);
    return copy;
  }
}

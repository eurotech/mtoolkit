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
package org.tigris.mtoolkit.iagent.internal.pmp;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class for reading big data (files for example) from the client. Blocks
 * the PMPInputStream until the reading is finished.
 */
public final class FileReader extends InputStream {
  private short          length = 0;
  private boolean        locked = true;
  private PMPInputStream is;

  /**
   * Constructs a FileReader. the length parameter shows the length of the data
   * that must be read from this InputStream before the PMPInputStream is
   * unlocked.
   */
  public FileReader(PMPInputStream is) {
    this.is = is;
    is.lock();
    checkEOF();
  }

  /**
   * Reads a single byte. If all the data is read unlocks the PMPInputStream.
   */
  /* (non-Javadoc)
   * @see java.io.InputStream#read()
   */
  public synchronized int read() throws IOException {
    if (locked) {
      if (length > 0) {
        try {
          int read = is.read();
          length--;
          return read;
        } catch (IOException ioExc) {
          close();
          throw ioExc;
        }
      }
      checkEOF();
      return read();
    }
    return -1;
  }

  /**
   * Reads into a byte[]. If all the data is read unlocks the PMPInputStream.
   */
  /* (non-Javadoc)
   * @see java.io.InputStream#read(byte[], int, int)
   */
  public synchronized int read(byte[] buff, int off, int len) throws IOException {
    if (locked) {
      if (length > 0) {
        try {
          int read = is.read(buff, off, length >= len ? len : length);
          length -= read;
          return read;
        } catch (IOException ioExc) {
          close();
          throw ioExc;
        }
      }
      checkEOF();
      return read(buff, off, len);
    }
    return -1;
  }

  /**
   * Unlocks the PMPInputStream. If not all the data was read, skips it first.
   */
  /* (non-Javadoc)
   * @see java.io.InputStream#close()
   */
  public synchronized void close() {
    if (locked) {
      locked = false;
      if (length > 0) {
        try {
          is.skip(length);
        } catch (IOException ioExc) {
        }
      }
      is.unlock();
    }
  }

  /**
   * Added to ensure that the PMPInputStream will be unlocked.
   */
  /* (non-Javadoc)
   * @see java.lang.Object#finalize()
   */
  protected void finalize() {
    close();
  }

  private void checkEOF() {
    if (length == 0) {
      try {
        length = PMPData.readShort(is);
        if (length == -1) {
          close();
        }
      } catch (IOException ioExc) {
        close();
      }
    }
  }
}

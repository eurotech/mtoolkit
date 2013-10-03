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

import org.tigris.mtoolkit.iagent.util.DebugUtils;

class PMPInputStream extends InputStream {

  protected PMPSessionThread  c;

  /** current message id */
  private short               msgID          = -1;
  /** data buffer */
  private byte[]              buffer;
  /** position in the data buffer */
  private short               position       = 0;
  /** the underlying (socket) InputStream */
  private InputStream         is;

  /** true, if there are more messages with the current message id to come */
  private boolean             more           = false;
  /** the length of the current message's body left to read */
  private short               messageL       = 0;
  /** the length of the current buffer */
  private short               currentL       = 0;

  protected int               timeout;

  /** synchronization flags */
  private boolean             locked         = false;
  private int                 waiting        = 0;

  /** common error messages */
  private static final String ERRMSG1        = "Protocol Error";
  private static final String ERRMSG2        = "Message ID Mismatch ";

  static int                  stream_timeout = 0;                     // default: no timeout, wait forever

  static {
    String stimeout = System.getProperty("iagent.pmp.stream_timeout"); //$NON-NLS-1$
    if (stimeout != null) {
      try {
        stream_timeout = Integer.parseInt(stimeout);
      } catch (NumberFormatException nfe) {
      }
    }
  }

  public PMPInputStream(InputStream is, PMPSessionThread c) {
    this.is = is;
    this.c = c;
    timeout = stream_timeout;
    buffer = new byte[4096];
  }

  /**
   * Positions the InputStream at the beginning of the next message's body. If
   * the current message is not fully read, discards it.
   */
  public short nextMessage() throws IOException {
    synchronized (this) {
      while (locked) {
        waiting++;
        try {
          wait();
        } catch (Exception ignore) {
        } // IllegalMonitorStateException - impossible
        waiting--;
      }
    }
    if (msgID != -1) {
      skipRest();
    }
    boolean ping = true;
    while (true) {
      try {
        msgID = PMPData.readShort(is);
      } catch (IOException ioExc) {
        if (ping) {
          c.ping();
          ping = false;
          continue;
        }
        throwException(ioExc);
      }
      break;
    }
    readHeader();
    fillBuffer();
    return msgID;
  }

  /** reads a byte from the current message's body */
  public int read() throws IOException {
    if (position == currentL) {
      if (messageL > 0) {
        fillBuffer();
        return read();
      } else if (more) {
        readNext();
        return read();
      } else
        return -1;
    }
    return buffer[position++] & 0xff;
  }

  /** reads len bytes from the current message's body */
  public int read(byte b[], int off, int len) throws IOException {
    if (position == currentL) {
      if (messageL > 0) {
        fillBuffer();
      } else if (more) {
        readNext();
      } else
        return -1;
    }
    int more1 = len - (currentL - position);
    int read = 0;
    if (more1 <= 0) {
      System.arraycopy(buffer, position, b, off, len);
      position += len;
      read = len;
    } else {
      int first = len - more1;
      System.arraycopy(buffer, position, b, off, first);
      if (messageL > 0) {
        fillBuffer();
        read = first + read(b, off + first, len - first);
      } else if (more) {
        readNext();
        read = first + read(b, off + first, len - first);
      } else {
        position += first;
        read = first;
      }
    }
    return read;
  }

  public int available() throws IOException {
    int available = (currentL - position) + messageL;
    if (available == 0 && more) {
      readNext();
      return available();
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(c, "available " + available);
    }
    return available;
  }

  /**
   * Reads the next message from the underlying InputStream. If it's message id
   * is different from the message id of the current message calls
   * throwException
   */
  private void readNext() throws IOException {
    checkNextMsgID();
    readHeader();
    fillBuffer();
  }

  /** skips toSkip bytes from the current message's body */
  public long skip(long toSkip) throws IOException {
    if (position == currentL) {
      if (messageL > 0) {
        fillBuffer();
      } else if (more) {
        readNext();
      } else
        return 0;
    }
    long more1 = toSkip - (currentL - position);
    long skipped = 0;
    if (more1 <= 0) {
      position += (int) toSkip;
      skipped = toSkip;
    } else {
      int first = (int) (toSkip - more1);
      if (messageL > 0) {
        fillBuffer();
        skipped = first + skip(toSkip - first);
      } else if (more) {
        readNext();
        skipped = first + skip(toSkip - first);
      } else {
        position += first;
        skipped = first;
      }
    }
    return skipped;
  }

  /**
   * Reads the next message's id from the underlying InputStream. If its
   * different from the message id of the current message calls throwException
   */
  private void checkNextMsgID() throws IOException {
    short nextMsgID = -1;
    try {
      nextMsgID = PMPData.readShort(is);
    } catch (IOException ioExc) {
      throwException(ioExc);
    }
    if (nextMsgID != msgID) {
      throwException(new IOException(ERRMSG2));
    }
  }

  /**
   * Reads the message's length & more flag
   */
  private void readHeader() throws IOException {
    int tmp = 0;
    try {
      messageL = PMPData.readShort(is);
      tmp = is.read();
      more = tmp == 0;
    } catch (IOException ioExc) {
      throwException(ioExc);
    }
    if (tmp == -1) {
      throwException(new IOException(ERRMSG1));
    }
  }

  /**
   * Reads the next part of the current message's body from the underlying
   * InputStream. If an exception occurs, calls throwException.
   */
  private void fillBuffer() throws IOException {
    currentL = (messageL > 4096) ? 4096 : messageL;
    int read = 0;
    int tmp = 0;
    while (read < currentL) {
      try {
        tmp = is.read(buffer, read, currentL - read);
      } catch (IOException ioExc) {
        throwException(ioExc);
      }
      if (tmp == -1) {
        throwException(new IOException(ERRMSG1));
      }
      read += tmp;
    }
    messageL -= currentL;
    position = 0;
  }

  /**
   * Discards the rest of the message and all following messages with the same
   * message id
   */
  private void skipRest() throws IOException {
    do {
      if (messageL > 0) {
        try {
          is.skip(messageL);
        } catch (IOException ioExc) {
          throwException(ioExc);
        }
      }
      if (more) {
        checkNextMsgID();
        readHeader();
      } else
        break;
    } while (true);
  }

  /** Calls disconnect on the PMPProcessor and throws the exception */
  protected void throwException(IOException ioExc) throws IOException {
    c.os.checkWaitStatus(ioExc.toString());
    throw ioExc;
  }

  protected synchronized void lock() {
    locked = true;
  }

  protected synchronized void unlock() {
    locked = false;
    if (waiting > 0) {
      notifyAll();
    }
  }

  public void close() throws IOException {
    is.close();
  }
}

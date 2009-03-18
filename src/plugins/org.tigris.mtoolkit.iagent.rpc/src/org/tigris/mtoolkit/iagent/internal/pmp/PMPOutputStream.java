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
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

class PMPOutputStream extends OutputStream {

	private PMPSessionThread c;
	protected Map answers = new Hashtable();

	private static IOException ioExc;
	private boolean closed = false;

	/** data buffer */
	protected byte[] buffer;
	/** current position in the data buffer */
	private short position;
	/** the underlying OutputStream */
	private OutputStream os;
	private short dataOffset;
	/** the current message id */
	private short clientMsgID = 100; // 0server;
	private short msgID;

	/** synchronization flags */
	private boolean locked = false;
	private int waiting = 0;

	protected boolean ping = false;

	protected long time = System.currentTimeMillis();

	public PMPOutputStream(OutputStream os, PMPSessionThread c) {
		this.os = os;
		this.c = c;
		buffer = new byte[4096];
		dataOffset = (short) 5;
	}

	/** writes a byte in the current message's body, and flush, if needed */
	public void write(int b) throws IOException {
		if (position == buffer.length) {
			flush(false);
		}
		buffer[position++] = (byte) b;
	}

	/** writes len bytes in the current message's body, and flushs, if needad */
	public void write(byte b[], int off, int len) throws IOException {
		int more = position + len - buffer.length;
		if (more > 0) {
			System.arraycopy(b, off, buffer, position, len - more);
			position = (short) buffer.length;
			flush(false);
			write(b, off + len - more, more);
		} else {
			System.arraycopy(b, off, buffer, position, len);
			position += len;
		}
	}

	public void write(InputStream is) throws IOException {
		// the cycle below is a result of a transformation of recursion into
		// iteration
		while (true) { // cycle until we have read the whole input stream
			int free = buffer.length - position;
			if (free < 10) {
				flush(false);
				free = buffer.length - position;
			}
			int toWrite = is.read(buffer, position + 2, free - 2);
			if (toWrite == -1) {
				writeShort((byte) -1, buffer, position);
				position += 2;
				break; // we have reached the end of input stream, exit the
				// cycle
			} else {
				writeShort((short) toWrite, buffer, position);
				position += (toWrite + 2);
				if (position == buffer.length)
					flush(false);
			}
		}
	}

	/** writes a short in the specified byte[] at the specified offset */
	private void writeShort(short s, byte[] b, int off) {
		b[off++] = (byte) ((s >>> 8) & 0xFF);
		b[off] = (byte) (s & 0xFF);
	}

	/** flushes the stream */
	public void flush() throws IOException {
		flush(false);
	}

	/** writes a message in the underlying OutputStream */
	private void flush(boolean last) throws IOException {
		if (closed)
			throw new IOException("Disconnected");
		if (last) {
			buffer[dataOffset - 1] = (byte) 1;
			writeBuffer();
		} else {
			buffer[dataOffset - 1] = (byte) 0;
			writeBuffer();
			position = dataOffset;
			writeShort(msgID, buffer, 0);
		}
	}

	private void writeBuffer() throws IOException {
		writeShort((short) (position - dataOffset), buffer, 2);
		os.write(buffer, 0, position);
		os.flush();
	}

	/** locks the stream & sets the current message id */
	public synchronized void begin(short msgID) {
		while (locked) {
			waiting++;
			try {
				wait();
			} catch (Exception ignore) {
			} // IllegalMonitorStateException - impossible
			// InterruptedException
			waiting--;
		}
		if (closed)
			return;
		this.msgID = msgID;
		locked = true;
		position = dataOffset;
		writeShort(this.msgID, buffer, 0);
	}

	protected synchronized short begin(PMPAnswer answer) {
		if (closed)
			return -1;
		while (locked) {
			waiting++;
			try {
				wait();
			} catch (Exception ignore) {
			}
			waiting--;
		}
		if (closed)
			return -1;
		locked = true;

		if (++clientMsgID <= 0)
			clientMsgID = 1;
		msgID = clientMsgID;
		try {
			writeShort(msgID, buffer, 0);
		} catch (Exception ignore) { // should not happen ...
		}
		position = dataOffset;
		if (answer != null) {
			synchronized (answers) {
				answers.put(new Short(msgID), answer);
			}
		}
		return msgID;
	}

	public void end(boolean checkClosed) throws IOException {
		try {
			flush(true);
		} catch (Exception exc) {
		} finally {
			unlock();
		}
		if (checkClosed && closed) {
			if (ioExc == null)
				ioExc = new IOException("Disconnected");
			throw ioExc;
		}
	}

	/** unlocks the stream */
	protected synchronized void unlock() {
		locked = false;
		if (waiting > 0) {
			notifyAll();
		}
	}

	public void close() {
		try {
			this.os.close();
		} catch (Exception ignore) {
		}
		try {
			synchronized (answers) {
				closed = true;
				for (Iterator it = answers.values().iterator(); it.hasNext();) {
					PMPAnswer tmp = (PMPAnswer) it.next();
					tmp.errMsg = "Connection is closed";
					tmp.finish();
					// tmp.free();
					tmp = null;
				}
				answers.clear();
			}
			unlock();
		} catch (Exception exc) {
		}
	}

	protected void checkWaitStatus(String msg) {
		if (ping)
			c.disconnect(msg, false);
		synchronized (answers) {
			for (Iterator it = answers.values().iterator(); it.hasNext();) {
				PMPAnswer answer = (PMPAnswer) it.next();
				if (answer.waiting) {
					try {
						answer.errMsg = "Connection is closing: " + msg;
						answer.finish();
						closed = true;
						c.disconnect(msg, false);
						return;
					} catch (Exception exc) {
					}
				}
			}
		}
		unlock();
		ping = true;
		try {
			c.ping();
		} catch (Exception exc) {
			c.disconnect(exc.getMessage(), false);
		}
	}
}

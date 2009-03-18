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

import org.tigris.mtoolkit.iagent.rpc.Remote;

public class RemoteInputStream implements Remote {

	private InputStream is;
	private byte[] buff = new byte[2048];
	private int off = 0;
	private int len = 0;
	private boolean eof = false;

	public RemoteInputStream(InputStream is) {
		this.is = is;
	}

	public synchronized String readln() throws IOException {
		return readln(null);
	}

	private String readln(StringBuffer sbuffer) throws IOException {
		if (len == off) {
			if (eof)
				return sbuffer == null ? null : sbuffer.toString();
			fill();
			return readln(sbuffer);
		}
		boolean two = false;
		int i = off;
		for (; i < len; i++) {
			if (buff[i] == '\n')
				break;
			if (buff[i] == '\r') {
				if (buff[i + 1] == '\n')
					two = true;
				break;
			}
		}
		if (sbuffer == null)
			sbuffer = new StringBuffer(new String(buff, off, i - off));
		else
			sbuffer.append(new String(buff, off, i - off));
		if (i == 2048) {
			fill();
			return readln(sbuffer);
		}
		off = two ? i + 2 : i + 1;
		return sbuffer.toString();
	}

	public synchronized byte[] read(int size) throws IOException {
		if (len == off) {
			if (eof)
				return null;
			fill();
			return read(size);
		}
		int s = size < len - off ? len - off : size;
		byte[] toReturn = new byte[s];
		System.arraycopy(buff, off, toReturn, 0, s);
		off += s;
		return toReturn;
	}

	private void fill() throws IOException {
		off = len = 0;
		do {
			int read = 0;
			try {
				read = is.read(buff, len, 2048 - len);
			} catch (IOException ioExc) {
				close();
				throw ioExc;
			}
			if (read == -1)
				eof = true;
			else {
				len += read;
				if (len == 2048)
					break;
			}
		} while (!eof);
	}

	public synchronized void close() {
		try {
			is.close();
		} catch (IOException ignore) {
		}
		off = len = 0;
		eof = true;
	}

	public static Class[] remote = { RemoteInputStream.class };

	public Class[] remoteInterfaces() {
		return remote;
	}
}

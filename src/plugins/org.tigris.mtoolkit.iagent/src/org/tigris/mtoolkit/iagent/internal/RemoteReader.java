/*******************************************************************************
 * Copyright (c) 2005, 2010 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal;

import java.io.IOException;
import java.io.InputStream;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;

/**
 * Helper class for reading big data (files for example) from the client
 * (actually from RemoteInputStream on the remote side). This class doesn't
 * block the PMPInputStream between reads.
 * 
 * @see RemoteInputStream
 */
public class RemoteReader extends InputStream {
	private static MethodSignature READ_METHOD = new MethodSignature("read", new String[] { "int" }, true);
	private static MethodSignature CLOSE_METHOD = new MethodSignature("close");

	private RemoteObject remoteInput;

	public RemoteReader(RemoteObject remoteInput) {
		if (remoteInput == null) {
			throw new IllegalArgumentException("Non-null remote object expected");
		}
		this.remoteInput = remoteInput;
	}

	public synchronized int read() throws IOException {
		byte[] b = new byte[1];
		if (read(b) < 0) {
			return -1;
		}
		return b[0] & 0xFF;
	}

	public synchronized int read(byte[] buff, int off, int len) throws IOException {
		try {
			byte[] temp = (byte[]) READ_METHOD.call(remoteInput, new Integer(len));
			if (temp == null) {
				return -1;
			}
			System.arraycopy(temp, 0, buff, off, temp.length);
			return temp.length;
		} catch (IAgentException e) {
			throw new IOException("Failed to read data: " + e.getMessage());
		}
	}

	/**
	 * Closes the remote stream and disposes the remote object.
	 */
	public synchronized void close() {
		try {
			CLOSE_METHOD.call(remoteInput);
		} catch (IAgentException e) {
			DebugUtils.info(this, "Failed to close remote input stream: " + e.getMessage());
		}
		try {
			remoteInput.dispose();
		} catch (PMPException e) {
			DebugUtils.info(this, "Failed to dispose remote object: " + e.getMessage());
		}
	}
}

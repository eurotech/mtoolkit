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
package org.tigris.mtoolkit.common;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads data from passed InputStream and collects it to buffer.
 */
public class ProcessOutputReader extends Thread {
	private volatile boolean stopReader = false;
	private InputStream inputStream;
	private StringBuffer output = new StringBuffer();

	public ProcessOutputReader(InputStream inputStream, String name) {
		super(name);
		this.inputStream = inputStream;
	}

	public void run() {
		try {
			byte[] buf = new byte[4096];
			int len = inputStream.available();
			while (len != -1 && !stopReader) {
				len = inputStream.read(buf);
				if (len > 0) {
					output.append(new String(buf, 0, len));
				}
				len = inputStream.available();
			}
		} catch (IOException io) {
		}
	}

	public void stopReader() {
		stopReader = true;
	}

	public String getOutput() {
		return output.toString();
	}
}

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
package org.tigris.mtoolkit.osgimanagement.internal.console;

public class StreamBuffer implements ConsoleListener {

	private final static int MAX_CAPACITY = 1024000;
	private StringBuffer sb = new StringBuffer();

	public StreamBuffer() {
	}

	public void append(String text) {
		int len = sb.length() + text.length();
		if (len > MAX_CAPACITY) {
			sb.delete(0, len - MAX_CAPACITY);
		}
		sb.append(text);
	}

	public String get() {
		return sb.toString();
	}

	public void clear() {
		sb.setLength(0);
	}

	public void dumpText(String text) {
		append(text);
	}

	public void appendText(String text) {
		append(text);
	}

	public void consoleTerminated() {
		clear();
	}

	public void disconnected() {
	}

}

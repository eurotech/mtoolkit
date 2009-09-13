/*******************************************************************************
 * Copyright (c) 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.internal.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleInputStream;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.VMManager;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;

public class ConsoleReader implements Runnable {

	private volatile boolean running = true;
	private IOConsoleInputStream input;
	private BufferedReader reader;
	private VMManager manager;
	private Thread thread;
	
	public ConsoleReader(IOConsole console, VMManager manager) {
		this.manager = manager;
		this.input = console.getInputStream();
		try {
			this.reader = new BufferedReader(new InputStreamReader(this.input, console.getEncoding()));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("Console has unsupported encoding");
		}
		thread = new Thread(this, "Remote Console - " + console.getName());
		thread.setDaemon(true);
		thread.start();
	}
	
	public void dispose() {
		running = false;
		thread.interrupt();
		try {
			input.close();
		} catch (IOException e) {
		}
	}
	
	public void run() {
		while (running) {
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					manager.executeFrameworkCommand(line);
				}
			} catch (IOException e) {
				FrameworkPlugin.error("Exception while reading user input", e);
			} catch (IAgentException e) {
				FrameworkPlugin.error("Failed to execute remote command", e);
			}
		}
	}

}

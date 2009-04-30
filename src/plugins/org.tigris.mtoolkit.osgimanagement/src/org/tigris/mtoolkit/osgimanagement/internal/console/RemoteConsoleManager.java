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

import java.io.IOException;
import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;

public class RemoteConsoleManager extends ConsoleManager {

	private DeviceConnector connector;
	private ServerConsole scon;
	private Object lock = new Object();

	public RemoteConsoleManager(Console console, DeviceConnector connector, ServerConsole sconsole) {
		super(console);
		scon = sconsole;
		reconnect(connector);
	}

	public void freeResources(boolean stopLocal) {
	}

	public void freeResources() {
	}

	public void execute(final String command) {
		synchronized (lock) {
			try {
				connector.getVMManager().executeFrameworkCommand(command);
			} catch (IAgentException e) {
				BrowserErrorHandler.processError(e, connector);
			}
		}
	}

	public void disconnected() {
		scon.disconnected();
	}

	public void reconnect(final DeviceConnector connector) {
		this.connector = connector;

		// to avoid GUI blocking if VMManager blocks for long time
		new Thread() {
			public void run() {
				try {
					connector.getVMManager().redirectFrameworkOutput(new ConsoleOutputStream());
				} catch (IAgentException e) {
					BrowserErrorHandler.processError(e, connector);
					scon.disconnect();
				}
			}
		}.start();
	}

	public class ConsoleOutputStream extends OutputStream {

		public void write(int b) throws IOException {
			write(new byte[b], 0, 1);
		}

		public void write(byte b[]) throws IOException {
			write(b, 0, b.length);
		}

		public void write(byte b[], int off, int len) throws IOException {
			String text = new String(b, off, len);
			scon.getConsoleListener().dumpText(text);
		}

	}
}
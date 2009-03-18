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

/**
 * This abstract class is responsible to the redirecting of the commands of the
 * Console.
 */
public abstract class ConsoleManager implements ConsoleListener {

	/**
	 * An instance of the Console component.
	 */
	protected Console console;

	/**
	 * Creates new Console Manager object. Initializes the socket console.
	 * 
	 * @param console
	 *            an instance of the remote console.
	 */
	public ConsoleManager(Console console) {
		this.console = console;
	}

	/**
	 * This method will be called when a command has been received. This method
	 * should process the remote execution of the command.
	 * 
	 * @param command
	 *            the command for executing.
	 */
	public abstract void execute(String command);

	public abstract void freeResources();

	public abstract void freeResources(boolean stopLocal);

	public void clear() {
		console.setText(""); //$NON-NLS-1$
	}

	/**
	 * Dumps the specified text in the console.
	 * 
	 * @param text
	 *            the text which will be displayed in the console.
	 */
	public void dumpText(String text) {
		if ((console != null) && !console.isDisposed()) {
			console.insertReply(text);
		}
	}

	public void appendText(String text) {
		if ((console != null) && !console.isDisposed()) {
			console.appendText(console.getLineDelimiter() + text);
		}
	}

	public void consoleTerminated() {
	}

	public void disconnected() {
	}
}
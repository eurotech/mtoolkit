package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import org.eclipse.jface.action.Action;
import org.tigris.mtoolkit.osgimanagement.internal.console.Console;

public class StateConsoleAction extends Action {
	private Console console;

	public StateConsoleAction(String label, Console console) {
		super(label);
		this.console = console;
	}

	public void run() {
		console.switchState();
	}
}

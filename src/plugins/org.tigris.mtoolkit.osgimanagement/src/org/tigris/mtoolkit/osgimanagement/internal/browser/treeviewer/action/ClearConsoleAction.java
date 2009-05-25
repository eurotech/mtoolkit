package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import org.eclipse.jface.action.Action;
import org.tigris.mtoolkit.osgimanagement.internal.console.Console;

public class ClearConsoleAction extends Action {
	private Console console;

	public ClearConsoleAction(String text, Console console) {
		super(text);
		this.console = console;
	}

	public void run() {
		console.clear();
	}

}

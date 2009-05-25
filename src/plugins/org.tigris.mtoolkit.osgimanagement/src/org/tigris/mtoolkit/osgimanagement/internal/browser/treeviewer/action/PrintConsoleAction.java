package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import org.eclipse.jface.action.Action;
import org.tigris.mtoolkit.osgimanagement.internal.console.Console;

public class PrintConsoleAction extends Action {

	private Console console;

	public PrintConsoleAction(String label, Console console) {
		super(label);
		this.console = console;
	}

	public void run() {
		org.tigris.mtoolkit.common.PluginUtilities.print(console.getShell(), console.getStyledText());
	}
}

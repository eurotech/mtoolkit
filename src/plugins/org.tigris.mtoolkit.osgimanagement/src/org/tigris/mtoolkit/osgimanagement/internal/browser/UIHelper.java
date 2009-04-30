package org.tigris.mtoolkit.osgimanagement.internal.browser;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class UIHelper {

	public static int openWindow(final Window window) {
		Display display = Display.getDefault();
		final int[] result = new int[1];
		display.syncExec(new Runnable() {
			public void run() {
				result[0] = window.open();
			}
		});
		return result[0];
	}
}

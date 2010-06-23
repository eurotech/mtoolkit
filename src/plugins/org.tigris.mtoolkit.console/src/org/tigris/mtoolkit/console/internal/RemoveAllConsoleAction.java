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
package org.tigris.mtoolkit.console.internal;

import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;

public class RemoveAllConsoleAction extends Action implements IConsoleListener {

	private static RemoveAllConsoleAction instance;
	
	public static RemoveAllConsoleAction getSingleton() {
		if (instance == null) {
			instance = new RemoveAllConsoleAction();
			ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(instance);
		}
		return instance;
	}
	
	public RemoveAllConsoleAction() {
		super("Remove All Disconnected");
		
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE_ALL));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
		
		updateState();
	}
	
	public void run() {
		IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
		for (int i = 0; i < consoles.length; i++) {
			if (!(consoles[i] instanceof RemoteConsole))
				continue;
			if (!((RemoteConsole) consoles[i]).isDisconnected())
				continue;
			ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] { consoles[i] });
		}
	}
	
	public void updateState() {
		IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
		for (int i = 0; i < consoles.length; i++) {
			if (!(consoles[i] instanceof RemoteConsole))
				continue;
			if (!((RemoteConsole) consoles[i]).isDisconnected())
				continue;
			setEnabled(true);
			return;
		}
		setEnabled(false);
		return;
	}

	public void consolesAdded(IConsole[] consoles) {
	}

	public void consolesRemoved(IConsole[] consoles) {
		for (int i = 0; i < consoles.length; i++) {
			if (consoles[i] instanceof RemoteConsole) {
				Display.getDefault().syncExec(new Runnable() {
					public void run() {
						updateState();
					}
				});
				break;
			}
		}
	}
	
}

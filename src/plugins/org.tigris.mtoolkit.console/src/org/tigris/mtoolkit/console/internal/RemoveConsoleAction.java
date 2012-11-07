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
package org.tigris.mtoolkit.console.internal;

import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.tigris.mtoolkit.console.utils.Messages;

public final class RemoveConsoleAction extends Action {
  private RemoteConsole console;

  public RemoveConsoleAction(RemoteConsole console) {
    super(Messages.RemoveConsoleAction_Remove_Console);
    this.console = console;
    setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE));
    setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE));
    setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE));
    updateState();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    RemoteConsole console = null;
    synchronized (this) {
      console = this.console;
    }
    if (console == null || !console.isDisconnected()) {
      return;
    }
    ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] {
      console
    });
    updateState();
  }

  public void updateState() {
    RemoteConsole console = null;
    synchronized (this) {
      console = this.console;
    }
    setEnabled(console != null && console.isDisconnected());
  }

  public synchronized void dispose() {
    console = null;
  }
}

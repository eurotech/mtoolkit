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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;

public final class RemoveAllConsoleAction extends Action implements IConsoleListener {

  public RemoveAllConsoleAction() {
    super(Messages.RemoveAllConsoleAction_Remove_All_Disconnected);
    setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
    setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE_ALL));
    setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_REMOVE_ALL));
    ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(this);
    updateState();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.action.Action#run()
   */
  @Override
  public void run() {
    IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
    for (int i = 0; i < consoles.length; i++) {
      if (!(consoles[i] instanceof RemoteConsole)) {
        continue;
      }
      if (!((RemoteConsole) consoles[i]).isDisconnected()) {
        continue;
      }
      ConsolePlugin.getDefault().getConsoleManager().removeConsoles(new IConsole[] {
        consoles[i]
      });
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.IConsoleListener#consolesAdded(org.eclipse.ui.console.IConsole[])
   */
  public void consolesAdded(IConsole[] consoles) {
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.IConsoleListener#consolesRemoved(org.eclipse.ui.console.IConsole[])
   */
  public void consolesRemoved(IConsole[] consoles) {
    for (int i = 0; i < consoles.length; i++) {
      if (consoles[i] instanceof RemoteConsole) {
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (!display.isDisposed()) {
          display.asyncExec(new Runnable() {
            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            public void run() {
              updateState();
            }
          });
        }
        break;
      }
    }
  }

  public void dispose() {
    ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
  }

  public void updateState() {
    IConsole[] consoles = ConsolePlugin.getDefault().getConsoleManager().getConsoles();
    for (int i = 0; i < consoles.length; i++) {
      if (!(consoles[i] instanceof RemoteConsole)) {
        continue;
      }
      if (!((RemoteConsole) consoles[i]).isDisconnected()) {
        continue;
      }
      setEnabled(true);
      return;
    }
    setEnabled(false);
    return;
  }
}

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

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

public final class ConsolePageParticipant implements IConsolePageParticipant, IPropertyChangeListener {
  private RemoteConsole console;
  private RemoveConsoleAction removeAction;
  private RemoveAllConsoleAction removeAllAction;

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.IConsolePageParticipant#activated()
   */
  public void activated() {
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.IConsolePageParticipant#deactivated()
   */
  public void deactivated() {
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.IConsolePageParticipant#dispose()
   */
  public void dispose() {
    this.console.removePropertyChangeListener(this);
    removeAllAction.dispose();
    removeAction.dispose();
  }

  /* (non-Javadoc)
   * @see org.eclipse.ui.console.IConsolePageParticipant#init(org.eclipse.ui.part.IPageBookViewPage, org.eclipse.ui.console.IConsole)
   */
  public void init(IPageBookViewPage page, IConsole console) {
    if (!(console instanceof RemoteConsole)) {
      return;
    }
    this.console = (RemoteConsole) console;
    this.console.addPropertyChangeListener(this);

    removeAction = new RemoveConsoleAction(this.console);
    removeAllAction = new RemoveAllConsoleAction();

    configureToolBar(page.getSite().getActionBars().getToolBarManager());
  }

  /* (non-Javadoc)
   * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
   */
  public Object getAdapter(Class adapter) {
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent event) {
    if (event.getProperty().equals(RemoteConsole.P_DISCONNECTED)) {
      removeAction.updateState();
      removeAllAction.updateState();
    }
  }

  private void configureToolBar(IToolBarManager mng) {
    mng.appendToGroup(IConsoleConstants.LAUNCH_GROUP, removeAction);
    mng.appendToGroup(IConsoleConstants.LAUNCH_GROUP, removeAllAction);
  }
}

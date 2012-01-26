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
package org.tigris.mtoolkit.common.installation;

import org.eclipse.swt.widgets.Shell;

public interface TargetSelectionDialog {
  /**
   * Gets selected installation target  
   * @return selected installation target or null if none
   */
  public InstallationTarget getSelectedTarget(Shell parentShell);
}

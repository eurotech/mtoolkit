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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class ObjectClass extends Model {

  private Long          nameID;
  private RemoteService service;

  public ObjectClass(String name, Long nameID, RemoteService service) {
    super(name);
    this.nameID = nameID;
    this.service = service;
  }

  public Long getNameID() {
    return nameID;
  }

  /**
   * @return
   */
  public RemoteService getService() {
    return service;
  }
}

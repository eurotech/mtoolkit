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

import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class ServicesCategory extends Model {
  public static final int      REGISTERED_SERVICES = 0;
  public static final int      USED_SERVICES       = 1;

  public static final String[] nodes               = {
      Messages.registered_services, Messages.services_in_use
                                                   };

  private int                  type;

  public ServicesCategory(int type) {
    super(getTitle(type));
    this.type = type;
  }

  public int getType() {
    return type;
  }

  private static String getTitle(int type) {
    if ((type > 2) || (type < 0)) {
      type = 0;
    }
    return nodes[type];
  }
}

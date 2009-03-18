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

public class ServicesCategory extends Model {

  private int type;
  public static String[] nodes;
  public static int REGISTERED = 0;
  public static int IN_USE = 1;

  static {
    nodes = new String[2];
    nodes[0] = Messages.registered_services;
    nodes[1] = Messages.services_in_use;
  }

  /**
	 * @param type
	 */
	private static String getTitle(int type) {
		if ((type > 2) || (type < 0)) {
      type = 0;
		}
		return nodes[type];
	}


  public ServicesCategory(Model parent, int type) {
    super(getTitle(type), parent);
    this.type = type; 
  }


	public int getKind() {
		return type;
	}
}
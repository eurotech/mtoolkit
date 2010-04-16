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
package org.tigris.mtoolkit.osgimanagement;

import java.util.Dictionary;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;

public interface DeviceTypeProvider {

	/**
	 * @since 5.0
	 */
	public Control createPanel(Composite parent, DeviceTypeProviderValidator validator);
	
	public void setProperties(IMemento config);

	/**
	 * @since 5.0
	 */
	public String validate();
	
	public Dictionary load(IMemento config);

	public void save(IMemento config);

	public String getTransportType();
	
	public void setEditable(boolean editable);
}

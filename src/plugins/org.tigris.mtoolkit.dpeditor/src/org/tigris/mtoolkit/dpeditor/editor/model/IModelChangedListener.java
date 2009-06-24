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
package org.tigris.mtoolkit.dpeditor.editor.model;

public interface IModelChangedListener {
	/**
	 * Called when there is a change in the model this listener is registered
	 * with.
	 * 
	 * @param event
	 *            a change event that describes the kind of the model change
	 */
	public void modelChanged(IModelChangedEvent event);

}

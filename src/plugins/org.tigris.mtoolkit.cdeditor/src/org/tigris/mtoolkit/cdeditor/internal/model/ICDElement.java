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
package org.tigris.mtoolkit.cdeditor.internal.model;


/**
 * Base interface for all elements in one component description.
 */
public interface ICDElement {

	public ICDElement getParent();

	public void fireModified(CDModelEvent event);

	/**
	 * Returns the model to which this element belongs.
	 * @return the model
	 */
	public ICDModel getModel();

	/**
	 * This method will use information from the passed event and will either
	 * reapply the change described in the event or reverse it depending on the
	 * value of reverse parameter.
	 * 
	 * @param event
	 *            the event which describe the change
	 * @param reverse
	 *            true if the change should be undone or false if the change
	 *            should be reapplied.
	 */
	public void executeOperation(CDModelEvent event, boolean reverse);

	public String print();
}

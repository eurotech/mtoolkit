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

public interface IModelChangeProvider {
	/**
	 * Adds the listener to the list of listeners that will be notified on model
	 * changes.
	 */
	public void addModelChangedListener(IModelChangedListener listener);

	/**
	 * Delivers change event to all the registered listeners.
	 * 
	 * @param event
	 *            a change event that will be passed to all the listeners
	 */
	public void fireModelChanged(IModelChangedEvent event);

	/**
	 * Notifies listeners that a property of a model object changed. This is a
	 * utility method that will create a model event and fire it.
	 * 
	 * @param object
	 *            an affected model object
	 * @property name of the property that has changed
	 */
	public void fireModelObjectChanged(Object object, String property);

	/**
	 * Takes the listener off the list of registered change listeners.
	 * 
	 * @param listener
	 *            the listener to be removed
	 */
	public void removeModelChangedListener(IModelChangedListener listener);
}

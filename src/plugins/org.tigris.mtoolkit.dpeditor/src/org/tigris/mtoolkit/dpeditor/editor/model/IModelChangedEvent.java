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

public interface IModelChangedEvent {

	/**
	 * Indicates a change where one or more objects are added to the model.
	 */
	int ADD = 1;
	/**
	 * Indicates a change where one or more new objects are added to the model.
	 */
	int INSERT = 2;

	/**
	 * Indicates a change where one or more objects are removed from the model.
	 */
	int REMOVE = 3;

	/**
	 * Indicates that the model has been reloaded and that listeners should
	 * perform full refresh.
	 */
	int WORLD_CHANGED = 99;

	/**
	 * indicates that a model object's property has been changed.
	 */
	int CHANGE = 4;

	/**
	 * indicates that a model object's should be edited.
	 */
	int EDIT = 5;

	/**
	 * Returns an array of model objects that are affected by the change.
	 * 
	 * @return array of affected objects
	 */
	public Object[] getChangedObjects();

	/**
	 * Returns a name of the object's property that has been changed if change
	 * type is CHANGE.
	 * 
	 * @return property that has been changed in the model object, or
	 *         <samp>null</samp> if type is not CHANGE or if more than one
	 *         property has been changed.
	 */
	public String getChangedProperty();

	/**
	 * Returns the type of change that occured in the model (one of INSERT,
	 * REMOVE, CHANGE or WORLD_CHANGED).
	 * 
	 * @return type of change
	 */
	public int getChangeType();
}

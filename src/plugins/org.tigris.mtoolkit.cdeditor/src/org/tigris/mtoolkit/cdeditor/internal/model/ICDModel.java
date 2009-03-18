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

import org.eclipse.core.runtime.IStatus;


/**
 * This interface represents the model of a declarative services. It can 
 * contain one or multiple components. It also maintains all actions in the 
 * model e.g. adding/removing components, raising appropriate events etc.
 */
public interface ICDModel extends ICDElement {

	public ICDComponent getSingleComponent();

	public void setSingleComponent(ICDComponent component);

	public boolean isSingle();

	public void setSingle(boolean single);

	public ICDComponent[] getComponents();

	public void addComponent(ICDComponent component);

	public void removeComponent(int index);

	public void removeComponent(ICDComponent component);

	public boolean registerComponent(ICDComponent component);

	public boolean unregisterComponent(ICDComponent component);

	public boolean canMoveComponentDown(ICDComponent component);

	public boolean canMoveComponentUp(ICDComponent component);

	public void moveDownComponent(ICDComponent component);

	public void moveUpComponent(ICDComponent component);

	public void addModifyListener(ICDModifyListener listener);

	public void removeModifyListener(ICDModifyListener listener);

	public boolean isDirty();

	public void setDirty(boolean dirtyState);

	/**
	 * <p>
	 * Returns the status of the validation for the passed element. The returned
	 * array of IStatus objects contains only the errors and warnings for the
	 * selected element. Validation issues for its childrens are not included.
	 * </p>
	 * <p>
	 * The returned array is sorted in descending order of statuses' severity -
	 * first are located the errors followed by warnings and any INFO statuses.
	 * </p>
	 * 
	 * @param element
	 *            the ICDElement object, whose validation status is required
	 * @return an array of IStatus objects containing any errors and warnings,
	 *         result from the element validation. If there are no issues found,
	 *         then an array with single OK element is returned.
	 */
	public IStatus[] getValidationStatus(ICDElement element);

	/**
	 * <p>
	 * Returns the status of the validation for the passed element and its
	 * childrens. The returned array will contain at least one error, warning or
	 * OK status indicating that there are no problems found.
	 * </p>
	 * 
	 * <p>
	 * The array is sorted in descending order - first are listed the errors
	 * followed by the warnings and in the end any INFO statuses found. This
	 * sorting is done for all of the aggregated statuses regardless the origin
	 * of the status.
	 * </p>
	 * 
	 * @param element
	 *            the ICDElement, whose validation status is queried
	 * @return an array of IStatus objects, indicating any errors and warnings
	 *         found in the passed ICDElement hierarchy. If there are no
	 *         problems found, an array with single OK element is returned.
	 */
	public IStatus[] getAggregatedValidationStatus(ICDElement element);

	public void validate();

	public void setEclipseContext(IEclipseContext projectContext);

	public IEclipseContext getProjectContext();
}

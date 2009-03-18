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
package org.tigris.mtoolkit.cdeditor.internal.parts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.IFormPart;

/**
 * This FormPart aggregates the lifecycle of multiple FormParts including
 * itself.
 * 
 */
public abstract class AggregatedFormPart extends AbstractFormPart {

	private List parts = new ArrayList();

	public void commit(boolean onSave) {
		for (Iterator it = parts.iterator(); it.hasNext();) {
			IFormPart part = (IFormPart) it.next();
			if (part.isDirty())
				part.commit(onSave);
		}
		doCommit(onSave);
		super.commit(onSave);
	}

	public boolean isDirty() {
		boolean partsDirty = false;
		for (Iterator it = parts.iterator(); it.hasNext();) {
			IFormPart part = (IFormPart) it.next();
			if (part.isDirty()) {
				partsDirty = true;
				break;
			}
		}
		return partsDirty || super.isDirty();
	}

	public boolean isStale() {
		boolean partsStale = false;
		for (Iterator it = parts.iterator(); it.hasNext();) {
			IFormPart part = (IFormPart) it.next();
			if (part.isStale()) {
				partsStale = true;
				break;
			}
		}
		return partsStale || super.isStale();
	}

	public void refresh() {
		for (Iterator it = parts.iterator(); it.hasNext();) {
			IFormPart part = (IFormPart) it.next();
			if (part.isStale())
				part.refresh();
		}
		doRefresh();
		super.refresh();
	}

	public void addPart(IFormPart part) {
		Assert.isNotNull(part);
		part.initialize(getManagedForm());
		parts.add(part);
	}

	protected List getParts() {
		return parts;
	}

	protected abstract void doCommit(boolean onSave);

	protected abstract void doRefresh();
}

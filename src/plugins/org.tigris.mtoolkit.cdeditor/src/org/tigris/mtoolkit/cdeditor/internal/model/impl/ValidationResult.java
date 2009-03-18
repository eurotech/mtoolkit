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
package org.tigris.mtoolkit.cdeditor.internal.model.impl;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.internal.core.text.IDocumentRange;

public class ValidationResult {

	private IStatus status;
	private IDocumentRange element;
	private int offset = -1;
	private int length = -1;

	public ValidationResult(IStatus status, IDocumentRange element, int offset, int length) {
		Assert.isNotNull(status);
		this.status = status;
		this.element = element;
		this.length = length;
		this.offset = offset;
	}
	
	public ValidationResult(IStatus status, IDocumentRange element) {
		this(status, element, element.getOffset(), element.getLength());
	}

	public IStatus getStatus() {
		return status;
	}

	public IDocumentRange getElement() {
		return element;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}

	public String toString() {
		return status.toString() + " (" + element.getOffset() + ", " + element.getLength() + ")";
	}
}

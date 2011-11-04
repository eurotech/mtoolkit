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
package org.tigris.mtoolkit.util;

/**
 * Exceptions of this class are thrown when there is inconsistent data in a
 * DPPFile object. For example when a bundle doesn't have a symbolic name or
 * version, or when a package header does not have a value.
 * 
 * @author todor
 * 
 */
public class InconsistentDataException extends Exception {

	/**
   * 
   */
  private static final long serialVersionUID = 123456L;

  /**
	 * Constructs a new InconsistentDataException without message
	 * 
	 */
	public InconsistentDataException() {
		super();
	}

	/**
	 * Constructs a new InconsistentDataException object that has the specified
	 * message
	 * 
	 * @param message
	 *            the message associated to this exception.
	 */
	public InconsistentDataException(String message) {
		super(message);
	}

}

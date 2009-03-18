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
package org.tigris.mtoolkit.iagent.pmp;

/**
 * Exception that represents an error in the pmp network protocol.
 */

public class PMPException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8711456364519497394L;

	private Throwable cause;

	/**
	 * Constructs a <code>PMPexception</code> with a specified error message.
	 * 
	 * @param errMsg
	 *            the error message
	 */
	public PMPException(String errMsg) {
		super(errMsg);
	}

	/**
	 * Constructs a <code>PMPexception</code> with a specified error message and
	 * basic exception.
	 * 
	 * @param errMsg
	 *            the error message
	 * @param exc
	 *            the Exception
	 */
	public PMPException(String errMsg, Throwable exc) {
		super(errMsg);
		cause = exc;
	}

	/**
	 * Returns the basic exception.
	 */
	public Throwable getCause() {
		return cause;
	}

}

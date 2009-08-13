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
package org.tigris.mtoolkit.iagent;

import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * An exception class which is thrown when some error occur during operations
 * execution.<br>
 * 
 * @version 1.0
 * @see IAgentErrors
 */
public class IAgentException extends Exception {

	private static final long serialVersionUID = 3593878826474004840L;
	private int errorCode;
	private Throwable causeException;

	/**
	 * Creates new {@link IAgentException} object with specified message.
	 * 
	 * @param aMessage
	 *            the message of this exception
	 */
	public IAgentException(String aMessage, int aErrorCode) {
		this(aMessage, aErrorCode, null);
	}

	public IAgentException(Error error) {
		super(error.getMessage());
		this.errorCode = error.getCode();
	}

	public IAgentException(String aMessage, int errorCode, Throwable aCause) {
		super(aMessage);
		this.errorCode = errorCode;
		this.causeException = aCause;
	}

	/**
	 * Returns the code of this exception
	 * 
	 * @return int which could be used to easily determine the reason for this
	 *         exception
	 */
	public int getErrorCode() {// TODO add code constants
		return errorCode;
	}

	/**
	 * Returns the nested exception
	 * 
	 * @return the exception, which is the reason of this exception
	 */
	public Throwable getCauseException() {
		return causeException;
	}

	public void printStackTrace(PrintStream s) {
		super.printStackTrace(s);
		if (causeException != null) {
			s.println("Nested exception:");
			causeException.printStackTrace(s);
		}
	}

	public void printStackTrace(PrintWriter s) {
		super.printStackTrace(s);
		if (causeException != null) {
			s.println("Nested exception:");
			causeException.printStackTrace(s);
		}
	}

}

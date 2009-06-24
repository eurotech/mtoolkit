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
package org.tigris.mtoolkit.dpeditor.editor.utils;

public class IncorrectDPPException extends Exception {

	/** Nested exception for holding wrapped exception */
	protected Throwable nested = null;
	protected String details;

	/**
	 * Construct a <code>IncorrectDPPException</code> with no specified detail
	 * message.
	 */

	public IncorrectDPPException() {
		super();
	}

	/**
	 * Construct <code>IncorrectDPPException</code> with the specified detail
	 * message.
	 * 
	 * @param msg
	 *            the detail message
	 */

	public IncorrectDPPException(String msg) {
		super(msg);
	}

	public IncorrectDPPException(String msg, String details) {
		super(msg);
		this.details = details;
	}

	/**
	 * Construct <code>IncorrectDPPException</code> with no specified detail
	 * message and the specified nested exception.
	 * 
	 * @param msg
	 *            the detail message
	 * @param th
	 *            the nested exception
	 */

	public IncorrectDPPException(Throwable th) {
		super();
		this.nested = th;
	}

	/**
	 * Construct <code>IncorrectDPPException</code> with the specified detail
	 * message and nested exception.
	 * 
	 * @param msg
	 *            the detail message
	 * @param th
	 *            the nested exception
	 */

	public IncorrectDPPException(String msg, Throwable th) {
		super(msg);
		this.nested = th;
	}

	public void setDetails(String details) {
		this.details = details;
	}

	/**
	 * Return the nested exception object.
	 */

	public Throwable getNested() {
		return nested;
	}

	/**
	 * Return the detail message, including the message from the nested
	 * exception if there is one.
	 */

	public String getMessage() {
		String message;
		message = super.getMessage();
		if (nested != null) {
			message += "; nested exception is: \n\t" + nested.toString();
		}
		return message;
	}

	/**
	 * Print the composite message and the embedded stack trace to the specified
	 * stream <code>ps</code>.
	 * 
	 * @param ps
	 *            the print stream
	 */

	public void printStackTrace(java.io.PrintStream ps) {
		if (nested == null) {
			super.printStackTrace(ps);
		} else {
			synchronized (ps) {
				ps.println(this);
				nested.printStackTrace(ps);
			}
		}
	}

	/**
	 * Print the composite message to <code>System.err</code>.
	 */

	public void printStackTrace() {
		printStackTrace(System.err);
	}

	/**
	 * Print the composite message and the embedded stack trace to the specified
	 * print writer <code>pw</code>
	 * 
	 * @param pw
	 *            the print writer
	 */

	public void printStackTrace(java.io.PrintWriter pw) {
		if (nested == null) {
			super.printStackTrace(pw);
		} else {
			synchronized (pw) {
				pw.println(this);
				nested.printStackTrace(pw);
			}
		}
	}
}

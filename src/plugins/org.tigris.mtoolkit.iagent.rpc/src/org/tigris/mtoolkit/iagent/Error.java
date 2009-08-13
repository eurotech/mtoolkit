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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.rpc.Externalizable;

public class Error implements Externalizable {

	/**
	 * This code is only valid with {@link #BUNDLE_EXCEPTION_TYPE} and means
	 * that the bundle has been previously uninstalled
	 */
	public static final int BUNDLE_UNINSTALLED_CODE = -10000;

	/**
	 * This code is only valid when type has value
	 * {@link #DEPLOYMENT_EXCEPTION_TYPE} and means that DeploymentPackage has
	 * been uninstalled or updated with different version
	 */
	public static final int DEPLOYMENT_UNINSTALLED_CODE = -10001;

	private static final byte CODE_PRESENT = 0x02;
	private static final byte MESSAGE_PRESENT = 0x04;
	private static final byte DETAILS_PRESENT = 0x01;

	private int code;
	private String message;
	private String details;

	public Error(int code, String message, String details) {
		this.code = code;
		this.message = message;
		this.details = details;
	}

	public Error(int code, String message) {
		this(code, message, null);
	}

	public Error() {
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public String getDetails() {
		return details;
	}

	public void readObject(InputStream is) throws Exception {
		DataInput in = new DataInputStream(is);
		byte objectHeader = in.readByte();

		if ((objectHeader & CODE_PRESENT) != 0)
			code = in.readInt();
		if ((objectHeader & MESSAGE_PRESENT) != 0)
			message = in.readUTF();
		if ((objectHeader & DETAILS_PRESENT) != 0)
			details = in.readUTF();
	}

	public void writeObject(OutputStream os) throws Exception {
		DataOutput out = new DataOutputStream(os);

		byte objectHeader = 0;
		if (code != 0)
			objectHeader |= CODE_PRESENT;
		if (message != null && message.length() > 0)
			objectHeader |= MESSAGE_PRESENT;
		if (details != null && details.length() > 0)
			objectHeader |= DETAILS_PRESENT;
		out.writeByte(objectHeader);

		if (code != 0)
			out.writeInt(code);
		if (message != null && message.length() > 0)
			out.writeUTF(message);
		if (details != null && details.length() > 0)
			out.writeUTF(details);
	}

	public String toString() {
		return "Error[code=" + code + ";message=" + message + ";details=" + details + "]";
	}
}

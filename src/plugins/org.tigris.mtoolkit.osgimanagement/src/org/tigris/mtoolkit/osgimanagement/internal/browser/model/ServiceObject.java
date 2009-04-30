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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import java.util.Hashtable;
import java.util.Vector;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteService;

public class ServiceObject {

	private RemoteService rService;
	private RemoteBundle rBundle;
	private RemoteBundle usedIn[];
	private boolean usedInInit = false;
	private String objectClass[];
	public static Hashtable usedInHashFWs = new Hashtable();

	public ServiceObject(RemoteService rService, RemoteBundle rBundle) {
		this.rService = rService;
		this.rBundle = rBundle;
	}

	public RemoteBundle getRegisteredIn() {
		return rBundle;
	}

	public RemoteBundle[] getUsedIn(FrameWork fw) throws IAgentException {
		if (!usedInInit) {
			Hashtable usedInHash = (Hashtable) usedInHashFWs.get(fw);
			// if still connecting
			if (usedInHash == null)
				return usedIn;
			Vector usedInVector = (Vector) usedInHash.get(new Long(rService.getServiceId()));
			if (usedInVector != null) {
				usedIn = (RemoteBundle[]) usedInVector.toArray(new RemoteBundle[0]);
			}
			usedInInit = true;
		}
		return usedIn;
	}

	public static void addUsedInBundle(RemoteService rService, RemoteBundle rBundle, FrameWork fw)
					throws IAgentException {
		Hashtable usedInHash = (Hashtable) usedInHashFWs.get(fw);
		if (usedInHash == null) {
			usedInHash = new Hashtable();
			usedInHashFWs.put(fw, usedInHash);
		}
		Vector usedInVector = (Vector) usedInHash.get(new Long(rService.getServiceId()));
		if (usedInVector == null) {
			usedInVector = new Vector();
			usedInHash.put(new Long(rService.getServiceId()), usedInVector);
		}
		if (!usedInVector.contains(rBundle)) {
			usedInVector.addElement(rBundle);
		}
	}

	public String[] getObjectClass() throws IAgentException {
		if (objectClass == null) {
			objectClass = rService.getObjectClass();
		}
		return objectClass;
	}

	public RemoteService getRemoteService() {
		return rService;
	}

}

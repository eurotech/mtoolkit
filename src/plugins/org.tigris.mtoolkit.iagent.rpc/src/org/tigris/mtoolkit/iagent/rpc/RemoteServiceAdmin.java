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
package org.tigris.mtoolkit.iagent.rpc;

import java.util.Dictionary;

public interface RemoteServiceAdmin {

	public static final String CUSTOM_SERVICE_EVENT = "iagent_service_event";

	public Dictionary[] getAllRemoteServices(String clazz, String filter);

	public Dictionary getProperties(long id);

	public long[] getUsingBundles(long id);

	public long getBundle(long id);

	public boolean isServiceStale(long id);

	public String checkFilter(String filter);

	public long getRemoteServiceID();
}

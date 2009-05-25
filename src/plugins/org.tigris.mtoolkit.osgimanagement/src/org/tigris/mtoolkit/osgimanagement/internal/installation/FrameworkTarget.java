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
package org.tigris.mtoolkit.osgimanagement.internal.installation;

import org.eclipse.jface.resource.ImageDescriptor;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.iagent.rpc.RemoteDeploymentAdmin;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;

public class FrameworkTarget implements InstallationTarget {
	private FrameWork fw;
	private static final String MIME_DP = "application/vnd.osgi.dp";

	public FrameworkTarget(FrameWork fw) {
		this.fw = fw;
	}

	public ImageDescriptor getIcon() {
		if (fw.isConnected()) {
			return ImageHolder.getImageDescriptor(ConstantsDistributor.SERVER_ICON_CONNECTED);
		}
		return ImageHolder.getImageDescriptor(ConstantsDistributor.SERVER_ICON_DISCONNECTED);
	}

	public String getName() {
		return fw.getName();
	}

	public String getUID() {
		return "framework_" + fw.getName().hashCode();
	}

	public FrameWork getFramework() {
		return fw;
	}

	public boolean isMimeTypeSupported(String type) {
		boolean supportDP = ((Boolean) fw.getConnector().getProperties().get(RemoteDeploymentAdmin.class.getName())).booleanValue();
		if (!supportDP && type.equals(MIME_DP))
			return false;
		return true;
	}
}

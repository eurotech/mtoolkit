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
package org.tigris.mtoolkit.osgimanagement.installation;

import org.eclipse.jface.resource.ImageDescriptor;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

/**
 * @since 5.0
 */
public class FrameworkTarget implements InstallationTarget {
	private Framework fw;

	public FrameworkTarget(Framework fw) {
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

	public Framework getFramework() {
		return fw;
	}

	public boolean isMimeTypeSupported(String type) {
		return true;
	}
}

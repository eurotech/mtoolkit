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
package org.tigris.mtoolkit.cdeditor.widgets;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.tigris.mtoolkit.cdeditor.internal.widgets.UIResources;

public class StatusShowingIcon {

	private static final int INDEX_OK = 0;
	private static final int INDEX_WARNING = 1;
	private static final int INDEX_ERROR = 2;

	private static final int INDEX_LAST = INDEX_ERROR;

	private ImageDescriptor baseIconDescriptor;
	private Image baseIcon;
	private Image[] decoratedImages = new Image[INDEX_LAST + 1];

	public StatusShowingIcon(Image baseIcon) {
		Assert.isNotNull(baseIcon);
		this.baseIcon = baseIcon;
		decoratedImages[INDEX_OK] = baseIcon;
	}

	public StatusShowingIcon(ImageDescriptor baseDescriptor) {
		Assert.isNotNull(baseDescriptor);
		this.baseIconDescriptor = baseDescriptor;
	}

	public Image getIcon(int severity) {
		if (Display.getCurrent() == null)
			return null;
		if (baseIcon == null) {
			baseIcon = baseIconDescriptor.createImage(Display.getCurrent());
			decoratedImages[INDEX_OK] = baseIcon;
		}
		int idx = getIndex(severity);
		if (idx == -1)
			return null;
		if (decoratedImages[idx] == null) {
			ImageDescriptor overlayDescriptor = getOverlayIconDescriptor(severity);
			if (overlayDescriptor == null)
				return null;
			ImageDescriptor overlayedIcon = new DecorationOverlayIcon(baseIcon, overlayDescriptor, IDecoration.BOTTOM_LEFT);
			decoratedImages[idx] = overlayedIcon.createImage(Display.getCurrent());
		}
		return decoratedImages[idx];
	}

	private ImageDescriptor getOverlayIconDescriptor(int severity) {
		switch (severity) {
		case IStatus.WARNING:
			return UIResources.getImageDescriptor(UIResources.OVERLAY_WARNING_ICON);
		case IStatus.ERROR:
			return UIResources.getImageDescriptor(UIResources.OVERLAY_ERROR_ICON);
		default:
			return null;
		}
	}

	private int getIndex(int severity) {
		switch (severity) {
		case IStatus.OK:
			return INDEX_OK;
		case IStatus.WARNING:
			return INDEX_WARNING;
		case IStatus.ERROR:
			return INDEX_ERROR;
		default:
			return -1;
		}
	}

	public void dispose() {
		if (baseIcon != null && baseIconDescriptor != null)
			baseIcon.dispose();
		for (int i = 0; i < decoratedImages.length; i++)
			if (decoratedImages[i] != null)
				decoratedImages[i].dispose();
	}
}

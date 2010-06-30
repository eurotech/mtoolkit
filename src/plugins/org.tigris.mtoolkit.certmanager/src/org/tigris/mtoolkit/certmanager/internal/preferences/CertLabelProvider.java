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
package org.tigris.mtoolkit.certmanager.internal.preferences;

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.certmanager.internal.images.ImageHolder;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;

public class CertLabelProvider extends LabelProvider implements ITableLabelProvider {

	private static final String ICON_CERTIFICATE = "signed_yes_tbl.gif";
	private static final String ICON_OVR_ERROR = "error_co.gif";

	private Image iconCertMissing;

	public CertLabelProvider() {
		super();

		Image iconCert = ImageHolder.getImage(ICON_CERTIFICATE);
		ImageDescriptor overlay = ImageHolder.getImageDescriptor(ICON_OVR_ERROR);
		iconCertMissing = new DecorationOverlayIcon(iconCert, overlay, IDecoration.BOTTOM_LEFT).createImage();
	}

	public Image getColumnImage(Object element, int columnIndex) {
		if (!(element instanceof ICertificateDescriptor) || columnIndex > 0) {
			return null;
		}
		ICertificateDescriptor cert = (ICertificateDescriptor) element;
		File keystore = new File(cert.getStoreLocation());
		if (!keystore.exists() || !keystore.isFile()) {
			return iconCertMissing;
		}
		return ImageHolder.getImage(ICON_CERTIFICATE);
	}

	public String getColumnText(Object element, int columnIndex) {
		if (!(element instanceof ICertificateDescriptor)) {
			return null;
		}
		ICertificateDescriptor cert = (ICertificateDescriptor) element;
		String columnText = null;
		switch (columnIndex) {
		case 0:
			columnText = cert.getAlias();
			break;
		case 1:
			columnText = cert.getStoreLocation();
			break;
		}
		return columnText;
	}

	public void dispose() {
		iconCertMissing.dispose();
	}
}

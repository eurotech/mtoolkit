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

import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;

public class CertLabelProvider extends LabelProvider implements ITableLabelProvider {

	public Image getColumnImage(Object element, int columnIndex) {
		return null;
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
}

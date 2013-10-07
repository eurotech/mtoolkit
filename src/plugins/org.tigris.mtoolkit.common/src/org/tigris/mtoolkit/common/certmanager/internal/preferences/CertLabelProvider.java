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
package org.tigris.mtoolkit.common.certmanager.internal.preferences;

import java.io.File;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;
import org.tigris.mtoolkit.common.images.UIResources;

public final class CertLabelProvider extends LabelProvider implements ITableLabelProvider {
  private Image iconCertMissing;

  public CertLabelProvider() {
    super();
    final Image iconCert = UIResources.getImage(UIResources.CERTIFICATE_ICON);
    ImageDescriptor overlay = UIResources.getImageDescriptor(UIResources.OVR_ERROR_ICON);
    iconCertMissing = new DecorationOverlayIcon(iconCert, overlay, IDecoration.BOTTOM_LEFT).createImage();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
   */
  public Image getColumnImage(Object element, int columnIndex) {
    if (columnIndex > 0) {
      return null;
    }
    if (!(element instanceof ICertificateDescriptor)) {
      return null;
    }
    ICertificateDescriptor cert = (ICertificateDescriptor) element;
    File keystore = new File(cert.getStoreLocation());
    if (!keystore.exists() || !keystore.isFile()) {
      return iconCertMissing;
    }
    return UIResources.getImage(UIResources.CERTIFICATE_ICON);
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
   */
  public String getColumnText(Object element, int columnIndex) {
    if (!(element instanceof ICertificateDescriptor)) {
      return null;
    }
    ICertificateDescriptor cert = (ICertificateDescriptor) element;
    switch (columnIndex) {
    case 0:
      return cert.getAlias();
    case 1:
      return cert.getStoreLocation();
    default:
      return null;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.BaseLabelProvider#dispose()
   */
  @Override
  public void dispose() {
    iconCertMissing.dispose();
  }
}

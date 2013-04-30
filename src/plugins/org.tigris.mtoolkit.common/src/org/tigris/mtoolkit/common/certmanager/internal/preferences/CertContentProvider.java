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

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;

final class CertContentProvider implements IStructuredContentProvider, ICertificateListener {
  private CertStorage storage;
  private TableViewer tableViewer;

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
   */
  public Object[] getElements(Object inputElement) {
    if (inputElement instanceof CertStorage) {
      return ((CertStorage) inputElement).getCertificates();
    }
    return new Object[0];
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IContentProvider#dispose()
   */
  public void dispose() {
    if (storage == null) {
      return;
    }
    storage.removeListener(this);
    CertStorage.release();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (oldInput instanceof CertStorage) {
      ((CertStorage) oldInput).removeListener(this);
    }
    if (newInput instanceof CertStorage) {
      storage = (CertStorage) newInput;
      storage.addListener(this);
    }
    if (viewer instanceof TableViewer) {
      tableViewer = (TableViewer) viewer;
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.certmanager.internal.preferences.ICertificateListener#certificateAdded(org.tigris.mtoolkit.common.certificates.ICertificateDescriptor)
   */
  public void certificateAdded(ICertificateDescriptor cert) {
    if (tableViewer != null) {
      tableViewer.add(cert);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.certmanager.internal.preferences.ICertificateListener#certificateRemoved(org.tigris.mtoolkit.common.certificates.ICertificateDescriptor)
   */
  public void certificateRemoved(ICertificateDescriptor cert) {
    if (tableViewer != null) {
      tableViewer.remove(cert);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.certmanager.internal.preferences.ICertificateModifyListener#certificateChanged(org.tigris.mtoolkit.common.certificates.ICertificateDescriptor)
   */
  public void certificateChanged(ICertificateDescriptor cert) {
    if (tableViewer != null) {
      tableViewer.update(cert, null);
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.certmanager.internal.preferences.ICertificateListener#certificateStorageRefreshed()
   */
  public void certificateStorageRefreshed() {
    if (tableViewer != null) {
      tableViewer.refresh();
    }
  }
}

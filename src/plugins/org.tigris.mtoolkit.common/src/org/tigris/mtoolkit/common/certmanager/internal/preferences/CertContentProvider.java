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

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.tigris.mtoolkit.common.certificates.ICertificateDescriptor;

final class CertContentProvider implements ITreeContentProvider, ICertificateListener {
  private CertStorage storage;
  private TreeViewer  treeViewer;

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
   */
  public Object[] getElements(Object inputElement) {
    if (inputElement instanceof CertStorage) {
      return ((CertStorage) inputElement).getCertificates();
    }
    return new Object[0];
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#dispose()
   */
  public void dispose() {
    if (storage == null) {
      return;
    }
    storage.removeListener(this);
    CertStorage.release();
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
   */
  public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    if (oldInput instanceof CertStorage) {
      ((CertStorage) oldInput).removeListener(this);
    }
    if (newInput instanceof CertStorage) {
      storage = (CertStorage) newInput;
      storage.addListener(this);
    }
    if (viewer instanceof TreeViewer) {
      treeViewer = (TreeViewer) viewer;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
   */
  public Object[] getChildren(Object parentElement) {
    if (parentElement instanceof CertStorage) {
      return ((CertStorage) parentElement).getCertificates();
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
   */
  public Object getParent(Object element) {
    if (element instanceof CertStorage) {
      return element;
    }
    return null;
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
   */
  public boolean hasChildren(Object element) {
    return false;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.certmanager.internal.preferences.ICertificateListener#certificateAdded(org.tigris.mtoolkit.common.certificates.ICertificateDescriptor)
   */
  public void certificateAdded(ICertificateDescriptor cert) {
    if (treeViewer != null) {
      treeViewer.refresh();
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.certmanager.internal.preferences.ICertificateListener#certificateRemoved(org.tigris.mtoolkit.common.certificates.ICertificateDescriptor)
   */
  public void certificateRemoved(ICertificateDescriptor cert) {
    if (treeViewer != null) {
      treeViewer.refresh();
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.certmanager.internal.preferences.ICertificateModifyListener#certificateChanged(org.tigris.mtoolkit.common.certificates.ICertificateDescriptor)
   */
  public void certificateChanged(ICertificateDescriptor cert) {
    if (treeViewer != null) {
      treeViewer.update(cert, null);
      treeViewer.refresh();
    }
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.certmanager.internal.preferences.ICertificateListener#certificateStorageRefreshed()
   */
  public void certificateStorageRefreshed() {
    if (treeViewer != null) {
      treeViewer.refresh();
    }
  }
}

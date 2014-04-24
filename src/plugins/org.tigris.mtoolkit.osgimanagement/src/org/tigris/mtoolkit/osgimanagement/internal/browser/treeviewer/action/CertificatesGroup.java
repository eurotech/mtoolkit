/*******************************************************************************
 * Copyright (c) 2005, 2013 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.security.cert.X509Certificate;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.security.auth.x500.X500Principal;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.tigris.mtoolkit.common.Messages;
import org.tigris.mtoolkit.common.gui.X509CertificateViewer;

public final class CertificatesGroup {
  private static int style = SWT.SINGLE | SWT.BORDER | SWT.VIRTUAL | SWT.H_SCROLL | SWT.V_SCROLL | SWT.Expand;

  public class CertificateChainElement {
    private X509Certificate   parent;
    private X509Certificate[] children;

    public CertificateChainElement(X509Certificate parent, X509Certificate[] children) {
      this.parent = parent;
      this.children = children;
    }

    /**
     * @return the parent
     */
    public X509Certificate getParent() {
      return parent;
    }

    /**
     * @return the children
     */
    public X509Certificate[] getChildren() {
      return children;
    }
  }

  private class CertLabelProvider extends LabelProvider {
    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
     */
    @Override
    public String getText(Object element) {
      X509Certificate dataElement;
      if (((CertificateChainElement) element).getChildren().length > 1) {
        dataElement = ((CertificateChainElement) element).getParent();
      } else {
        dataElement = ((CertificateChainElement) element).children[0];
      }
      String name = dataElement.getSubjectX500Principal().getName(X500Principal.RFC2253);
      return parseName(name);
    }

    private String parseName(String name) {
      String cName = "";
      String organization = "";
      String orgUnit = "";
      StringTokenizer tokenizer = new StringTokenizer(name, ",");
      while (tokenizer.hasMoreTokens()) {
        String nextToken = tokenizer.nextToken().trim();
        int index = nextToken.indexOf("=");
        if (index != -1) {
          String key = nextToken.substring(0, index);
          String val = nextToken.substring(index + 1);
          if (key.equals("CN")) {
            cName = val;
          } else if (key.equals("O")) {
            organization = val;
          } else if (key.equals("OU")) {
            orgUnit = val;
          }
        }
      }
      if (cName.equals("") && organization.equals("") && orgUnit.equals("")) {
        return name;
      }
      return cName + " (" + organization + ", " + orgUnit + ")";
    }
  }

  private ITreeContentProvider  cpCertificatesGroup = new ITreeContentProvider() {
                                                      /* (non-Javadoc)
                                                       * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
                                                       */
                                                      public Object[] getElements(Object inputElement) {
                                                        if (inputElement instanceof Dictionary) {
                                                          Object[] children = getData((Dictionary) inputElement);
                                                          return children;
                                                        }
                                                        if (inputElement instanceof CertificateChainElement) {
                                                          return ((CertificateChainElement) inputElement).getChildren();
                                                        }
                                                        return new Object[0];
                                                      }

                                                      /* (non-Javadoc)
                                                       * @see org.eclipse.jface.viewers.IContentProvider#dispose()
                                                       */
                                                      public void dispose() {
                                                      }

                                                      /* (non-Javadoc)
                                                       * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
                                                       */
                                                      public void inputChanged(Viewer viewer, Object oldInput,
                                                          Object newInput) {
                                                      }

                                                      /* (non-Javadoc)
                                                       * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
                                                       */
                                                      public Object[] getChildren(Object parentElement) {
                                                        if (parentElement instanceof Dictionary) {
                                                          Object[] children = getData((Dictionary) parentElement);
                                                          return children;
                                                        }
                                                        if (parentElement instanceof CertificateChainElement) {
                                                          Object[] children = ((CertificateChainElement) parentElement)
                                                              .getChildren();
                                                          return children;
                                                        }
                                                        return new Object[0];
                                                      }

                                                      /* (non-Javadoc)
                                                       * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
                                                       */
                                                      public Object getParent(Object element) {
                                                        if (element instanceof CertificateChainElement) {
                                                          return ((CertificateChainElement) element).getParent();
                                                        }
                                                        return null;
                                                      }

                                                      /* (non-Javadoc)
                                                       * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
                                                       */
                                                      public boolean hasChildren(Object element) {
                                                        if (element instanceof Dictionary) {
                                                          return true;
                                                        }
                                                        if (element instanceof CertificateChainElement
                                                            && ((CertificateChainElement) element).getChildren().length > 1) {
                                                          return true;
                                                        }
                                                        return false;
                                                      }

                                                      private Object[] getData(Dictionary data) {
                                                        Vector dataVector = new Vector();
                                                        Enumeration keys = data.keys();
                                                        while (keys.hasMoreElements()) {
                                                          X509Certificate parent = (X509Certificate) keys.nextElement();
                                                          List children = (List) data.get(parent);
                                                          dataVector.addAll(getElements(parent, children));
                                                        }
                                                        return dataVector.toArray();
                                                      }

                                                      private Vector getElements(X509Certificate parent, List children) {
                                                        Vector elements = new Vector<CertificateChainElement>();
                                                        Iterator i = children.iterator();
                                                        while (i.hasNext()) {
                                                          X509Certificate element = (X509Certificate) i.next();
                                                          X509Certificate[] elementChildren = new X509Certificate[1];
                                                          elementChildren[0] = element;
                                                          CertificateChainElement chainElement = new CertificateChainElement(
                                                              parent, elementChildren);
                                                          elements.add(chainElement);
                                                        }
                                                        return elements;
                                                      }
                                                    };

  private Group                 signContentGroup;
  private Tree                  treeCertificates;
  private TreeViewer            certificatesViewer;
  private Composite             certificateDetails;
  private X509CertificateViewer xcv;

  public CertificatesGroup(Composite parent) {
    this(parent, style);
  }

  public CertificatesGroup(Composite parent, int style) {
    signContentGroup = new Group(parent, SWT.NONE);
    signContentGroup.setText(Messages.dlgCertMan_descr);
    initContent(style);
  }

  public void setContent(Dictionary certificates) {
    certificatesViewer.setInput(certificates);
    int itemCount = treeCertificates.getItemCount();
    if (itemCount != 0) {
      TreeItem item = treeCertificates.getItem(0);
      treeCertificates.setSelection(item);
      refreshViewer(item.getData());
      treeCertificates.forceFocus();
    }
  }

  public Control getControl() {
    return signContentGroup;
  }

  private void initContent(int style) {

    GridData gridData = new GridData(GridData.FILL_BOTH);
    signContentGroup.setLayoutData(gridData);
    signContentGroup.setLayout(new GridLayout());

    SashForm sashComposite = new SashForm(signContentGroup, SWT.HORIZONTAL);
    sashComposite.setLayout(new FillLayout());
    sashComposite.setLayoutData(gridData);

    treeCertificates = new Tree(sashComposite, style);
    treeCertificates.setLayout(new GridLayout());
    treeCertificates.setLayoutData(gridData);

    certificateDetails = new Composite(sashComposite, SWT.NONE);
    GridLayout gridLayout = new GridLayout();
    gridLayout.horizontalSpacing = 0;
    gridLayout.marginHeight = 0;
    gridLayout.marginWidth = 0;
    gridLayout.verticalSpacing = 0;
    certificateDetails.setLayout(gridLayout);
    certificateDetails.setLayoutData(gridData);

    certificatesViewer = new TreeViewer(treeCertificates);
    certificatesViewer.setContentProvider(cpCertificatesGroup);
    certificatesViewer.setLabelProvider(new CertLabelProvider());
    treeCertificates.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent e) {
        refreshViewer(e.item.getData());
      }
    });

    gridData.widthHint = 415 + treeCertificates.getBorderWidth() * 2;
    gridData.heightHint = 100;

    treeCertificates.pack();
    signContentGroup.pack();
  }

  private void refreshViewer(Object data) {
    if (data instanceof CertificateChainElement) {
      CertificateChainElement element = (CertificateChainElement) data;
      if (xcv == null) {
        xcv = new X509CertificateViewer(certificateDetails, SWT.NONE);
      }
      X509Certificate dataElement;
      if (element.getChildren().length > 1) {
        dataElement = element.getParent();
      } else {
        dataElement = element.children[0];
      }
      xcv.setCertificate(dataElement);
      xcv.getControl().setSize(certificateDetails.getSize());
    }
  }
}

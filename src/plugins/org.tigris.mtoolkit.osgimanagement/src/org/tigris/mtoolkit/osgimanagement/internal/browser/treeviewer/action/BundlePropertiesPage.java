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

import java.util.Dictionary;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.tigris.mtoolkit.common.gui.PropertiesPage;

public class BundlePropertiesPage extends PropertiesPage {

  public static String        HEADERS_KEY      = "headers";
  public static String        CERTIFICATES_KEY = "certificates";
  protected CertificatesGroup certGroup;

  /**
   * @param headers
   */
  @Override
  public void setData(Dictionary props) {
    super.setData((Dictionary) props.get(HEADERS_KEY));
    certGroup.setContent((Dictionary) props.get(CERTIFICATES_KEY));
  }

  @Override
  protected Control createPage(Composite parent) {
    // Connect properties group
    Control control = super.createPage(parent);

    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayout(new GridLayout());
    composite.setLayoutData(new GridData(GridData.FILL_BOTH));

    certGroup = new CertificatesGroup(composite);
    return control;
  }

}

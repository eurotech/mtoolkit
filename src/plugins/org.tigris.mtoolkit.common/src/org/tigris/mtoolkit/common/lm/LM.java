/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.lm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

public final class LM {

  private static final String EXTENSION_POINT_LM = "org.tigris.mtoolkit.common.lm";

  private static List         lcl                = null;

  private LM() {
  }

  public static void verify(IProgressMonitor monitor) throws CoreException {
    verify(monitor, Collections.EMPTY_MAP);
  }

  public static synchronized void verify(IProgressMonitor monitor, Map args) throws CoreException {
    if (lcl == null) {
      initialize();
    }
    Iterator lcs = lcl.iterator();
    while (lcs.hasNext()) {
      ILC lc = (ILC) lcs.next();
      lc.verify(monitor, args);
    }
  }

  private static void initialize() {
    lcl = new ArrayList();
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint lMExtPoint = registry.getExtensionPoint(EXTENSION_POINT_LM);
    if (lMExtPoint == null) {
      return;
    }
    IConfigurationElement[] elements = lMExtPoint.getConfigurationElements();
    if (elements == null || elements.length == 0) {
      return;
    }
    for (int i = 0; i < elements.length; i++) {
      try {
        lcl.add(elements[i].createExecutableExtension("class"));
      } catch (CoreException e) {
        //Just leave that
      }
    }
  }
}

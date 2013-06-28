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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.logic;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public final class ViewSorter extends ViewerSorter {
  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(Viewer viewer, Object e1, Object e2) {
    int result = 0;
    Object name1 = getName(e1);
    Object name2 = getName(e2);
    if (name1 != null && name2 != null) {
      if (name1 instanceof Long) {
        result = ((Long) name1).compareTo((Long) name2);
      } else {
        result = getComparator().compare(name1, name2);
      }
    }
    if (result != 0) {
      return result;
    } else {
      result = super.compare(viewer, e1, e2);
      return result;
    }
  }

  /* (non-Javadoc)
   * @see org.eclipse.jface.viewers.ViewerComparator#isSorterProperty(java.lang.Object, java.lang.String)
   */
  @Override
  public boolean isSorterProperty(Object element, String property) {
    return property.equals(IBasicPropertyConstants.P_TEXT);
  }

  private Object getName(Object object) {
    if (object instanceof Category) {
      return ((Model) object).getName();
    }
    if (object instanceof Bundle) {
      return ((Model) object).getName();
    }
    if (object instanceof ObjectClass) {
      return ((ObjectClass) object).getName();
    }
    return null;
  }
}

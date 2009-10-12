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
package org.tigris.mtoolkit.osgimanagement.internal.browser.logic;

import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;

public class ListUtil {

	static class NameSorter extends ViewerSorter {
		public boolean isSorterProperty(Object element, Object propertyId) {
			return propertyId.equals(IBasicPropertyConstants.P_TEXT);
		}
	}

	public static class BundleSorter extends NameSorter {
		public int compare(Viewer viewer, Object e1, Object e2) {
			int result = 0;
			Object name1 = getName(e1);
			Object name2 = getName(e2);
			if (name1 != null && name2 != null) {
				if (name1 instanceof Long) {
					result = ((Long) name1).compareTo((Long) name2);
				} else {
					result = getComparator().compare((String) name1, (String) name2);
				}

			}
			if (result != 0) {
				return result;
			} else {
				result = super.compare(viewer, e1, e2);
				return result;
			}
		}

		private Object getName(Object object) {
			if (object instanceof Category)
				return ((Model) object).getName();
			if (object instanceof Bundle)
				return ((Model) object).getName();
			if (object instanceof ObjectClass)
				return ((ObjectClass) object).getName();
			return null;
		}

	}

	public static final ViewerSorter BUNDLE_SORTER = new BundleSorter();

	public ListUtil() {
		super();
	}
}

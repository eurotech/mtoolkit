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
package org.tigris.mtoolkit.cdeditor.internal.providers;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDElement;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperties;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDProperty;
import org.tigris.mtoolkit.cdeditor.internal.widgets.UIResources;
import org.tigris.mtoolkit.cdeditor.widgets.StatusShowingIcon;


public class PropertiesLabelProvider extends LabelProvider implements
		ITableLabelProvider {

	private StatusShowingIcon singlePropIcon = new StatusShowingIcon(UIResources.getImageDescriptor(UIResources.SINGLE_PROPERTY_ICON));
	private StatusShowingIcon multiPropIcon = new StatusShowingIcon(UIResources.getImageDescriptor(UIResources.ARRAY_PROPERTY_ICON));
	private StatusShowingIcon propertiesIcon = new StatusShowingIcon(UIResources.getImageDescriptor(UIResources.PROPERTIES_ICON));
	private StatusShowingIcon valueIcon = new StatusShowingIcon(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FILE));

	private static final int NAME_COLUMN = 0;
	private static final int VALUE_COLUMN = 1;
	private static final int TYPE_COLUMN = 2;

	public Image getColumnImage(Object element, int columnIndex) {
		if (columnIndex == NAME_COLUMN) {
			if (element instanceof ICDElement) {
				ICDModel model = ((ICDElement) element).getModel();
				if (model == null) {
					return null;
				}
				IStatus validationStatus = model.getAggregatedValidationStatus((ICDElement) element)[0];
				if (element instanceof ICDProperty) {
					if (((ICDProperty) element).isMultiValue())
						return multiPropIcon.getIcon(validationStatus.getSeverity());
					else
						return singlePropIcon.getIcon(validationStatus.getSeverity());
				}

				if (element instanceof ICDProperties) {
					return propertiesIcon.getIcon(validationStatus.getSeverity());
				}
			}
			if (element instanceof String) {
				return valueIcon.getIcon(IStatus.OK);
			}
		}
		return null;
	}

	public String getColumnText(Object element, int columnIndex) {

		if (element instanceof ICDProperty) {

			switch (columnIndex) {
			case NAME_COLUMN:
				return ((ICDProperty) element).getName();
			case VALUE_COLUMN:
				String value = getCorrectValue((ICDProperty) element);
				return value;
			case TYPE_COLUMN:
				int typeIdx = ((ICDProperty) element).getType();
				return typeIdx != ICDProperty.TYPE_UNKNOWN ? ICDProperty.TYPE_NAMES[((ICDProperty) element).getType() - 1] : "(invalid)";
			}
		} else if (element instanceof ICDProperties) {
			switch (columnIndex) {
			case NAME_COLUMN:
				return ((ICDProperties) element).getEntry();
			case VALUE_COLUMN:
				return "";
			case TYPE_COLUMN:
				return "Resource";
			}
		} else if (element instanceof String) {
			switch (columnIndex) {
			case NAME_COLUMN:
				return element.toString();
			case VALUE_COLUMN:
				return "";
			case TYPE_COLUMN:
				return "";
			}
		}
		return super.getText(element);
	}

	private String getCorrectValue(ICDProperty prop) {
		if (!prop.isMultiValue()) {
			return prop.getValue();
		} else {
			String bufferedValue = "[";
			String[] propValues = prop.getValues();
			for (int i = 0; i < propValues.length; i++) {
				bufferedValue += propValues[i] + ",";
			}
			// now we should replace last ',' with ']'
			if (bufferedValue.length() <= 1) {
				return "";
			}
			bufferedValue = bufferedValue.substring(0, bufferedValue.length() - 1) + ']';

			return bufferedValue;
		}
	}

}

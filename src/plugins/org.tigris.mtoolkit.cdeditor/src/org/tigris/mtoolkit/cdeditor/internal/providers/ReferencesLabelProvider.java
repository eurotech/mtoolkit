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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDReference;
import org.tigris.mtoolkit.cdeditor.internal.text.StringHelper;
import org.tigris.mtoolkit.cdeditor.internal.widgets.UIResources;
import org.tigris.mtoolkit.cdeditor.widgets.StatusShowingIcon;


public class ReferencesLabelProvider extends StyledCellLabelProvider {

	private StatusShowingIcon icon = new StatusShowingIcon(UIResources.getImageDescriptor(UIResources.REFERENCE_ICON));

	public Image getImage(Object element) {
		if (element instanceof ICDReference) {
			ICDReference ref = (ICDReference) element;
			ICDModel model = ref.getModel();
			if (model != null) {
				IStatus validationStatus = model.getAggregatedValidationStatus(ref)[0];
				return icon.getIcon(validationStatus.getSeverity());
			}
		}
		return null;
	}

	public void update(ViewerCell cell) {
		Object element = cell.getElement();
		if (element instanceof ICDReference) {
			ICDReference reference = (ICDReference) element;

			String className = reference.getInterface();
			String name = reference.getName();
			String policy = reference.getPolicy() != ICDReference.POLICY_UNKNOWN ? ICDReference.POLICY_NAMES[reference.getPolicy() - 1] : "invalid policy";
			String cardinality = reference.getCardinality() != ICDReference.CARDINALITY_UNKNOWN ? ICDReference.CARDINALITY_NAMES_SHORT[reference.getCardinality() - 1] : "invalid cardinality";

			String referenceText = className;
			List styles = new ArrayList();
			if (referenceText == null)
				referenceText = "";
			int stylePos = referenceText.length();
			TextStyle style = new TextStyle();
			styles.add(new StyleRange(0, stylePos, style.foreground, style.background));

			if (name != null && name.length() > 0) {
				referenceText += " - ";
				referenceText += reference.getName();
				int rangeLength = reference.getName().length() + 3;
				StyledString.QUALIFIER_STYLER.applyStyles(style);
				styles.add(new StyleRange(stylePos, rangeLength, style.foreground, style.background));
				stylePos += rangeLength;
			}

			String detailsText = StringHelper.buildCommaSeparatedList(new String[] { policy, cardinality });
			if (detailsText != null && detailsText.length() > 0) {
				referenceText += " [" + detailsText + "]";
				int rangeLength = detailsText.length() + 3;
				StyledString.QUALIFIER_STYLER.applyStyles(style);
				styles.add(new StyleRange(stylePos, rangeLength, style.foreground, style.background));
			}

			cell.setText(referenceText);
			cell.setStyleRanges((StyleRange[]) styles.toArray(new StyleRange[styles.size()]));
			cell.setImage(getImage(element));
		}
		super.update(cell);
	}

	public void dispose() {
		if (icon != null)
			icon.dispose();
	}
}

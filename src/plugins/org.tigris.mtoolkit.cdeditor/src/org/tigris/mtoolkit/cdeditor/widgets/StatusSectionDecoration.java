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
package org.tigris.mtoolkit.cdeditor.widgets;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.Section;
import org.tigris.mtoolkit.cdeditor.internal.widgets.UIResources;

/**
 * A wrapper class for Section widget, which adds an API for displaying
 * indication for errors and warnings in the Section title area. The class uses
 * IStatus objects.
 * 
 */
public class StatusSectionDecoration {

	private Label fStatusIcon;
	
	private Section fSection;
	
	private StatusLine fStatusLine;
	
	private IStatus fLastStatus;
	
	public StatusSectionDecoration(Section section) {
		this.fSection = section;
	
		fStatusIcon = new Label(section, SWT.LEFT);
		fStatusIcon.setVisible(!section.isExpanded());
		
		section.addExpansionListener(new ExpansionAdapter() {
			public void expansionStateChanging(ExpansionEvent e) {
				fStatusIcon.setVisible(!e.getState());
			}
		});
		
		section.setTextClient(fStatusIcon);
	}

	public void updateStatus(IStatus status) {
		fLastStatus = status;
		updateSectionDecoration();
	}
	
	public IStatus getStatus() {
		return fLastStatus;
	}
	
	public Section getSection() {
		return fSection;
	}
	
	private void updateSectionDecoration() {
		fStatusIcon.setImage(findImage(fLastStatus));
		if (fLastStatus == null || fLastStatus.isOK()) {
			fStatusIcon.setToolTipText("");
		} else {
			fStatusIcon.setToolTipText(fLastStatus.getMessage());
		}
		if (fStatusLine != null)
			fStatusLine.updateStatus(fLastStatus);
	}
	
	public Control createStatusLine(Composite parent) {
		fStatusLine = new StatusLine(parent, SWT.NONE);
		return fStatusLine;
	}
	
	private Image findImage(IStatus status) {
		if (status == null || status.isOK()) {
			// XXX: return Blank image with the same sizes as the other icons
			/*	This is a simple fix for two GUI issues:
				1. the size of the section title - it was bigger when we have icon - using blank one, cause
				the title to always have the same height
				2. icon wasn't refreshed when we have collapsed section, because it's size was 0 and 
				was effectively hidden.
			*/ 
			return UIResources.getImage(UIResources.BLANK_ICON);
		} else if (status.matches(IStatus.ERROR)) {
			return UIResources.getImage(UIResources.SMALL_ERROR_ICON);
		} else if (status.matches(IStatus.WARNING)) {
			return UIResources.getImage(UIResources.SMALL_WARNING_ICON);
		} else if (status.matches(IStatus.INFO)) {
			return UIResources.getImage(UIResources.SMALL_INFO_ICON);
		}
		return null;
	}
}

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
package org.tigris.mtoolkit.dpeditor.editor.forms;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;

/**
 * ScrollableForm is a custom control that renders a scrollable not editable
 * text area, which presents the contents of the given file or some text
 * contents.
 */
public class ScrollableForm extends Form {

	/** Composite that provide a surface for adding the text area */
	private Composite container;

	/** <code>boolean</code> flag that indicate if vertical will be fit */
	private boolean verticalFit;
	/**
	 * <code>boolean</code> flag that indicate is the area will be with scroll.
	 * The default value is <code>true</code>
	 */
	private boolean scrollable = true;

	/**
	 * The amount that the receiver's value will be modified by when the
	 * right/left arrows are pressed to the argument, which must be at least
	 * one.
	 */
	private static final int HBAR_INCREMENT = 10;
	/**
	 * The amount that the receiver's value will be modified by when the page
	 * increment/decrement areas are selected to the argument, which must be at
	 * least one.
	 */
	private static final int HPAGE_INCREMENT = 40;
	/**
	 * The amount that the receiver's value will be modified by when the up/down
	 * arrows are pressed to the argument, which must be at least one.
	 */
	private static final int VBAR_INCREMENT = 10;
	/**
	 * The amount that the receiver's value will be modified by when the page
	 * increment/decrement areas are selected to the argument, which must be at
	 * least one.
	 */
	private static final int VPAGE_INCREMENT = 40;

	/**
	 * Creates the empty ScrollableForm.
	 */
	public ScrollableForm() {
	}

	/**
	 * Provides a surface for put the text area.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.Form#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public Control createControl(Composite parent) {

		container = createParent(parent);
		Control formControl = super.createControl(container);
		if (container instanceof ScrolledComposite) {
			ScrolledComposite sc = (ScrolledComposite) container;
			sc.setContent(formControl);
			/*
			 * Point formSize = formControl.computeSize(SWT.DEFAULT,
			 * SWT.DEFAULT); sc.setMinWidth(formSize.x);
			 * sc.setMinHeight(formSize.y);
			 */
		}
		GridData gd = new GridData(GridData.FILL_BOTH);
		formControl.setLayoutData(gd);
		container.setBackground(formControl.getBackground());
		return container;
	}

	/**
	 * Creates the parent composite, in which is placed a ScrolledComposite,
	 * which provides scrollbars and will scroll its content when the user uses
	 * the scrollbars.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @return the created control
	 */
	protected Composite createParent(Composite parent) {
		Composite result = null;
		if (isScrollable()) {
			ScrolledComposite scomp = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
			if (isVerticalFit()) {
				scomp.setExpandHorizontal(true);
				scomp.setExpandVertical(true);
			}
			initializeScrollBars(scomp);
			result = scomp;
		} else {
			result = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			result.setLayout(layout);
		}
		return result;
	}

	/**
	 * Initializes the scroll bars of the given <code>ScrolledComposite</code>.
	 * 
	 * @param scomp
	 *            the composite which scroll bars must be initialized
	 */
	private void initializeScrollBars(ScrolledComposite scomp) {
		ScrollBar hbar = scomp.getHorizontalBar();
		if (hbar != null) {
			hbar.setIncrement(HBAR_INCREMENT);
			hbar.setPageIncrement(HPAGE_INCREMENT);
		}
		ScrollBar vbar = scomp.getVerticalBar();
		if (vbar != null) {
			vbar.setIncrement(VBAR_INCREMENT);
			vbar.setPageIncrement(VPAGE_INCREMENT);
		}
	}

	/**
	 * Checks if this form will contain the scroll bars or not.
	 * 
	 * @return <code>true</code> if the form will have a scroll bars,
	 *         <code>false</code> otherwise
	 */
	public boolean isScrollable() {
		return scrollable;
	}

	/**
	 * Checks if the ScrolledComposite resize the content object to be as wide
	 * and as tall as the ScrolledComposite.
	 * 
	 * @return <code>true</code> when composite resize the content object,
	 *         <code>false</code> otherwise
	 */
	public boolean isVerticalFit() {
		return verticalFit;
	}

	/**
	 * Sets the flag, which is responsible to create the ScrolledComposite or
	 * the regular Composite.
	 * 
	 * @param newScrollable
	 *            <code>true</code> if this Form will be create the
	 *            ScrolledComposite, <code>false</code> when the Form will be
	 *            created regular Composite
	 */
	public void setScrollable(boolean newScrollable) {
		scrollable = newScrollable;
	}

	/**
	 * Sets the flag, which is responsible to the created ScrolledComposite
	 * resize the content object to be as wide and as tall as the
	 * ScrolledComposite or not.
	 * 
	 * @param newVerticalFit
	 *            <code>true</code> when composite resize the content object,
	 *            <code>false</code> otherwise
	 */
	public void setVerticalFit(boolean newVerticalFit) {
		verticalFit = newVerticalFit;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.Form#update()
	 */
	public void update() {
		super.update();
		container.setVisible(false);
		if (container instanceof ScrolledComposite) {
			updateScrolledComposite();
		} else {
			container.layout(true);
		}
		container.setVisible(true);
	}

	/**
	 * Updates the minimum width and height at which the ScrolledComposite will
	 * begin scrolling the content with the horizontal and vertical scroll bar
	 * if only the container is instance of ScrolledComposite.
	 */
	public void updateScrollBars() {
		if (container instanceof ScrolledComposite) {
			updateScrolledComposite();
		}
	}

	/**
	 * Updates the minimum width and height at which the ScrolledComposite will
	 * begin scrolling the content with the horizontal and vertical scroll bar.
	 */
	public void updateScrolledComposite() {
		ScrolledComposite sc = (ScrolledComposite) container;
		Control formControl = getControl();
		Point newSize = formControl.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		formControl.setSize(newSize);
		sc.setMinWidth(newSize.x);
		sc.setMinHeight(2 * newSize.y);
	}

	/**
	 * Disposes of the operating system resources associated with the created
	 * Composite and all its descendents.
	 */
	public void dipose() {
		container.dispose();
		super.dispose();
	}
}
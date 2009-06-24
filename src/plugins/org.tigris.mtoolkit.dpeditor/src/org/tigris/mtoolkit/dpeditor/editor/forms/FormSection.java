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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;

/**
 * FormSection is a custom control that renders a header, description and custom
 * section and will be added in to the Form.
 */
public abstract class FormSection implements IPropertyChangeListener {

	/** The selection */
	public static final int SELECTION = 1;

	/** Label that holds the header of the FormSection */
	protected Label header;
	/** Label that holds the description of the FormSection */
	protected Label descriptionLabel;
	/** Separator control between the header label and the description label */
	protected Control separator;

	/** Holds the header text */
	private String headerText;
	/** Holds the description of the FormSection */
	private String description;

	/**
	 * <code>boolean</code> flag which presents if the form section is changed
	 * or not
	 */
	private boolean dirty;
	/** <code>boolean</code> value if description will be panted or not */
	private boolean descriptionPainted = true;
	/** <code>boolean</code> value if section header will be panted or not */
	private boolean headerPainted = true;

	/** Holds all elements of this section as a header, description and so on */
	private Composite control;
	/** Holds the main part of the section custom elements */
	private Control client;

	/*
	 * This is a custom layout for the section. Both the header and the
	 * description labels will wrap and they will use client's size to calculate
	 * needed height.
	 */
	class SectionLayout extends Layout {

		static final int CLIENT_WIDTH = 263;
		static final int CLIENT_HEIGHT = 126;

		int vspacing = 3;
		int sepHeight = 2;

		protected Point computeSize(Composite parent, int wHint, int hHint, boolean flush) {
			int width = 0;
			int height = 0;
			int cwidth = 0;

			if (wHint != SWT.DEFAULT)
				width = wHint;
			if (hHint != SWT.DEFAULT)
				height = hHint;
			cwidth = width;

			if (hHint == SWT.DEFAULT && headerPainted && header != null) {
				Point hsize = header.computeSize(cwidth, SWT.DEFAULT, flush);
				height += hsize.y;
				height += vspacing;
			}

			if (hHint == SWT.DEFAULT) {
				height += sepHeight;
				height += vspacing;
			}
			if (hHint == SWT.DEFAULT && descriptionPainted && descriptionLabel != null) {
				Point dsize = descriptionLabel.computeSize(cwidth, SWT.DEFAULT, flush);
				height += dsize.y;
				height += vspacing;
			}
			return new Point(width, height);
		}

		protected void layout(Composite parent, boolean flush) {
			int width = parent.getClientArea().width;
			int height = parent.getClientArea().height;
			int y = 0;
			if (headerPainted && header != null) {
				Point hsize = header.computeSize(width, SWT.DEFAULT, flush);
				header.setBounds(0, y, width, hsize.y);
				y += hsize.y + vspacing;
			}
			if (separator != null) {
				separator.setBounds(0, y, width, 2);
				y += sepHeight + vspacing;
			}
			if (descriptionPainted && descriptionLabel != null) {
				Point dsize = descriptionLabel.computeSize(width, SWT.DEFAULT, flush);
				descriptionLabel.setBounds(0, y, width, dsize.y);
				y += dsize.y + vspacing;
			}
			if (client != null) {
				client.setBounds(0, y, width, height - y);
			}
		}
	}

	/**
	 * Constructor of the FormSection
	 */
	public FormSection() {
		// Description causes problems re word wrapping
		// and causes bad layout in schema and
		// component editors when in Motif - turning off
		if (SWT.getPlatform().equals("motif")) {
			descriptionPainted = false;
		}
		JFaceResources.getFontRegistry().addListener(this);
	}

	/**
	 * Notifies all custom elements of this <code>FormSection</code> of the new
	 * values of the components and sets that this form section is changed and
	 * need to be saved. This method must be overwrite of all classes that
	 * extends <code>FormSection</code>
	 * 
	 * @param onSave
	 *            <code>boolean</code> parameter to sets if the changes will be
	 *            saved or not
	 */
	public abstract void commitChanges(boolean onSave);

	/**
	 * This method is called from the <code>createControl</code> method and must
	 * be overwrite from the custom implementation of the FormSection class and
	 * to put all custom components in this form section.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the created
	 *            client composite which will be holds all custom controls
	 * @return Returns the composite control which will be holds all custom
	 *         controls
	 */
	public abstract Composite createClient(Composite parent);

	/**
	 * Constructs a new instance of the control given its parent and creates a
	 * header, description and a custom controls for this FormSection.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance
	 * @return a new instance of the control with all his components
	 */
	public final Control createControl(Composite parent) {
		Composite section = FormWidgetFactory.createComposite(parent);
		SectionLayout slayout = new SectionLayout();
		section.setLayout(slayout);
		section.setData(this);

		if (headerPainted) {
			header = FormWidgetFactory.createHeadingLabel(section, getHeaderText(), SWT.WRAP);
		}

		separator = FormWidgetFactory.createCompositeSeparator(section);
		if (descriptionPainted && description != null) {
			descriptionLabel = FormWidgetFactory.createLabel(section, description, SWT.WRAP);
		}
		client = createClient(section);
		section.setData(this);
		control = section;
		return section;
	}

	/**
	 * Creates the text field with the given parent and one column cell that the
	 * control will take up. Creates the label before the text field with the
	 * given parent and the given label text and tool tip.
	 * 
	 * @param parent
	 *            the parent of the label and text field
	 * @param label
	 *            the label text
	 * @param tooltip
	 *            the label tool tip
	 * @return Returns the created text field
	 */
	protected Text createText(Composite parent, String label, String tooltip) {
		return createText(parent, label, tooltip, 1);
	}

	/**
	 * Creates the text field with the given parent and the number of column
	 * cells that the control will take up. Creates the label before the text
	 * field with the given parent and the given label text and tool tip.
	 * 
	 * @param parent
	 *            the parent of the label and text field
	 * @param label
	 *            the label text
	 * @param tooltip
	 *            the label tool tip
	 * @param span
	 *            specifies the number of column cells that the control will
	 *            take up
	 * @return Returns the created text field
	 */
	protected Text createText(Composite parent, String label, String tooltip, int span) {
		Label l = FormWidgetFactory.createLabel(parent, label);
		if (tooltip != null) {
			l.setToolTipText(tooltip);
		}
		Text text = FormWidgetFactory.createText(parent, "");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = span;
		text.setLayoutData(gd);
		return text;
	}

	/**
	 * Creates the text field with the given parent and the number of column
	 * cells that the control will take up.
	 * 
	 * @param parent
	 *            the parent of the text field
	 * @param span
	 *            specifies the number of column cells that the control will
	 *            take up
	 * @return Returns the created text field
	 */
	protected Text createText(Composite parent, int span) {
		Text text = FormWidgetFactory.createText(parent, "");
		GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = span;
		text.setLayoutData(gd);
		return text;
	}

	/**
	 * Calls dispose for each one components of the FormSection: for header and
	 * description label, for separator control and for custom created composite
	 * control.
	 */
	public void dispose() {
		JFaceResources.getFontRegistry().removeListener(this);
		if (header != null)
			header.dispose();
		if (descriptionLabel != null)
			descriptionLabel.dispose();
		separator.dispose();
		control.dispose();
		client.dispose();
	}

	/**
	 * Returns the description of this FormSection.
	 * 
	 * @return Returns the description of the FormSection
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the header text of this FormSection.
	 * 
	 * @return Returns the header text of the FormSection
	 */
	public String getHeaderText() {
		return headerText;
	}

	/**
	 * Initializes the all custom created controls with the given
	 * <code>Object</code>
	 * 
	 * @param input
	 */
	public void initialize(Object input) {
	}

	/**
	 * Returns if the description will be painted or not. Default value is
	 * <code>true</code>
	 * 
	 * @return Returns if the description will be painted or not
	 */
	public boolean isDescriptionPainted() {
		return descriptionPainted;
	}

	/**
	 * @return
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * @return
	 */
	public boolean isHeaderPainted() {
		return headerPainted;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse
	 * .jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent arg0) {
		if (control != null && header != null) {
			header.setFont(JFaceResources.getBannerFont());
			control.layout(true);
		}
	}

	/**
	 * Notifies that the section is changed
	 * 
	 * @param source
	 *            the FormSection which will be changed
	 * @param changeType
	 *            the type of changing
	 * @param changeObject
	 *            the changing element
	 */
	public void sectionChanged(FormSection source, int changeType, Object changeObject) {
	}

	/**
	 * Sets the new description into the description label in this FormSection
	 * 
	 * @param newDescription
	 *            the new description
	 */
	public void setDescription(String newDescription) {
		// we will trim the new lines so that we can
		// use layout-based word wrapping instead
		// of hard-coded one
		description = trimNewLines(newDescription);
		if (descriptionLabel != null)
			descriptionLabel.setText(newDescription);
	}

	/**
	 * Sets the <code>boolean</code> flag which shows if the description will be
	 * appeared in this FormSection. The default value is <code>true</code>.
	 * 
	 * @param newDescriptionPainted
	 *            value of flag which shows or hide the description of this
	 *            FormSection
	 */
	public void setDescriptionPainted(boolean newDescriptionPainted) {
		descriptionPainted = newDescriptionPainted;
	}

	/**
	 * Sets is this FormSection changing or not
	 * 
	 * @param newDirty
	 *            <code>true</code> if the FormSection is changing, otherwise
	 *            <code>false</code>
	 */
	public void setDirty(boolean newDirty) {
		dirty = newDirty;
	}

	/**
	 * Sets the focus.
	 */
	public void setFocus() {
		// empty default implementation
	}

	/**
	 * Sets the <code>boolean</code> flag which shows if the header title will
	 * be appeared in this FormSection. The default value is <code>true</code>.
	 * 
	 * @param newHeaderPainted
	 *            value of flag which shows or hide the header title of this
	 *            FormSection
	 */
	public void setHeaderPainted(boolean newHeaderPainted) {
		headerPainted = newHeaderPainted;
	}

	/**
	 * Sets the given text in to the header title.
	 * 
	 * @param newHeaderText
	 *            the new header title text
	 */
	public void setHeaderText(String newHeaderText) {
		headerText = newHeaderText;
		if (header != null)
			header.setText(headerText);
	}

	/**
	 * Returns a copy of the given string, with all new lines omitted.
	 * 
	 * @param text
	 *            the text in which will be removed the new lines
	 * @return A copy of the given string with all new lines removed, or this
	 *         string if it has no new lines.
	 */
	private String trimNewLines(String text) {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '\n')
				buff.append(' ');
			else
				buff.append(c);
		}
		return buff.toString();
	}

	/**
	 * Updates the values of this FormSection.
	 */
	public void update() {
	}
}

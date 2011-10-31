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
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;

/**
 * Static class that sharing all created methods for controls: text, label,
 * combo, group and so on.
 */
public class FormWidgetFactory {
	/** Holds the default header color */
	public static final String DEFAULT_HEADER_COLOR = "__default__header__";
	/** Holds the color of the composite separator */
	public static final String COLOR_COMPOSITE_SEPARATOR = "__compSep";

	/** The default style of constructed controls */
	public static final int BORDER_STYLE = SWT.NONE;

	/** The KeyListener */
	private static KeyListener deleteListener;
	/** The PaintListener */
	private static BorderPainter borderPainter;

	/**
	 * Holds <code>boolean</code> flag shows is this control is initialized or
	 * not.
	 */
	private static boolean isInit = false;

	/**
	 * Class provide methods that deal with the events that are generated when
	 * the control needs to be painted.
	 */
	static class BorderPainter implements PaintListener {
		public void paintControl(PaintEvent event) {
			Composite composite = (Composite) event.widget;
			Control[] children = composite.getChildren();
			for (int i = 0; i < children.length; i++) {
				Control c = children[i];
				if (c instanceof Text || c instanceof Canvas || c instanceof CCombo) {
					Rectangle b = c.getBounds();
					GC gc = event.gc;
					gc.setForeground(c.getBackground());
					gc.drawRectangle(b.x - 1, b.y - 1, b.width + 1, b.height + 1);
					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
					gc.drawRectangle(b.x - 2, b.y - 2, b.width + 3, b.height + 3);
				} else if (c instanceof Table || c instanceof Tree) {
					Rectangle b = c.getBounds();
					GC gc = event.gc;
					gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BORDER));
					gc.drawRectangle(b.x - 1, b.y - 1, b.width + 2, b.height + 2);
				}
			}
		}
	}

	/**
	 * Creates an etched border given its parent and sets the receiver's title,
	 * which is the string that will be displayed as the receiver's
	 * <em>title</em> of the created group.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param title
	 *            the text, which will be displayed as a title
	 * @return Returns an etched border with an optional title
	 */
	public static Group createGroup(Composite parent, String title) {
		return createGroup(parent, title, SWT.NULL);
	}

	/**
	 * Creates an etched border given its parent and a style value. Sets the
	 * receiver's title, which is the string that will be displayed as the
	 * receiver's <em>title</em> of the created group.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param title
	 *            the text, which will be displayed as a title
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return Returns an etched border with an optional title
	 */
	public static Group createGroup(Composite parent, String title, int style) {
		Group group = new Group(parent, style);
		group.setBackgroundMode(SWT.INHERIT_DEFAULT);
		if (title != null)
			group.setText(title);
		return group;
	}

	/**
	 * Creates the control that allow the user to choose an item from a list of
	 * items given its parent and style.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created control
	 */
	public static Combo createCombo(Composite parent, int style) {
		Combo combo = new Combo(parent, style);
		return combo;
	}

	/**
	 * Creates the control given it parent and it style, that allow the user to
	 * choose an item from list of items and to write your own item.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created control
	 */
	public static CCombo createCCombo(Composite parent, int style) {
		CCombo combo = new CCombo(parent, style);
		return combo;
	}

	/**
	 * Creates the control that allow the user to choose an item from a list of
	 * items given it parent and sets the default style to be
	 * <code>SWT.NULL</code>.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @return the created control
	 */
	public static Combo createCombo(Composite parent) {
		return createCombo(parent, SWT.NULL);
	}

	/**
	 * Creates the control given it parent and sets the default style to be
	 * <code>SWT.NULL</code>, that allow the user to choose an item from list of
	 * items and to write your own item.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @return the created control
	 */
	public static CCombo createCCombo(Composite parent) {
		return createCCombo(parent, SWT.NULL);
	}

	/**
	 * Creates a selectable user object that issues notification when pressed
	 * and released given its parent, a style and a text.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param text
	 *            the button label
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created control
	 */
	public static Button createButton(Composite parent, String text, int style) {
		int flatStyle = SWT.FLAT;
		Button button = new Button(parent, style | flatStyle);
		if (text != null)
			button.setText(text);
		return button;
	}

	/**
	 * Creates a selectable user object that issues notification when pressed
	 * and released given its parent and a text. Sets the style to be
	 * <code>SWT.NULL</code>
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param text
	 *            the button label
	 * @return the created control
	 */
	public static Button createRadioButton(Composite parent, String text) {
		Button button = new Button(parent, SWT.RADIO);
		if (text != null)
			button.setText(text);
		return button;
	}

	/**
	 * Creates the control which is capable of containing other controls given
	 * it parent and sets a style <code>SWT.NULL</code>. Adds the mouse listener
	 * of this control.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @return the created control
	 */
	public static Composite createComposite(Composite parent) {
		return createComposite(parent, SWT.NULL);
	}

	/**
	 * Creates the SashForm from given parent and sets a style
	 * <code>SWT.NULL</code> The SashForm lays out its children in a Row or
	 * Column arrangement (as specified by the orientation) and places a Sash
	 * between the children. Adds the mouse listener of this control.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @return the created control
	 */
	public static SashForm createSashForm(Composite parent) {
		SashForm composite = new SashForm(parent, SWT.NULL);
		composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
		return composite;
	}

	/**
	 * Creates the control which is capable of containing other controls given
	 * its parent and a style. Adds the mouse listener of this control.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created control
	 */
	public static Composite createComposite(Composite parent, int style) {
		Composite composite = new Composite(parent, style);
		composite.setBackgroundMode(SWT.INHERIT_DEFAULT);
		return composite;
	}

	/**
	 * Creates the control which is separator given it parent and sets a style
	 * <code>SWT.NONE</code>.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @return the created control
	 */
	public static Composite createCompositeSeparator(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BORDER));
		return composite;
	}

	/**
	 * Creates the non-selectable object that displays a string with the
	 * specified font size for heading given its parent, a text and a style.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param text
	 *            the receiver's text
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created control
	 */
	public static Label createHeadingLabel(Composite parent, String text, /*
																		 * Color
																		 * bg,
																		 */
	int style) {
		Label label = new Label(parent, style);
		if (text != null)
			label.setText(text);
		label.setFont(JFaceResources.getFontRegistry().get(JFaceResources.BANNER_FONT));
		return label;
	}

	/**
	 * Creates the non-selectable object that displays a string given its
	 * parent, a text and sets a style <code>SWT.NONE</code>.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param text
	 *            the receiver's text
	 * @return the created control
	 */
	public static Label createLabel(Composite parent, String text) {
		return createLabel(parent, text, SWT.NONE);
	}

	/**
	 * Creates the non-selectable object that displays a string given its
	 * parent, a text and a style.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param text
	 *            the receiver's text
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created control
	 */
	public static Label createLabel(Composite parent, String text, int style) {
		Label label = new Label(parent, style);
		if (text != null)
			label.setText(text);
		return label;
	}

	/**
	 * Creates the non-selectable object that displays a string given its parent
	 * and a style which must be combined with the line separator behavior.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created control
	 */
	public static Label createSeparator(Composite parent, int style) {
		Label label = new Label(parent, SWT.SEPARATOR | style);
		return label;
	}

	/**
	 * Creates the table component given its parent and a style.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created table
	 */
	public static Table createTable(Composite parent, int style) {
		Table table = new Table(parent, BORDER_STYLE | style);
		hookDeleteListener(table);
		return table;
	}

	/**
	 * Creates the object that allow the user to enter and modify text given its
	 * parent, a text value and sets a style <code>SWT.SINGLE</code>.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created text
	 */
	public static Text createText(Composite parent, String value) {
		return createText(parent, value, BORDER_STYLE | SWT.SINGLE);
	}

	/**
	 * Creates the object that allow the user to enter and modify text given its
	 * parent, a text value and a style.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param value
	 *            the value of the text in this component
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created text
	 */
	public static Text createText(Composite parent, String value, int style) {
		Text text = new Text(parent, style);
		if (value != null)
			text.setText(value);
		return text;
	}

	/**
	 * Creates a selectable object that displays a hierarchy of items and issues
	 * notification when an item in the hierarchy is selected given its parent
	 * and a style.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the new
	 *            instance (cannot be null)
	 * @param style
	 *            the style of control describing the behavior of the control
	 * @return the created tree
	 */
	public static Tree createTree(Composite parent, int style) {
		Tree tree = new Tree(parent, BORDER_STYLE | style);
		hookDeleteListener(tree);
		return tree;
	}

	/**
	 * Returns the background color
	 * 
	 * @return the background color
	 * @deprecated
	 */
	public static Color getBackgroundColor() {
		return null;
	}

	/**
	 * Creates and adds key listener to the given control
	 * 
	 * @param control
	 *            the control in which will be added key listener
	 */
	public static void hookDeleteListener(Control control) {
		if (deleteListener == null) {
			deleteListener = new KeyAdapter() {
				public void keyPressed(KeyEvent event) {
				}
			};
		}
		control.addKeyListener(deleteListener);
	}

	/**
	 * Creates if needed and adds to the given parent this paint listener.
	 * 
	 * @param parent
	 *            the control in which will be added paint listener
	 */
	public static void paintBordersFor(Composite parent) {
		if (BORDER_STYLE == SWT.BORDER)
			return;
		if (borderPainter == null)
			borderPainter = new BorderPainter();
		parent.addPaintListener(borderPainter);
	}

	public static void init() {
		if (!isInit) {
			isInit = true;
		}
	}

	public static void close() {
		if (isInit) {
			isInit = false;
		}
	}
}

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

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Form is a custom control that renders a title and an optional background
 * image above the body composite. The form supports status messages. These
 * messages can have various severity (error, warning, info or none).
 * <p>
 * Children of the form are sections. One Form can has one or more sections if
 * the custom registered in the same Form.
 * 
 */
public class Form implements PaintListener, IPropertyChangeListener {

  /** The title of the Form */
  private String    title;
  /** Holds all sections in this Form */
  private Vector    sections;
  /** Composite thats provide a surface for drawing arbitrary graphics. */
  private Composite control;
  /** Holds the font of the title */
  private Font      titleFont;

  /** Holds value of the title horizontal margin */
  private int       TITLE_HMARGIN  = 10;
  /** Holds value of the title vertical margin */
  private int       TITLE_VMARGIN  = 5;

  /** Shows if header of this form will be visible or not */
  private boolean   headingVisible = true;

  /**
   * This is a custom layout for the form. This layout arranges form sections,
   * depending on the title height.
   */
  class FormLayout extends Layout {

    @Override
    protected Point computeSize(Composite composite, int wHint, int hHint, boolean flushCache) {

      if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT) {
        return new Point(wHint, hHint);
      }
      Control client = composite.getChildren()[0];
      Point csize = client.computeSize(SWT.DEFAULT, SWT.DEFAULT, flushCache);
      if (headingVisible) {
        csize.y += getTitleHeight();
      }
      return csize;
    }

    @Override
    protected void layout(Composite composite, boolean flushCache) {

      Rectangle clientArea = composite.getClientArea();
      Control client = composite.getChildren()[0];
      int theight = headingVisible ? getTitleHeight() : 0;
      client.setBounds(clientArea.x, clientArea.y + theight, clientArea.width, clientArea.height - theight);
    }
  }

  /**
   * Creates the form content control.
   */
  public Form() {
    titleFont = JFaceResources.getHeaderFont();
    JFaceResources.getFontRegistry().addListener(this);
  }

  /**
   * Checks if the given control is instance of <code>Text</code> and checks if
   * the given <code>id</code> is one of the <code>ActionFactory.CUT</code> ,
   * <code>ActionFactory.COPY</code>, <code>ActionFactory.PASTE</code>,
   * <code>ActionFactory.SELECT_ALL</code>, <code>ActionFactory.DELETE</code>
   * 
   * @param id
   *          id of the action factory
   * @param control
   *          checked control
   * @return <code>true</code> if the control is text and the id is one of the
   *         <code>ActionFactory</code> cut, copy, paste, select all or delete,
   *         <code>false</code> otherwise
   */
  private boolean canPerformDirectly(String id, Control control) {
    if (control instanceof Text) {
      Text text = (Text) control;
      if (id.equals(ActionFactory.CUT.getId())) {
        text.cut();
        return true;
      }
      if (id.equals(ActionFactory.COPY.getId())) {
        text.copy();
        return true;
      }
      if (id.equals(ActionFactory.PASTE.getId())) {
        text.paste();
        return true;
      }
      if (id.equals(ActionFactory.SELECT_ALL.getId())) {
        text.selectAll();
        return true;
      }
      if (id.equals(ActionFactory.DELETE.getId())) {
        int count = text.getSelectionCount();
        if (count == 0) {
          int caretPos = text.getCaretPosition();
          text.setSelection(caretPos, caretPos + 1);
        }
        text.insert("");
        return true;
      }
    }
    return false;
  }

  /**
   * Notifies the all listeners of the each <code>FormSection</code> of this
   * Form of the new values of the components and sets that this form is changed
   * and need to be saved.
   * 
   * @param onSave
   *          <code>boolean</code> parameter to sets if the changes will be
   *          saved or not
   */
  public void commitChanges(boolean onSave) {
    if (sections != null) {
      for (Iterator iter = sections.iterator(); iter.hasNext();) {
        FormSection section = (FormSection) iter.next();
        if (section.isDirty()) {
          section.commitChanges(onSave);
        }
      }
    }
  }

  /**
   * Provides a surface for drawing arbitrary graphics. Constructs a new
   * instance of the control given its parent and creates the new sections in
   * this Form.
   * 
   * @param parent
   *          a composite control which will be the parent of the new instance
   *          (cannot be null)
   * @return a new instance of the control with all his components
   */
  public Control createControl(Composite parent) {
    control = new Canvas(parent, SWT.NONE);
    control.setBackgroundMode(SWT.INHERIT_DEFAULT);
    control.addPaintListener(this);
    control.setLayout(new FormLayout());
    control.setFocus();
    Composite formParent = FormWidgetFactory.createComposite(control);
    createFormClient(formParent);
    return control;
  }

  /**
   * This method is called from the <code>createControl</code> method and must
   * be overwrite from the custom implementation of the Form class and to put
   * all form section in this form.
   * 
   * @param parent
   *          a composite control which will be the parent of the created client
   *          form
   */
  protected void createFormClient(Composite parent) {
    FormWidgetFactory.createComposite(parent);
  }

  /**
   * Calls dispose for each one of the FormSection added in this Form
   */
  public void dispose() {

    if (sections != null) {
      for (Iterator iter = sections.iterator(); iter.hasNext();) {
        FormSection section = (FormSection) iter.next();
        section.dispose();
        section = null;
      }
    }
    JFaceResources.getFontRegistry().removeListener(this);
  }

  /**
   * Returns the container that occupies the body of the form. Use this
   * container as a parent for the controls that should be in the form.
   * 
   * @return Returns the control in which are all of the form sections.
   */
  public Composite getControl() {
    return control;
  }

  /**
   * Gets the title of this Form.
   * 
   * @return Returns the title of this Form
   */
  public String getTitle() {
    return title;
  }

  /**
   * Gets the height of the title
   * 
   * @return Returns as an <code>int</code> value the height of the title
   */
  private int getTitleHeight() {
    if ((title == null) || (title.equals(""))) {
      return 0;
    }
    GC gc = new GC(control);
    gc.setFont(titleFont);
    FontMetrics fm = gc.getFontMetrics();
    int fontHeight = fm.getHeight();
    gc.dispose();

    int height = fontHeight + TITLE_VMARGIN + TITLE_VMARGIN;
    return height;
  }

  /**
   * Calls the initialize method of the all added FormSections
   * 
   * @param model
   */
  public void initialize(Object model) {
    if (sections != null) {
      for (Iterator iter = sections.iterator(); iter.hasNext();) {
        FormSection section = (FormSection) iter.next();
        section.initialize(model);
      }
    }
  }

  /**
   * Returns if the heading is visible or not
   * 
   * @return Returns <code>true</code> if heading is visible, <code>false</code>
   *         otherwise
   */
  public boolean isHeadingVisible() {
    return headingVisible;
  }

  /**
   * Sets the chosen form font, foreground and the title of the given
   * <code>GC</code>
   * 
   * @param form
   *          the form control
   * @param gc
   *          the <code>GC</code> in which will be set the chosen font,
   *          foreground and title
   */
  private void paint(Control form, GC gc) {
    gc.setFont(titleFont);
    gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
    gc.drawText(getTitle(), TITLE_HMARGIN, TITLE_VMARGIN, true);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.swt.events.PaintListener#paintControl(org.eclipse.swt.events
   * .PaintEvent)
   */
  public final void paintControl(PaintEvent event) {
    if (!headingVisible) {
      return;
    }
    GC gc = event.gc;
    Control form = (Control) event.widget;
    paint(form, gc);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse
   * .jface.util.PropertyChangeEvent)
   */
  public void propertyChange(PropertyChangeEvent arg0) {
    titleFont = JFaceResources.getHeaderFont();
    if (control != null) {
      control.layout(true);
      control.redraw();
    }
  }

  /**
   * Adds the given <code>FormSection</code> in a <code>Vector</code> with all
   * registered in this Form <code>FormSection</code>
   * 
   * @param section
   *          the <code>FormSelection</code> that will be added
   */
  public void registerSection(FormSection section) {
    if (sections == null) {
      sections = new Vector();
    }
    sections.addElement(section);
  }

  /**
   * Sets the focus on the first section of all added sections in this form
   */
  public void setFocus() {
    if (sections != null && sections.size() > 0) {
      FormSection firstSection = (FormSection) sections.firstElement();
      firstSection.setFocus();
    }
  }

  /**
   * Sets the heading visible or not depends on the given <code>boolean</code>
   * value.
   * 
   * @param newHeadingVisible
   *          the value on which depends if the heading will be visible or not,
   *          <code>true</code> the heading will become visible,
   *          <code>false</code> the heading will become invisible
   */
  public void setHeadingVisible(boolean newHeadingVisible) {
    if (newHeadingVisible != headingVisible) {
      headingVisible = newHeadingVisible;
      if (control != null) {
        control.layout(true);
      }
    }
  }

  /**
   * Sets the new title of the Form
   * 
   * @param newTitle
   *          the new title of the Form
   */
  public void setTitle(String newTitle) {
    title = newTitle;
    if (control != null) {
      control.redraw();
    }
  }

  /**
   * Calls the <code>update</code> method of all registered in this Form
   * <code>FormSection</code>s.
   */
  public void update() {
    if (sections != null) {
      for (Iterator iter = sections.iterator(); iter.hasNext();) {
        FormSection section = (FormSection) iter.next();
        section.update();
      }
    }
  }

  // Clipboard Actions

  /**
   * Returns the control which is on focus.
   * 
   * @return Returns the control which is on focus
   */
  protected Control getFocusControl() {
    Control control = getControl();
    if (control == null || control.isDisposed()) {
      return null;
    }
    Display display = control.getDisplay();
    Control focusControl = display.getFocusControl();
    if (focusControl == null || focusControl.isDisposed()) {
      return null;
    }
    return focusControl;
  }

  /**
   * Gets focus control and checks if in this control can perform directly (if
   * the focus control is text field and the action id is one of the cut, copy,
   * paste, select all and delete).
   * 
   * @param actionId
   *          the action factory id
   * @return Returns <code>true</code> if the focus control is text field and
   *         the action id is one of the <code>ActionFactory</code> cut, copy,
   *         paste, select all and delete, <code>false</code> otherwise
   */
  public boolean doGlobalAction(String actionId) {
    Control focusControl = getFocusControl();
    if (focusControl == null) {
      return false;
    }
    return canPerformDirectly(actionId, focusControl);
  }
}

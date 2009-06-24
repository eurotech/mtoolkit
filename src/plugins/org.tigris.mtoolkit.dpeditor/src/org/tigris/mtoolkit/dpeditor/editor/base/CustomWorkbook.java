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
package org.tigris.mtoolkit.dpeditor.editor.base;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.tigris.mtoolkit.dpeditor.editor.SourceFormPage;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormSelectionListener;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook;

/**
 * Custom created work area.
 */
public class CustomWorkbook extends SelectionAdapter implements IFormWorkbook {

	/**
	 * Holds all pages in this custom area. The key is the FormPage, the value
	 * is created TabItem that presents the FormPage.
	 */
	private Hashtable pages;
	/** The current page */
	private IFormPage currentPage;
	/**
	 * The folder that holds all pages and allows the user to select a notebook
	 * page from set of pages.
	 */
	private CTabFolder tabFolder;

	// form selection listeners
	/** Holds all listeners added to this custom area. */
	private Vector listeners = new Vector();

	/**
	 * Holds the value is the first page must be selected when the editor become
	 * visible.
	 */
	private boolean firstPageSelected = true;
	/** Holds the value of this if the pages are visible or invisible */
	private boolean disable;

	/** The selected page before the current page */
	private IFormPage oldPage;

	/**
	 * Creates a new instance of this custom area.
	 */
	public CustomWorkbook() {
		pages = new Hashtable();
	}

	/**
	 * Adds to the all listeners of this custom area the given
	 * <code>IFormSelectionListener</code>.
	 * 
	 * @param listener
	 *            the new added <code>IFormSelectionListener</code>
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#addFormSelectionListener(org.tigris.mtoolkit.dpeditor.editor.forms.IFormSelectionListener)
	 */
	public void addFormSelectionListener(IFormSelectionListener listener) {
		listeners.addElement(listener);
	}

	/**
	 * Adds a tab for the given Form page.
	 * 
	 * @param page
	 *            the page which will be added
	 * 
	 * @see #addPage(IFormPage, CTabFolder)
	 */
	public void addPage(IFormPage page) {

		addPage(page, tabFolder);
		if (firstPageSelected && currentPage == null) {
			selectPage(page);
		}
	}

	/**
	 * Adds a tab for the given page. If the page is a single page, the data is
	 * set to the tab item, else if the page contains several pages, a tab
	 * folder is created for it.
	 * 
	 * @param page
	 *            the form page to be added
	 */

	private void addPage(IFormPage page, CTabFolder folder) {

		CTabItem item = new CTabItem(folder, SWT.CENTER);
		item.setText(page.getLabel());
		item.setToolTipText(page.getTitle());

		if (page instanceof DPPMultiFormPage) {
			DPPMultiFormPage multiPage = (DPPMultiFormPage) page;
			CTabFolder innerFolder = new CTabFolder(tabFolder, SWT.BOTTOM);
			innerFolder.addSelectionListener(this);

			for (Iterator iter = multiPage.getPages(); iter.hasNext();) {
				IFormPage nextPage = (IFormPage) iter.next();
				addPage(nextPage, innerFolder);
			}
			item.setControl(innerFolder);
			item.setData(multiPage);
		} else {
			item.setData(page);
		}

		pages.put(page, item);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.
	 * events.SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent e) {
		CTabItem item = (CTabItem) e.item;
		if (item.getData() instanceof IFormPage) {
			IFormPage page = (IFormPage) item.getData();
			if (page != null) {
				selectPage(page);
			}
		}
	}

	/**
	 * Creates the SWT controls of this custom area.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		tabFolder = new CTabFolder(parent, SWT.BOTTOM);
		tabFolder.addSelectionListener(this);
		// listener to resize visible components
		tabFolder.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				if (currentPage != null) {
					setControlSize(currentPage.getControl());
				}
			}
		});
	}

	/**
	 * Notifies all added <code>IFormSelectionListener</code>s the occurred
	 * selection change.
	 * 
	 * @param page
	 *            the page that was selected
	 */
	private void fireSelectionChanged(IFormPage page) {
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IFormSelectionListener listener = (IFormSelectionListener) iter.next();
			listener.formSelected(page);
		}
	}

	/**
	 * Returns the composite control which is a parent of this form page
	 * 
	 * @return the page composite control
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#getControl()
	 */
	public Control getControl() {
		return tabFolder;
	}

	/**
	 * Returns the current visible and selected page.
	 * 
	 * @return the current page
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#getCurrentPage()
	 */
	public IFormPage getCurrentPage() {
		return currentPage;
	}

	/**
	 * Checks is the first page of the editor is selected and visible.
	 * 
	 * @return <code>true</code> if the first page is selected, otherwise
	 *         returns <code>false</code>
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#isFirstPageSelected()
	 */
	public boolean isFirstPageSelected() {
		return firstPageSelected;
	}

	/**
	 * Removes the given <code>IFormSelectionListener</code> from the Vector
	 * with all added listeners in this custom workbook.
	 * 
	 * @param listener
	 *            the listener will be removed
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#removeFormSelectionListener(org.tigris.mtoolkit.dpeditor.editor.forms.IFormSelectionListener)
	 */
	public void removeFormSelectionListener(IFormSelectionListener listener) {
		listeners.removeElement(listener);
	}

	/**
	 * Removes the given page from the custom area. Removes and corresponding
	 * with this page tab item.
	 * 
	 * @param page
	 *            the page which will be removed
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#removePage(org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
	 */
	public void removePage(IFormPage page) {
		CTabItem item = (CTabItem) pages.get(page);
		if (item != null) {
			item.dispose();
		}
	}

	/**
	 * Select the given page again.
	 * 
	 * @param page
	 *            the page that will be selected
	 */
	private void reselectPage(final IFormPage page) {
		tabFolder.getDisplay().asyncExec(new Runnable() {
			public void run() {
				selectPage(page);
			}
		});
	}

	/**
	 * Selects the given new form page and switch the current page to be the new
	 * one.
	 * 
	 * @param page
	 *            the new page will be selected
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#selectPage(org.tigris.mtoolkit.dpeditor.editor.forms.IFormPage)
	 */
	public void selectPage(final IFormPage page) {
		final IFormPage oldPage = currentPage;
		this.oldPage = currentPage;
		currentPage = page;

		// It may take a while
		BusyIndicator.showWhile(tabFolder.getDisplay(), new Runnable() {
			public void run() {
				switchPages(oldPage, page);
			}
		});
	}

	/**
	 * Returns the previously selected page.
	 * 
	 * @return the previously selected page
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#getOldPage()
	 */
	public IFormPage getOldPage() {
		return oldPage;
	}

	/**
	 * Sets the control size, depending on the given control size.
	 * 
	 * @param control
	 *            the control
	 */
	private void setControlSize(Control control) {
		if (control == null) {
			return;
		}
		CTabFolder parent = (CTabFolder) control.getParent();
		Rectangle bounds = parent.getBounds();
		Rectangle offset = parent.getClientArea();
		bounds.x += offset.x;
		bounds.y += offset.y;
		bounds.width = offset.width;
		bounds.height = offset.height;
		control.setBounds(bounds);
		control.moveAbove(parent);
	}

	/**
	 * Sets the given control to be visible.
	 * 
	 * @param control
	 *            the control
	 */
	private void setControlVisible(Control control) {
		if (control == null) {
			return;
		}
		setControlSize(control);
		control.setVisible(true);
	}

	/**
	 * Sets if the first page will be visible.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormWorkbook#setFirstPageSelected(boolean)
	 */
	public void setFirstPageSelected(boolean newFirstPageSelected) {
		firstPageSelected = newFirstPageSelected;
	}

	/**
	 * Switches between the pages and the tab items.
	 * 
	 * @param oldPage
	 *            the page that will become invisible
	 * @param newPage
	 *            the page that will become visible and will be selected
	 */
	private void switchPages(IFormPage oldPage, IFormPage newPage) {

		if (oldPage != null && oldPage != newPage) {
			boolean okToSwitch = oldPage.becomesInvisible(newPage);
			if (!okToSwitch) {
				// We must try to go back to the old page
				reselectPage(oldPage);
				return;
			}
		}
		if (newPage == null)
			return;

		CTabItem parentItem = (CTabItem) pages.get(newPage);
		if (newPage.getControl() == null) {
			if (newPage instanceof DPPMultiFormPage) {
				CTabFolder innerFolder = (CTabFolder) parentItem.getControl();
				CTabItem[] innerItems = innerFolder.getItems();
				for (int i = 0; i < innerItems.length; i++) {
					IFormPage innerPage = (IFormPage) innerItems[i].getData();
					if (innerPage.getControl() == null) {
						innerPage.createControl(innerFolder);
					}
					innerItems[i].setControl(innerPage.getControl());
					if (disable) {
						innerPage.setEditable(false);
					}
				}
			} else {
				newPage.createControl(parentItem.getParent());
				if (disable && !(newPage instanceof SourceFormPage)) {
					newPage.setEditable(false);
				}
			}
		}

		if (newPage instanceof DPPMultiFormPage) {
			tabFolder.setSelection((CTabItem) pages.get(newPage));
			CTabFolder innerFolder = (CTabFolder) parentItem.getControl();
			innerFolder.setSelection(innerFolder.getItem(0));
		} else {
			parentItem.getParent().setSelection(parentItem);
		}

		if (oldPage != null && oldPage != newPage) {
			Control oldControl = oldPage.getControl();
			if (oldControl != null) {
				oldControl.setVisible(false);
			}
		}
		if (newPage instanceof DPPMultiFormPage) {
			CTabFolder innerFolder = (CTabFolder) parentItem.getControl();
			newPage.becomesVisible(oldPage);
			newPage = (IFormPage) innerFolder.getItem(0).getData();
		}

		Control newControl = newPage.getControl();
		newPage.becomesVisible(oldPage);
		setControlVisible(newControl);
		fireSelectionChanged(newPage);
	}

	/**
	 * Disables all components in the editor pages, in order to make editing
	 * impossible. Invoked when the manifest editor is opened for a file from
	 * archive, and not from file system.
	 */
	public void disablePages() {
		disable = true;
		int count = tabFolder.getItemCount();
		if (count != -1) {
			for (int i = 0; i < count; i++) {
				CTabItem next = tabFolder.getItem(i);
				IFormPage iFormPage = ((IFormPage) next.getData());
				if (iFormPage.getControl() != null) {
					iFormPage.setEditable(false);
				}
			}
		}
	}
}

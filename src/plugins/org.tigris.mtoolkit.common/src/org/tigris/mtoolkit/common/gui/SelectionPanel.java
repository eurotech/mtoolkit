/*******************************************************************************
 * Copyright (c) 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.gui;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

/**
 * @since 6.0
 */
public final class SelectionPanel extends Composite {
  private static final Object[] NO_CHILDREN         = new Object[0];

  private final TreeViewer      viewer;
  private final FilteredTree    tree;
  private final ListenerList    checkStateListeners = new ListenerList();

  public SelectionPanel(Composite parent, int style) {
    super(parent, style);

    setLayout(new GridLayout());
    boolean isSingleSelection = (getStyle() & SWT.SINGLE) != 0;

    final int treeStyle = SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.VIRTUAL | style;
    PatternFilter patternFilter = new PatternFilter();
    patternFilter.setIncludeLeadingWildcard(true);

    if (isSingleSelection) {
      tree = new FilteredTree(this, treeStyle, patternFilter, true);
    } else {
      tree = new FilteredCheckboxTree(this, treeStyle, patternFilter);
    }

    viewer = tree.getViewer();
    Tree control = (Tree) viewer.getControl();
    control.setLayoutData(new GridData(GridData.FILL_BOTH));
    control.setLinesVisible(false);
    viewer.setUseHashlookup(true);
    viewer.setLabelProvider(new LabelProvider());
    viewer.setContentProvider(new ITreeContentProvider() {
      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
       */
      public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.IContentProvider#dispose()
       */
      public void dispose() {
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
       */
      public boolean hasChildren(Object element) {
        return getElements(element).length > 0;
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
       */
      public Object getParent(Object element) {
        return null;
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
       */
      public Object[] getElements(Object inputElement) {
        return getChildren(inputElement);
      }

      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
       */
      public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof Object[]) {
          return (Object[]) parentElement;
        }
        return NO_CHILDREN;
      }
    });

    viewer.setSorter(new ViewerSorter() {
      /* (non-Javadoc)
       * @see org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
       */
      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        String name1 = null;
        String name2 = null;
        IBaseLabelProvider labelProvider = ((TreeViewer) viewer).getLabelProvider();
        if (labelProvider instanceof ILabelProvider) {
          ILabelProvider lp = (ILabelProvider) labelProvider;
          name1 = lp.getText(e1);
          name2 = lp.getText(e2);
        }
        if (name1 == null) {
          name1 = "";
        }
        if (name2 == null) {
          name2 = "";
        }
        return name1.compareTo(name2);
      }
    });
    if (viewer instanceof CheckboxTreeViewer) {
      setUpCheckboxViewer((CheckboxTreeViewer) viewer);
    }
  }

  /**
   * @param chbList
   */
  private void setUpCheckboxViewer(final CheckboxTreeViewer chbList) {
    Composite buttonsPanel = new Composite(this, SWT.NONE);
    buttonsPanel.setLayout(new GridLayout(2, true));
    buttonsPanel.setLayoutData(new GridData());

    chbList.addCheckStateListener(new ICheckStateListener() {
      public void checkStateChanged(CheckStateChangedEvent event) {
        fireCheckStateChanged(event);
      }
    });

    Button btnSelect = new Button(buttonsPanel, SWT.PUSH);
    btnSelect.setText("Select all");
    btnSelect.addSelectionListener(new SelectionAdapter() {
      /* (non-Javadoc)
       * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
       */
      @Override
      public void widgetSelected(SelectionEvent event) {
        selectAll(true);
      }
    });
    GridData gridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
    btnSelect.setLayoutData(gridData);

    Button btnDeselect = new Button(buttonsPanel, SWT.PUSH);
    btnDeselect.setText("Deselect all");
    btnDeselect.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent event) {
        selectAll(false);
      }
    });
    gridData = new GridData(SWT.FILL, SWT.CENTER, false, false);
    btnDeselect.setLayoutData(gridData);
  }

  /**
   * Sets to the given value the checked state for all elements. Does not fire
   * events to check state listeners.
   *
   * @param state
   */
  public void selectAll(boolean state) {
    TreeItem[] items = ((Tree) viewer.getControl()).getItems();
    for (int i = 0; i < items.length; i++) {
      items[i].setChecked(state);
    }
  }

  public void setItems(Object[] items) {
    viewer.setInput(items);
    viewer.refresh();
  }

  public Object[] getSelected() {
    if (viewer instanceof CheckboxTreeViewer) {
      return ((CheckboxTreeViewer) viewer).getCheckedElements();
    } else {
      ISelection selection = viewer.getSelection();
      if (selection instanceof IStructuredSelection) {
        return ((IStructuredSelection) selection).toArray();
      }
    }
    return null;
  }

  /**
   * Returns the viewer used for displaying items. It uses <code>ListItem</code>
   * for holding items. The returned viewer can be used to specify custom label
   * providers, etc.
   *
   * @return the viewer
   * @see ListItem
   */
  public TreeViewer getViewer() {
    return viewer;
  }

  /**
   * Adds a listener for changes to the checked state of elements in this panel.
   * Has no effect if an identical listener is already registered. This panel
   * supports sending events with multiple check changes at once (i.e. from
   * Select All/Deselect All buttons)
   *
   * @param listener
   *          a check state listener
   */
  public void addCheckStateListener(ICheckStateListener listener) {
    checkStateListeners.add(listener);
  }

  /**
   * Removes the given check state listener. Has no effect if an identical
   * listener is not registered.
   *
   * @param listener
   *          a check state listener
   */
  public void removeCheckStateListener(ICheckStateListener listener) { // NO_UCD
    checkStateListeners.remove(listener);
  }

  private void fireCheckStateChanged(final CheckStateChangedEvent event) {
    Object[] array = checkStateListeners.getListeners();
    for (int i = 0; i < array.length; i++) {
      final ICheckStateListener l = (ICheckStateListener) array[i];
      SafeRunnable.run(new SafeRunnable() {
        /* (non-Javadoc)
         * @see org.eclipse.core.runtime.ISafeRunnable#run()
         */
        public void run() {
          l.checkStateChanged(event);
        }
      });
    }
  }
}

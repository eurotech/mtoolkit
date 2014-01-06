/*
 * Copyright (c) 2012 by ProSyst Software GmbH
 * http://www.prosyst.com
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of ProSyst Software GmbH. You shall not disclose such Confidential
 * Information and shall use it only in accordance with the terms
 * of the license agreement you entered into with ProSyst.
 */
package org.tigris.mtoolkit.common.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

public final class FilteredCheckboxTree extends FilteredTree {

  private WorkbenchJob refreshJob;

  /**
   * Create a new instance of the receiver.
   *
   * @parent the parent Composite
   * @treeStyle the style bits for the Tree
   * @filter the filter to be used
   * @useNewLook true if the new 3.5 look should be used
   */
  public FilteredCheckboxTree(Composite parent, int treeStyle, PatternFilter filter) {
    super(parent, treeStyle, filter, true);
  }

  /**
   * Return the offset of the Tree from the top of the container.
   *
   * @return the offset in pixels
   */
  public int getTreeLocationOffset() {
    GridLayout layout = (GridLayout) getLayout();
    int space = layout.horizontalSpacing + layout.marginTop + ((GridData) getLayoutData()).verticalIndent + 1;
    Text filterText = getFilterControl();
    if (filterText != null) {
      space += filterText.getSize().y;
    }
    return space;
  }

  /**
   * Reset filter and refresh tree
   */
  public void resetFilter() {
    // Reset filter filed and start a synchronous refresh job
    Text filterText = getFilterControl();
    if (filterText != null) {
      filterText.setText(this.initialText);
    }
    refreshJob.cancel();
    refreshJob.runInUIThread(new NullProgressMonitor());
  }

  @Override
  public void setEnabled(boolean enabled) {
    if ((filterText.getStyle() & SWT.ICON_CANCEL) == 0) { // filter uses FilteredTree new look, not native
      int filterColor = enabled ? SWT.COLOR_LIST_BACKGROUND : SWT.COLOR_WIDGET_BACKGROUND;
      filterComposite.setBackground(getDisplay().getSystemColor(filterColor));
    }
    filterText.setEnabled(enabled);
    treeViewer.getTree().setEnabled(enabled);
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.dialogs.FilteredTree#doCreateRefreshJob()
   */
  @Override
  protected WorkbenchJob doCreateRefreshJob() {
    WorkbenchJob job = super.doCreateRefreshJob();
    refreshJob = job;
    return job;
  }

  /*
   * (non-Javadoc)
   * @see org.eclipse.ui.dialogs.FilteredTree#doCreateTreeViewer(org.eclipse.swt.widgets.Composite, int)
   */
  @Override
  protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
    return new FilterableCheckboxTreeViewer(parent, style);
  }

  /**
   * CheckboxTreeViewer that represents nodes internally.
   */
  public class FilterableCheckboxTreeViewer extends CheckboxTreeViewer {
    /**
     * FilterableCheckboxTreeViewer node.
     */
    class FilteredCheckboxTreeItem {
      Object itemData;                  // data
      byte   checkedState;              // checkbox state
      List   children = new ArrayList();

      public FilteredCheckboxTreeItem(Object itemData, byte checkedState, Map itemCache, FilteredCheckboxTreeItem parent) {
        this.itemData = itemData;
        this.checkedState = checkedState;
        itemCache.put(itemData, this);
        if (parent != null) {
          parent.children.add(this);
        }
      }
    }

    static final byte STATE_NONE           = 0;
    static final byte STATE_CHECKED        = 1;
    static final byte STATE_GREYED         = 2;

    static final byte STATE_CHECKED_GREYED = 3;

    /* Tree nodes cache */
    Map  itemCache           = new HashMap();
    /* pre-refresh listener cache */
    List refreshingListeners = new ArrayList();

    /**
     * Create a new FilterableCheckboxTreeViewer.
     *
     * @parent the parent control
     * @style the SWT style bits
     */
    public FilterableCheckboxTreeViewer(Composite parent, int style) {
      super(parent, style);
      addCheckStateListener(new ICheckStateListener() {

        public void checkStateChanged(CheckStateChangedEvent event) {
          FilteredCheckboxTreeItem item = (FilteredCheckboxTreeItem) itemCache.get(event.getElement());
          if (item != null) {
            item.checkedState = event.getChecked() ? STATE_CHECKED : STATE_NONE;
          }
        }
      });
    }

    /**
     * Add a {@link PreRefreshNotifier}
     */
    public void addPreRefreshNotifier(PreRefreshNotifier notifier) {
      if (refreshingListeners.contains(notifier)) {
        return;
      }
      refreshingListeners.add(notifier);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#getChecked(java.lang.Object)
     */
    @SuppressWarnings("null")
    @Override
    public boolean getChecked(Object element) {
      Widget testFindItem = getViewer().testFindItem(element);
      testFindItem = null;
      if (testFindItem == null) {
        if (itemCache.containsKey(element)) {
          FilteredCheckboxTreeItem item = (FilteredCheckboxTreeItem) itemCache.get(element);
          if (item.checkedState == STATE_CHECKED) {
            return true;
          }
          if (item.checkedState == STATE_CHECKED_GREYED) {
            return true;
          }
          if (item.checkedState == STATE_GREYED) {
            return true;
          } else if (item.checkedState == STATE_NONE) {
            return false;
          }
        }
      }
      return super.getChecked(element);
    }

    public Object[] getCheckedChildren(Object element) {
      FilteredCheckboxTreeItem item = (FilteredCheckboxTreeItem) itemCache.get(element);
      List checkedChildren = new ArrayList();
      if (item != null) {
        List children = item.children;
        Iterator iterator = children.iterator();
        while (iterator.hasNext()) {
          FilteredCheckboxTreeItem child = (FilteredCheckboxTreeItem) iterator.next();
          if (child.checkedState == STATE_CHECKED) {
            checkedChildren.add(child.itemData);
          }
        }
      }
      return checkedChildren.toArray();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#getCheckedElements()
     */
    @Override
    public Object[] getCheckedElements() {
      Iterator iterator = itemCache.values().iterator();
      List checkedElements = new LinkedList();
      while (iterator.hasNext()) {
        FilteredCheckboxTreeItem item = (FilteredCheckboxTreeItem) iterator.next();
        Widget testFindItem = getViewer().testFindItem(item.itemData);
        if (testFindItem == null) {
          if (item.checkedState == STATE_CHECKED || item.checkedState == STATE_CHECKED_GREYED
              || item.checkedState == STATE_GREYED) {
            checkedElements.add(item.itemData);
          }
        } else {
          if (((TreeItem) testFindItem).getChecked()) {
            checkedElements.add(testFindItem.getData());
          }
        }
      }
      return checkedElements.toArray();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#setChecked(java.lang.Object, boolean)
     */
    @Override
    public boolean setChecked(Object element, boolean state) {
      if (itemCache.containsKey(element)) {
        FilteredCheckboxTreeItem item = (FilteredCheckboxTreeItem) itemCache.get(element);
        item.checkedState = state ? STATE_CHECKED : STATE_NONE;
      }
      return super.setChecked(element, state);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#setCheckedElements(java.lang.Object[])
     */
    @Override
    public void setCheckedElements(Object[] elements) {
      Set s = new HashSet(itemCache.keySet());
      s.removeAll(new HashSet(Arrays.asList(elements)));
      for (int i = 0; i < elements.length; i++) {
        FilteredCheckboxTreeItem item = (FilteredCheckboxTreeItem) itemCache.get(elements[i]);
        if (item != null) {
          item.checkedState = STATE_CHECKED;
        }
      }
      for (Iterator iterator = s.iterator(); iterator.hasNext();) {
        Object object = iterator.next();
        FilteredCheckboxTreeItem item = (FilteredCheckboxTreeItem) itemCache.get(object);
        if (item != null) {
          item.checkedState = STATE_NONE;
        }
      }
      super.setCheckedElements(elements);
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#setGrayChecked(java.lang.Object, boolean)
     */
    @Override
    public boolean setGrayChecked(Object element, boolean state) {
      if (itemCache.containsKey(element)) {
        FilteredCheckboxTreeItem item = (FilteredCheckboxTreeItem) itemCache.get(element);
        item.checkedState = state ? STATE_CHECKED_GREYED : STATE_NONE;
      }
      return super.setGrayChecked(element, state);
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#setSubtreeChecked(java.lang.Object, boolean)
     */
    @Override
    public boolean setSubtreeChecked(Object element, boolean state) {
      byte newState = state ? STATE_CHECKED : STATE_NONE;
      TreeItem item = (TreeItem) testFindItem(element);
      FilteredCheckboxTreeItem filteredCheckboxTreeItem = (FilteredCheckboxTreeItem) itemCache.get(element);
      if (item != null && filteredCheckboxTreeItem != null) {
        filteredCheckboxTreeItem.checkedState = newState;
        TreeItem[] items = item.getItems();
        for (int i = 0; i < items.length; i++) {
          item = items[i];
          if (item != null) {
            filteredCheckboxTreeItem = (FilteredCheckboxTreeItem) itemCache.get(item.getData());
            if (filteredCheckboxTreeItem != null) {
              filteredCheckboxTreeItem.checkedState = newState;
            }
          }
        }
      }
      return super.setSubtreeChecked(element, state);
    }

    /*
     * Change checked state.
     */
    private void doApplyCheckedState(Item item, Object element) {
      // item update
      super.doUpdateItem(item, element);

      // checked state update
      TreeItem treeItem = (TreeItem) item;
      if (itemCache.containsKey(element)) {
        byte state = ((FilteredCheckboxTreeItem) itemCache.get(element)).checkedState;
        if (state == STATE_CHECKED_GREYED) {
          treeItem.setGrayed(true);
          treeItem.setChecked(true);
        } else if (state == STATE_CHECKED) {
          treeItem.setChecked(true);
          treeItem.setGrayed(false);
        } else if (state == STATE_GREYED) {
          treeItem.setGrayed(true);
          treeItem.setChecked(false);
        } else {
          treeItem.setGrayed(false);
          treeItem.setChecked(false);
        }
      }
    }

    /*
     * Return all the Tree items
     */
    private ArrayList getAllTreeItems(TreeItem[] roots) {
      ArrayList list = new ArrayList();
      for (int i = 0; i < roots.length; i++) {
        TreeItem item = roots[i];
        list.add(item);
        list.addAll(getAllTreeItems(item.getItems()));
      }
      return list;
    }

    /**
     * Return checked state of Tree item.
     */
    private byte getItemState(TreeItem item) {
      if (item.getChecked() && item.getGrayed()) {
        return STATE_CHECKED_GREYED;
      } else if (item.getChecked()) {
        return STATE_CHECKED;
      } else if (item.getGrayed()) {
        return STATE_GREYED;
      } else {
        return STATE_NONE;
      }
    }

    /**
     * Save checked state of all Tree items.
     */
    @SuppressWarnings("unused")
    private void saveCheckedState() {
      TreeItem[] items = treeViewer.getTree().getItems();
      for (int i = 0; i < items.length; i++) {
        TreeItem item = items[i];
        if (!itemCache.containsKey(item.getData())) {
          new FilteredCheckboxTreeItem(item.getData(), getItemState(item), itemCache, null);
        }
        FilteredCheckboxTreeItem filteredCheckboxTreeItem = (FilteredCheckboxTreeItem) itemCache.get(item.getData());
        filteredCheckboxTreeItem.checkedState = getItemState(item);
        saveCheckedState(filteredCheckboxTreeItem, item);
      }
    }

    /**
     * Save checked state of subtree rooted at item.
     */
    @SuppressWarnings("unused")
    private void saveCheckedState(FilteredCheckboxTreeItem parent, TreeItem parentItem) {
      TreeItem[] items = parentItem.getItems();
      for (int i = 0; i < items.length; i++) {
        TreeItem item = items[i];
        if (!itemCache.containsKey(item.getData())) {
          new FilteredCheckboxTreeItem(item.getData(), getItemState(item), itemCache, parent);
        }
        FilteredCheckboxTreeItem filteredCheckboxTreeItem = (FilteredCheckboxTreeItem) itemCache.get(item.getData());
        filteredCheckboxTreeItem.checkedState = getItemState(item);
        saveCheckedState(filteredCheckboxTreeItem, item);
      }
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.AbstractTreeViewer#internalRefresh(java.lang.Object, boolean)
     */
    @Override
    protected void internalRefresh(Object element, boolean updateLabels) {
      String text = FilteredCheckboxTree.this.getFilterString();
      boolean initial = initialText != null && initialText.equals(text);
      boolean filtered = (text != null && text.length() > 0 && !initial);

      // Send a notification to pre-refresh listeners
      for (Iterator iterator = refreshingListeners.iterator(); iterator.hasNext();) {
        PreRefreshNotifier notifier = (PreRefreshNotifier) iterator.next();
        notifier.preRefresh(FilterableCheckboxTreeViewer.this, filtered);
      }
      saveCheckedState();
      super.internalRefresh(element, updateLabels);
      treeViewer.expandAll();
    }

    /*
     * (non-Javadoc)
     * @see org.eclipse.jface.viewers.CheckboxTreeViewer#preservingSelection(java.lang.Runnable)
     */
    @Override
    protected void preservingSelection(Runnable updateCode) {
      super.preservingSelection(updateCode);

      // set checked state
      ArrayList allTreeItems = getAllTreeItems(treeViewer.getTree().getItems());
      for (Iterator iterator = allTreeItems.iterator(); iterator.hasNext();) {
        TreeItem item = (TreeItem) iterator.next();
        doApplyCheckedState(item, item.getData());
      }
    }

    @Override
    protected void unmapAllElements() {
      itemCache = new HashMap();
      super.unmapAllElements();
    }

  } // FilterableCheckboxTreeViewer

  /**
   * Interface for pre-refresh notification.
   */
  public interface PreRefreshNotifier {
    /**
     * Called before the refresh is performed.
     */
    public void preRefresh(FilterableCheckboxTreeViewer viewer, boolean filtered);
  }
}

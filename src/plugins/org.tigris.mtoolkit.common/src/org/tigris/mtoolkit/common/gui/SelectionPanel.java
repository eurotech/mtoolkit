package org.tigris.mtoolkit.common.gui;

import java.util.ArrayList;

import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

/**
 * @since 6.0
 */
public class SelectionPanel extends Composite {

	private CheckboxTableViewer list;
	private Text filterText;
	private ListItem[] listItems = new ListItem[0];

	public SelectionPanel(Composite parent, int style) {
		super(parent, style);

		setLayout(new GridLayout());
		list = CheckboxTableViewer.newCheckList(this, SWT.BORDER);
		list.setContentProvider(new IStructuredContentProvider() {
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}

			public void dispose() {
			}

			public Object[] getElements(Object inputElement) {
				return inputElement instanceof Object[] ? (Object[]) inputElement : new Object[0];
			}
		});
		list.setSorter(new ViewerSorter() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				String name1 = null;
				String name2 = null;
				IBaseLabelProvider labelProvider = ((TableViewer) viewer).getLabelProvider();
				if (labelProvider instanceof ITableLabelProvider) {
					ITableLabelProvider tlp = (ITableLabelProvider) labelProvider;
					name1 = tlp.getColumnText(e1, 0);
					name2 = tlp.getColumnText(e2, 0);
				} else if (labelProvider instanceof ILabelProvider) {
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
		list.setLabelProvider(new LabelProvider());
		list.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		filterText = new Text(this, SWT.BORDER | SWT.SINGLE);
		filterText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		filterText.setMessage("<type filter here>");

		list.addFilter(new ViewerFilter() {
			public Object[] filter(Viewer viewer, Object parent, Object[] elements) {
				String filter = filterText.getText().trim();
				if ("".equals(filter)) {
					return elements;
				}
				ArrayList out = new ArrayList();
				for (int i = 0; i < elements.length; i++) {
					if (match(elements[i], filter, viewer)) {
						out.add(elements[i]);
					}
				}
				return out.toArray();
			}

			public boolean select(Viewer viewer, Object parentElement, Object element) {
				String filter = filterText.getText().trim();
				return match(element, filter, viewer);
			}

			private boolean match(Object element, String filter, Viewer viewer) {
				IBaseLabelProvider labelProvider = ((TableViewer) viewer).getLabelProvider();
				if (labelProvider instanceof ITableLabelProvider) {
					ITableLabelProvider tlp = (ITableLabelProvider) labelProvider;
					int numCols = ((TableViewer) viewer).getTable().getColumnCount();
					if (numCols == 0) {
						// in case the table is used as list
						numCols = 1;
					}
					for (int i = 0; i < numCols; i++) {
						String txt = tlp.getColumnText(element, i);
						if (txt != null && txt.indexOf(filter) != -1) {
							return true;
						}
					}
				} else if (labelProvider instanceof ILabelProvider) {
					ILabelProvider lp = (ILabelProvider) labelProvider;
					String txt = lp.getText(element);
					if (txt != null && txt.indexOf(filter) != -1) {
						return true;
					}
				}
				return false;
			}
		});

		list.setCheckStateProvider(new ICheckStateProvider() {
			public boolean isGrayed(Object element) {
				return false;
			}

			public boolean isChecked(Object element) {
				return ((ListItem) element).checked;
			}
		});

		list.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				((ListItem) event.getElement()).checked = event.getChecked();
			}
		});
		final FilterJob filterJob = new FilterJob("", list);
		filterText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				filterJob.cancel();
				filterJob.schedule(300);
				TableItem[] items = ((Table) list.getControl()).getItems();
				for (int i = 0; i < items.length; i++) {
					list.setChecked(items[i].getData(), ((ListItem) items[i].getData()).checked);
				}
			}
		});

		Composite buttonsPanel = new Composite(this, SWT.NONE);
		buttonsPanel.setLayout(new GridLayout(2, false));
		buttonsPanel.setLayoutData(new GridData());

		Button btnSelect = new Button(buttonsPanel, SWT.PUSH);
		btnSelect.setText("Select all");
		btnSelect.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				selectAll(true);
			}
		});
		GridData gridData = new GridData();
		gridData.widthHint = 80;
		btnSelect.setLayoutData(gridData);

		Button btnDeselect = new Button(buttonsPanel, SWT.PUSH);
		btnDeselect.setText("Deselect all");
		btnDeselect.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				selectAll(false);
			}
		});
		gridData = new GridData();
		gridData.widthHint = 80;
		btnDeselect.setLayoutData(gridData);
	}

	private void selectAll(boolean state) {
		list.setAllChecked(state);
		for (int i = 0; i < listItems.length; i++) {
			listItems[i].checked = state;
		}
	}

	public void setItems(Object items[]) {
		listItems = new ListItem[items.length];
		for (int i = 0; i < listItems.length; i++) {
			listItems[i] = new ListItem();
			listItems[i].element = items[i];
		}
		list.setInput(listItems);
	}

	public Object[] getSelected() {
		int size = listItems.length;
		ArrayList out = new ArrayList(size);
		for (int i = 0; i < size; ++i) {
			if (listItems[i].checked) {
				out.add(listItems[i].element);
			}
		}
		return out.toArray();
	}

	/**
	 * Returns the viewer used for displaying items. It uses
	 * <code>ListItem</code> for holding items. The returned viewer can be used
	 * to specify custom label providers, etc.
	 * 
	 * @return the viewer
	 * @see ListItem
	 */
	public CheckboxTableViewer getViewer() {
		return list;
	}

	public ListItem[] getListItems() {
		return listItems;
	}
}

package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.model.Model;

public class ShowServicePropertiesInTree extends SelectionProviderAction implements IStateAction {

	private List frameworks = new ArrayList();

	public ShowServicePropertiesInTree(ISelectionProvider provider, String label) {
		super(provider, label);
	}

	public void run() {
		boolean newState = !((FrameworkImpl) frameworks.get(0)).isShownServicePropertiss();

		for (int i = 0; i < frameworks.size(); i++) {
			FrameworkImpl framework = (FrameworkImpl) frameworks.get(i);
			framework.setShowServicePropertiesInTree(newState);

			if (newState) {
				framework.refreshAction();
			} else {
				framework.clearServicePropertiesNodes((Model) frameworks.get(i));
			}
		}

		if (!newState) {
			((TreeViewer) getSelectionProvider()).refresh();
		}
	}

	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		TreeItem[] items = ((TreeViewer) getSelectionProvider()).getTree().getItems();
		boolean srvcPropertiesShown = (frameworks.isEmpty()) ? false : ((FrameworkImpl) frameworks.get(0))
				.isShownServicePropertiss();

		// tree structure has been updated (nodes added/removed )
		if (items.length != frameworks.size()) {
			frameworks.clear();

			for (int i = 0; i < items.length; i++) {
				Object o = items[i].getData();

				if (o instanceof FrameworkImpl) {
					frameworks.add(o);
					((FrameworkImpl) o).setShowServicePropertiesInTree(srvcPropertiesShown);
				}
			}
		}

		// all nodes of the tree have been removed
		if (frameworks.isEmpty()) {
			setEnabled(false);
			setChecked(false);
		} else {
			setEnabled(true);
			setChecked(srvcPropertiesShown);
		}
	}
}

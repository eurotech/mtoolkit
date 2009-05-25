package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;

public class ShowServicePropertiesInTree extends SelectionProviderAction {

	private List frameworks = new ArrayList();

	public ShowServicePropertiesInTree(ISelectionProvider provider, String label) {
		super(provider, label);
	}

	public void run() {
		boolean newState = !((FrameWork) frameworks.get(0)).isShownServicePropertiss();
		for (int i = 0; i < frameworks.size(); i++) {
			((FrameWork) frameworks.get(i)).setShowServicePropertiesInTree(newState);
		}

		for (int i = 0; i < frameworks.size(); i++)
			((FrameWork) frameworks.get(i)).refreshAction();

		// FrameWorkView.getActiveInstance().addFilter();
	}

	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	private void updateState(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			this.setEnabled(false);
			return;
		}

		Iterator iter = selection.iterator();
		frameworks.clear();
		while (iter.hasNext()) {
			Object selElement = iter.next();
			if (selElement instanceof Model) {
				FrameWork fw = ((Model) selElement).findFramework();
				if (!frameworks.contains(fw))
					;
				frameworks.add(fw);
			} else if (selElement instanceof FrameWork)
				if (!frameworks.contains(selElement))
					frameworks.add(selElement);
		}

		if (frameworks.isEmpty()) {
			this.setEnabled(false);
			return;
		}

		this.setEnabled(true);

		boolean show = ((FrameWork) frameworks.get(0)).isShownServicePropertiss();
		if (show)
			setChecked(true);
		else
			setChecked(false);
	}
}

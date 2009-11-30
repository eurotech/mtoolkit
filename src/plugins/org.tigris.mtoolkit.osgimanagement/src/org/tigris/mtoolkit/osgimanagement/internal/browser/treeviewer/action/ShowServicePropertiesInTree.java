package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
			((FrameworkImpl) frameworks.get(i)).setShowServicePropertiesInTree(newState);
		}

		for (int i = 0; i < frameworks.size(); i++)
			((FrameworkImpl) frameworks.get(i)).refreshAction();

		// FrameWorkView.getActiveInstance().addFilter();
	}

	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.isEmpty()) {
			this.setEnabled(false);
			return;
		}

		Iterator iter = selection.iterator();
		frameworks.clear();
		while (iter.hasNext()) {
			Object selElement = iter.next();
			if (selElement instanceof Model) {
				FrameworkImpl fw = (FrameworkImpl) ((Model) selElement).findFramework();
				if (fw != null && !frameworks.contains(fw)) {
					frameworks.add(fw);
				}
			} else if (selElement instanceof FrameworkImpl)
				if (!frameworks.contains(selElement)) {
					frameworks.add(selElement);
				}
		}

		if (frameworks.isEmpty()) {
			this.setEnabled(false);
			return;
		}

		this.setEnabled(true);

		boolean show = ((FrameworkImpl) frameworks.get(0)).isShownServicePropertiss();
		if (show)
			setChecked(true);
		else
			setChecked(false);
	}
}

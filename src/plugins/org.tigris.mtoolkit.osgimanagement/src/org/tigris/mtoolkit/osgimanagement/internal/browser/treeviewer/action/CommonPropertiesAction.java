package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.tigris.mtoolkit.osgimanagement.IStateAction;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;

public class CommonPropertiesAction extends SelectionProviderAction implements IStateAction {

	private TreeViewer parentView;
	private SelectionProviderAction action;

	public CommonPropertiesAction(ISelectionProvider provider, String text) {
		super(provider, text);
		this.parentView = (TreeViewer) provider;
		this.setEnabled(false);
		this.setText(text + "@Ctrl+Enter");
	}

	public void run() {
		IStructuredSelection selection = getStructuredSelection();

		Object element = selection.getFirstElement();

		if (element instanceof ObjectClass)
			action = new ServicePropertiesAction(parentView, Messages.property_action_label);
		else if (element instanceof Bundle)
			action = new BundlePropertiesAction(parentView, Messages.property_action_label);
		else if (element instanceof DeploymentPackage)
			action = new DPPropertiesAction(parentView, Messages.property_action_label);
		else if (element instanceof FrameworkImpl)
			action = new PropertyAction(parentView, Messages.property_action_label);
		else
			return;
		action.run();
	}

	public void selectionChanged(IStructuredSelection selection) {
		updateState(selection);
	}

	public void updateState(IStructuredSelection selection) {
		if (selection.size() != 1) {
			this.setEnabled(false);
		} else {
			Object element = selection.getFirstElement();
			if ((element instanceof ObjectClass)
							|| (element instanceof Bundle)
							|| (element instanceof DeploymentPackage)
							|| (element instanceof FrameworkImpl)) {
				this.setEnabled(true);
			} else {
				this.setEnabled(false);
			}
		}

	}
}

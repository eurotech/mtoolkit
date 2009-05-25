package org.tigris.mtoolkit.osgimanagement.internal;

import java.util.List;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

public class MyViewerFilter extends ViewerFilter {

	private List itemsToBeShown;

	public void setInput(List visibleItems) {
		if (this.itemsToBeShown != null)
			this.itemsToBeShown.clear();
		this.itemsToBeShown = visibleItems;
	}

	public boolean select(Viewer viewer, Object parentElement, Object element) {
		return itemsToBeShown.contains(element);
	}

}

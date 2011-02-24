package org.tigris.mtoolkit.common.gui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * @since 6.0
 */
public class FilterJob extends Job {

	private StructuredViewer viewer;

	public FilterJob(String name, StructuredViewer viewer) {
		super(name);
		this.viewer = viewer;
	}

	protected IStatus run(IProgressMonitor monitor) {
		Display display = PlatformUI.getWorkbench().getDisplay();
		if (!display.isDisposed()) {
			display.asyncExec(new Runnable() {
				public void run() {
					if (viewer.getControl().isDisposed()) {
						return;
					}
					viewer.refresh();
				}
			});
		}
		return Status.OK_STATUS;
	}

}

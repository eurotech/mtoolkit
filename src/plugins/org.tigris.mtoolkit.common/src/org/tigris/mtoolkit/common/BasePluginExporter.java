package org.tigris.mtoolkit.common;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;

/**
 * @since 5.0
 */
public class BasePluginExporter {

	private volatile IStatus result = null;

	public BasePluginExporter() {
		super();
	}

	public IStatus getResult() {
		return result;
	}

	protected void setResult(IStatus result) {
		synchronized (this) {
			this.result = result;
			notifyAll();
		}
	}

	public boolean hasFinished() {
		return getResult() != null;
	}

	public IStatus join(long timeout) throws InterruptedException {
		long start = System.currentTimeMillis();
		synchronized (this) {
			while (result == null && (System.currentTimeMillis() - start < timeout)) {
				wait(timeout - System.currentTimeMillis() + start);
			}
		}
		return result;
	}

	public class ExportErrorDialog extends MessageDialog {
		private File logLocation;

		public ExportErrorDialog(String title, File logLocation) {
			super(PlatformUI.getWorkbench().getDisplay().getActiveShell(), title, null, null, MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
			this.logLocation = logLocation;
		}

		protected Control createMessageArea(Composite composite) {
			Link link = new Link(composite, SWT.WRAP);
			try {
				link.setText(NLS.bind("Errors occurred during the export operation. The ant tasks generated log files which can be found at {0}", "<a>" + logLocation.getCanonicalPath() + "</a>")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			} catch (IOException e) {
			}
			GridData data = new GridData();
			data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
			link.setLayoutData(data);
			link.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					try {
						Program.launch(logLocation.getCanonicalPath());
					} catch (IOException ex) {
					}
				}
			});
			return link;
		}
	}

}
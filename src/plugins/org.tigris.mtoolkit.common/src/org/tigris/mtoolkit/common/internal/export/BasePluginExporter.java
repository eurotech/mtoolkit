/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.internal.export;

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
import org.tigris.mtoolkit.common.PluginUtilities;

/**
 * @since 5.0
 */
public abstract class BasePluginExporter implements IPluginExporter {
  private volatile IStatus result = null;

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.internal.export.IPluginExporter#getResult()
   */
  public IStatus getResult() {
    return result;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.internal.export.IPluginExporter#hasFinished()
   */
  public boolean hasFinished() {
    return getResult() != null;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.internal.export.IPluginExporter#join(long)
   */
  public IStatus join(long timeout) throws InterruptedException {
    long start = System.currentTimeMillis();
    synchronized (this) {
      while (result == null && (System.currentTimeMillis() - start < timeout)) {
        wait(timeout - System.currentTimeMillis() + start);
      }
    }
    return result;
  }

  protected void setResult(IStatus result) {
    synchronized (this) {
      this.result = result;
      notifyAll();
    }
  }

  /**
   * @since 6.0
   */
  protected class ExportErrorDialog extends MessageDialog {
    private File logLocation;

    public ExportErrorDialog(String title, File logLocation) {
      super(PluginUtilities.getActiveWorkbenchShell(), title, null, null, MessageDialog.ERROR, new String[] {
        IDialogConstants.OK_LABEL
      }, 0);
      this.logLocation = logLocation;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.IconAndMessageDialog#createMessageArea(org.eclipse.swt.widgets.Composite)
     */
    @Override
    protected Control createMessageArea(Composite composite) {
      Link link = new Link(composite, SWT.WRAP);
      try {
        link.setText(NLS
            .bind(
                "Errors occurred during the export operation. The ant tasks generated log files which can be found at {0}", "<a>" + logLocation.getCanonicalPath() + "</a>")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      } catch (IOException e) {
      }
      GridData data = new GridData();
      data.widthHint = convertHorizontalDLUsToPixels(IDialogConstants.MINIMUM_MESSAGE_AREA_WIDTH);
      link.setLayoutData(data);
      link.addSelectionListener(new SelectionAdapter() {
        /* (non-Javadoc)
         * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
         */
        @Override
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

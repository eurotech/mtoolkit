/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IconAndMessageDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class PluginUtilities {

  private static SafeRunnableDialog errorDialog;

  public static final String        INSTALLED_PLATFORM = Platform.getOS();

  public static final char[]        INVALID_RESOURCE_CHARACTERS;
  public static final String[]      INVALID_RESOURCE_NAMES;

  public static final String        VERSION_3_4_0      = "3.4.0";
  public static final String        VERSION_3_5_0      = "3.5.0";
  /**
   * @since 5.0
   */
  public static final String        VERSION_3_6_0      = "3.6.0";

  static {
    char[] chars = null;
    String[] names = null;
    if (INSTALLED_PLATFORM.equals(Platform.OS_WIN32)) {
      // taken from
      // http://support.microsoft.com/support/kb/articles/q177/5/06.asp
      chars = new char[] {
      '"', '*', '/', ':', '<', '>', '?', '\\', '|'
      };
      // list taken from
      // http://support.microsoft.com/support/kb/articles/Q216/6/54.ASP
      names = new String[] {
      "aux", "clock$", "com1", "com2", "com3", "com4", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
      "com5", "com6", "com7", "com8", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      "com9", "con", "lpt1", "lpt2", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      "lpt3", "lpt4", "lpt5", "lpt6", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      "lpt7", "lpt8", "lpt9", "nul", "prn"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
    } else {
      // only front slash and null char are invalid on UNIXes
      // taken from
      // http://www.faqs.org/faqs/unix-faq/faq/part2/section-2.html
      // backslash and colon are illegal path segments regardless of
      // filesystem.
      chars = new char[] {
      '\0', '/', ':', '\\'
      };
    }
    INVALID_RESOURCE_CHARACTERS = chars;
    INVALID_RESOURCE_NAMES = names == null ? new String[0] : names;
  }

  /**
   * @param pathName
   * @return
   * 
   */
  public static boolean containsArticles(String pathName) {
    if (INSTALLED_PLATFORM.equals(Platform.OS_WIN32)) {
      // on windows, filename suffixes are not relevant to name validity
      int dot = pathName.indexOf('.');
      pathName = dot == -1 ? pathName : pathName.substring(0, dot);
      return Arrays.binarySearch(INVALID_RESOURCE_NAMES, pathName.toLowerCase()) >= 0;
    }
    return false;
  }

  /**
   * Validate specified file name for correctness on current platform
   * 
   * @param fileName
   * @return
   */
  public static boolean isValidFileName(String fileName) {
    if (fileName == null || fileName.length() == 0) {
      return false;
    }
    if (!containsArticles(fileName)) {
      for (int i = 0; i < fileName.length(); i++) {
        for (int c = 0; c < INVALID_RESOURCE_CHARACTERS.length; c++) {
          if (INVALID_RESOURCE_CHARACTERS[c] == fileName.charAt(i)) {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }

  /**
   * Validate specified absolute or relative path for correctness on current
   * platform
   * 
   * @param path
   * @return
   */
  public static boolean isValidPath(String path) {
    if (path == null || path.length() == 0) {
      return false;
    }
    if (INSTALLED_PLATFORM.equals(Platform.OS_WIN32)) {
      for (int c = 0; c < INVALID_RESOURCE_CHARACTERS.length; c++) {
        char character = INVALID_RESOURCE_CHARACTERS[c];
        if (character != '\\' && character != '/' && character != ':') {
          if (path.indexOf(character) != -1) {
            return false;
          }
        }
      }
    }
    IPath filePath = new Path(path);
    for (int i = 0, segmentCount = filePath.segmentCount(); i < segmentCount; i++) {
      if (!isValidFileName(filePath.segment(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Validate specified version name for correctness
   * 
   * @param versionName
   * @return
   */
  public static boolean isValidVersion(String versionName) {
    int ind = versionName.indexOf('.');
    String buffer;
    if (ind == -1) {
      try {
        Integer.parseInt(versionName);
        return true;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    buffer = versionName.substring(0, ind);
    try {
      Integer.parseInt(buffer);
    } catch (NumberFormatException e) {
      return false;
    }
    versionName = versionName.substring(ind + 1);
    while ((ind = versionName.indexOf('.')) != -1) {
      buffer = versionName.substring(0, ind);
      try {
        Integer.parseInt(buffer);
      } catch (NumberFormatException e) {
        return false;
      }
      versionName = versionName.substring(ind + 1);

    }
    try {
      Integer.parseInt(versionName);
    } catch (NumberFormatException e) {
      return false;
    }
    return true;
  }

  public static void print(Shell shell, StyledText styledText) {
    PrintDialog prDialog = new PrintDialog(shell, SWT.APPLICATION_MODAL);
    PrinterData printerData = prDialog.open();

    // make print action
    if (printerData != null) {
      Printer pr = new Printer(printerData);
      Runnable runnable = styledText.print(pr);
      runnable.run();
      pr.dispose();
    }
  }

  public static void print(Shell shell, String strText) {
    StyledText styledText = new StyledText(shell, SWT.MULTI);
    styledText.setText(strText);
    print(shell, styledText);
  }

  static List   errorTable     = new ArrayList();
  static String line_Separator = System.getProperty("line.separator"); //$NON-NLS-1$

  public static void showErrorDialog(Shell parent, String title, String message, String reason) {
    showDialog(parent, title, message, reason, null, null, IStatus.ERROR);
  }

  public static void showErrorDialog(Shell parent, String title, String message, String reason, Throwable e) {
    showDialog(parent, title, message, reason, e, null, IStatus.ERROR);
  }

  public static void showErrorDialog(Shell parent, String title, String message, String reason, Throwable e,
      Throwable nested) {
    showDialog(parent, title, message, reason, e, nested, IStatus.ERROR);
  }

  public static void showWarningDialog(Shell parent, String title, String message, String reason) {
    showDialog(parent, title, message, reason, null, null, IStatus.WARNING);
  }

  public static void showInformationDialog(Shell parent, String title, String message, String reason) {
    showDialog(parent, title, message, reason, null, null, IStatus.INFO);
  }

  public static void showMessageDialog(Shell parent, String title, String message, String reason) {
    showDialog(parent, title, message, reason, null, null, IStatus.OK);
  }

  public static void showDetailsErrorDialog(Shell parent, String title, String message, String details) {
    DetailsErrorDialog err = new DetailsErrorDialog(parent, title, message, details);
    err.open();
  }

  public static int showConfirmationDialog(Shell shell, String title, String message) {
    MessageBox mb = new MessageBox(shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
    mb.setMessage(message);
    mb.setText(title);
    return mb.open();
  }

  private static void showDialog(Shell parent, String title, String message, String reason, Throwable e,
      Throwable nested, int code) {
    if (title == null) {
      title = ""; //$NON-NLS-1$
    }
    if (message == null) {
      message = ""; //$NON-NLS-1$
    }
    if (reason == null) {
      reason = Messages.MessageDialog_NoDetails;
    }
    errorTable.clear();
    String ex = null;
    if (e != null) {
      ex = dumpToText(e);
      List sTokensList = getStringTokens(ex);
      Object[] sTokens = sTokensList.toArray();

      for (int i = 0; i < sTokens.length; i++) {
        errorTable.add(new Status(code, UtilitiesPlugin.PLUGIN_ID, 1, (String) sTokens[i], null));
      }
    }
    if (nested != null) {
      ex = dumpToText(nested);
      List sTokensList = getStringTokens(ex);
      Object[] sTokens = sTokensList.toArray();

      for (int i = 0; i < sTokens.length; i++) {
        errorTable.add(new Status(code, UtilitiesPlugin.PLUGIN_ID, 1, (String) sTokens[i], null));
      }
    }

    if (errorDialog == null || errorDialog.getShell() == null) {
      errorDialog = new SafeRunnableDialog(getStatus(reason, code));
    }

    try {
      if (errorDialog.getShell() == null) {
        errorDialog.open();
      } else {
        errorDialog.addStatus(getStatus(reason, code));
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }

  public static IStatus getStatus(String reason, int code) {
    if (errorTable.size() == 0) {
      return new Status(code, UtilitiesPlugin.PLUGIN_ID, 1, reason, null);
    }
    IStatus[] errors = new IStatus[errorTable.size()];
    errorTable.toArray(errors);
    return new MultiStatus(UtilitiesPlugin.PLUGIN_ID, IStatus.OK, errors, reason, null);
  }

  /**
   * This method parses exception from String into List items
   * 
   * @param str
   * @return
   */
  public static List getStringTokens(String str) {
    java.util.List tokens = new ArrayList();
    int i = str.indexOf(line_Separator);
    int br = 0;
    while (i != -1) {
      if (br == 0) {
        tokens.add(str.substring(0, i).trim());
      } else {
        tokens.add("    " + str.substring(0, i).trim()); //$NON-NLS-1$
      }
      str = str.substring(i + 1).trim();
      i = str.indexOf(line_Separator);
      br++;
    }
    return tokens;
  }

  public static String dumpToText(Throwable t) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream(1024);
    PrintStream printOut = new PrintStream(stream);
    t.printStackTrace(printOut);
    return stream.toString();
  }

  /**
   * This method is used for compare two strings. It is used for comparing file
   * names, because only on Windows OS letters case are independenet.
   * 
   * @param str1
   *          - the first string
   * @param str2
   *          - the second string
   * @return <code>TRUE</code> if string equals, otherwise returns
   *         <code>FALSE</code>
   */
  public static boolean equalsOSDep(String str1, String str2) {
    boolean result;
    if (INSTALLED_PLATFORM.equals(Platform.OS_WIN32)) {
      result = str1.equalsIgnoreCase(str2);
    } else {
      result = str1.equals(str2);
    }
    return result;
  }

  public static void main(String[] args) {
    String version = "1.0.2.34"; //$NON-NLS-1$
    System.out.println(isValidVersion(version));
  }

  static class SafeRunnableDialog extends ErrorDialog {

    private TableViewer statusListViewer;

    private Collection  statuses = new ArrayList();

    /**
     * Create a new instance of the receiver on a status.
     * 
     * @param status
     *          The status to display.
     */
    SafeRunnableDialog(IStatus status) {

      super(null, JFaceResources.getString("error"), status.getMessage(), //$NON-NLS-1$
          status, IStatus.ERROR);

      setShellStyle(SWT.DIALOG_TRIM | SWT.MODELESS | SWT.RESIZE | SWT.MIN | getDefaultOrientation());

      setStatus(status);
      statuses.add(status);

      setBlockOnOpen(false);

      String reason = JFaceResources.getString("SafeRunnableDialog_checkDetailsMessage"); //$NON-NLS-1$
      if (status.getException() != null) {
        reason = status.getException().getMessage() == null ? status.getException().toString() : status.getException()
            .getMessage();
      }
      this.message = JFaceResources.format(JFaceResources.getString("SafeRunnableDialog_reason"), new Object[] { //$NON-NLS-1$
              status.getMessage(), reason
          });
    }

    /**
     * Method which should be invoked when new errors become available for
     * display
     */
    void refresh() {

      if (AUTOMATED_MODE) {
        return;
      }

      createStatusList((Composite) dialogArea);
      updateEnablements();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.dialogs.ErrorDialog#createDialogArea(org.eclipse
     * .swt.widgets.Composite)
     */
    @Override
    protected Control createDialogArea(Composite parent) {
      Control area = super.createDialogArea(parent);
      createStatusList((Composite) area);
      return area;
    }

    /**
     * Create the status list if required.
     * 
     * @param parent
     *          the Control to create it in.
     */
    private void createStatusList(Composite parent) {
      if (isMultipleStatusDialog()) {
        if (statusListViewer == null) {
          // The job list doesn't exist so create it.
          setMessage(JFaceResources.getString("SafeRunnableDialog_MultipleErrorsMessage")); //$NON-NLS-1$
          getShell().setText(JFaceResources.getString("SafeRunnableDialog_MultipleErrorsTitle")); //$NON-NLS-1$
          createStatusListArea(parent);
          showDetailsArea();
        }
        refreshStatusList();
      }
    }

    /*
     * Update the button enablements
     */
    private void updateEnablements() {
      Button details = getButton(IDialogConstants.DETAILS_ID);
      if (details != null) {
        details.setEnabled(true);
      }
    }

    /**
     * This method sets the message in the message label.
     * 
     * @param messageString
     *          - the String for the message area
     */
    private void setMessage(String messageString) {
      // must not set null text in a label
      message = messageString == null ? "" : messageString; //$NON-NLS-1$
      if (messageLabel == null || messageLabel.isDisposed()) {
        return;
      }
      messageLabel.setText(message);
    }

    /**
     * Create an area that allow the user to select one of multiple jobs that
     * have reported errors
     * 
     * @param parent
     *          - the parent of the area
     */
    private void createStatusListArea(Composite parent) {
      // Display a list of jobs that have reported errors
      statusListViewer = new TableViewer(parent, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
      statusListViewer.setComparator(getViewerComparator());
      Control control = statusListViewer.getControl();
      GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
      data.heightHint = convertHeightInCharsToPixels(10);
      control.setLayoutData(data);
      statusListViewer.setContentProvider(getStatusContentProvider());
      statusListViewer.setLabelProvider(getStatusListLabelProvider());
      statusListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
        public void selectionChanged(SelectionChangedEvent event) {
          handleSelectionChange();
        }
      });
      applyDialogFont(parent);
      statusListViewer.setInput(this);
    }

    /**
     * Return the label provider for the status list.
     * 
     * @return CellLabelProvider
     */
    private CellLabelProvider getStatusListLabelProvider() {
      return new CellLabelProvider() {
        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.CellLabelProvider#update(org.eclipse
         * .jface.viewers.ViewerCell)
         */
        @Override
        public void update(ViewerCell cell) {
          cell.setText(((IStatus) cell.getElement()).getMessage());

        }
      };
    }

    /**
     * Return the content provider for the statuses.
     * 
     * @return IStructuredContentProvider
     */
    private IStructuredContentProvider getStatusContentProvider() {
      return new IStructuredContentProvider() {
        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.IStructuredContentProvider#getElements
         * (java.lang.Object)
         */
        public Object[] getElements(Object inputElement) {
          return statuses.toArray();
        }

        /*
         * (non-Javadoc)
         *
         * @see org.eclipse.jface.viewers.IContentProvider#dispose()
         */
        public void dispose() {

        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.IContentProvider#inputChanged(org
         * .eclipse.jface.viewers.Viewer, java.lang.Object,
         * java.lang.Object)
         */
        public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

        }
      };
    }

    /*
     * Return whether there are multiple errors to be displayed
     */
    private boolean isMultipleStatusDialog() {
      return statuses.size() > 1;
    }

    /**
     * Return a viewer sorter for looking at the jobs.
     * 
     * @return ViewerSorter
     */
    private ViewerComparator getViewerComparator() {
      return new ViewerComparator() {
        /*
         * (non-Javadoc)
         *
         * @see
         * org.eclipse.jface.viewers.ViewerComparator#compare(org.eclipse
         * .jface.viewers.Viewer, java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(Viewer testViewer, Object e1, Object e2) {
          String message1 = ((IStatus) e1).getMessage();
          String message2 = ((IStatus) e2).getMessage();
          if (message1 == null) {
            return 1;
          }
          if (message2 == null) {
            return -1;
          }

          return message1.compareTo(message2);
        }
      };
    }

    /**
     * Refresh the contents of the viewer.
     */
    void refreshStatusList() {
      if (statusListViewer != null && !statusListViewer.getControl().isDisposed()) {
        statusListViewer.refresh();
        Point newSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
        getShell().setSize(newSize);
      }
    }

    /**
     * Get the single selection. Return null if the selection is not just one
     * element.
     * 
     * @return IStatus or <code>null</code>.
     */
    private IStatus getSingleSelection() {
      ISelection rawSelection = statusListViewer.getSelection();
      if (rawSelection != null && rawSelection instanceof IStructuredSelection) {
        IStructuredSelection selection = (IStructuredSelection) rawSelection;
        if (selection.size() == 1) {
          return (IStatus) selection.getFirstElement();
        }
      }
      return null;
    }

    /**
     * The selection in the multiple job list has changed. Update widget
     * enablements and repopulate the list.
     */
    void handleSelectionChange() {
      IStatus newSelection = getSingleSelection();
      setStatus(newSelection);
      updateEnablements();
      showDetailsArea();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.jface.dialogs.ErrorDialog#shouldShowDetailsButton()
     */
    @Override
    protected boolean shouldShowDetailsButton() {
      return true;
    }

    /**
     * Add the status to the receiver.
     * 
     * @param status
     */
    public void addStatus(IStatus status) {
      statuses.add(status);
      refresh();

    }
  }

  public static class DetailsErrorDialog extends IconAndMessageDialog {

    private String  title;
    private Button  detailsButton;
    private String  details;
    private boolean open;
    private Text    detailsArea;

    public DetailsErrorDialog(Shell parentShell, String title, String message, String details) {
      super(parentShell);
      this.title = title;
      this.message = message;
      this.details = details;
    }

    @Override
    protected int getShellStyle() {
      return super.getShellStyle() | SWT.RESIZE;
    }

    @Override
    protected Image getImage() {
      return super.getErrorImage();
    }

    @Override
    protected void buttonPressed(int id) {
      if (id == IDialogConstants.DETAILS_ID) {
        // was the details button pressed?
        toggleDetailsArea();
      } else {
        super.buttonPressed(id);
      }
    }

    @Override
    protected void configureShell(Shell shell) {
      super.configureShell(shell);
      shell.setText(title);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
      // create OK and Details buttons
      createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
      createDetailsButton(parent);
    }

    private void createDetailsArea(Composite parent) {
      detailsArea = new Text(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
      detailsArea.setText(details);

      GridData detailsData = new GridData(SWT.FILL, SWT.FILL, true, true);
      detailsData.horizontalSpan = 2;
      detailsData.verticalSpan = 3;
      detailsData.exclude = true;
      detailsArea.setLayoutData(detailsData);
      detailsArea.setVisible(false);
    }

    protected void createDetailsButton(Composite parent) {
      detailsButton = createButton(parent, IDialogConstants.DETAILS_ID, IDialogConstants.SHOW_DETAILS_LABEL, false);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
      createMessageArea(parent);
      createDetailsArea(parent);
      // createSupportArea(parent);
      // create a composite with standard margins and spacing
      Composite composite = new Composite(parent, SWT.NONE);
      GridLayout layout = new GridLayout();
      layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
      layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
      layout.verticalSpacing = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
      layout.horizontalSpacing = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
      layout.numColumns = 2;
      composite.setLayout(layout);
      GridData childData = new GridData(GridData.FILL_BOTH);
      childData.horizontalSpan = 2;
      composite.setLayoutData(childData);
      composite.setFont(parent.getFont());

      return composite;
    }

    @Override
    protected void createDialogAndButtonArea(Composite parent) {
      super.createDialogAndButtonArea(parent);
      if (this.dialogArea instanceof Composite) {
        // Create a label if there are no children to force a smaller
        // layout
        Composite dialogComposite = (Composite) dialogArea;
        if (dialogComposite.getChildren().length == 0) {
          new Label(dialogComposite, SWT.NULL);
        }
      }
    }

    private void toggleDetailsArea() {
      Point windowSize = super.getShell().getSize();

      if (open) {
        detailsArea.setVisible(false);
        ((GridData) detailsArea.getLayoutData()).exclude = true;
        detailsArea.getParent().layout(true);
        detailsButton.setText(IDialogConstants.SHOW_DETAILS_LABEL);
      } else {
        detailsArea.setVisible(true);
        ((GridData) detailsArea.getLayoutData()).exclude = false;
        detailsArea.getParent().layout(true);
        detailsButton.setText(IDialogConstants.HIDE_DETAILS_LABEL);
      }
      open = !open;

      Point newSize = super.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
      newSize.x = windowSize.x;
      Point location = super.getShell().getLocation();
      Rectangle clientArea = super.getShell().getDisplay().getClientArea();
      int max = clientArea.height - clientArea.y - location.y;
      if (open) {
        newSize.y = Math.min(newSize.y, max);
      }
      super.getShell().setSize(newSize);
    }
  }

  /**
   * Returns specified bundle version
   * 
   * @param bundleName
   *          bundle symbolic name
   * @return Version for specified bundle
   */
  @SuppressWarnings("cast")
  public static Version getBundleVersion(String bundleName) {
    return new Version((String) Platform.getBundle(bundleName).getHeaders().get(Constants.BUNDLE_VERSION));
  }

  /**
   * Returns true if bundle version is greater or equal to specified version
   * 
   * @param bundleName
   *          bundle symbolic name
   * @param version
   *          version to check - like PluginUtilities.VERSION_3_5_0)
   * @return true if bundle version is greater or equal to specified version
   */
  public static boolean compareVersion(String bundleName, String version) {
    Version bundleVersion = getBundleVersion(bundleName);
    Version compatibleRange = new Version(version);
    return compatibleRange.compareTo(bundleVersion) <= 0;
  }

  /**
   * @since 5.0
   */
  public static Shell getActiveWorkbenchShell() {
    final IWorkbench workbench = PlatformUI.getWorkbench();
    if (workbench == null) {
      return null;
    }
    IWorkbenchWindow aWindow = workbench.getActiveWorkbenchWindow();
    if (aWindow != null) {
      return aWindow.getShell();
    }
    return null;
  }

  public static IStringVariableManager getStringVariableManager() {
    VariablesPlugin plugin = VariablesPlugin.getDefault();
    if (plugin == null) {
      return null;
    }
    return plugin.getStringVariableManager();
  }
}

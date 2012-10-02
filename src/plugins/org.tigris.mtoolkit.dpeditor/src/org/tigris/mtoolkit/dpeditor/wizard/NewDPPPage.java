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
package org.tigris.mtoolkit.dpeditor.wizard;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.wizards.TypedViewerFilter;
import org.eclipse.jdt.internal.ui.wizards.dialogfields.Separator;
import org.eclipse.jdt.ui.JavaElementComparator;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.osgi.framework.Version;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.dpeditor.IHelpContextIds;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;

/**
 * Create the page of the <code>NewDPPWizard</code>
 */
public class NewDPPPage extends WizardPage implements ModifyListener, KeyListener, SelectionListener {

  /** Browse button label */
  public static final String BROWSE_BUTTON         = ResourceManager.getString("DPPEditor.Browse_Button"); //$NON-NLS-1$
  /** Browse button label */
  public static final String BROWSE_BUTTON2        = ResourceManager.getString("DPPEditor.Browse_Button2"); //$NON-NLS-1$

  /** Target folder label */
  public static final String TARGET_FOLDER_LABEL   = "NewDPPWizard.target_folder_label";                   //$NON-NLS-1$
  /** Deployment package file name label */
  public static final String DPP_FILE_NAME_LABEL   = "NewDPPWizard.file_name_label";                       //$NON-NLS-1$
  /** Deployment package symbolic name label */
  public static final String SYMBOLIC_NAME_LABEL   = "NewDPPWizard.symbolic_name_label";                   //$NON-NLS-1$
  /** Deployment package version label */
  public static final String VERSION_LABEL         = "NewDPPWizard.version_label";                         //$NON-NLS-1$
  /** Option label */
  public static final String OPTION_LABEL          = "NewDPPWizard.option_label";                          //$NON-NLS-1$
  /** Option check box label */
  public static final String OPTION_CHECKBOX_LABEL = "NewDPPWizard.option_checkbox_label";                 //$NON-NLS-1$

  /** Text field for the target folder */
  private Text               targetFolText;
  /** The browse button for the target folder */
  private Button             targetFolButton;
  /**
   * The string value of the target folder loaded when the target folder sets
   * from the selected element
   */
  private String             targetFolTxt          = "";
  /** Text field for the deployment package file name */
  private Text               fileNameText;
  /** Text field for the deployment package symbolic name */
  private Text               symbolicNameText;
  /**
   * The <code>boolean</code> flag shows if the symbolic name text must be set
   * the text from the deployment package file name text field
   */
  private boolean            symbolicNameFlag      = false;
  /** Text field for the deployment package version */
  private Text               versionText;
  /** The container of the selection */
  private IContainer         parentFolder;
  /** The option check box button */
  private Button             optionButton;
  /** The option text field */
  private Text               optionText;
  /** The option browse button */
  private Button             optionBrowseButton;

  /** The selected project */
  private IProject           prj;

  /**
   * The <code>boolean</code> flag shows if the wizard is just open and if the
   * wizard is just open do not makes one validation of the values
   */
  private boolean            firstTimeOpen         = true;

  /**
   * Constructor of the NewDPPPage. Creates the new wizard page with given name,
   * title and description
   *
   * @param pageName
   *          the name of the page
   * @param title
   *          the title for this wizard page or <code>null</code> if none
   * @param description
   *          the description of the page
   */
  protected NewDPPPage(String pageName, String title, String description) {
    super(pageName, title, null);
    setDescription(description);
    setPageComplete(false);
  }

  /**
   * Creates the fields of this page
   *
   * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
   */
  public void createControl(Composite parent) {
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.NEW_DPP);
    Composite composite = new Composite(parent, SWT.NONE);
    GridLayout layout = new GridLayout();
    composite.setLayout(layout);
    createFields(composite);
    setControl(composite);
  }

  /**
   * Creates label with the given parent and text.
   *
   * @param parent
   *          the parent of the label
   * @param text
   *          the text of the label
   * @return created label
   */
  public Label createLabel(Composite parent, String text) {
    Label label = new Label(parent, SWT.NONE);
    if (text != null) {
      label.setText(text);
    }
    return label;
  }

  /**
   * Creates label with the given text in front of the created text field with
   * the given parent and tool tip
   *
   * @param parent
   *          the parent of the text field
   * @param label
   *          the text of the label of the text field
   * @param tooltip
   *          the tool tip text of the label
   * @return created text field
   */
  protected Text createTextLabel(Composite parent, String label, String tooltip) {
    Label l = createLabel(parent, label);
    if (tooltip != null) {
      l.setToolTipText(tooltip);
    }
    Text text = createText(parent);
    return text;
  }

  /**
   * Creates text field with the given parent.
   *
   * @param parent
   *          the parent of the text field
   * @return created text field
   */
  protected Text createText(Composite parent) {
    Text text = new Text(parent, SWT.BORDER | SWT.SINGLE);
    text.setText("");
    GridData gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_CENTER);
    gd.grabExcessHorizontalSpace = true;
    gd.horizontalSpan = 1;
    text.setLayoutData(gd);
    text.addKeyListener(this);
    return text;
  }

  /**
   * Creates the button with given parent, text and style.
   *
   * @param parent
   *          the parent of the button
   * @param text
   *          button's text
   * @param style
   *          button's style
   * @return
   */
  public Button createButton(Composite parent, String text, int style) {
    Button button = new Button(parent, style | SWT.NONE);
    if (text != null) {
      button.setText(text);
    }
    return button;
  }

  /**
   * Creates the check box button with given parent and text.
   *
   * @param parent
   *          the parent of the button
   * @param text
   *          button's text
   * @return the created check box button
   */
  protected Button createCheckBox(Composite parent, String label) {
    Button button = new Button(parent, SWT.CHECK);
    GridData grid = new GridData(GridData.FILL_HORIZONTAL);

    button.setText(label);
    button.setLayoutData(grid);

    return button;
  }

  /**
   * Creates all fields of this wizard page
   *
   * @param container
   *          the parent container
   */
  private void createFields(Composite container) {
    Composite composite = new Composite(container, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 3;
    composite.setLayout(layout);
    composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    targetFolText = createTextLabel(composite, ResourceManager.getString(TARGET_FOLDER_LABEL, ""), //$NON-NLS-1$
        ResourceManager.getString(TARGET_FOLDER_LABEL, "")); //$NON-NLS-1$
    targetFolText.setText(targetFolTxt);

    targetFolButton = createButton(composite, BROWSE_BUTTON, SWT.PUSH);
    targetFolButton.addSelectionListener(this);
    GridData gd = new GridData();
    gd.verticalAlignment = GridData.BEGINNING;
    targetFolButton.setLayoutData(gd);

    fileNameText = createTextLabel(composite, ResourceManager.getString(DPP_FILE_NAME_LABEL, ""), //$NON-NLS-1$
        ResourceManager.getString(DPP_FILE_NAME_LABEL, "")); //$NON-NLS-1$
    fileNameText.setText("");
    Label l = new Label(composite, SWT.NONE);
    l.setText(""); //$NON-NLS-1$

    Separator separator = new Separator(SWT.SEPARATOR | SWT.HORIZONTAL);
    separator.doFillIntoGrid(composite, 3, targetFolText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);

    symbolicNameText = createTextLabel(composite, ResourceManager.getString(SYMBOLIC_NAME_LABEL, ""), //$NON-NLS-1$
        ResourceManager.getString(SYMBOLIC_NAME_LABEL, "")); //$NON-NLS-1$
    symbolicNameText.setText("");
    l = new Label(composite, SWT.NONE);
    l.setText(""); //$NON-NLS-1$

    versionText = createTextLabel(composite, ResourceManager.getString(VERSION_LABEL, ""), //$NON-NLS-1$
        ResourceManager.getString(VERSION_LABEL, "")); //$NON-NLS-1$
    versionText.setText("1.0.0");
    l = new Label(composite, SWT.NONE);
    l.setText(""); //$NON-NLS-1$

    // Options Group
    Group optionsGroup = new Group(container, SWT.NONE);
    optionsGroup.setText(ResourceManager.getString(OPTION_LABEL));
    GridData optionsData = new GridData(GridData.FILL_HORIZONTAL);
    optionsGroup.setLayoutData(optionsData);
    GridLayout optionsGrid = new GridLayout();
    optionsGroup.setLayout(optionsGrid);

    optionButton = createCheckBox(optionsGroup, ResourceManager.getString(OPTION_CHECKBOX_LABEL));
    ((GridData) optionButton.getLayoutData()).horizontalSpan = 3;
    optionButton.addSelectionListener(this);
    Composite composite1 = new Composite(optionsGroup, SWT.NONE);
    GridLayout layout1 = new GridLayout();
    layout1.numColumns = 2;
    GridData grid = new GridData(GridData.FILL_HORIZONTAL);
    composite1.setLayout(layout);
    composite1.setLayoutData(grid);
    optionText = createText(composite1);
    ((GridData) optionText.getLayoutData()).horizontalSpan = 2;
    optionText.setEditable(false);
    optionBrowseButton = createButton(composite1, BROWSE_BUTTON2, SWT.PUSH);
    optionBrowseButton.addSelectionListener(this);
    optionCheckAction();

    targetFolText.addModifyListener(this);
    fileNameText.addModifyListener(this);
    symbolicNameText.addModifyListener(this);
    versionText.addModifyListener(this);
    optionText.addModifyListener(this);
  }

  /**
   * Validates all fields of the page, sets error message if needed and call
   * <code>setPageComplete</code> method of this page
   *
   * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
   */
  public void modifyText(ModifyEvent e) {
    String folText = targetFolText == null ? "" : targetFolText.getText();
    String nameText = fileNameText == null ? "" : fileNameText.getText();
    String newMessage = null;
    boolean isNameOK = true;
    boolean isVersionOK = true;
    boolean isSymbolicOK = true;
    Object source = e.getSource();
    if (source == fileNameText) {
      String text = fileNameText.getText();
      isNameOK = PluginUtilities.isValidFileName(text + ".dpp");
      if (!isNameOK) {
        newMessage = ResourceManager.getString("NewDPPPage.error_file_name");
      }
      if (symbolicNameText != null) {
        String symbText = symbolicNameText.getText();
        if (symbText == null || !symbolicNameFlag) {
          symbolicNameText.removeModifyListener(this);
          String txt = text;
          if (text.endsWith(".dpp")) {
            txt = text.substring(0, text.lastIndexOf(".dpp"));
          }
          symbolicNameText.setText(txt);
          symbolicNameText.addModifyListener(this);
        }
      }
    } else if (source == symbolicNameText) {
      symbolicNameFlag = true;
      if (symbolicNameText.getText().equals("")) {
        symbolicNameFlag = false;
      }
      String symbText = symbolicNameText.getText();
      if (symbText != null) {
        if (!DPPUtilities.isCorrectPackage(symbText)) {
          if (symbText.startsWith(".") || symbText.endsWith(".")) {
            newMessage = ResourceManager.format("NewDPPPage.error_sn", new Object[] {
              symbText
            });
          } else if (symbText.indexOf(" ") != -1) {
            newMessage = ResourceManager.format("NewDPPPage.error_sn_space", new Object[] {
              symbText
            });
          } else {
            newMessage = ResourceManager.format("NewDPPPage.error_sn_identifier", new Object[] {
              symbText
            });
          }
          isSymbolicOK = false;
        }
      }
    } else if (source == versionText) {
      String verTxt = versionText.getText();
      String versionTxt = verTxt;
      if (versionTxt.indexOf(" ") != -1) {
        isVersionOK = false;
      } else {
        if (isVersionOK) {
          try {
            Version.parseVersion(verTxt);
          } catch (IllegalArgumentException ex) {
            isVersionOK = false;
          }
        }
      }
      if (!isVersionOK) {
        newMessage = ResourceManager.getString("NewDPPPage.error_version");
      }
    }

    boolean notFileExists = true;
    if (!firstTimeOpen) {
      if (folText.equals("")) {
        newMessage = ResourceManager.getString("NewDPPPage.error_empty_target");
      } else {
        IPath path = new Path(folText);
        path = path.removeFirstSegments(1);
        String newFilePath = path.toOSString() + File.separator + getFileName();
        if (prj != null && !newFilePath.equals("" + File.separator)) {
          IFile file = prj.getFile(newFilePath);
          if (file.exists()) {
            notFileExists = false;
            newMessage = ResourceManager.getString("NewDPPPage.error_file_exists");
          }
        }
      }
      if (nameText.equals("")) {
        newMessage = ResourceManager.getString("NewDPPPage.error_empty_file");
      }
    } else {
      firstTimeOpen = false;
    }
    if (newMessage == null || !newMessage.equals("")) {
      setErrorMessage(newMessage);
    }
    setPageComplete(!folText.equals("") && isNameOK && !nameText.equals("") && isVersionOK && notFileExists
        && isSymbolicOK);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.swt.events.KeyListener#keyPressed(org.eclipse.swt.events.
   * KeyEvent )
   */
  public void keyPressed(KeyEvent e) {
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.swt.events.KeyListener#keyReleased(org.eclipse.swt.events
   * .KeyEvent )
   */
  public void keyReleased(KeyEvent e) {
    keyReleaseOccured(e);
  }

  /**
   * If escape is pressed restore old value of the text field
   *
   * @param e
   *          the key event
   */
  protected void keyReleaseOccured(KeyEvent e) {
    if (e.character == '\u001b') { // Escape character
      if (e.getSource() instanceof Text) {
        Text text = (Text) e.getSource();
        String value = text.getText();
        text.setText(value); // restore old
      }
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse
   * .swt.events.SelectionEvent)
   */
  public void widgetDefaultSelected(SelectionEvent e) {
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt
   * .events.SelectionEvent)
   */
  public void widgetSelected(SelectionEvent e) {
    Object obj = e.getSource();
    if (obj instanceof Button) {
      Button but = (Button) obj;
      if (but == targetFolButton) {
        browseAction(targetFolText, but);
      } else if (but == optionBrowseButton) {
        optionBrowseAction();
      } else if (but == optionButton) {
        optionCheckAction();
      }
    }
  }

  /**
   * Opens file dialog, set chosen file in to the given text field, change the
   * container of this selection and project if this needed.
   *
   * @param fileText
   *          the text field in which will be set the selected file
   * @param browseButton
   *          the pushed button
   */
  protected void browseAction(Text fileText, Button browseButton) {
    String selectedPath = null;
    Class[] acceptedClasses = new Class[] {
        IJavaModel.class, IPackageFragmentRoot.class, IJavaElement.class, IJavaProject.class
    };
    ViewerFilter filter = new TypedViewerFilter(acceptedClasses) {
      @Override
      public boolean select(Viewer viewer, Object parent, Object element) {
        if (element instanceof IPackageFragmentRoot) {
          try {
            return (((IPackageFragmentRoot) element).getKind() == IPackageFragmentRoot.K_SOURCE);
          } catch (JavaModelException e) {
            JavaPlugin.log(e.getStatus()); // just log, no UI in
            // validation
            return false;
          }
        }
        return super.select(viewer, parent, element);
      }
    };
    StandardJavaElementContentProvider provider = new StandardJavaElementContentProvider();
    ILabelProvider labelProvider = new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT);
    ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(getShell(), labelProvider, provider);
    dialog.setComparator(new JavaElementComparator());
    dialog.setTitle(ResourceManager.getString("NewDPPPage.select_folder_dlg_title"));
    dialog.setMessage(ResourceManager.getString("NewDPPPage.select_folder_dlg_description"));
    dialog.addFilter(filter);
    dialog.setAllowMultiple(false);
    dialog.setInput(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot()));
    setInitialSelection(dialog);
    dialog.setHelpAvailable(false);

    if (dialog.open() == Window.OK) {
      Object obj = dialog.getFirstResult();
      if (obj instanceof IResource) {
        parentFolder = ((IResource) obj).getParent();
        selectedPath = ((IResource) obj).getFullPath().toString();
      } else if (obj instanceof IJavaElement) {
        parentFolder = ((IJavaElement) obj).getResource().getParent();
        selectedPath = ((IJavaElement) obj).getPath().toString();
      } else if (obj instanceof IFolder) {
        parentFolder = (IFolder) obj;
        selectedPath = parentFolder.getFullPath().toString();
      }
      prj = ((IJavaElement) obj).getResource().getProject();
      fileText.setText(selectedPath);
      browseButton.setFocus();

    }
  }

  private void setInitialSelection(ElementTreeSelectionDialog dialog) {

    if (targetFolText == null) {
      return;
    }
    String value = targetFolText.getText();
    if (value.equals("")) {
      return;
    }
    IPath path = new Path(value);
    if (!ResourcesPlugin.getWorkspace().getRoot().exists(path)) {
      return;
    }
    if (path.segmentCount() == 1) { // project
      dialog.setInitialSelection(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject(value)));
    }
    if (path.segmentCount() >= 2) {
      IFile f = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
      if (f.exists()) { // file
        dialog.setInitialSelection(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
      } else { // folder
        dialog.setInitialSelection(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFolder(path)));
      }
    }
  }

  /**
   * Opens file dialog, set chosen file in to the option text field.
   */
  private void optionBrowseAction() {
    String selectedFile = null;
    String[] extNames = new String[] {
      "*.dpp"
    };
    String[] ext = new String[] {
      "*.dpp"
    };
    String fileExt = ".dpp";
    FileDialog dialog = new FileDialog(DPPErrorHandler.getShell(), SWT.OPEN);
    dialog.setFilterNames(extNames);
    dialog.setFilterExtensions(ext);
    String path = DPPUtil.getFileDialogPath(optionText.getText());
    dialog.setFilterPath(path);
    dialog.setText("Open");

    selectedFile = dialog.open();

    if (selectedFile != null) {
      DPPUtil.fileDialogLastSelection = selectedFile;
      if (!selectedFile.endsWith(fileExt)) {
        selectedFile += fileExt;
      }
      File file = new File(selectedFile);
      if (file.exists()) {
        try {
          DPPFile dpp = new DPPFile(file);
          String symbName = dpp.getPackageHeaders().getSymbolicName();
          String version = dpp.getPackageHeaders().getVersion();
          symbolicNameText.setText(symbName);
          versionText.setText(version);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      optionText.setText(selectedFile);
      optionBrowseButton.setFocus();
    }
  }

  /**
   * Sets the option text field and browse button enabled or disabled, depends
   * on selection of the option check box button.
   */
  private void optionCheckAction() {
    boolean selection = optionButton.getSelection();
    optionText.setEnabled(selection);
    optionBrowseButton.setEnabled(selection);
    if (!selection) {
      optionText.setText("");
    }
  }

  /**
   * Sets the given container.
   *
   * @param parentFol
   *          the new container
   */
  public void setParentFolder(IContainer parentFol) {
    this.parentFolder = parentFol;
    if (parentFolder != null) {
      targetFolTxt = parentFol.getFullPath().toString();
    }
  }

  /**
   * Returns the container of the selection or the container of the chosen
   * element.
   */
  public IContainer getParentFolder() {
    return parentFolder;
  }

  /**
   * Sets the project.
   *
   * @param prj
   *          the new project
   */
  public void setProject(IProject prj) {
    this.prj = prj;
  }

  /**
   * Returns the project.
   */
  public IProject getProject() {
    return prj;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.jface.wizard.WizardPage#getShell()
   */
  @Override
  public Shell getShell() {
    Display display = PlatformUI.getWorkbench().getDisplay();
    Shell shell = display.getActiveShell();
    return shell;
  }

  /**
   * Returns the chosen target folder
   */
  public String getTargetFolder() {
    return targetFolText.getText();
  }

  /**
   * Returns the chosen deployment package file name
   */
  public String getFileName() {
    String name = fileNameText == null ? "" : fileNameText.getText();
    if (!name.equals("")) {
      if (name.endsWith(".")) {
        name += "dpp";
      }
      if (!name.endsWith(".dpp")) {
        name += ".dpp";
      }
    }
    return name;
  }

  /**
   * Returns the chosen deployment package symbolic name
   */
  public String getSymbolicName() {
    return symbolicNameText.getText();
  }

  /**
   * Returns the chosen deployment package version
   */
  public String getVersion() {
    return versionText.getText();
  }

  /**
   * Returns the selected dpp file, which properties will be set to the created
   * dpp file.
   */
  public String getDPPPropertyFile() {
    String result = null;
    if (optionButton.getSelection()) {
      result = optionText.getText();
      if (result.equals("")) {
        result = null;
      }
    }
    return result;
  }

  public boolean performFinish() {
    return true;
  }

  /**
   * The <code>WizardPage</code> implementation of this <code>IWizard</code>
   * method returns the value of an internal state variable set by
   * <code>setPageComplete</code>. Subclasses may extend.
   */
  @Override
  public boolean isPageComplete() {
    boolean result = super.isPageComplete();
    String folText = targetFolText == null ? "" : targetFolText.getText();
    String nameText = fileNameText == null ? "" : fileNameText.getText();
    if (!nameText.equals("")) {
      if (nameText.endsWith(".")) {
        nameText += "dpp";
      } else {
        if (!nameText.endsWith(".dpp")) {
          nameText += ".dpp";
        }
      }
    }
    String versionTxt = versionText == null ? "" : versionText.getText();
    String newMessage = null;
    boolean isNameOK = true;
    boolean isVersionOK = true;
    boolean isTargetOK = true;
    boolean isSymbolicOK = true;
    boolean isDeriveOk = true;

    boolean notFileExists = true;
    if (!firstTimeOpen) {
      isNameOK = PluginUtilities.isValidFileName(nameText + ".dpp");
      if (symbolicNameText != null) {
        String symbText = symbolicNameText.getText();
        symbText = symbolicNameText.getText();
        if (!DPPUtilities.isCorrectPackage(symbText)) {
          if (symbText.startsWith(".") || symbText.endsWith(".")) {
            newMessage = ResourceManager.format("NewDPPPage.error_sn", new Object[] {
              symbText
            });
          } else if (symbText.indexOf(" ") != -1) {
            newMessage = ResourceManager.format("NewDPPPage.error_sn_space", new Object[] {
              symbText
            });
          } else {
            newMessage = ResourceManager.format("NewDPPPage.error_sn_identifier", new Object[] {
              symbText
            });
          }
          isSymbolicOK = false;
        }
      }
      if (!isNameOK) {
        newMessage = ResourceManager.getString("NewDPPPage.error_file_name");
      }
      String verTxt = versionTxt;
      if (verTxt.indexOf(" ") != -1) {
        isVersionOK = false;
      } else {
        if (isVersionOK) {
          try {
            Version.parseVersion(verTxt);
          } catch (IllegalArgumentException ex) {
            isVersionOK = false;
          }
        }
      }
      if (!isVersionOK) {
        newMessage = ResourceManager.getString("NewDPPPage.error_version");
      }
      if (symbolicNameText.getText().equals("")) {
        symbolicNameFlag = false;
      }
      if (folText.equals("")) {
        newMessage = ResourceManager.getString("NewDPPPage.error_empty_target");
      } else {
        IPath path = new Path(folText);
        path = path.removeFirstSegments(1);
        String newFilePath = path.toOSString() + File.separator + getFileName();
        if (prj != null && !newFilePath.equals("" + File.separator)) {
          IFile file = prj.getFile(newFilePath);
          if (file.exists()) {
            notFileExists = false;
            newMessage = ResourceManager.getString("NewDPPPage.error_file_exists");
          }
        }
      }
      if (nameText.equals("")) {
        newMessage = ResourceManager.getString("NewDPPPage.error_empty_file");
      }
      if (prj == null) {
        newMessage = ResourceManager.getString("NewDPPWizard.notInTheProjectError");
        isTargetOK = false;
      } else {
        String tmp = targetFolText.getText();
        if (tmp.equals("")) {
          newMessage = ResourceManager.getString("NewDPPPage.error_empty_file");
          isTargetOK = false;
        }
        IPath path = new Path(tmp);
        try {
          if (PDEPlugin.getWorkspace().getRoot().exists(path)) {
            IPath p = new Path(tmp);
            if (p.segments().length == 1) {
              prj = PDEPlugin.getWorkspace().getRoot().getProject(tmp);
            }
            if (p.segments().length >= 2) {
              prj = PDEPlugin.getWorkspace().getRoot().getFolder(new Path(tmp)).getProject();
            }
          }
        } catch (IllegalArgumentException iae) {
        }
      }
    }
    if (optionButton.getSelection()) {
      File file = new File(optionText.getText());
      if (!file.exists()) {
        isDeriveOk = false;
      }
    }

    if (newMessage == null || !newMessage.equals("")) {
      setErrorMessage(newMessage);
    }
    result = (!folText.equals("") && isNameOK && !nameText.equals("") && isVersionOK && notFileExists && isTargetOK
        && isSymbolicOK && isDeriveOk);
    return result;
  }

  public String getPrjRootPath() {
    return prj.getLocation().toOSString();
  }
}

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
package org.tigris.mtoolkit.dpeditor.editor;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.dpeditor.editor.base.DPPFormSection;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormText;
import org.tigris.mtoolkit.dpeditor.editor.forms.FormWidgetFactory;
import org.tigris.mtoolkit.dpeditor.editor.forms.IFormTextListener;
import org.tigris.mtoolkit.dpeditor.editor.model.DPPFileModel;
import org.tigris.mtoolkit.dpeditor.editor.model.IModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.editor.model.ModelChangedEvent;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.BuildInfo;
import org.tigris.mtoolkit.util.DPPFile;

/**
 * The FormSection for the build area of the deployment package editor.
 * Represents the all information from the <code>BuildInfo</code>. Contains a
 * header, description and custom section, that presents the build properties of
 * the deployment package file.
 */
public class BuildSection extends DPPFormSection implements IFormTextListener,
		ModifyListener, SelectionListener {

	/** The title of this section */
	public static final String SECTION_TITLE = "DPPEditor.BuildSection.title";
	/** The description of the section */
	public static final String SECTION_DESC = "DPPEditor.BuildSection.desc";
	/** The Browse button label */
	public static final String BROWSE_BUTTON = "...";

	/** The Build location label */
	public static final String BUILD_LOCATION_LABEL = "DPPEditor.BuildSection.Build_Location_Label"; //$NON-NLS-1$
	/** The Deployment package file name label */
	public static final String DP_FILE_LABEL = "DPPEditor.BuildSection.DP_File_Name_Label"; //$NON-NLS-1$
	/** The ant file name label */
	public static final String ANT_FILE_LABEL = "DPPEditor.BuildSection.Ant_File_Name_Label"; //$NON-NLS-1$

	/** The parent composite in which all components will be added */
	private Composite container;
	/**
	 * The <code>boolean</code> flag that shows if the update of the section is
	 * needed
	 */
	private boolean updateNeeded;

	/** The text field, thats presents the deployment package's file */
	private FormText dpFileText;
	/** The text field, thats presents the ant file */
	private FormText antFileText;
	/**
	 * The browse button, through which the deployment package file can be
	 * chosen
	 */
	private Button dpFileButton;
	/** The browse button, through which the ant file can be chosen */
	private Button antFileButton;
	/** The file extension that the file dialog must be filtered */
	private String extension = "";

	/** The corresponding form page */
	private BuildFormPage page;

	/**
	 * Creates the new instance of the FormSection which parent is the given
	 * page form.
	 * 
	 * @param formPage
	 *            the parent form page
	 */
	public BuildSection(BuildFormPage page) {
		super(page);
		this.page = page;
		setHeaderText(ResourceManager.getString(SECTION_TITLE, ""));
		setDescription(ResourceManager.getString(SECTION_DESC, ""));
	}

	/**
	 * This method is called from the <code>createControl</code> method and puts
	 * all custom components in this form section.
	 * 
	 * @param parent
	 *            a composite control which will be the parent of the created
	 *            client composite which will be holds all custom controls
	 * @return Returns the composite control which will be holds all custom
	 *         controls
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#createClient(org.eclipse.swt.widgets.Composite)
	 */
	public Composite createClient(Composite parent) {
		container = FormWidgetFactory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		container.setLayout(layout);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, "");

		createMainFields(container);

		container.pack();
		return container;
	}

	/**
	 * Creates the principle fields of this section.
	 * 
	 * @param container
	 *            a composite control which will be the parent of the created
	 *            composite
	 */
	private void createMainFields(Composite container) {
		Text text = null;
		text = createText(container, ResourceManager.getString(DP_FILE_LABEL, ""), ResourceManager.getString(DP_FILE_LABEL, "")); //$NON-NLS-1$
		dpFileText = new FormText(text);
		dpFileText.addFormTextListener(this);

		dpFileButton = FormWidgetFactory.createButton(container, BROWSE_BUTTON, SWT.PUSH);
		dpFileButton.addSelectionListener(this);
		GridData gd = new GridData();
		gd.verticalAlignment = GridData.BEGINNING;
		dpFileButton.setLayoutData(gd);

		text = createText(container, ResourceManager.getString(ANT_FILE_LABEL, ""), ResourceManager.getString(ANT_FILE_LABEL, "")); //$NON-NLS-1$
		antFileText = new FormText(text);
		antFileText.addFormTextListener(this);

		antFileButton = FormWidgetFactory.createButton(container, BROWSE_BUTTON, SWT.PUSH);
		antFileButton.addSelectionListener(this);
		gd = new GridData();
		gd.verticalAlignment = GridData.BEGINNING;
		antFileButton.setLayoutData(gd);
	}

	// Logic part
	/**
	 * Initializes the all custom created controls with the given
	 * <code>Object</code>
	 * 
	 * @param input
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#initialize(java.lang.Object)
	 */
	public void initialize(Object input) {
		update(input);
	}

	/**
	 * Updates the values of this FormSection.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#update()
	 */
	public void update() {
		this.update(getFormPage().getModel());
	}

	/**
	 * Updates the values of this section, as gets all the new values from the
	 * given input object.
	 * 
	 * @param input
	 *            the Object from which will be gets the new values.
	 */
	public void update(Object input) {
		DPPFileModel model = (DPPFileModel) input;
		if (model == null) {
			return;
		}
		DPPFile dppFile = model.getDPPFile();
		BuildInfo buildInfo = dppFile.getBuildInfo();
		if (buildInfo == null) {
			buildInfo = new BuildInfo();
		}
		File file = dppFile.getFile();
		String fileName = file.getName();
		int index = fileName.lastIndexOf(".");
		if (index != -1) {
			fileName = fileName.substring(0, index);
		}
		String dppFileName = file.getParentFile().getAbsolutePath() + File.separator + fileName;
		buildInfo.setBuildLocation("");
		String value = buildInfo.getDpFileName();
		boolean setNewValue = false;
		if (value == null || value.equals("")) {
			buildInfo.setDpFileName(dppFileName + ".dp");
			setNewValue = true;
		}
		value = buildInfo.getAntFileName();
		if (value == null || value.equals("")) {
			buildInfo.setAntFileName(dppFileName + "_build.xml");
			setNewValue = true;
		}
		if (setNewValue) {
			dppFile.setBuildInfo(buildInfo);
		}

		setIfDefined(dpFileText, buildInfo.getDpFileName());
		setIfDefined(antFileText, buildInfo.getAntFileName());
		FormWidgetFactory.paintBordersFor(dpFileText.getControl().getParent());
		updateNeeded = false;
	}

	/**
	 * Sets to the given text field the given value if not null or the empty
	 * string if the value is <code>null</code>
	 * 
	 * @param formText
	 *            the text field
	 * @param value
	 *            the value that will be set to the text field
	 */
	private void setIfDefined(FormText formText, String value) {
		if (value != null) {
			value = setRelativePath(value);
			formText.setValue(value, true);
		} else {
			formText.setValue("", true); //$NON-NLS-1$
		}
	}

	/**
	 * Returns if one of the values of the text fields are changed.
	 * 
	 * @return <code>true</code> if one of the values of text fields are
	 *         changed, and <code>false</code> when no one of the values are
	 *         changed
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#isDirty()
	 */
	public boolean isDirty() {
		return dpFileText.isDirty() || antFileText.isDirty();
	}

	/**
	 * Notifies all listeners of the text fields for the changes of the values
	 * and sets this changes.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.FormSection#commitChanges(boolean)
	 */
	public void commitChanges(boolean onSave) {
		dpFileText.commit();
		antFileText.commit();
	}

	/**
	 * Sets the new values from the given text field to the model.
	 * 
	 * @param text
	 *            the text field which value will be set to the model
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormTextListener#textDirty(org.tigris.mtoolkit.dpeditor.editor.forms.FormText)
	 */
	public void textDirty(FormText text) {
		DPPFileModel model = (DPPFileModel) getFormPage().getModel();
		DPPFile dppFile = model.getDPPFile();
		BuildInfo buildInfo = dppFile.getBuildInfo();
		forceDirty();
		text.setDirty(true);
		setInfoValues(dppFile, buildInfo, text);
		update();
	}

	/**
	 * Notifies all existing <code>IModelChangedListener</code>'s of a change of
	 * the model.
	 * 
	 * @see org.tigris.mtoolkit.dpeditor.editor.forms.IFormTextListener#textValueChanged(org.tigris.mtoolkit.dpeditor.editor.forms.FormText)
	 */
	public void textValueChanged(FormText text) {
		forceDirty();
		text.removeFormTextListener(this);
		DPPFileModel model = ((DPPFileModel) getFormPage().getModel());
		DPPFile dppFile = model.getDPPFile();
		BuildInfo info = dppFile.getBuildInfo();
		setInfoValues(dppFile, info, text);
		model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.EDIT, new Object[] { info }, null));
		page.updateDocumentIfSource();
		String value = text.getValue();
		value = setRelativePath(value);
		text.setValue(value);
		text.addFormTextListener(this);
	}

	/**
	 * Sets the new values from the given text field to the given
	 * <code>BuildInfo</code> and set this info to the given deployment package
	 * file.
	 * 
	 * @param dppFile
	 *            the Deployment package file, in which the new value will be
	 *            set
	 * @param info
	 *            the BuildInfo, which value will be updated
	 * @param text
	 *            the text field which value will be set to the model
	 */
	private void setInfoValues(DPPFile dppFile, BuildInfo info, FormText text) {
		if (info == null) {
			info = new BuildInfo();
			info.setBuildLocation("");
			info.setDpFileName("");
			info.setAntFileName("");
		}
		if (text == dpFileText) {
			info.setDpFileName(text.getValue());
		} else if (text == antFileText) {
			info.setAntFileName(text.getValue());
		}
		dppFile.setBuildInfo(info);
	}

	/**
	 * Sets to the corresponding <code>BuildInfo</code> object the values from
	 * the given text field.
	 * 
	 * @param text
	 *            the text field which value will be set to the
	 *            <code>BuildInfo</code>
	 */
	private void setBuildValues(FormText text) {
		DPPFileModel model = (DPPFileModel) getFormPage().getModel();
		DPPFile dppFile = model.getDPPFile();
		BuildInfo buildInfo = dppFile.getBuildInfo();
		if (text == dpFileText) {
			buildInfo.setDpFileName(text.getValue());
		} else if (text == antFileText) {
			buildInfo.setAntFileName(text.getValue());
		}
		dppFile.setBuildInfo(buildInfo);
		model.fireModelChanged(new ModelChangedEvent(IModelChangedEvent.ADD, new Object[] { buildInfo }, null));
	}

	/**
	 * Sets that the model was changed.
	 */
	private void forceDirty() {
		setDirty(true);
		DPPFileModel model = (DPPFileModel) getFormPage().getModel();
		model.setDirty(true);
		getFormPage().getEditor().fireSaveNeeded();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events
	 * .ModifyEvent)
	 */
	public void modifyText(ModifyEvent e) {
		page.updateDocumentIfSource();
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
			if (but == dpFileButton) {
				extension = "*.dp";
				browseAction(dpFileText, but);
			} else if (but == antFileButton) {
				extension = "*.xml";
				browseAction(antFileText, but);
			}
		}
	}

	/**
	 * Creates and opens the file chooser dialog and sets the chosen file to the
	 * given text field and gained focus to the given browse button.
	 * 
	 * @param fileText
	 *            the text field in which chosen file will be appear as text
	 * @param browseButton
	 *            the pushed button
	 */
	protected void browseAction(FormText fileText, Button browseButton) {
		String selectedFile = null;
		String[] extNames = new String[] { extension };
		String[] ext = new String[] { extension };
		String fileExt = extension.substring(1);
		FileDialog dialog = new FileDialog(DPPErrorHandler.getShell(), SWT.OPEN);
		dialog.setFilterNames(extNames);
		dialog.setFilterExtensions(ext);
		String path = DPPUtil.getFileDialogPath(fileText.getValue());
		dialog.setFilterPath(path);

		selectedFile = dialog.open();

		if (selectedFile != null) {
			DPPUtil.fileDialogLastSelection = selectedFile;
			if (!selectedFile.endsWith(fileExt)) {
				selectedFile += fileExt;
			}
			selectedFile = setRelativePath(selectedFile);
			fileText.setValue(selectedFile);
			setBuildValues(fileText);
			browseButton.setFocus();
		}
	}

	protected String setRelativePath(String value) {
		DPPFileModel model = (DPPFileModel) getFormPage().getModel();
		IFile ifile = model.getFile();
		String location = ifile.getProject().getLocation().toOSString();
		if (value.toLowerCase().startsWith(location.toLowerCase() + File.separator)) {
			value = "<.>" + value.substring(location.length());
		}
		return value;
	}
}

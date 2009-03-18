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
package org.tigris.mtoolkit.cdeditor.internal.wizards;

import java.io.ByteArrayInputStream;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.ide.DialogUtil;
import org.eclipse.ui.internal.wizards.newresource.ResourceMessages;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;
import org.tigris.mtoolkit.cdeditor.internal.ComponentDescriptionEditor;
import org.tigris.mtoolkit.cdeditor.internal.model.CDFactory;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDComponent;
import org.tigris.mtoolkit.cdeditor.internal.model.ICDModel;
import org.tigris.mtoolkit.cdeditor.internal.model.IEclipseContext;
import org.tigris.mtoolkit.cdeditor.internal.model.PathFilter;
import org.tigris.mtoolkit.cdeditor.internal.model.Validator;
import org.tigris.mtoolkit.cdeditor.internal.text.BuildModelProcessor;
import org.tigris.mtoolkit.cdeditor.internal.text.BundleModelProcessor;
import org.tigris.mtoolkit.cdeditor.internal.text.PDETextModelHelper;
import org.tigris.mtoolkit.cdeditor.internal.widgets.CreateComponentGroup;
import org.tigris.mtoolkit.cdeditor.internal.widgets.UIResources;


public class NewComponentDescriptionWizard extends BasicNewResourceWizard {

	private NewComponentResourcePage mainPage;
	private NewComponentDescriptionPage secondPage;

	public NewComponentDescriptionWizard() {
		super();
	}

	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
		super.init(workbench, currentSelection);
		setWindowTitle("New Components Description");
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(UIResources.getImageDescriptor(UIResources.NEW_DESCRIPTION_BANNER));
	}

	public boolean canFinish() {
		if (mainPage != null && mainPage.isPageComplete() && !mainPage.createSingleComponentDescription())
			return true;
		else
			return super.canFinish();
	}

	private String getContents() {
		ICDModel model = CDFactory.createModel("osgi-components", null);
		if (mainPage.createSingleComponentDescription()) {
			ICDComponent component = secondPage.createComponent();
			model.setSingleComponent(component);
			model.setSingle(true);
		}
		return model.print();
	}

	protected IEclipseContext getEclipseContext() {
		if (mainPage == null)
			return null;
		IPath containerPath = mainPage.getContainerFullPath();
		if (containerPath == null || containerPath.segmentCount() < 1)
			return null;

		IProject selectedProject = ResourcesPlugin.getWorkspace().getRoot().getProject(containerPath.segment(0));
		return (IEclipseContext) selectedProject.getAdapter(IEclipseContext.class);
	}

	public boolean performFinish() {
		// TODO: We should use workspace operation to create our file
		// we receive workspace synchronization and progress indication
		// almost for free
		IFile file = mainPage.createNewFile();
		if (file == null) {
			return false;
		} else {
			byte[] bytes = getContents().getBytes();
			try {
				file.setContents(new ByteArrayInputStream(bytes), 0, null);
			} catch (CoreException e) {
				try {
					file.delete(true, null);
				} catch (CoreException e1) {
					CDEditorPlugin.log(e1);
				}
				CDEditorPlugin.error("Unable to set the contents of the newly created file", e);
				return false;
			}
		}

		updateManifest(file);
		updateBuildProperties(file);

		// set our editor as default for the new file
		IDE.setDefaultEditor(file, ComponentDescriptionEditor.EDITOR_ID);

		selectAndReveal(file);

		IWorkbenchWindow dw = getWorkbench().getActiveWorkbenchWindow();
		try {
			if (dw != null) {
				IWorkbenchPage page = dw.getActivePage();
				if (page != null) {
					IDE.openEditor(page, file, ComponentDescriptionEditor.EDITOR_ID, true);
				}
			}
		} catch (PartInitException e) {
			DialogUtil.openError(dw.getShell(), ResourceMessages.FileResource_errorMessage, e.getMessage(), e);
		}
		return true;
	}

	private void updateManifest(final IFile createdDescription) {
		IPluginModelBase pluginModel = PluginRegistry.findModel(createdDescription.getProject());
		if (pluginModel == null)
			// TODO: Notify the user that the project is not suitable for update
			return;
		IFile manifest = createdDescription.getProject().getFile(ICoreConstants.MANIFEST_PATH);
		if (!manifest.exists())
			return;
		try {
			PDETextModelHelper.processBundleModel(manifest, new BundleModelProcessor() {
				public Object processBundleModel(IBundleModel model, IDocument document, IProgressMonitor monitor) {
					IBundle bundle = model.getBundle();
					IManifestHeader header = bundle.getManifestHeader("Service-Component");
					if (header == null) {
						bundle.setHeader("Service-Component", prettyFormatManifestHeader(createdDescription.getProjectRelativePath().toString()));
					} else if (!isDescriptionReferenced(header.getValue(), createdDescription.getProjectRelativePath())) {
						String currentValue = header.getValue();
						header.setValue(prettyFormatManifestHeader(currentValue + "," + createdDescription.getProjectRelativePath()));
					}
					return null;
				}
			}, null);
		} catch (CoreException e) {
			CDEditorPlugin.error("Failed to update the bundle manifest with the new component description", e);
		}
	}

	private boolean isDescriptionReferenced(String manifestHeader, IPath newDescriptionPath) {
		StringTokenizer tokenizer = new StringTokenizer(manifestHeader, ",");
		while (tokenizer.hasMoreTokens()) {
			String nextPath = tokenizer.nextToken().trim();
			PathFilter filter = new PathFilter(nextPath);
			if (filter.matchPath(newDescriptionPath))
				return true;
		}
		return false;
	}

	private void updateBuildProperties(final IFile description) {
		IPluginModelBase pluginModel = PluginRegistry.findModel(description.getProject());
		if (pluginModel == null)
			// TODO: Notify the user that the project is not suitable for update
			return;
		IFile buildProps = description.getProject().getFile(ICoreConstants.BUILD_PROPERTIES_PATH);
		if (!buildProps.exists())
			return;
		final IPath descPath = description.getProjectRelativePath();
		try {
			PDETextModelHelper.processBuildModel(buildProps, new BuildModelProcessor() {
				public Object processBuildModel(IBuildModel model, IDocument document, IProgressMonitor monitor) throws CoreException {
					IBuild build = model.getBuild();
					IBuildEntry binIncludesEntry = build.getEntry(IBuildEntry.BIN_INCLUDES);
					if (binIncludesEntry == null) {
						binIncludesEntry = model.getFactory().createEntry(IBuildEntry.BIN_INCLUDES);
						build.add(binIncludesEntry);
					}
					String[] tokens = binIncludesEntry.getTokens();
					for (int i = 0; i < tokens.length; i++) {
						IPath tokenPath = new Path(tokens[i]);
						if (tokenPath.isPrefixOf(descPath)) {
							// the file is already included
							return null;
						}
					}
					binIncludesEntry.addToken(descPath.toString());
					return null;
				}
			}, null);
		} catch (CoreException e) {
			CDEditorPlugin.error("Failed to update the bundle build properties with the new component description", e);
		}
	}

	private String prettyFormatManifestHeader(String value) {
		String[] components = value.split(",");
		StringBuffer buffer = new StringBuffer(value.length() + components.length * 3);
		for (int i = 0; i < components.length; i++) {
			if (i != 0)
				buffer.append(",\n ");
			buffer.append(components[i].trim());
		}
		return buffer.toString();
	}

	public void addPages() {
		mainPage = new NewComponentResourcePage(getSelection());
		addPage(mainPage);
		secondPage = new NewComponentDescriptionPage(mainPage);
		addPage(secondPage);
	}

	public static class NewComponentResourcePage extends
			WizardNewFileCreationPage {

		private Button multiButton;
		private Button singleButton;

		private SelectionListener updateButtonsListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// this button influence the enablement of "Finish"
				getContainer().updateButtons();
			}
		};

		public NewComponentResourcePage(IStructuredSelection selection) {
			super("New Components Description", selection);
			setDescription("Create a file for the components descriptions");
			setTitle("Components Description");
			// TODO: Permit only .xml extensions of the file, because currently
			// it is little hard to support arbitrary extensions (the problems
			// arise from the different expectations from different users).
			setFileExtension("xml");
		}

		protected void createAdvancedControls(Composite parent) {
			Group typeGroup = new Group(parent, SWT.SHADOW_IN);
			typeGroup.setText("Description type");
			FillLayout layout = new FillLayout(SWT.VERTICAL);
			layout.spacing = 3;
			layout.marginHeight = 4;
			layout.marginWidth = 16;
			typeGroup.setLayout(layout);
			typeGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			singleButton = new Button(typeGroup, SWT.RADIO);
			singleButton.setText("Create single-component description");
			singleButton.setSelection(true);
			singleButton.addSelectionListener(updateButtonsListener);

			multiButton = new Button(typeGroup, SWT.RADIO);
			multiButton.setText("Create multi-component description");
			multiButton.addSelectionListener(updateButtonsListener);

		}

		protected IStatus validateLinkedResource() {
			return Status.OK_STATUS;
		}

		protected void createLinkTarget() {
			// nothing to be done
		}

		public boolean createSingleComponentDescription() {
			return singleButton.getSelection();
		}

		public boolean canFlipToNextPage() {
			return createSingleComponentDescription() && super.canFlipToNextPage();
		}

	}

	public class NewComponentDescriptionPage extends WizardPage implements
			CreateComponentGroup.InputValidator {

		private CreateComponentGroup group;
		private NewComponentResourcePage resourcePage;
		private boolean manualInput;

		protected NewComponentDescriptionPage(
				NewComponentResourcePage resourcePage) {
			super("New Component Description");
			setDescription("Specify name and implemention class of the new component");
			setTitle("Component");
			this.resourcePage = resourcePage;
		}

		public void createControl(Composite parent) {
			group = new CreateComponentGroup(this, getEclipseContext());

			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout());

			Group g = new Group(composite, SWT.SHADOW_IN);
			g.setText("Component Properties");
			g.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			FillLayout layout = new FillLayout();
			layout.spacing = 2;
			layout.marginHeight = 6;
			layout.marginWidth = 6;
			g.setLayout(layout);
			group.createContents(g);

			setControl(composite);
			validate(group);
			manualInput = false; // validation will be considered manual input
		}

		public void validate(CreateComponentGroup aGroup) {
			manualInput = true;
			IStatus status = Validator.validateComponent(aGroup.getComponentName(), aGroup.getComponentClass(), null);
			setErrorMessage(status.matches(IStatus.ERROR) ? status.getMessage() : null);
			setMessage(status.matches(IStatus.WARNING) ? status.getMessage() : null, WARNING);
			setPageComplete(!status.matches(IStatus.ERROR));
		}

		private String suggestComponentName(String filename) {
			int idx;
			idx = filename.lastIndexOf('.');
			// no '.' -> the whole filename
			String name = (idx == -1) ? filename : filename.substring(0, idx);
			// allowed special symbols in filenames are: ~`!@#$%^&()-+=[]{};',. 
			name = name.replace('~', '_').replace('`', '_').replace('!', '_').replace('@', '_')
						.replace('#', '_').replace('$', '_').replace('%', '_').replace('^', '_')
						.replace('&', '_').replace('(', '_').replace(')', '_').replace('-', '_')
						.replace('+', '_').replace('=', '_').replace('[', '_').replace(']', '_')
						.replace('{', '_').replace('}', '_').replace(';', '_').replace('\'', '_')
						.replace(',', '_')/*.replace('.', '_')*/.replace(' ', '_');

			return name;
		}

		private String suggestComponentClass(String compName) {
			return compName.concat(".Component");
		}

		public void setVisible(boolean visible) {
			if (visible && !manualInput) {
				String suggestedName = suggestComponentName(resourcePage.getFileName());
				String suggestedClass = suggestComponentClass(suggestedName);
				group.initialize(suggestedName, suggestedClass);
				validate(group);
				manualInput = false; // validation will be considered manual
				// input
			}
			super.setVisible(visible);
		}

		public ICDComponent createComponent() {
			ICDComponent component = CDFactory.createComponent();
			group.commitGroup(component);
			return component;
		}

	}
}

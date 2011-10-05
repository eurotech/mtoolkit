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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.dpeditor.DPActivator;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.DPPFile;

/**
 * Generates deployment package by the passed deployment package file object.
 */
public class BuildExportWizard extends Wizard implements IExportWizard {

	/** The name of the page of the wizard */
	private String WIZARD_BUILD_PAGE_NAME = "Build";

	private boolean isFinishPressed = false;
	private boolean isFinishPerformed = false;
	/** Selected deployment package file */
	private DPPFile dppFile;
	/** The project of the selected deployment package file or the selected file */
	private IProject project;

	/**
	 * Returns if the finish is performed.
	 */
	public boolean isFinishPerformed() {
		return isFinishPerformed;
	}

	/**
	 * Implements <code>org.eclipse.jface.wizard.Wizard</code> method to perform
	 * any special finish processing for the ant wizard.
	 */
	public boolean performFinish() {
		// Save dirty editors if possible but do not stop if not all are saved
		boolean save = PlatformUI.getWorkbench().saveAllEditors(true);
		if (!save)
			return false;
		isFinishPerformed = true;
		boolean res = performFinishDelegate();
		return res;
	}

	/**
	 * Implements <code>org.eclipse.jface.wizard.Wizard</code> method.
	 * Initializes this creation wizard using the passed workbench and object
	 * selection.
	 * <p>
	 * This method is called after the no argument constructor and before other
	 * methods are called.
	 * </p>
	 * Initializes the deployment package file with the current selection.
	 * 
	 * @param workbench
	 *            the current workbench
	 * @param selection
	 *            the current object selection
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(ResourceManager
				.getString("BuildExportWizard.build_title"));
		setNeedsProgressMonitor(true);
		Object firstElement = selection.getFirstElement();
		if (firstElement instanceof DPPFile) {
			BuildPage page = (BuildPage) getPage(WIZARD_BUILD_PAGE_NAME);
			page.setDPPFileProject((DPPFile) firstElement, null);
		} else if (firstElement instanceof IFile) {
			IFile file = (IFile) firstElement;
			project = file.getProject();
			IPath path = file.getLocation();
			String fileExtension = path.getFileExtension();
			if (fileExtension != null && fileExtension.equals("dpp")) {
				try {
					dppFile = new DPPFile(path.toFile(), project.getLocation()
							.toOSString());
				} catch (IOException e) {
				}
			}
		} else if (firstElement instanceof IProject) {
			IProject dpPrj = (IProject) firstElement;
			try {
				if (!dpPrj.hasNature("org.tigris.mtoolkit.dpproject.DPNature")) {
					return;
				}
				IFile file = dpPrj.getFile("package.dpp");
				if (!file.exists()) {
					return;
				}
				IPath path = new Path(file.getLocation().toString());
				dppFile = new DPPFile(path.toFile(), dpPrj.getLocation()
						.toOSString());
			} catch (IOException e) {
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Adds the page of this <code>BuildExportWizard</code> before the wizard
	 * opens.
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		setDefaultPageImageDescriptor(DPPImageDescriptor.BUILD_DP_IMAGE_WIZARD);
		WizardPage page = new BuildPage(
				WIZARD_BUILD_PAGE_NAME,
				ResourceManager.getString("BuildExportWizard.build_page_title"),
				ResourceManager
						.getString("BuildExportWizard.build_page_description"));
		addPage(page);
		BuildPage buildPage = (BuildPage) getPage(WIZARD_BUILD_PAGE_NAME);
		buildPage.setDPPFileProject(dppFile, project);
	}

	/**
	 * Disposes all the pages controls.
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#dispose()
	 */
	public void dispose() {
		super.dispose();
		if (!isFinishPressed) {
			BuildPage page = (BuildPage) getPage(WIZARD_BUILD_PAGE_NAME);
			Object obj = page.getDPPFile();
			if (obj instanceof DPPFile) {
				DPPFile dppFile = (DPPFile) obj;
			}
		}
	}

	/**
	 * Performs any special finish processing for the ant wizard. Creates
	 * deployment package by the passed <code>DPPFile</code> object
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinishDelegate() {
		isFinishPressed = true;
		BuildPage page = (BuildPage) getPage(WIZARD_BUILD_PAGE_NAME);
		boolean isFinishSuccessful = page.performFinish();
		if (!isFinishSuccessful) {
			return false;
		}
		DPPFile dppFile = page.getDPPFile();
		ProgressRun progressRun = new ProgressRun(dppFile);
		ProgressMonitorDialog progress = new ProgressMonitorDialog(Display
				.getCurrent().getActiveShell());
		try {
			progress.run(true, true, progressRun);
		} catch (InvocationTargetException e) {
			String msg = ResourceManager.getString("BuildExportWizard.errorMessage")
					+ ((e.getTargetException().getMessage() != null) ? ": " + e.getTargetException().getMessage() : "");
			DPPErrorHandler.processError(msg, true);
			return false;
		} catch (InterruptedException e) {
			DPPErrorHandler.processError(e.toString(), true);
			return false;
		}
		return true;
	}

	class ProgressRun implements IRunnableWithProgress {
		DPPFile dppFile;
		IProject iProject;

		public ProgressRun(DPPFile file) {
			this.dppFile = file;
			IPath path = new Path(dppFile.getFile().getAbsolutePath());
			IFile[] iFiles = ResourcesPlugin.getWorkspace().getRoot()
					.findFilesForLocation(path);
			if (iFiles.length != 0) {
				iProject = iFiles[0].getProject();
			}
		}

		public void run(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException {
			DPPUtil.generateDeploymentPackage(dppFile, monitor, this.iProject,
					DPPUtil.TYPE_EXPORT_DPP);
		}
	}
}

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
import java.util.Vector;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.BuildInfo;
import org.tigris.mtoolkit.util.BundleInfo;
import org.tigris.mtoolkit.util.CertificateInfo;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.InconsistentDataException;
import org.tigris.mtoolkit.util.PackageHeaders;
import org.tigris.mtoolkit.util.ResourceInfo;

/**
 * Generates new deployment package file object.
 */
public class NewDPPWizard extends Wizard implements INewWizard {

	private static final String ERROR_EDITOR_COULD_NOT_BE_CREATED = "NewDPPWizard.error_editor_could_not_be_created";

	/** The name of the page of the wizard */
	private String WIZARD_NEW_DPP_PAGE_NAME = ResourceManager.getString("NewDPPWizard.page_name");

	private boolean isFinishPerformed = false;
	/** Holds selected container or parent container */
	private IContainer parentFol;
	/** Holds selected project */
	private IProject project;
	/** Holds selected project path */
	private String prjRootPath = "";
	private DPPFile dppFile;

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
		isFinishPerformed = true;
		boolean res = performFinishDelegate();
		if (res) {
			NewDPPPage page = (NewDPPPage) getPage(WIZARD_NEW_DPP_PAGE_NAME);
			page.performFinish();
			IPath path = new Path(dppFile.getFile().getAbsolutePath());
			IFile[] iFiles = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(path);
			if (iFiles.length != 0) {
				IFile r = iFiles[0];
				if (r.isAccessible()) {
					FileEditorInput fei = new FileEditorInput(r);
					String id = "org.tigris.mtoolkit.dpeditor.editor.DPPEditor";
					try {
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(fei, id, true);
					} catch (PartInitException e) {
						DPPErrorHandler.processError(e, ResourceManager.getString(ERROR_EDITOR_COULD_NOT_BE_CREATED), true);
					}
				}
			}
		}
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
	 * Initializes the container, project and project path depending of the
	 * given selection.
	 * 
	 * @param workbench
	 *            the current workbench
	 * @param selection
	 *            the current object selection
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		setWindowTitle(ResourceManager.getString("NewDPPWizard.window_title"));
		setNeedsProgressMonitor(true);
		Object firstElement = selection.getFirstElement();
		if (firstElement == null) {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			for (int i = 0; i < projects.length; i++) {
				if (projects[i].isOpen()) {
					firstElement = projects[i];
					break;
				}
			}
			if (firstElement == null) {
				firstElement = ResourcesPlugin.getWorkspace().getRoot();
			}
		}
		if (firstElement instanceof IFile) {
			IFile file = (IFile) firstElement;
			parentFol = file.getParent();
			project = file.getProject();
			prjRootPath = project.getLocation().toString();
		} else if (firstElement instanceof IFolder) {
			parentFol = (IFolder) firstElement;
			project = parentFol.getProject();
			prjRootPath = project.getLocation().toString();
		} else if (firstElement instanceof IProject) {
			parentFol = (IProject) firstElement;
			project = parentFol.getProject();
			prjRootPath = parentFol.getLocation().toString();
		} else {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			if (projects != null && projects.length >= 1) {
				project = projects[0];
			}
		}
	}

	/**
	 * Adds the page of this <code>NewDPPWizard</code> before the wizard opens.
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#addPages()
	 */
	public void addPages() {
		setDefaultPageImageDescriptor(DPPImageDescriptor.NEW_DPP_IMAGE_WIZARD);
		WizardPage page = new NewDPPPage(WIZARD_NEW_DPP_PAGE_NAME, ResourceManager.getString("NewDPPWizard.new_dpp_page_title"), ResourceManager.getString("NewDPPWizard.new_dpp_page_description"));
		addPage(page);
		NewDPPPage dppPage = (NewDPPPage) getPage(WIZARD_NEW_DPP_PAGE_NAME);
		dppPage.setParentFolder(parentFol);
		dppPage.setProject(project);
	}

	/**
	 * Disposes all the pages controls.
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#dispose()
	 */
	public void dispose() {
		super.dispose();
	}

	/**
	 * Performs any special finish processing for the ant wizard. Creates new
	 * deployment package file object for the chosen values.
	 * 
	 * @see org.eclipse.jface.wizard.IWizard#performFinish()
	 */
	public boolean performFinishDelegate() {
		NewDPPPage page = (NewDPPPage) getPage(WIZARD_NEW_DPP_PAGE_NAME);
		page.performFinish();
		String targetFolder = page.getTargetFolder();
		String fileName = page.getFileName();
		String symbolicName = page.getSymbolicName();
		String version = page.getVersion();
		String dppPropFileName = page.getDPPPropertyFile();
		IPath path = new Path(targetFolder);
		path = path.removeFirstSegments(1);
		String newFilePath = path.toOSString() + File.separator + fileName;
		prjRootPath = page.getPrjRootPath();
		project = page.getProject();
		File newDppFile = new File(prjRootPath, newFilePath);
		File parentFile = newDppFile.getParentFile();
		DPPFile dppProperty = null;
		if (dppPropFileName != null) {
			File dppPropFile = new File(dppPropFileName);
			try {
				dppProperty = new DPPFile(dppPropFile, prjRootPath);
			} catch (IOException e1) {
				e1.printStackTrace();
				dppProperty = null;
			}
		}
		if (!parentFile.exists()) {
			parentFile.mkdirs();
		}
		try {
			dppFile = new DPPFile(newDppFile, prjRootPath);
			PackageHeaders pkgHeaders = dppFile.getPackageHeaders();
			if (pkgHeaders == null) {
				pkgHeaders = new PackageHeaders();
			}
			pkgHeaders.setSymbolicName(symbolicName);
			pkgHeaders.setVersion(version);
			if (dppProperty != null) {
				PackageHeaders propPkgHeaders = dppProperty.getPackageHeaders();
				String contactAddress = propPkgHeaders.getContactAddress();
				pkgHeaders.setContactAddress(contactAddress);
				String copyRight = propPkgHeaders.getCopyRight();
				pkgHeaders.setCopyRight(copyRight);
				String description = propPkgHeaders.getDescription();
				pkgHeaders.setDescription(description);
				String docURL = propPkgHeaders.getDocURL();
				pkgHeaders.setDocURL(docURL);
				String fixPack = propPkgHeaders.getFixPack();
				pkgHeaders.setFixPack(fixPack);
				String license = propPkgHeaders.getLicense();
				pkgHeaders.setLicense(license);
				Vector otherHeaders = propPkgHeaders.getOtherHeaders();
				pkgHeaders.setOtherHeaders(otherHeaders);
				String vendor = propPkgHeaders.getVendor();
				pkgHeaders.setVendor(vendor);
			}
			dppFile.setPackageHeaders(pkgHeaders);
			if (dppProperty != null) {
				Vector infos = dppProperty.getBundleInfos();
				Vector copyInfos = (Vector) infos.clone();
				Vector dppBundles = dppFile.getBundleInfos();
				for (int i = 0; i < copyInfos.size(); i++) {
					dppBundles.addElement((BundleInfo) copyInfos.elementAt(i));
				}
				Vector resInfos = dppProperty.getResourceInfos();
				copyInfos = (Vector) resInfos.clone();
				Vector dppResInfos = dppFile.getResourceInfos();
				for (int i = 0; i < copyInfos.size(); i++) {
					dppResInfos.addElement((ResourceInfo) copyInfos.elementAt(i));
				}
				Vector certInfos = dppProperty.getCertificateInfos();
				copyInfos = (Vector) certInfos.clone();
				Vector dppCertInfos = dppFile.getCertificateInfos();
				for (int i = 0; i < copyInfos.size(); i++) {
					dppCertInfos.addElement((CertificateInfo) copyInfos.elementAt(i));
				}
			}
			BuildInfo buildInfo = dppFile.getBuildInfo();
			if (buildInfo == null) {
				buildInfo = new BuildInfo();
			}
			dppFile.setBuildInfo(buildInfo);
			dppFile.save();
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (IOException ioe) {
			DPPErrorHandler.processError(ioe.getCause(), ResourceManager.getString("NewDPPWizard.errorMessage"));
		} catch (InconsistentDataException e) {
			DPPErrorHandler.processError(e.getCause(), ResourceManager.getString("NewDPPWizard.errorMessage"));
		} catch (CoreException e) {
		} finally {
			try {
				parentFol.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (Exception e) {
				DPPErrorHandler.processError(e.getCause(), ResourceManager.getString("NewDPPWizard.errorMessage"));
			}
		}
		return true;
	}
}

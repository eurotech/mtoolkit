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
package org.tigris.mtoolkit.dpeditor.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.internal.ui.JavaPlugin;
//import org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.osgi.framework.Version;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.common.ReflectionUtils;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.dpeditor.util.ResourceManager;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.InconsistentDataException;

public class QuickBuildActionDelegate extends Action implements
		IWorkbenchWindowActionDelegate {

	private DPPFile dppFile;
	private String prjRootPath = "";
	private IContainer parent;

	public QuickBuildActionDelegate() {
		super(ResourceManager.getString("QuickBuild.Popup.quickbuild"));
	}

	/*
	 * (non-Javadoc) Method declared on IWorkbenchActionDelegate
	 */
	public void dispose() {
	}

	/*
	 * (non-Javadoc) Method declared on IWorkbenchActionDelegate
	 */
	public void init(IWorkbenchWindow window) {
	}

	public void run(IAction action) {
		run();
	}

	/**
	 * The <code>WindowActionDelegate</code> implementation of this
	 * <code>IActionDelegate</code> method does nothing - we will let simple
	 * rules in the XML config file react to selections.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		if (selection instanceof DPPFile) {
			dppFile = (DPPFile) selection;
			action.setEnabled(true);
		} else if (selection instanceof IFile) {
			IFile file = (IFile) selection;
			parent = file.getParent();
			prjRootPath = file.getProject().getLocation().toOSString();
			IPath path = file.getLocation();
			String fileExtension = path.getFileExtension();
			if (fileExtension != null && fileExtension.equals("dpp")) {
				try {
					dppFile = new DPPFile(path.toFile(), prjRootPath);
					action.setEnabled(false);
				} catch (IOException e) {
					action.setEnabled(true);
				}
			}
		} else if (selection instanceof TreeSelection) {
			TreeSelection treeSel = (TreeSelection) selection;
			Object obj = treeSel.getFirstElement();
			if (obj instanceof IFile) {
				IFile file = (IFile) obj;
				parent = file.getParent();
				prjRootPath = file.getProject().getLocation().toOSString();
				IPath path = file.getLocation();
				String fileExtension = path.getFileExtension();
				if (fileExtension != null && fileExtension.equals("dpp")) {
					try {
						dppFile = new DPPFile(path.toFile(), prjRootPath);
					} catch (IOException e) {
					}
				}
			} else if (obj instanceof File) {
				File file = (File) obj;
				prjRootPath = file.getParent();
				String fileExtension = file.getName();
				if (fileExtension != null && fileExtension.endsWith("dpp")) {
					try {
						dppFile = new DPPFile(file, prjRootPath);
					} catch (IOException e) {
					}
				}
			}
		}
	}

	public void run() {
		String className = PluginUtilities.compareVersion("org.eclipse.jdt.ui", PluginUtilities.VERSION_3_5_0) ? "org.eclipse.jdt.ui.refactoring.RefactoringSaveHelper" : "org.eclipse.jdt.internal.ui.refactoring.RefactoringSaveHelper";
		
		try {
			Object rsh = ReflectionUtils.newInstance(className, new Class[] { int.class }, new Object[] {new Integer(1)});
			Boolean result = (Boolean) ReflectionUtils.invokeMethod(rsh, "saveEditors", new Class[] {Shell.class}, new Object[] {JavaPlugin.getActiveWorkbenchShell()});
			if (result.booleanValue()) {
				dppFile.restoreFromFile();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		ProgressRun progressRun = new ProgressRun(dppFile);
		ProgressMonitorDialog progress = new ProgressMonitorDialog(Display.getCurrent().getActiveShell());
		try {
			progress.run(true, true, progressRun);
		} catch (InvocationTargetException e) {
			DPPErrorHandler.processError(e.getCause().toString(), true);
		} catch (InterruptedException e) {
			DPPErrorHandler.processError(e.toString(), true);
		}
	}

	class ProgressRun implements IRunnableWithProgress {
		DPPFile dppFile;

		public ProgressRun(DPPFile file) {
			this.dppFile = file;
		}

		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			DPPUtil.generateDeploymentPackage(dppFile, monitor, parent, DPPUtil.TYPE_QUICK_BUILD_DPP);
		}
	}
}
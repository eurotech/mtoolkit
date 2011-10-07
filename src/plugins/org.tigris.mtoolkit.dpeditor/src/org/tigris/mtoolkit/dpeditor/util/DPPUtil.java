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
package org.tigris.mtoolkit.dpeditor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.tigris.mtoolkit.common.IPluginExporter;
import org.tigris.mtoolkit.common.IPluginExporter;
import org.tigris.mtoolkit.common.PluginExporter;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.dpeditor.DPActivator;
import org.tigris.mtoolkit.dpeditor.editor.dialog.CertificatesPasswordsDialog;
import org.tigris.mtoolkit.dpeditor.editor.dialog.ChangeBundleJarNameDialog;
import org.tigris.mtoolkit.util.BuildInfo;
import org.tigris.mtoolkit.util.BundleInfo;
import org.tigris.mtoolkit.util.CertificateInfo;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;
import org.tigris.mtoolkit.util.DeploymentPackageGenerator;

public class DPPUtil {

	public static final int TYPE_QUICK_BUILD_DPP = 0;
	public static final int TYPE_EXPORT_DPP = 1;
	public static final int TYPE_EXPORT_ANT = 2;
	private final static String[] jobText = new String[] { "QuickBuild", "BuildExportWizard", "AntExportWizard" };
	public static String fileDialogLastSelection;

	static List errorTable = new ArrayList();

	private static Point displayLoc = null;
	private static CertificatesPasswordsDialog certDialog;
	private static ChangeBundleJarNameDialog changeBundleNameDialog;

	/**
	 * Performs specified by type export
	 * 
	 * @param dppFile
	 *            source file to export from
	 * @param monitor
	 *            progress monitor
	 * @param project
	 *            plug-in or other project (dpp file source project)
	 * @param type
	 *            type of export (quick build, export dp, export xml)
	 * @throws InvocationTargetException
	 */
	public static void generateDeploymentPackage(final DPPFile dppFile, IProgressMonitor monitor, final IContainer project, int type) throws InvocationTargetException {
		BuildInfo buildInfo = dppFile.getBuildInfo();
		String dppFileName = dppFile.getFile().getAbsolutePath();
		if (dppFileName.endsWith(".dpp")) {
			dppFileName = dppFileName.substring(0, dppFileName.lastIndexOf('.'));
		}
		buildInfo.setBuildLocation("");
		String value = buildInfo.getDpFileName();
		if (value == null || value.equals("")) {
			buildInfo.setDpFileName(dppFileName + ".dp");
		}
		value = buildInfo.getAntFileName();
		if (value == null || value.equals("")) {
			buildInfo.setAntFileName(dppFileName + "_build.xml");
		}
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		monitor.beginTask(ResourceManager.getString(jobText[type] + ".progress"), 4000);
		monitor.subTask(ResourceManager.getString("DPPEditor.CheckWorkspaceProjectsTask"));
		monitor.worked(400);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		final Vector infos = dppFile.getBundleInfos();
		Vector projectPaths = new Vector();
		Vector prjInfos = new Vector();
		for (int i = 0; i < infos.size(); i++) {
			if (monitor.isCanceled()) {
				return;
			}
			BundleInfo info = (BundleInfo) infos.elementAt(i);
			String path = info.getBundlePath();
			if (path.endsWith(".project")) {
				boolean isFromWorkspace = false;
				for (int j = 0; j < projects.length; j++) {
					if (monitor.isCanceled()) {
						return;
					}
					IProject prj = projects[j];
					String location = prj.getLocation().toOSString();
					if (path.startsWith(location)) {
						isFromWorkspace = true;
						projectPaths.addElement(path);
						prjInfos.addElement(info);
						break;
					}
				}
				if (!isFromWorkspace) {
					DPPErrorHandler.processError(ResourceManager.format("DPPEditor.ProjectError", new String[] { path }), true);
					return;
				}
			}
		}
		monitor.subTask(ResourceManager.getString("DPPEditor.CheckBundleTask"));
		monitor.worked(800);

		final Control shellControl = DPPErrorHandler.getShell();
		shellControl.getDisplay().syncExec(new Runnable() {
			public void run() {
				displayLoc = shellControl.getLocation();
			}
		});
		if (!DPActivator.getDefault().isAcceptAutomaticallyChanges()) {
			shellControl.getDisplay().syncExec(new Runnable() {
				public void run() {
					changeBundleNameDialog = new ChangeBundleJarNameDialog(DPPErrorHandler.getShell(), displayLoc, shellControl.getSize());
					changeBundleNameDialog.setDPPFile(dppFile);
					changeBundleNameDialog.open();
				}
			});
			if (changeBundleNameDialog.openResult == Window.OK) {
				Hashtable selectedJars = changeBundleNameDialog.getSelectedJars();
				Enumeration keys = selectedJars.keys();
				while (keys.hasMoreElements()) {
					String key = (String) keys.nextElement();
					String valuePair = (String) selectedJars.get(key);
					for (int i = 0; i < infos.size(); i++) {
						if (monitor.isCanceled()) {
							return;
						}
						BundleInfo info = (BundleInfo) infos.elementAt(i);
						String bundlePath = info.getBundlePath();
						if (bundlePath.toLowerCase().equals(key.toLowerCase())) {
							updateBundlePath(info, valuePair);
						}
					}
				}
			}
		} else {
			if (infos != null) {
				for (int i = 0; i < infos.size(); i++) {
					if (monitor.isCanceled()) {
						return;
					}
					BundleInfo info = (BundleInfo) infos.elementAt(i);
					String bundlePath = info.getBundlePath();
					File bundlePathFile = new File(bundlePath);
					File findLastJar = DPPUtilities.findLastJar(bundlePathFile);
					if (!bundlePath.toLowerCase().equals(findLastJar.toString().toLowerCase())) {
						updateBundlePath(info, findLastJar.toString());
					}
				}
			}
		}

		DPPUtil.updateBundlesData(dppFile);

		monitor.subTask(ResourceManager.getString("DPPEditor.BuildProjectsTask"));
		monitor.worked(1200);
		try {
			@SuppressWarnings("rawtypes")
			Hashtable prjJars = buildProjectsInWorkspace(projectPaths);
			for (int i = 0; i < prjInfos.size(); i++) {
				if (monitor.isCanceled()) {
					return;
				}
				BundleInfo info = (BundleInfo) prjInfos.elementAt(i);
				String path = info.getBundlePath();
				String prjJar = (String) prjJars.get(path);
				String name = info.getName();
				info.setBundlePath(prjJar);
				info.setName(name);
			}
		} catch (Throwable t) {
			throw new InvocationTargetException(t);
			
		}

		monitor.subTask(ResourceManager.getString("DPPEditor.CheckCertsPassTask"));
		monitor.worked(1600);

		final String prjRootPath = project.getLocation().toOSString();
		shellControl.getDisplay().syncExec(new Runnable() {
			public void run() {
				certDialog = new CertificatesPasswordsDialog(DPPErrorHandler.getShell(), displayLoc, shellControl.getSize());
				certDialog.setDPPFileLocation(dppFile, prjRootPath);
				certDialog.open();
			}
		});
		if (certDialog.openResult == Window.OK) {
			Vector dppCertsInfos = dppFile.getCertificateInfos();
			Vector certsInfos = certDialog.getCertificateInfos();
			for (int i = 0; i < certsInfos.size(); i++) {
				CertificateInfo info = (CertificateInfo) certsInfos.elementAt(i);
				for (int j = 0; j < dppCertsInfos.size(); j++) {
					if (monitor.isCanceled()) {
						return;
					}
					CertificateInfo cert = (CertificateInfo) dppCertsInfos.elementAt(j);
					if (cert.getAlias().equals(info.getAlias())) {
						cert.setStorepass(info.getStorepass());
						cert.setKeypass(info.getKeypass());
					}
				}
			}
		} else if (certDialog.openResult == Window.CANCEL) {
			dppFile.getCertificateInfos().clear();
		}

		try {
			monitor.subTask(ResourceManager.getString(jobText[type] + ".CreateTaskName"));
			monitor.worked(2000);
			if (dppFile != null) {
				DeploymentPackageGenerator dpGenerator = new DeploymentPackageGenerator();
				dpGenerator.setMonitor(monitor);
				switch (type) {
				case TYPE_QUICK_BUILD_DPP:
				case TYPE_EXPORT_DPP: {
					dpGenerator.generateDeploymentPackage(dppFile, prjRootPath);
					String error = dpGenerator.getError();
					if (error.equals("") && !monitor.isCanceled()) {
						monitor.subTask(ResourceManager.getString(jobText[type] + ".SignTaskName"));
						dpGenerator.signDP(dppFile);
					}
					break;
				}
				case TYPE_EXPORT_ANT: {
					dpGenerator.generateAntFile(dppFile, prjRootPath, true);
					break;
				}
				}
				String error = dpGenerator.getError();
				String psOutput = dpGenerator.getOutput();
				if (!error.equals("")) {
					DPPErrorHandler.processError(error + "\n" + psOutput, true);
				} else {
					if (!dpGenerator.getErrorStream().equals("")) {
						DPPErrorHandler.processError(psOutput, true);
					} else {
						if (!dpGenerator.getOutputStream().equals("")) {
							DPPErrorHandler.processWarning(psOutput, true);
						}
					}
				}
				if (project != null) {
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				}
			}
			monitor.done();
		} catch (Throwable ex) {
			ex.printStackTrace();
			throw new InvocationTargetException(ex);
		}
	}
	
	public static boolean isValidExportDestination(File exportDest) {
		boolean isValidFile = false;

		try {
			String path = exportDest.getAbsolutePath();

			if (path.equals(exportDest.getCanonicalPath()) && PluginUtilities.isValidPath(path)) {
				IPath filePath = new Path(path);
				String fileName = filePath.lastSegment();

				if (!fileName.startsWith(".") && fileName.endsWith(".dp")) {
					isValidFile = true;
				}		
			}
		} catch (IOException ex) {
			isValidFile = false;
		}

		return isValidFile;
	}

	public static void updateBundlePath(BundleInfo info, String path) {
		// for midlets and R3 bundles this headers could be missing in manifest
		String symbName = info.getBundleSymbolicName();
		String ver = info.getBundleVersion();
		// sets new bundle path (new jar version) and update symbolic name and
		// version
		info.setBundlePath(path);
		if (info.getBundleSymbolicName() == null || info.getBundleSymbolicName().equals("")) {
			info.setBundleSymbolicName(symbName);
		}
		if (info.getBundleVersion() == null || info.getBundleVersion().equals("")) {
			info.setBundleVersion(ver);
		}
	}

	// read data (symbolic name and bundle version) from manifests for bundles
	// in specified dppFile and updates it dppFile
	public static void updateBundlesData(DPPFile file) {
		Vector infos = file.getBundleInfos();
		if (infos != null) {
			for (int i = 0; i < infos.size(); i++) {
				BundleInfo info = (BundleInfo) infos.elementAt(i);
				updateBundleData(info, file.getProjectLocation());
			}
		}
	}

	public static void updateBundleData(BundleInfo info, String projectLocation) {
		String path = info.getBundlePath();

		if (path.startsWith("<.>" + File.separator)) {
			path = projectLocation + path.substring(3);
		}
		if (!(new File(path)).exists())
			return;
		Manifest mf = null;
		if (path.endsWith(".jar")) {
			try {
				JarFile jf;
				jf = new JarFile(path);
				mf = jf.getManifest();
				jf.close();
			} catch (IOException e) {
				return;
			}
		} else {
			path = path.substring(0, path.length() - 8) + "META-INF/MANIFEST.MF";
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(path);
				mf = new Manifest(fis);
			} catch (FileNotFoundException e) {
				return;
			} catch (IOException e) {
				return;
			} finally {
				try {
					if (fis != null) {
						fis.close();
					}
				} catch (IOException e) {
				}
			}
		}
		if (mf == null)
			return;
		String value = mf.getMainAttributes().getValue("Bundle-SymbolicName");
		if (value != null) {
			info.setBundleSymbolicName(value);
		}
		String bunVersion = mf.getMainAttributes().getValue("Bundle-Version");
		if (bunVersion != null) {
			info.setBundleVersion(bunVersion);
		}
	}

	// wait flag
	private static boolean running = false;;

	public static Hashtable buildProjectsInWorkspace(Vector prjInfos) throws Throwable {
		Hashtable result = new Hashtable();
		Workspace workspace = (Workspace) ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		Vector pluginBundlePrjs = new Vector();
		IPluginModelBase base[] = PluginRegistry.getWorkspaceModels();
		for (int i = 0; i < base.length; i++) {
			IPluginModelBase pluginModelBase = base[i];
			BundleDescription bundleDescr = pluginModelBase.getBundleDescription();
		}
		for (int j = 0; j < prjInfos.size(); j++) {
			String prjInfo = (String) prjInfos.elementAt(j);
			for (int i = 0; i < projects.length; i++) {
				IProject project = projects[i];
				String prjLocation = project.getLocation().toOSString() + File.separator + ".project";
				if (prjInfo.equals(prjLocation)) {
					try {
						if (!project.isOpen()) {						
							throw new Exception("Can not export closed project \"" + project.getName() + "\" to .jar file!");
						}
						boolean isPlugin = isPluginProject(project);
						if (isPlugin) {
							IModel model = PluginRegistry.findModel(project);
							if (model != null && isValidModel(model) && hasBuildProperties((IPluginModelBase) model)) {
								try {
									try {
										project.refreshLocal(IResource.DEPTH_INFINITE, null);
									} catch (Exception e) {
										throw e;
									}
									project.build(IncrementalProjectBuilder.FULL_BUILD, null);
								} catch (Exception e) {
									throw e;
								}

								IPluginExporter exporter = PluginExporter.getInstance();

								if (exporter == null) {
									throw new Exception("Unable to find suitable exporter for your version of Eclipse");
								}

								FeatureExportInfo info = new FeatureExportInfo();
								info.toDirectory = true;
								info.useJarFormat = true;
								info.exportSource = false;
								File file = root.getLocation().toFile();
								if (!file.exists()) {
									file.mkdirs();
								}
								info.destinationDirectory = file.toString();
								info.zipFileName = null;
								IPluginModelBase findModel = PluginRegistry.findModel(project);

								info.items = new IModel[] { findModel };
								info.signingInfo = null;
								info.qualifier = null;
								String version = "1.0.0";
								if (findModel != null) {
									version = findModel.getBundleDescription().getVersion().toString();
								}

								if (version.endsWith("qualifier")) {
									info.qualifier = exporter.getQualifier();
									version = version.replaceAll("qualifier", info.qualifier);
								}
								String jarFile = null;
								if (findModel != null)
									jarFile = file.toString() + File.separator + "plugins" + File.separator + findModel.getBundleDescription().getSymbolicName() + "_" + version + ".jar";

								result.put(prjInfo, jarFile);

								IStatus exportResult = exporter.syncExportPlugins(info, new NullProgressMonitor());

								if (!exportResult.isOK()) {
									if (exportResult.matches(IStatus.ERROR)) {
										if (exportResult.getException() != null) {
											throw exportResult.getException();
										}
										throw new Exception("Error while exporting project " + project.getName());
									}
									if (exportResult.matches(IStatus.CANCEL)) {
										throw new Exception("Exporting project " + project.getName() + " was canceled.");
									}
								}
							}
						} else {
							throw new Exception("The project " + project.getName() + " is not Plug-in Project!");
						}
					} catch (CoreException e) {
						e.printStackTrace();
						throw e;
					}
				}
			}
		}
		return result;
	}

	public static boolean isPluginProject(IProject selProject) {
		IPath prjPath = selProject.getProject().getLocation().append(".project");
		String prjLocation = prjPath.makeAbsolute().toOSString();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = root.getProjects();
		boolean isFromWorkspace = false;
		boolean isOpen = true;
		for (int j = 0; j < projects.length; j++) {
			IProject prj = projects[j];
			String location = prj.getLocation().toOSString();
			if (prjLocation.startsWith(location + File.separator)) {
				selProject = prj;
				isOpen = selProject.isOpen();
				isFromWorkspace = true;
				break;
			}
		}
		if (!isFromWorkspace) {
			return false;
		}
		if (selProject != null) {
			IProjectDescription descr = null;
			try {
				if (!isOpen) {
					descr = ResourcesPlugin.getWorkspace().loadProjectDescription(selProject.getProject().getLocation().append(".project"));
				} else {
					descr = selProject.getDescription();
				}
			} catch (CoreException e) {
				DPPErrorHandler.processError(e);
				return false;
			}

			boolean hasPluginNature = false;
			boolean hasJavaNature = false;
			String[] natureIds = descr.getNatureIds();
			for (int k = 0; k < natureIds.length; k++) {
				String string = natureIds[k];
				if ("org.eclipse.pde.PluginNature".equals(string)) {
					hasPluginNature = true;
				}
				if ("org.eclipse.jdt.core.javanature".equals(string)) {
					hasJavaNature = true;
				}
			}
			if (!hasJavaNature || !hasPluginNature) {
				return false;
			}
		}
		return true;
	}

	protected static boolean isValidModel(IModel model) {
		return model != null && model instanceof IPluginModelBase;
	}

	private static boolean hasBuildProperties(IPluginModelBase model) {
		File file = new File(model.getInstallLocation(), "build.properties");
		return file.exists();
	}

	/**
	 * Shows the error dialog with the given parent, title, message, reason.
	 * 
	 * @param parent
	 *            the parent shell of the error dialog
	 * @param title
	 *            the title of the error dialog
	 * @param message
	 *            the message of the error dialog
	 * @param reason
	 *            the reason to show this error dialog
	 */
	public static void showErrorDialog(Shell parent, String title, String message, String reason) {
		showDialog(parent, title, message, reason, null, null, IStatus.ERROR);
	}

	/**
	 * Shows the error dialog with the given parent, title, message, reason and
	 * exception.
	 * 
	 * @param parent
	 *            the parent shell of the error dialog
	 * @param title
	 *            the title of the error dialog
	 * @param message
	 *            the message of the error dialog
	 * @param reason
	 *            the reason to show this error dialog
	 * @param e
	 *            the exception
	 */
	public static void showErrorDialog(Shell parent, String title, String message, String reason, Throwable e) {
		showDialog(parent, title, message, reason, e, null, IStatus.ERROR);
	}

	/**
	 * Shows the error dialog with the given parent, title, message, reason and
	 * exception.
	 * 
	 * @param parent
	 *            the parent shell of the error dialog
	 * @param title
	 *            the title of the error dialog
	 * @param message
	 *            the message of the error dialog
	 * @param reason
	 *            the reason to show this error dialog
	 * @param e
	 *            the exception
	 * @param nested
	 *            the nested exception
	 */
	public static void showErrorDialog(Shell parent, String title, String message, String reason, Throwable e, Throwable nested) {
		showDialog(parent, title, message, reason, e, nested, IStatus.ERROR);
	}

	/**
	 * Shows the warning dialog with the given parent, title, message and
	 * reason.
	 * 
	 * @param parent
	 *            the parent shell of the warning dialog
	 * @param title
	 *            the title of the warning dialog
	 * @param message
	 *            the message of the warning dialog
	 * @param reason
	 *            the reason to show this warning dialog
	 */
	public static void showWarningDialog(Shell parent, String title, String message, String reason) {
		showDialog(parent, title, message, reason, null, null, IStatus.WARNING);
	}

	/**
	 * Shows the information dialog with the given parent, title, message and
	 * reason.
	 * 
	 * @param parent
	 *            the parent shell of the information dialog
	 * @param title
	 *            the title of the information dialog
	 * @param message
	 *            the message of the information dialog
	 * @param reason
	 *            the reason to show this information dialog
	 */
	public static void showInformationDialog(Shell parent, String title, String message, String reason) {
		showDialog(parent, title, message, reason, null, null, IStatus.INFO);
	}

	/**
	 * Shows the dialog with the given parent, title, message and reason.
	 * 
	 * @param parent
	 *            the parent shell of the dialog
	 * @param title
	 *            the title of the dialog
	 * @param message
	 *            the message of the dialog
	 * @param reason
	 *            the reason to show this dialog
	 */
	public static void showMessageDialog(Shell parent, String title, String message, String reason) {
		showDialog(parent, title, message, reason, null, null, IStatus.OK);
	}

	/**
	 * Shows the dialog with the given parent, title, message, reason, exception
	 * and code of the status.
	 * 
	 * @param parent
	 *            the parent shell of the dialog
	 * @param title
	 *            the title of the dialog
	 * @param message
	 *            the message of the dialog
	 * @param reason
	 *            the reason to be shown this dialog
	 * @param e
	 *            the exception
	 * @param nested
	 *            the nested exception
	 * @param code
	 *            the severity; one of <code>Status.OK</code>,
	 *            <code>Status.ERROR</code>, <code>Status.INFO</code>,
	 *            <code>Status.WARNING</code>, or <code>Status.CANCEL</code>
	 */
	private static void showDialog(Shell parent, String title, String message, String reason, Throwable e, Throwable nested, int code) {
		if (title == null)
			title = ""; //$NON-NLS-1$
		if (message == null)
			message = ""; //$NON-NLS-1$
		if (reason == null)
			reason = ResourceManager.getString("MessageDialog.no_details", "No Details."); //$NON-NLS-1$  //$NON-NLS-2$
		errorTable.clear();
		String ex = null;
		if (e != null) {
			ex = DPPUtilities.dumpToText(e);
			List sTokensList = DPPUtilities.getStringTokens(ex);
			Object[] sTokens = sTokensList.toArray();

			for (int i = 0; i < sTokens.length; i++) {
				errorTable.add(new Status(code, DPActivator.PLUGIN_ID, 1, (String) sTokens[i], e));
			}
		}
		if (nested != null) {
			ex = DPPUtilities.dumpToText(nested);
			List sTokensList = DPPUtilities.getStringTokens(ex);
			Object[] sTokens = sTokensList.toArray();

			for (int i = 0; i < sTokens.length; i++) {
				errorTable.add(new Status(code, DPActivator.PLUGIN_ID, 1, (String) sTokens[i], null));
			}
		}
		ErrorDialog.openError(parent, title, message, getStatus(reason, code));
	}

	public static IStatus getStatus(String reason, int code) {
		if (errorTable.size() == 0) {
			return new Status(code, DPActivator.PLUGIN_ID, 1, reason, null);
		}
		IStatus[] errors = new IStatus[errorTable.size()];
		errorTable.toArray(errors);
		return new MultiStatus(DPActivator.PLUGIN_ID, IStatus.OK, errors, reason, null);
	}

	public static String getFileDialogPath(String path) {
		if (path == null || path.equals("")) {
			path = fileDialogLastSelection;
		} else {
			File file = new File(path);
			if (file.getParentFile() != null && file.getParentFile().exists()) {
				path = file.getParentFile().getAbsolutePath();
			} else {
				path = fileDialogLastSelection;
			}
		}
		return path;
	}

	public static boolean isAlreadyInTheTable(String bundleName, TableItem currentItem) {
		Table table = currentItem.getParent();
		int size = table.getItems().length;
		if (size == 0)
			return false;
		for (int i = 0; i < size; i++) {
			if (currentItem == table.getItem(i))
				continue;
			if (bundleName.equals(table.getItem(i).getText(1))) {
				return true;
			}
		}
		return false;
	}
}

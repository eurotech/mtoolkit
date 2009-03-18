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
package org.tigris.mtoolkit.cdeditor.internal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.cdeditor.internal.text.StyleManager;
import org.tigris.mtoolkit.cdeditor.internal.widgets.UIResources;


/**
 * The activator class controls the plug-in life cycle
 */
public class CDEditorPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.tigris.mtoolkit.cdeditor"; //$NON-NLS-1$

	public static final boolean DEBUG = false;

	// The shared instance
	private static CDEditorPlugin plugin;

	private static BundleContext bundleContext;

	private ColorRegistry colorRegistry;

	/**
	 * The constructor
	 */
	public CDEditorPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
	 * )
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		bundleContext = context;
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
	 * )
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static CDEditorPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 * 
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	public static void error(String message, Exception e) {
		CDEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, message, e));
	}

	public static void log(Throwable e) {
		CDEditorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR, PLUGIN_ID, e.getMessage(), e));
	}

	public ColorRegistry getColorRegistry() {
		if (colorRegistry == null) {
			colorRegistry = createColorRegistry();
			initializeColors(colorRegistry);
		}
		return colorRegistry;
	}

	private ColorRegistry createColorRegistry() {
		// If we are in the UI Thread use that
		if (Display.getCurrent() != null) {
			return new ColorRegistry(Display.getCurrent());
		}

		if (PlatformUI.isWorkbenchRunning()) {
			return new ColorRegistry(PlatformUI.getWorkbench().getDisplay());
		}

		// Invalid thread access if it is not the UI Thread
		// and the workbench is not created.
		throw new SWTError(SWT.ERROR_THREAD_INVALID_ACCESS);
	}

	protected void initializeColors(ColorRegistry registry) {
		StyleManager.initializeColorRegistry(registry);
	}

	protected void initializeImageRegistry(ImageRegistry reg) {
		super.initializeImageRegistry(reg);
		UIResources.initializeImageRegistry(reg);
	}

	public static Status newStatus(int severity, String message) {
		return new Status(severity, PLUGIN_ID, message);
	}

	public static void debug(String message) {
		if (DEBUG)
			System.out.println("[CDEditor] ".concat(message));
	}

	public static final void newCoreException(int severity, String message, Throwable e) throws CoreException {
		throw new CoreException(new Status(severity, PLUGIN_ID, message, e));
	}
}

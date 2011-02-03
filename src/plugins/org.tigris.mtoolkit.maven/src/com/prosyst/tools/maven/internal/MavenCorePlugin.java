package com.prosyst.tools.maven.internal;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class MavenCorePlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.prosyst.tools.maven.core"; //$NON-NLS-1$

	// TODO: Move to Equinox Debug infrastructure
	public static final boolean DEBUG = Boolean.getBoolean("mtoolkit.maven.debug");
	private static final String DEBUG_TAG = "[Maven Core] ";

	// The shared instance
	private static MavenCorePlugin plugin;

	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	// it is private for now to limit unnecessary usage
	private static MavenCorePlugin getDefault() {
		return plugin;
	}

	public static IPath getMetadataLocation() {
		return getDefault().getStateLocation();
	}

	public static void error(String message, Throwable t) {
		log(newStatus(IStatus.ERROR, message, t));
	}

	public static void warning(String message, Throwable t) {
		log(newStatus(IStatus.WARNING, message, t));
	}

	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	public static void throwException(int severity, String message, Throwable t) throws CoreException {
		throw newException(severity, message, t);
	}

	public static CoreException newException(int severity, String message, Throwable t) {
		return new CoreException(newStatus(severity, message, t));
	}

	public static CoreException newException(IStatus status) {
		return new CoreException(status);
	}

	public static void debug(String message) {
		debug(message, null);
	}

	public static void debug(String message, Throwable t) {
		if (!DEBUG)
			return;
		System.out.println(DEBUG_TAG.concat(message));
		if (t != null)
			// TODO: Add line prefix while dumping the stack trace
			t.printStackTrace(System.out);
	}

	public static IStatus newStatus(int severity, String message, Throwable t) {
		return new Status(severity, PLUGIN_ID, message, t);
	}

	public static IStatus newStatus(int severity, int code, String message, Throwable t) {
		return new Status(severity, PLUGIN_ID, code, message, t);
	}

	public static IStatus newStatus(String message, Throwable t, List<IStatus> children) {
		IStatus[] statuses = (IStatus[]) children.toArray(new IStatus[children.size()]);
		return new MultiStatus(PLUGIN_ID, 0, statuses, message, t);
	}

	public static CoreException newException(int severity, int code, String message, Throwable t) {
		return newException(newStatus(severity, code, message, t));
	}

}

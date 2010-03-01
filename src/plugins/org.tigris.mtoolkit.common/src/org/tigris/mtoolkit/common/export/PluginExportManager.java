package org.tigris.mtoolkit.common.export;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.tigris.mtoolkit.common.IPluginExporter;
import org.tigris.mtoolkit.common.PluginExporter;
import org.tigris.mtoolkit.common.UtilitiesPlugin;

public class PluginExportManager {

	private static final String EXPORTED_FILENAME_EXTENSION = ".jar";
	private static final String EXPORT_FILENAME_SEPARATOR = "_";
	
	private IPluginExporter exporter;
	private Map/*<IPluginModelBase,String>*/ bundlesToExport = new HashMap();
	private static final Object NOT_EXPORTED = new Object();
	
	private PluginExportManager(IPluginExporter exporter) {
		this.exporter = exporter;
	}
	
	public void addBundle(IPluginModelBase model) {
		if (model.getUnderlyingResource() == null) {
			// target platform bundle -> directly set the location
			bundlesToExport.put(model, model.getInstallLocation());
		}
		bundlesToExport.put(model, NOT_EXPORTED);
	}
	
	public void addBundle(String symbolicName, String version) throws CoreException {
		addBundle(findModel(symbolicName, version));
	}
	
	public static IPluginModelBase findModel(String symbolicName, String version) throws CoreException {
		ModelEntry entry = PluginRegistry.findEntry(symbolicName);
		if (entry == null)
			throw UtilitiesPlugin.newException(IStatus.ERROR, "Cannot find plugin " + symbolicName + " in current target platform", null);
		if (version == null)
			return entry.getModel();
		IPluginModelBase[] activeModels = entry.getActiveModels();
		for (int i = 0; i < activeModels.length; i++) {
			IPluginModelBase model = activeModels[i];
			if (version.equals(model.getPluginBase().getVersion()))
				return model;
		}
		throw UtilitiesPlugin.newException(IStatus.ERROR, "Cannot find plugin " + symbolicName + " (" + version + ") in current target platform", null);
	}
	
	public String getLocation(IPluginModelBase model) {
		if (bundlesToExport.get(model) == NOT_EXPORTED)
			return null;
		return (String) bundlesToExport.get(model);
	}
	
	public String getLocation(String symbolicName, String version) throws CoreException {
		return getLocation(findModel(symbolicName, version));
	}
	
	public IStatus export(String location, IProgressMonitor monitor) {
		FeatureExportInfo exportInfo = new FeatureExportInfo();
		exportInfo.toDirectory = true;
		exportInfo.useJarFormat = true;
		exportInfo.destinationDirectory = location;
		exportInfo.qualifier = exporter.getQualifier();
		List bundlesForExport = getBundlesForExport();
		exportInfo.items = bundlesForExport.toArray();

		IStatus result = exporter.syncExportPlugins(exportInfo, monitor);
		if (result.isOK()) {
			for (Iterator it = bundlesForExport.iterator(); it.hasNext();) {
				IPluginModelBase model = (IPluginModelBase) it.next();
				setBundlesExportLocation(model, determineLocationAfterExport(model, exportInfo));
			}
		}
		return result;
	}
	
	private String determineLocationAfterExport(IPluginModelBase model, FeatureExportInfo info) {
		IPath path = new Path(info.destinationDirectory).append("plugins");
		return path.append(determineExportedFilename(model, info)).toString();
	}
	
	private String determineExportedFilename(IPluginModelBase model, FeatureExportInfo info) {
		String qualifier = info.qualifier;
		String symbolicName = model.getPluginBase().getId();
		String version = model.getPluginBase().getVersion();
		if (version == null)
			// PDE uses 0.0.0 as default version for exported plugins in the filename
			version = "0.0.0";
		version = replaceQualifier(version, info.qualifier);
		return symbolicName + EXPORT_FILENAME_SEPARATOR + version + EXPORTED_FILENAME_EXTENSION;
	}
	
	private static String replaceQualifier(String version, String qualifier) {
		int qualifierIdx;
		if ((qualifierIdx = version.indexOf("qualifier")) != -1)
			version = version.substring(0, qualifierIdx) + qualifier + version.substring(qualifierIdx + "qualifier".length(), version.length());
		return version;
	}
	
	private void setBundlesExportLocation(IPluginModelBase model, String location) {
		if (bundlesToExport.get(model) == null)
			throw new IllegalStateException("Internal error: cannot save export location for bundle, which is not in export list");
		bundlesToExport.put(model, location);
	}
	
	private List getBundlesForExport() {
		List bundles = new ArrayList();
		for (Iterator it = bundlesToExport.entrySet().iterator(); it.hasNext();) {
			Entry entry = (Entry) it.next();
			if (entry.getValue() == NOT_EXPORTED)
				bundles.add(entry.getKey());
		}
		return bundles;
	}
	
	public void dispose() {
		// do nothing currently
	}
	
	public static PluginExportManager create(IPluginExporter exporter) {
		return new PluginExportManager(exporter);
	}
	
	public static PluginExportManager create() {
		return create(PluginExporter.getInstance());
	}
}

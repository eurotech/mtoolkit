/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.common.export;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.exports.FeatureExportInfo;
import org.osgi.framework.Version;
import org.tigris.mtoolkit.common.PDEUtils;

/**
 * @since 5.0
 */
public final class PluginExportManager {
  private static final String                 EXPORTED_FILENAME_EXTENSION = ".jar";
  private static final String                 EXPORT_FILENAME_SEPARATOR   = "_";
  private static final String                 EMPTY_VERSION               = Version.emptyVersion.toString();
  private static final Object                 NOT_EXPORTED                = new Object();

  private final IPluginExporter               exporter;
  private final Map<IPluginModelBase, Object> bundlesToExport             = new HashMap<IPluginModelBase, Object>();

  private PluginExportManager(IPluginExporter exporter) {
    this.exporter = exporter;
  }

  public void addBundle(IPluginModelBase model) {
    if (model.getUnderlyingResource() == null) {
      // target platform bundle -> directly set the location
      bundlesToExport.put(model, model.getInstallLocation());
    } else {
      bundlesToExport.put(model, NOT_EXPORTED);
    }
  }

  public String getLocation(IPluginModelBase model) {
    if (bundlesToExport.get(model) == NOT_EXPORTED) {
      return null;
    }
    return (String) bundlesToExport.get(model);
  }

  public IStatus export(String location, IProgressMonitor monitor) {
    List bundlesForExport = getBundlesForExport();
    if (bundlesForExport.size() == 0) {
      return Status.OK_STATUS;
    }
    FeatureExportInfo exportInfo = new FeatureExportInfo();
    exportInfo.toDirectory = true;
    exportInfo.useJarFormat = true;
    exportInfo.destinationDirectory = location;
    exportInfo.qualifier = exporter.getQualifier();
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

  /**
   * Removes packed workspace bundles.
   */
  public void deleteTempResources() {
    for (Iterator it = bundlesToExport.entrySet().iterator(); it.hasNext();) {
      Entry entry = (Entry) it.next();
      IPluginModelBase bundle = (IPluginModelBase) entry.getKey();
      if (PDEUtils.isTargetPlatformBundle(bundle)) {
        continue;
      }
      String expLocation = (String) entry.getValue();
      if (!NOT_EXPORTED.equals(expLocation)) {
        new File(expLocation).delete();
      }
    }
  }

  public void dispose(boolean freeTempResources) {
    if (freeTempResources) {
      deleteTempResources();
    }
  }

  public static PluginExportManager create() {
    if (PluginExporter_35.isCompatible()) {
      return new PluginExportManager(new PluginExporter_35());
    }
    throw new IllegalStateException("No plugin export manager available for this eclipse platform");
  }

  private String determineLocationAfterExport(IPluginModelBase model, FeatureExportInfo info) {
    IPath path = new Path(info.destinationDirectory).append("plugins");
    return path.append(determineExportedFilename(model, info)).toString();
  }

  private String determineExportedFilename(IPluginModelBase model, FeatureExportInfo info) {
    //String qualifier = info.qualifier;
    String symbolicName = model.getPluginBase().getId();
    String version = model.getPluginBase().getVersion();
    if (version == null) {
      // PDE uses 0.0.0 as default version for exported plugins in the filename
      version = EMPTY_VERSION;
    }
    version = replaceQualifier(version, info.qualifier);
    return symbolicName + EXPORT_FILENAME_SEPARATOR + version + EXPORTED_FILENAME_EXTENSION;
  }

  private static String replaceQualifier(String version, String qualifier) {
    int qualifierIdx;
    if ((qualifierIdx = version.indexOf("qualifier")) != -1) {
      version = version.substring(0, qualifierIdx) + qualifier
          + version.substring(qualifierIdx + "qualifier".length(), version.length());
    }
    return version;
  }

  private void setBundlesExportLocation(IPluginModelBase model, String location) {
    if (bundlesToExport.get(model) == null) {
      throw new IllegalStateException(
          "Internal error: cannot save export location for bundle, which is not in export list");
    }
    bundlesToExport.put(model, location);
  }

  private List getBundlesForExport() {
    List bundles = new ArrayList();
    for (Iterator it = bundlesToExport.entrySet().iterator(); it.hasNext();) {
      Entry entry = (Entry) it.next();
      if (entry.getValue() == NOT_EXPORTED) {
        bundles.add(entry.getKey());
      }
    }
    return bundles;
  }
}

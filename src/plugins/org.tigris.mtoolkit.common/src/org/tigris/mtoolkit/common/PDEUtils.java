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
package org.tigris.mtoolkit.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ModelEntry;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.osgi.framework.Version;

public final class PDEUtils {
  private PDEUtils() {
  }

  /**
   * Finds bundle with specified symbolic name and version. Workspace and target
   * platform bundles are searched, but bundles from workspace are preferably
   * returned over TP bundles.If version is present but exact match is not found
   * bundle with the same symbolic name with the highest version is returned if
   * any. Only active and valid plug-ins are returned.
   *
   * @param symbolicName
   * @param version
   * @return the bundle or null
   */
  public static IPluginModelBase findBundle(String symbolicName, String version) {
    ModelEntry entry = PluginRegistry.findEntry(symbolicName);
    if (entry == null) {
      return null;
    }
    IPluginModelBase plugin = findBundle(entry.getWorkspaceModels(), symbolicName, version, true, IMatchRules.PERFECT);
    if (plugin != null) {
      return plugin;
    }
    plugin = findBundle(entry.getExternalModels(), symbolicName, version, true, IMatchRules.PERFECT);
    if (plugin != null) {
      return plugin;
    }
    return getMax(entry.getActiveModels());
  }

  /**
   * Finds bundle with specified symbolic name and version from the target
   * platform according to match rule.Workspace and target platform bundles are
   * searched, but bundles from workspace are preferably returned over TP
   * bundles.Only valid plug-ins are returned.
   *
   * @param symbolicName
   * @param version
   * @param enabledOnly
   * @param match
   * @return the bundle or null
   */
  public static IPluginModelBase findBundle(String symbolicName, String version, boolean enabledOnly, int match) { // NO_UCD
    ModelEntry entry = PluginRegistry.findEntry(symbolicName);
    if (entry == null) {
      return null;
    }
    return findBundle(entry.getActiveModels(), symbolicName, version, enabledOnly, match);
  }

  /**
   * Finds bundle by its description
   *
   * @param desc
   * @return the bundle or null
   */
  public static IPluginModelBase findBundle(BundleDescription desc) {
    return PluginRegistry.findModel(desc);
  }

  /**
   * Finds bundle with specified symbolic name and version from the target
   * platform according to match rule. Only valid plug-ins are returned.
   *
   * @param symbolicName
   * @param version
   * @param enabledOnly
   * @param match
   * @return the bundle or null
   */
  public static IPluginModelBase findTargetPlatformBundle(String symbolicName, String version, boolean enabledOnly,
      int match) {
    ModelEntry entry = PluginRegistry.findEntry(symbolicName);
    if (entry == null) {
      return null;
    }
    return findBundle(entry.getExternalModels(), symbolicName, version, enabledOnly, match);
  }

  /**
   * Finds bundle with specified symbolic name and version from the target
   * platform.If version is present but exact match is not found bundle with the
   * same symbolic name with the highest version is returned if any. Only active
   * and valid plug-ins are returned.
   *
   * @param symbolicName
   * @param version
   * @return the bundle or null
   */
  public static IPluginModelBase findTargetPlatformBundle(String symbolicName, String version) {
    ModelEntry entry = PluginRegistry.findEntry(symbolicName);
    if (entry == null) {
      return null;
    }
    IPluginModelBase[] externalModels = entry.getExternalModels();
    IPluginModelBase plugin = findBundle(externalModels, symbolicName, version, true, IMatchRules.PERFECT);
    if (plugin == null) {
      plugin = getMax(externalModels);
    }
    return plugin;
  }

  /**
   * Finds bundle with specified symbolic name and version from the workspace
   * according to match rule.
   *
   * @param symbolicName
   * @param version
   * @param enabledOnly
   * @param match
   * @return the bundle or null
   */
  public static IPluginModelBase findWorkspaceBundle(String symbolicName, String version, boolean enabledOnly, int match) {
    ModelEntry entry = PluginRegistry.findEntry(symbolicName);
    if (entry == null) {
      return null;
    }
    return findBundle(entry.getWorkspaceModels(), symbolicName, version, enabledOnly, match);
  }

  /**
   * Finds bundle with specified symbolic name and version from the workspace.
   * If version is present but exact match is not found bundle with the same
   * symbolic name with the highest version is returned if any. Only active and
   * valid plug-ins are returned.
   *
   * @param symbolicName
   * @param version
   * @return the bundle or null
   */
  public static IPluginModelBase findWorkspaceBundle(String symbolicName, String version) {
    ModelEntry entry = PluginRegistry.findEntry(symbolicName);
    if (entry == null) {
      return null;
    }
    IPluginModelBase[] workspaceModels = entry.getWorkspaceModels();
    IPluginModelBase plugin = findBundle(workspaceModels, symbolicName, version, true, IMatchRules.PERFECT);
    if (plugin == null) {
      plugin = getMax(workspaceModels);
    }
    return plugin;
  }

  /**
   * Gets bundles from the target based on their enablement
   *
   * @param enabledOnly
   * @return bundles array
   */
  public static IPluginModelBase[] getTargetPlatfomBundles(boolean enabledOnly) {
    IPluginModelBase[] externalModels = PluginRegistry.getExternalModels();
    if (enabledOnly) {
      List models = new ArrayList();
      for (int i = 0; i < externalModels.length; i++) {
        if (!selectBundle(externalModels[i], enabledOnly)) {
          continue;
        }
        models.add(externalModels[i]);
      }
      return (IPluginModelBase[]) models.toArray(new IPluginModelBase[models.size()]);
    }
    return externalModels;
  }

  /**
   * Gets all enabled bundles from the workspace which are currently valid.
   *
   * @return bundles array
   */
  public static IPluginModelBase[] getWorkspaceBundles() {
    IPluginModelBase[] workspaceModels = PluginRegistry.getWorkspaceModels();
    List models = new ArrayList();
    for (int i = 0; i < workspaceModels.length; i++) {
      if (selectBundle(workspaceModels[i], true)) {
        models.add(workspaceModels[i]);
      }
    }
    return (IPluginModelBase[]) models.toArray(new IPluginModelBase[models.size()]);
  }

  /**
   * Gets all active bundles in current context
   *
   * @return bundles array
   */
  public static IPluginModelBase[] getActiveBundles() {
    List allModels = new ArrayList();
    allModels.addAll(Arrays.asList(PDEUtils.getWorkspaceBundles()));
    allModels.addAll(Arrays.asList(PDEUtils.getTargetPlatfomBundles(true)));
    return (IPluginModelBase[]) allModels.toArray(new IPluginModelBase[allModels.size()]);
  }

  /**
   * Checks if a bundle is from target
   *
   * @return bundle currently belongs to target
   */
  public static boolean isTargetPlatformBundle(IPluginModelBase bundle) {
    return bundle.getUnderlyingResource() == null;
  }

  /**
   * Returns true if the given version number is an empty version as defined by
   * {@link Version}. Used in cases where it would be inappropriate to parse the
   * actual version number.
   *
   * @param version
   *          version string to check
   * @return true if empty version
   */
  public static boolean isEmptyVersion(String version) {
    if (version == null) {
      return true;
    }
    version = version.trim();
    return version.length() == 0 || version.equals(Version.emptyVersion.toString());
  }

  /**
   * Returns bundle version number defined by {@link Version} or empty version
   * if none.
   *
   * @param version
   *          version string to check
   * @return input version number or empty version, never null.
   */
  public static Version getBundleVersion(String version) {
    if (isEmptyVersion(version)) {
      return Version.emptyVersion;
    }
    return new Version(version);
  }

  private static IPluginModelBase findBundle(IPluginModelBase[] models, String symbolicName, String version,
      boolean active, int match) {
    List<IPluginModelBase> results = new ArrayList<IPluginModelBase>();
    for (int i = 0; i < models.length; i++) {
      if (selectBundle(models[i], active)) {
        if (isMatch(models[i].getPluginBase(), symbolicName, version, match)) {
          results.add(models[i]);
        }
      }
    }
    return getMax(results.toArray(new IPluginModelBase[results.size()]));
  }

  private static IPluginModelBase getMax(IPluginModelBase[] models) {
    if (models.length == 0) {
      return null;
    }
    if (models.length == 1) {
      if (selectBundle(models[0], true)) {
        return models[0];
      }
      return null;
    }
    IPluginModelBase max = null;
    Version maxV = null;
    for (int i = 0; i < models.length; i++) {
      IPluginModelBase model = models[i];
      if (selectBundle(model, true)) {
        String versionStr = model.getPluginBase().getVersion();
        Version version = validateVersion(versionStr).isOK() ? new Version(versionStr) : Version.emptyVersion;
        if (max == null) {
          max = model;
          maxV = version;
        } else {
          if (isGreaterOrEqualTo(version, maxV)) {
            max = model;
            maxV = version;
          }
        }
      }
    }
    return max;
  }

  private static boolean isMatch(IPluginBase base, String id, String version, int match) {
    // if version is null, then match any version with same ID
    if (base == null) {
      return false; // guard against invalid plug-ins
    }
    if (base.getId() == null) {
      return false; // guard against invalid plug-ins
    }
    if (version == null) {
      return base.getId().equals(id);
    }
    return compare(base.getId(), base.getVersion(), id, version, match);
  }

  private static IStatus validateVersion(String versionString) {
    try {
      if (versionString != null) {
        new Version(versionString.trim());
      }
    } catch (IllegalArgumentException e) {
      UtilitiesPlugin.newException(IStatus.ERROR, "The specified version does not have the correct format", e);
    }
    return Status.OK_STATUS;
  }

  private static boolean compare(String id1, String version1, String id2, String version2, int match) {
    if (!(id1.equals(id2))) {
      return false;
    }
    try {
      Version v1 = Version.parseVersion(version1);
      Version v2 = Version.parseVersion(version2);

      switch (match) {
      case IMatchRules.NONE:
      case IMatchRules.COMPATIBLE:
        return isCompatibleWith(v1, v2);
      case IMatchRules.EQUIVALENT:
        return isEquivalentTo(v1, v2);
      case IMatchRules.PERFECT:
        return v1.equals(v2);
      case IMatchRules.GREATER_OR_EQUAL:
        return isGreaterOrEqualTo(v1, v2);
      }
    } catch (RuntimeException e) {
    }
    return version1.equals(version2);
  }

  private static boolean isCompatibleWith(Version v1, Version v2) {
    if (v1.getMajor() != v2.getMajor()) {
      return false;
    }
    if (v1.getMinor() > v2.getMinor()) {
      return true;
    }
    if (v1.getMinor() < v2.getMinor()) {
      return false;
    }
    if (v1.getMicro() > v2.getMicro()) {
      return true;
    }
    if (v1.getMicro() < v2.getMicro()) {
      return false;
    }
    return v1.getQualifier().compareTo(v2.getQualifier()) >= 0;
  }

  private static boolean isEquivalentTo(Version v1, Version v2) {
    if (v1.getMajor() != v2.getMajor() || v1.getMinor() != v2.getMinor()) {
      return false;
    }
    if (v1.getMicro() > v2.getMicro()) {
      return true;
    }
    if (v1.getMicro() < v2.getMicro()) {
      return false;
    }
    return v1.getQualifier().compareTo(v2.getQualifier()) >= 0;
  }

  private static boolean isGreaterOrEqualTo(Version v1, Version v2) {
    if (v1.getMajor() > v2.getMajor()) {
      return true;
    }
    if (v1.getMajor() == v2.getMajor()) {
      if (v1.getMinor() > v2.getMinor()) {
        return true;
      }
      if (v1.getMinor() == v2.getMinor()) {
        if (v1.getMicro() > v2.getMicro()) {
          return true;
        }
        if (v1.getMicro() == v2.getMicro()) {
          return v1.getQualifier().compareTo(v2.getQualifier()) >= 0;
        }
      }
    }
    return false;
  }

  private static boolean selectBundle(IPluginModelBase bundle, boolean enabledOnly) {
    if (enabledOnly && !bundle.isEnabled()) {
      return false;
    }
    return bundle.getBundleDescription() != null;
  }
}

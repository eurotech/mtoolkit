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
package org.tigris.mtoolkit.common.installation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.UtilitiesPlugin;

public final class InstallationHistory {
  private static final int                 HISTORY_SIZE        = 10;
  private static final String              STORAGE_FILE        = "installation_history.xml";
  private static final String              ROOT_TYPE           = "history";
  private static final String              PROCESSOR_TYPE      = "processor";
  private static final String              PROCESSOR_NAME_ATTR = "name";
  private static final String              TARGET_TYPE         = "target";
  private static final String              TARGET_UID_ATTR     = "uid";

  private static InstallationHistory       defaultInstance     = null;

  private Dictionary<String, List<String>> history             = null;

  private InstallationHistory() {
  }

  /**
   * Returns default instance.
   *
   * @return the default instance
   */
  public static synchronized InstallationHistory getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new InstallationHistory();
      defaultInstance.restoreHistory();
    }
    return defaultInstance;
  }

  /**
   * Promotes usage of specified target for passed processor.
   *
   * @param target
   *          the target to promote
   * @param processor
   *          the processor
   */
  public void promoteHistory(InstallationTarget target, InstallationItemProcessor processor) {
    if (history == null) {
      history = new Hashtable<String, List<String>>();
    }

    List<String> targets = history.get(processor.getGeneralTargetName());
    if (targets == null) {
      targets = new ArrayList<String>();
      targets.add(target.getUID());
      history.put(processor.getGeneralTargetName(), targets);
      return;
    }

    int pos = targets.indexOf(target.getUID());
    if (pos >= 0) {
      targets.add(0, targets.remove(pos));
    } else {
      targets.add(0, target.getUID());
    }

    purgeTargets(targets, processor);

    if (targets.size() > HISTORY_SIZE) {
      targets.subList(HISTORY_SIZE, targets.size()).clear();
    }
  }

  /**
   * Returns list with UIDs (String) that specify most recent targets used. If
   * there is no history for passed processor, empty list is returned.
   *
   * @param processor
   *          processor for which to return history
   * @return list with recent UIDs
   */
  public List<String> getHistoryUIDs(InstallationItemProcessor processor) {
    if (history == null) {
      return Collections.EMPTY_LIST;
    }
    List<String> targets = history.get(processor.getGeneralTargetName());
    if (targets == null) {
      return Collections.EMPTY_LIST;
    }
    return targets;
  }

  /**
   * Returns array with InstallationTarget elements that were most recently used
   * for given processor. If there is no history for the passed processor, empty
   * array is returned. This method intersects the current targets returned by
   * the processor with the targets that were previously promoted.
   *
   * @param processor
   *          processor for which to return history
   * @return array with recent targets
   */
  public InstallationTarget[] getHistory(InstallationItemProcessor processor) {
    int connectedIndex = 0;
    List<String> recent = getHistoryUIDs(processor);
    List<InstallationTarget> result = new ArrayList<InstallationTarget>();
    InstallationTarget[] currentTargets = processor.getInstallationTargets();
    Iterator<String> iterator = recent.iterator();
    while (iterator.hasNext()) {
      String uid = iterator.next();
      for (int i = 0; i < currentTargets.length; i++) {
        if (uid.equals(currentTargets[i].getUID())) {
          if (currentTargets[i].isConnected()) {
            result.add(connectedIndex, currentTargets[i]);
            connectedIndex++;
          } else {
            result.add(currentTargets[i]);
          }
        }
      }
    }
    for (int i = 0; i < currentTargets.length; i++) {
      if (currentTargets[i].isConnected() && !result.contains(currentTargets[i])) {
        result.add(connectedIndex, currentTargets[i]);
        connectedIndex++;
      }
    }
    return result.toArray(new InstallationTarget[result.size()]);
  }

  /**
   * Saves history. This allows later restoring the saved history.
   */
  public void saveHistory() {
    if (history == null) {
      return;
    }
    List<InstallationItemProcessor> processors = InstallationRegistry.getInstance().getProcessors();
    XMLMemento config = XMLMemento.createWriteRoot(ROOT_TYPE);
    Enumeration<String> en = history.keys();
    while (en.hasMoreElements()) {
      String processorName = en.nextElement();
      InstallationItemProcessor processor = null;
      for (int i = 0; i < processors.size(); i++) {
        InstallationItemProcessor candidate = processors.get(i);
        if (candidate.getGeneralTargetName().equals(processorName)) {
          processor = candidate;
          break;
        }
      }
      if (processor == null) {
        UtilitiesPlugin.error("Cannot find processor with name:" + processorName, null);
        continue;
      }

      IMemento processorItem = null;
      List<String> targets = history.get(processorName);
      Iterator<String> iterator = targets.iterator();
      InstallationTarget[] currentTargets = processor.getInstallationTargets();
      while (iterator.hasNext()) {
        String uid = iterator.next();
        for (int i = 0; i < currentTargets.length; i++) {
          if (uid.equals(currentTargets[i].getUID()) && !currentTargets[i].isTransient()) {
            if (processorItem == null) {
              processorItem = config.createChild(PROCESSOR_TYPE);
              processorItem.putString(PROCESSOR_NAME_ATTR, processorName);
            }
            IMemento targetItem = processorItem.createChild(TARGET_TYPE);
            targetItem.putString(TARGET_UID_ATTR, uid);
          }
        }
      }
    }
    OutputStreamWriter writer = null;
    try {
      File configFile = new File(UtilitiesPlugin.getDefault().getStateLocation().toFile(), STORAGE_FILE);
      writer = new OutputStreamWriter(new FileOutputStream(configFile), "utf-8");
      config.save(writer);
    } catch (IOException e) {
      UtilitiesPlugin.error("Failed to save installation history", e);
    } finally {
      FileUtils.close(writer);
    }
  }

  /**
   * Restores previously saved history.
   */
  public void restoreHistory() {
    XMLMemento config = null;
    InputStreamReader reader = null;
    try {
      File configFile = new File(UtilitiesPlugin.getDefault().getStateLocation().toFile(), STORAGE_FILE);
      reader = new InputStreamReader(new FileInputStream(configFile), "utf-8");
      config = XMLMemento.createReadRoot(reader);
    } catch (FileNotFoundException e) {
      // do nothing
    } catch (IOException e) {
      UtilitiesPlugin.error("Failed to load installation history", e);
    } catch (WorkbenchException e) {
      UtilitiesPlugin.error("Failed to load installation history", e);
    } finally {
      FileUtils.close(reader);
    }

    if (config == null) {
      return;
    }

    history = new Hashtable<String, List<String>>();
    IMemento[] processors = config.getChildren(PROCESSOR_TYPE);
    for (int i = 0; i < processors.length; i++) {
      String processorName = processors[i].getString(PROCESSOR_NAME_ATTR);
      if (processorName == null) {
        continue;
      }
      List<String> targetUids = new ArrayList<String>();
      IMemento[] targets = processors[i].getChildren(TARGET_TYPE);
      for (int k = 0; k < targets.length; k++) {
        String uid = targets[k].getString(TARGET_UID_ATTR);
        if (uid == null) {
          continue;
        }
        targetUids.add(uid);
      }
      history.put(processorName, targetUids);
    }
  }

  /**
   * Purges the passed list of target UIDs from targets, that are not valid
   * anymore for given processor.
   *
   * @param targets
   *          the list of target UIDs to purge
   * @param processor
   *          processor to obtain fresh list of available targets
   */
  private void purgeTargets(List<String> targetUIDs, InstallationItemProcessor processor) {
    InstallationTarget[] currentTargets = processor.getInstallationTargets();
    Iterator<String> iterator = targetUIDs.iterator();
    while (iterator.hasNext()) {
      String t = iterator.next();
      boolean found = false;
      for (int i = 0; i < currentTargets.length; i++) {
        if (currentTargets[i].getUID().equals(t)) {
          found = true;
          break;
        }
      }
      if (!found) {
        iterator.remove();
      }
    }
  }
}

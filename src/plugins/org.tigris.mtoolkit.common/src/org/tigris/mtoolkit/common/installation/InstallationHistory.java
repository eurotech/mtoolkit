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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ui.IMemento;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.tigris.mtoolkit.common.UtilitiesPlugin;

public final class InstallationHistory {
  private static final int HISTORY_SIZE = 10;
  private static final String STORAGE_FILE = "installation_history.xml";
  private static final String ROOT_TYPE = "history";
  private static final String PROCESSOR_TYPE = "processor";
  private static final String PROCESSOR_NAME_ATTR = "name";
  private static final String TARGET_TYPE = "target";
  private static final String TARGET_UID_ATTR = "uid";

  private static Hashtable history = null;
  private static InstallationHistory defaultInstance = null;

  private InstallationHistory() {
  }

  /**
   * Returns default instance.
   * 
   * @return the default instance
   */
  public static InstallationHistory getDefault() {
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
   *            the target to promote
   * @param processor
   *            the processor
   */
  public void promoteHistory(InstallationTarget target, InstallationItemProcessor processor) {
    if (history == null) {
      history = new Hashtable();
    }

    List targets = (List) history.get(processor.getGeneralTargetName());
    if (targets == null) {
      targets = new ArrayList();
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
   * Purges the passed list of target UIDs from targets, that are not valid
   * anymore for given processor.
   * 
   * @param targets
   *            the list of target UIDs to purge
   * @param processor
   *            processor to obtain fresh list of available targets
   */
  private void purgeTargets(List targetUIDs, InstallationItemProcessor processor) {
    InstallationTarget[] currentTargets = processor.getInstallationTargets();
    Iterator iterator = targetUIDs.iterator();
    while (iterator.hasNext()) {
      String t = (String) iterator.next();
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

  /**
   * Returns list with UIDs (String) that specify most recent targets used. If
   * there is no history for passed processor, empty list is returned.
   * 
   * @param processor
   *            processor for which to return history
   * @return list with recent UIDs
   */
  public List getHistoryUIDs(InstallationItemProcessor processor) {
    if (history == null) {
      return new ArrayList();
    }
    List targets = (List) history.get(processor.getGeneralTargetName());
    if (targets == null) {
      return new ArrayList();
    }
    return targets;
  }

  /**
   * Returns array with InstallationTarget elements that were most recently
   * used for given processor. If there is no history for the passed
   * processor, empty array is returned. This method intersects the current
   * targets returned by the processor with the targets that were previously
   * promoted.
   * 
   * @param processor
   *            processor for which to return history
   * @return array with recent targets
   */
  public InstallationTarget[] getHistory(InstallationItemProcessor processor) {
    int connectedIndex = 0;
    List recent = getHistoryUIDs(processor);
    Iterator iterator = recent.iterator();
    List result = new ArrayList();
    InstallationTarget[] currentTargets = processor.getInstallationTargets();
    while (iterator.hasNext()) {
      String uid = (String) iterator.next();
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
    return (InstallationTarget[]) result.toArray(new InstallationTarget[result.size()]);
  }

  /**
   * Saves history. This allows later restoring the saved history.
   */
  public void saveHistory() {
    if (history == null) {
      return;
    }

    List processors = InstallationRegistry.getInstance().getProcessors();
    XMLMemento config = XMLMemento.createWriteRoot(ROOT_TYPE);
    Enumeration en = history.keys();
    while (en.hasMoreElements()) {
      String processorName = (String) en.nextElement();
      InstallationItemProcessor processor = null;
      for (int i = 0; i < processors.size(); i++) {
        InstallationItemProcessor candidate = (InstallationItemProcessor) processors.get(i);
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
      List targets = (List) history.get(processorName);
      Iterator iterator = targets.iterator();
      InstallationTarget[] currentTargets = processor.getInstallationTargets();
      while (iterator.hasNext()) {
        String uid = (String) iterator.next();
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

    try {
      File configFile = new File(UtilitiesPlugin.getDefault().getStateLocation().toFile(), STORAGE_FILE);
      FileOutputStream stream = new FileOutputStream(configFile);
      OutputStreamWriter writer = new OutputStreamWriter(stream, "utf-8");
      config.save(writer);
      writer.close();
    } catch (IOException e) {
      UtilitiesPlugin.error("Failed to save installation history", e);
    }
  }

  /**
   * Restores previously saved history.
   */
  public void restoreHistory() {
    XMLMemento config = null;
    try {
      File configFile = new File(UtilitiesPlugin.getDefault().getStateLocation().toFile(), STORAGE_FILE);
      InputStream input = new FileInputStream(configFile);
      InputStreamReader reader = new InputStreamReader(input, "utf-8");
      config = XMLMemento.createReadRoot(reader);
    } catch (FileNotFoundException e) {
      // do nothing
    } catch (IOException e) {
      UtilitiesPlugin.error("Failed to load installation history", e);
    } catch (WorkbenchException e) {
      UtilitiesPlugin.error("Failed to load installation history", e);
    }

    if (config == null) {
      return;
    }

    history = new Hashtable();
    IMemento[] processors = config.getChildren(PROCESSOR_TYPE);
    for (int i = 0; i < processors.length; i++) {
      String processorName = processors[i].getString(PROCESSOR_NAME_ATTR);
      if (processorName == null) {
        continue;
      }
      List targetUids = new ArrayList();
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
}

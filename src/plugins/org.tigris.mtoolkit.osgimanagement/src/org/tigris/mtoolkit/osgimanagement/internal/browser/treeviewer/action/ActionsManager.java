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
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.SameShellProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.tigris.mtoolkit.common.installation.BaseFileItem;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.console.ConsoleManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessor;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkTarget;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.UIHelper;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.RemoteBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.StartBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.StopBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.UninstallBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.UpdateBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.UpdatePreverifyOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui.PropertySheet;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

public class ActionsManager {
  private static final String MIME_JAR   = "application/java-archive"; //$NON-NLS-1$
  private static final String JAR_FILTER = "*.jar";                   //$NON-NLS-1$

  public static void addFrameworkAction(TreeRoot treeRoot) {
    String frameworkName = generateName(treeRoot);
    FrameworkImpl newFrameWork = new FrameworkImpl(frameworkName, false);
    PropertySheet sheet = new PropertySheet(treeRoot, newFrameWork, true);
    sheet.open();
  }

  // generates unique name for the new FrameWork
  public static String generateName(TreeRoot treeRoot) {
    HashMap frameWorkMap = treeRoot.getFrameWorkMap();
    int index = 1;
    String frameWorkName;
    do {
      frameWorkName = Messages.new_framework_default_name + " (" + index + ")";
      index++;
    } while (frameWorkMap.containsKey(frameWorkName));

    return frameWorkName;
  }

  public static void deinstallBundleAction(Bundle bundle) {
    RemoteBundleOperation job = new UninstallBundleOperation(bundle);
    job.schedule();
  }

  public static void installBundleAction(final FrameworkImpl framework, TreeViewer parentView) {
    final File[] files = Util.openFileSelectionDialog(parentView.getControl().getShell(),
        Messages.install_bundle_title, JAR_FILTER, Messages.bundle_filter_label, true);
    if (files == null || files.length == 0) {
      return;
    }

    Job job = new Job("Installing to " + framework.getName()) {
      @Override
      public IStatus run(IProgressMonitor monitor) {
        FrameworkProcessor processor = new FrameworkProcessor();
        processor.setUseAdditionalProcessors(false);
        InstallationTarget target = new FrameworkTarget(framework);

        IStatus status = Status.OK_STATUS;
        List items = new ArrayList();
        for (int i = 0; i < files.length; i++) {
          InstallationItem item = new BaseFileItem(files[i], MIME_JAR);
          items.add(item);
          if (monitor.isCanceled()) {
            break;
          }
        }
        status = processor.processInstallationItems(
            (InstallationItem[]) items.toArray(new InstallationItem[items.size()]), null, target, monitor);

        monitor.done();
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        return status;
      }
    };
    job.schedule();
  }

  public static void frameworkPropertiesAction(FrameworkImpl framework, ColumnViewer parentView) {
    PropertyDialogAction action = new PropertyDialogAction(new SameShellProvider(parentView.getControl()), parentView);
    action.run();
  }

  public static void removeFrameworkAction(final FrameworkImpl framework) {
    Job removeJob = new Job("Remove device") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        framework.dispose();
        return Status.OK_STATUS;
      }
    };
    // When disconnect action is scheduled before this action, we need
    // to be sure that it is completed before we dispose the framework
    removeJob.setRule(new FwMutexRule(framework));
    removeJob.schedule();
    ConsoleManager.disconnectConsole(framework.getConnector());
  }

  public static void startBundleAction(Bundle bundle) {
    String name = bundle.getName();
    try {
      name = name + " (" + bundle.getVersion() + ")";
    } catch (IAgentException e) {
    }
    RemoteBundleOperation job = new StartBundleOperation(name, bundle);
    job.schedule();
  }

  public static void stopBundleAction(Bundle bundle) {
    if (bundle.getID() == 0) {
      MessageDialog dialog = new MessageDialog(FrameWorkView.getShell(), "Stop bundle", null, NLS.bind(
          Messages.stop_system_bundle, bundle.getName()), MessageDialog.QUESTION, new String[] {
          "Continue", "Cancel"
      }, 0);
      int statusCode = UIHelper.openWindow(dialog);
      if (statusCode != Window.OK) {
        return;
      }
    }

    RemoteBundleOperation job = new StopBundleOperation(bundle);
    job.schedule();
  }

  public static void updateBundleAction(final Bundle bundle, TreeViewer parentView) {
    final File[] files = Util.openFileSelectionDialog(parentView.getControl().getShell(), Messages.update_bundle_title,
        JAR_FILTER, Messages.bundle_filter_label, false);
    if (files == null || files.length == 0) {
      return;
    }

    RemoteBundleOperation preverifyJob = new UpdatePreverifyOperation(bundle, files[0]);
    preverifyJob.setUser(false);
    preverifyJob.setSystem(true);
    preverifyJob.addJobChangeListener(new JobChangeAdapter() {
      @Override
      public void done(IJobChangeEvent event) {
        if (event.getResult() != null && event.getResult().isOK()) {
          // everything is OK, do the update
          RemoteBundleOperation job = new UpdateBundleOperation(bundle, files[0]);
          job.schedule();
        }
      }
    });
    preverifyJob.schedule();
  }

  public static void disconnectFrameworkAction(final FrameworkImpl fw) {
    disconnectConsole(fw);
    Job disconnectJob = new Job("Disconnect device") {
      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          disconnectFramework0(fw);
          return Status.OK_STATUS;
        } catch (Throwable t) {
          return new Status(IStatus.ERROR, FrameworkPlugin.PLUGIN_ID, t.getMessage(), t);
        }
      }
    };
    disconnectJob.setRule(new FwMutexRule(fw));
    disconnectJob.schedule();
  }

  public static void disconnectConsole(FrameworkImpl fw) {
    ConsoleManager.disconnectConsole(fw.getConnector());
  }

  private static void disconnectFramework0(FrameworkImpl fw) {
    try {
      fw.userDisconnect = true;
      if (fw.isAutoConnected()) {
        fw.disconnect();
      } else {
        // wait if connect operation is still active
        DeviceConnector connector = fw.getConnector();
        // if the connection fails, connector will be null, so we need
        // to recheck the condition
        if (connector != null) {
          connector.closeConnection();
        }
      }
    } catch (IAgentException e) {
      BrowserErrorHandler.processError(e, true);
      e.printStackTrace();
    }
  }

  public static void connectFrameworkAction(FrameworkImpl framework) {
    FrameworkConnectorFactory.connectFrameWork(framework);
  }

  public static void refreshBundleAction(Bundle bundle) {
    FrameworkImpl fw = ((FrameworkImpl) bundle.findFramework());
    if (fw != null) {
      fw.refreshBundleAction(bundle);
    }
  }

  private static class FwMutexRule implements ISchedulingRule {
    private Framework fw;

    public FwMutexRule(Framework fw) {
      this.fw = fw;
    }

    public boolean isConflicting(ISchedulingRule rule) {
      return (rule instanceof FwMutexRule) && (((FwMutexRule) rule).fw == fw);
    }

    public boolean contains(ISchedulingRule rule) {
      return (rule instanceof FwMutexRule) && (((FwMutexRule) rule).fw == fw);
    }
  }

  /**
   * @param bundleCat
   */
  public static void refreshCategoryAction(Category category) {
    FrameworkImpl fw = ((FrameworkImpl) category.findFramework());
    if (fw != null) {
      fw.refreshCategoryAction(category);
    }
  }

  /**
   * @param service
   */
  public static void refreshObjectClassAction(ObjectClass service) {
    FrameworkImpl fw = ((FrameworkImpl) service.findFramework());
    if (fw != null) {
      fw.refreshObjectClassAction(service);
    }
  }

}

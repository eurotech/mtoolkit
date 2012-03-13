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
package org.tigris.mtoolkit.osgimanagement.installation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.Bundle;
import org.tigris.mtoolkit.common.installation.InstallationConstants;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationTarget;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.osgimanagement.Util;
import org.tigris.mtoolkit.osgimanagement.installation.PluginProvider.PluginItem;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConnectFrameworkJob;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.InstallBundleOperation;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.internal.preferences.FrameworkPreferencesPage;
import org.tigris.mtoolkit.osgimanagement.model.Framework;

/**
 * @since 5.0
 */
public final class FrameworkProcessor implements InstallationItemProcessor {
  public static final String MIME_ZIP = "application/zip";
  public static final String MIME_JAR = "application/java-archive";

  private static final Map properties;
  private static final String PROP_JVM_NAME = "jvm.name";
  private static final String ANDROID_TRANSPORT_TYPE = "android";

  private static final FrameworkProcessorExtension bundlesProcessor = new BundlesProcessor();
  private static final String EXTENSION_POINT_PROCESSORS = "org.tigris.mtoolkit.osgimanagement.frameworkProcessorExtensions";
  private static final List<FrameworkProcessorExtension> extensions = new ArrayList<FrameworkProcessorExtension>();

  private static FrameworkProcessor defaultinstance;
  private boolean useAdditionalProcessors = true;

  static {
    Map props = new HashMap(1, 1);
    props.put(InstallationConstants.TESTING_SUPPORTED, Boolean.TRUE);
    properties = Collections.unmodifiableMap(props);

    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint procExtPoint = registry.getExtensionPoint(EXTENSION_POINT_PROCESSORS);
    IConfigurationElement[] elements = procExtPoint.getConfigurationElements();
    if (elements != null) {
      for (int i = 0; i < elements.length; i++) {
        try {
          extensions.add((FrameworkProcessorExtension) elements[i].createExecutableExtension("class"));
        } catch (CoreException e) {
          FrameworkPlugin.log(e.getStatus());
        }
      }
    }
  }

  public static FrameworkProcessor getDefault() {
    if (defaultinstance == null) {
      defaultinstance = new FrameworkProcessor();
    }
    return defaultinstance;
  }

  public boolean getUseAdditionalProcessors() {
    return useAdditionalProcessors;
  }

  public void setUseAdditionalProcessors(boolean enable) {
    this.useAdditionalProcessors = enable;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#
   * getInstallationTargets()
   */
  public InstallationTarget[] getInstallationTargets() {
    FrameworkImpl[] fws = FrameWorkView.getFrameworks();
    if (fws == null || fws.length == 0) {
      return new InstallationTarget[0];
    }
    List targets = new ArrayList(fws.length);
    for (int i = 0; i < fws.length; i++) {
      targets.add(new FrameworkTarget(fws[i]));
    }
    return (InstallationTarget[]) targets.toArray(new InstallationTarget[targets.size()]);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#
   * getGeneralTargetName()
   */
  public String getGeneralTargetName() {
    return "OSGi Framework";
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#
   * getGeneralTargetImageDescriptor()
   */
  public ImageDescriptor getGeneralTargetImageDescriptor() {
    return ImageHolder.getImageDescriptor(ConstantsDistributor.SERVER_ICON_CONNECTED);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.tigris.mtoolkit.common.installation.InstallationItemProcessor#isSupported
   * (java.lang.Object)
   */
  public boolean isSupported(Object target) {
    return (target instanceof Framework);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#
   * getInstallationTarget(java.lang.Object)
   */
  public InstallationTarget getInstallationTarget(Object target) {
    if (target instanceof Framework) {
      return new FrameworkTarget((Framework) target);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#
   * getProperties()
   */
  public Map getProperties() {
    return properties;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#
   * getSupportedMimeTypes()
   */
  public String[] getSupportedMimeTypes() {
    List<String> mimeTypes = new ArrayList<String>();
    mimeTypes.add(MIME_JAR);
    mimeTypes.add(MIME_ZIP);
    if (useAdditionalProcessors) {
      for (int i = 0; i < extensions.size(); i++) {
        String additionalMT[] = extensions.get(i).getSupportedMimeTypes();
        for (int j = 0; j < additionalMT.length; j++) {
          if (mimeTypes.indexOf(additionalMT[j]) == -1) {
            mimeTypes.add(additionalMT[j]);
          }
        }
      }
    }
    return mimeTypes.toArray(new String[mimeTypes.size()]);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.common.installation.InstallationItemProcessor#processInstallationItems(org.tigris.mtoolkit.common.installation.InstallationItem[], 
   *                                                                                                 java.util.Map, 
   *                                                                                                 org.tigris.mtoolkit.common.installation.InstallationTarget, 
   *                                                                                                 org.eclipse.core.runtime.IProgressMonitor)
   */
  public IStatus processInstallationItems(final InstallationItem[] items, Map args, InstallationTarget target,
      final IProgressMonitor monitor) {
    // TODO use multi status to handle errors and warnings
    SubMonitor subMonitor = SubMonitor.convert(monitor, items.length * 2);
    subMonitor.beginTask(Messages.connecting_operation_title, 10);

    try {
      Framework framework = ((FrameworkTarget) target).getFramework();
      if (!framework.isConnected()) {
        Job connectJob = new ConnectFrameworkJob(framework);
        connectJob.schedule();
        try {
          connectJob.join();
          if (!connectJob.getResult().isOK()) {
            return connectJob.getResult();
          }
          int counter = 0;
          while (!framework.isConnected() && counter++ < 100) {
            Thread.sleep(50);
          }
        } catch (InterruptedException ex) {
          return Util.newStatus(IStatus.ERROR, ex.getMessage(), ex);
        }
      }
      subMonitor.worked(1);
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }

      final DeviceConnector connector = framework.getConnector();
      if (connector == null) {
        return Util.newStatus(IStatus.ERROR, NLS.bind("Could not establish connection to {0}", framework), null);
      }

      Map preparationProps = new Hashtable();
      preparationProps.putAll(framework.getSigningProperties());
      if (!preparationProps.containsKey(PROP_JVM_NAME)) {
        String transportType = (String) connector.getProperties().get(DeviceConnector.TRANSPORT_TYPE);
        if (ANDROID_TRANSPORT_TYPE.equals(transportType)) {
          preparationProps.put(PROP_JVM_NAME, "Dalvik");
        }
      }
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }

      subMonitor.worked(1);
      subMonitor.setTaskName(Messages.preparing_operation_title);
      for (int i = 0; i < items.length; i++) {
        IStatus status = items[i].prepare(subMonitor.newChild(1), preparationProps);
        if (status == null) {
          continue;
        }
        if (status.matches(IStatus.CANCEL) || status.matches(IStatus.ERROR)) {
          return status;
        }
      }

      subMonitor.worked(4);
      List<InstallationItem> itemsToInstall = new ArrayList<InstallationItem>();
      for (InstallationItem item : items) {
        IStatus status = processItem0(item, preparationProps, framework, subMonitor.newChild(1), itemsToInstall);
        if (status == null) {
          continue;
        }
        if (status.matches(IStatus.CANCEL)) {
          return status;
        }
        if (status.matches(IStatus.ERROR)) {
          FrameworkPlugin.log(status);
        }
      }

      if (!itemsToInstall.isEmpty()) {
        for (InstallationItem item : itemsToInstall) {
          try {
            RemoteBundle bundle = installBundle(item, framework, subMonitor);
            if (bundle != null) {
              startBundle(bundle, subMonitor);
            }
          } catch (CoreException e) {
            final IStatus status = e.getStatus();
            if (status.matches(IStatus.CANCEL)) {
              monitor.setCanceled(true);
              break;
            }
            FrameworkPlugin.log(status);
          } catch (Exception e) {
            FrameworkPlugin.log(Util.newStatus(IStatus.ERROR, e.getMessage(), e));
          }
        }
      }

    } finally {
      monitor.done();
    }
    if (monitor.isCanceled()) {
      return Status.CANCEL_STATUS;
    }
    return Status.OK_STATUS;
  }

  private IStatus processItem0(final InstallationItem item, Map preparationProps, Framework framework,
      final IProgressMonitor monitor, List<InstallationItem> itemsToInstall) {
    try {
      FrameworkProcessorExtension processor = findProcessor(item, monitor);
      if (processor instanceof BundlesProcessor) {
        bundlesProcessor.processItem(item, itemsToInstall, preparationProps, framework, monitor);
        itemsToInstall.add(item);
      } else if (!processor.processItem(item, itemsToInstall, preparationProps, framework, monitor)) {
        InstallationItem[] children = item.getChildren();
        if (children != null) {
          for (InstallationItem childItem : children) {
            int prio = bundlesProcessor.getPriority(childItem);
            if (prio == FrameworkProcessorExtension.PRIORITY_NOT_SUPPPORTED) {
              throw new CoreException(Util.newStatus(IStatus.ERROR, "No suitable processor found.", null));
            }
            bundlesProcessor.processItem(childItem, itemsToInstall, preparationProps, framework, monitor);
            itemsToInstall.add(childItem);
          }
        }
      }
    } catch (CoreException e) {
      return e.getStatus();
    }
    return Status.OK_STATUS;
  }

  private FrameworkProcessorExtension findProcessor(final InstallationItem item, final IProgressMonitor monitor)
      throws CoreException {
    FrameworkProcessorExtension[] processors = getExtensions(item);
    if (processors.length == 0) {
      throw new CoreException(Util.newStatus(IStatus.ERROR, "No suitable processor found.", null));
    } else if (processors.length == 1) {
      return processors[0];
    }
    final FrameworkProcessorExtension processor[] = new FrameworkProcessorExtension[] { processors[0] };
    try {
      final IWorkbench workbench = PlatformUI.getWorkbench();
      final FrameworkProcessorExtension prArr[] = new FrameworkProcessorExtension[processors.length];
      System.arraycopy(processors, 0, prArr, 0, processors.length);
      final Display display = workbench.getDisplay();
      if (display == null || display.isDisposed()) {
        // Eclipse workbench is not active
        throw new CoreException(Status.CANCEL_STATUS);
      }
      display.syncExec(new Runnable() {
        /*
         * (non-Javadoc)
         * 
         * @see java.lang.Runnable#run()
         */
        public void run() {
          ListDialog dialog = new ListDialog(FrameWorkView.getShell());
          dialog.setTitle("Select processor");
          dialog.setLabelProvider(new LabelProvider() {
            /* (non-Javadoc)
             * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
             */
            public Image getImage(Object element) {
              return ((FrameworkProcessorExtension) element).getImage();
            }

            /* (non-Javadoc)
             * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
             */
            public String getText(Object element) {
              return ((FrameworkProcessorExtension) element).getName();
            }
          });
          dialog.setMessage(NLS.bind("Select installation processor for {0}", item.getName()));
          dialog.setContentProvider(new ArrayContentProvider());
          dialog.setInput(prArr);
          dialog.setInitialSelections(new Object[] { prArr[0] });
          if (dialog.open() == Window.CANCEL) {
            monitor.setCanceled(true);
          } else {
            processor[0] = (FrameworkProcessorExtension) dialog.getResult()[0];
          }
        }
      });
    } catch (Exception e) {
      throw new CoreException(Util.newStatus(IStatus.ERROR, "Remote content installation failed", e));
    }
    if (monitor.isCanceled()) {
      throw new CoreException(Status.CANCEL_STATUS);
    }
    return processor[0];
  }

  private FrameworkProcessorExtension[] getExtensions(InstallationItem item) {
    List<ExtensionWrapper> processors = new ArrayList<ExtensionWrapper>();
    int priority = bundlesProcessor.getPriority(item);
    if (priority != FrameworkProcessorExtension.PRIORITY_NOT_SUPPPORTED) {
      processors.add(new ExtensionWrapper(priority, bundlesProcessor));
    }
    if (useAdditionalProcessors) {
      for (int i = 0; i < extensions.size(); i++) {
        FrameworkProcessorExtension processor = extensions.get(i);
        priority = processor.getPriority(item);
        if (priority != FrameworkProcessorExtension.PRIORITY_NOT_SUPPPORTED) {
          processors.add(new ExtensionWrapper(priority, processor));
        }
      }
      if (processors.size() > 0) {
        Collections.sort(processors);
        int maxIndex = processors.size() - 1;
        int maxPriority = processors.get(maxIndex).priority;
        for (int i = maxIndex - 1; i >= 0; i--) {
          if (processors.get(i).priority == maxPriority) {
            maxIndex = i;
          } else {
            break;
          }
        }
        processors = processors.subList(maxIndex, processors.size());
      }
    }
    FrameworkProcessorExtension[] results = new FrameworkProcessorExtension[processors.size()];
    for (int i = 0; i < processors.size(); i++) {
      results[i] = processors.get(i).processor;
    }
    return results;
  }

  private static RemoteBundle installBundle(InstallationItem item, Framework framework, IProgressMonitor monitor)
      throws CoreException {
    File bundle = null;
    InputStream input = null;
    try {
      input = item.getInputStream();
      bundle = FrameworkPlugin.saveFile(input, item.getName());
      InstallBundleOperation operation = new InstallBundleOperation((FrameworkImpl) framework);
      return operation.installBundle(bundle, monitor);
    } catch (Exception e) {
      throw new CoreException(Util.newStatus(IStatus.ERROR, "Unable to install bundle", e));
    } finally {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e) {
        }
      }
      if (bundle != null) {
        bundle.delete();
      }
    }
  }

  private static void startBundle(RemoteBundle installedItem, IProgressMonitor monitor) throws Exception {
    if (!FrameworkPreferencesPage.isAutoStartBundlesEnabled()) {
      return;
    }
    final RemoteBundle remoteBundle = (RemoteBundle) installedItem;
    if (remoteBundle.getType() == RemoteBundle.BUNDLE_TYPE_FRAGMENT) {
      // Fragment bundles cannot be started
      return;
    }
    final String symbolicName = remoteBundle.getSymbolicName();
    final String version = remoteBundle.getVersion();
    Job job = new Job(NLS.bind("Starting bundle {0}({1})", symbolicName, version)) {
      /* (non-Javadoc)
       * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
       */
      @Override
      public IStatus run(IProgressMonitor monitor) {
        int flags = FrameworkPreferencesPage.isActivationPolicyEnabled() ? Bundle.START_ACTIVATION_POLICY : 0;
        try {
          remoteBundle.start(flags);
        } catch (IAgentException e) {
          // only log this exception, because the user requested install bundle
          StatusManager.getManager().handle(Util.handleIAgentException(e), StatusManager.LOG);
          return Status.CANCEL_STATUS;
        } finally {
          monitor.done();
        }
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
      }
    };
    job.schedule();
  }

  private static final class BundlesProcessor implements FrameworkProcessorExtension {
    public static final String[] SUPPORTED_MIME_TYPES = new String[] { MIME_JAR, MIME_ZIP };

    /* (non-Javadoc)
     * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessorExtension#getName()
     */
    public String getName() {
      return "Bundles processor";
    }

    /* (non-Javadoc)
     * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessorExtension#getImage()
     */
    public Image getImage() {
      return ImageHolder.getImage(FrameWorkView.BUNDLES_GROUP_IMAGE_PATH);
    }

    /* (non-Javadoc)
     * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessorExtension#getSupportedMimeTypes()
     */
    public String[] getSupportedMimeTypes() {
      return SUPPORTED_MIME_TYPES;
    }

    /* (non-Javadoc)
     * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessorExtension#getPriority(org.tigris.mtoolkit.common.installation.InstallationItem)
     */
    public int getPriority(InstallationItem item) {
      String mimeType = item.getMimeType();
      if (MIME_JAR.equals(mimeType) || MIME_ZIP.equals(mimeType)) {
        return PRIORITY_NORMAL;
      }
      return PRIORITY_NOT_SUPPPORTED;
    }

    /* (non-Javadoc)
     * @see org.tigris.mtoolkit.osgimanagement.installation.FrameworkProcessorExtension#processItem(org.tigris.mtoolkit.common.installation.InstallationItem, 
     *                                                                                              java.util.List, 
     *                                                                                              java.util.Map, 
     *                                                                                              org.tigris.mtoolkit.osgimanagement.model.Framework, 
     *                                                                                              org.eclipse.core.runtime.IProgressMonitor)
     */
    public boolean processItem(InstallationItem item, List<InstallationItem> dependencies, Map preparationProps,
        Framework framework, IProgressMonitor monitor) throws CoreException {
      if (item instanceof PluginItem) {
        IStatus status = ((PluginItem) item).checkAdditionalBundles((FrameworkImpl) framework, monitor, dependencies,
            preparationProps);
        if (status != null) {
          if (status.matches(IStatus.CANCEL)) {
            throw new CoreException(Status.CANCEL_STATUS);
          }
          if (status.matches(IStatus.ERROR)) {
            throw new CoreException(status);
          }
        }
      }
      return true;
    }
  }

  private static final class ExtensionWrapper implements Comparable<ExtensionWrapper> {
    int priority;
    FrameworkProcessorExtension processor;

    public ExtensionWrapper(int priority, FrameworkProcessorExtension processor) {
      this.priority = priority;
      this.processor = processor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ExtensionWrapper o) {
      return priority - o.priority;
    }
  }
}

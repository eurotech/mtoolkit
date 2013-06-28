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
package org.tigris.mtoolkit.osgimanagement.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TreeAdapter;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.tigris.mtoolkit.common.FileUtils;
import org.tigris.mtoolkit.common.PluginUtilities;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.ContentTypeActionsProvider;
import org.tigris.mtoolkit.osgimanagement.ISystemBundlesProvider;
import org.tigris.mtoolkit.osgimanagement.ToolbarIMenuCreator;
import org.tigris.mtoolkit.osgimanagement.installation.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.BundlesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServicesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AddAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.CommonPropertiesAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ConnectAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.DeInstallBundleAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.DisconnectAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.FindAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.GotoServiceAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.InstallBundleAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.RefreshAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.RemoveAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ShowBundleIDAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ShowBundleVersionAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ShowFrameworkConsole;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ShowServicePropertiesInTree;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.StartAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.StopAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.UpdateBundleAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ViewAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.logic.ViewContentProvider;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.logic.ViewLabelProvider;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;
import org.tigris.mtoolkit.osgimanagement.model.Framework;
import org.tigris.mtoolkit.osgimanagement.model.Model;
import org.tigris.mtoolkit.osgimanagement.model.SimpleNode;

// TODO:Remove static access to ui widgets and model entries
public final class FrameworksView extends ViewPart implements ConstantsDistributor {
  public static final String                        VIEW_ID                      = FrameworkPlugin.PLUGIN_ID
                                                                                     + ".frameworkview";
  public static final String                        PROPERTIES_IMAGE_PATH        = "properties.gif";                                          //$NON-NLS-1$
  public static final String                        UPDATE_BUNDLE_IMAGE_PATH     = "update_bundle.gif";                                       //$NON-NLS-1$
  public static final String                        STOP_BUNDLE_IMAGE_PATH       = "stop_bundle.gif";                                         //$NON-NLS-1$
  public static final String                        START_BUNDLE_IMAGE_PATH      = "start_bundle.gif";                                        //$NON-NLS-1$
  public static final String                        UNINSTALL_BUNDLE_IMAGE_PATH  = "uninstall_bundle.gif";                                    //$NON-NLS-1$
  public static final String                        INSTALL_BUNDLE_IMAGE_PATH    = "install_bundle.gif";                                      //$NON-NLS-1$
  public static final String                        DISCONNECT_ACTION_IMAGE_PATH = "disconnect_action.gif";                                   //$NON-NLS-1$
  public static final String                        CONNECT_ACTION_IMAGE_PATH    = "connect_action.gif";                                      //$NON-NLS-1$
  public static final String                        REMOVE_ACTION_ACTION_PATH    = "remove_action.gif";                                       //$NON-NLS-1$
  public static final String                        ADD_ACTION_IMAGE_PATH        = "add_action.gif";                                          //$NON-NLS-1$
  public static final String                        BUNDLES_GROUP_IMAGE_PATH     = "bundles_group.gif";                                       //$NON-NLS-1$
  public static final String                        SEARCH_IMAGE_PATH            = "search_action.gif";
  public static final String                        REFRESH_IMAGE_PATH           = "refresh_action.gif";
  public static final String                        CONSOLE_IMAGE_PATH           = "console.gif";

  private static final String                       SYSTEM_BUNDLES_EXT_POINT_ID  = "org.tigris.mtoolkit.osgimanagement.systemBundlesProvider";

  private static AddAction                          addFrameworkAction;
  private static RemoveAction                       removeFrameworkAction;
  private static ConnectAction                      connectAction;
  private static DisconnectAction                   disconnectAction;
  private static InstallBundleAction                installBundleAction;
  private static DeInstallBundleAction              deinstallBundleAction;
  private static StartAction                        startAction;
  private static StopAction                         stopAction;
  private static UpdateBundleAction                 updateBundleAction;
  private static CommonPropertiesAction             commonPropertiesAction;
  private static ShowServicePropertiesInTree        showServPropsInTreeAction;
  private static ShowFrameworkConsole               showConsoleAction;
  private static RefreshAction                      refreshAction;
  private static ViewAction                         viewServicesAction;
  private static ViewAction                         viewBundlesAction;
  private ShowBundleIDAction                        showBundleIDAction;
  private GotoServiceAction                         gotoServiceAction;
  private ShowBundleVersionAction                   showBundleVersionAction;
  private FindAction                                findAction;

  private final FilterJob                           filterJob                    = new FilterJob();
  private final ViewerFilter                        filter                       = new FrameworksViewerFilter();

  private Text                                      filterField;
  private TreeViewer                                tree;
  private IWorkbenchPage                            activePage;
  private MenuManager                               mgr;
  private ToolbarIMenuCreator                       bundlesTB;
  private static TreeRoot                           treeRoot;

  private static HashMap                            activeInstances;
  private String                                    notFoundText                 = null;

  // System bundles providers
  private static final List<ISystemBundlesProvider> systemBundlesProviders       = new ArrayList<ISystemBundlesProvider>();

  static {
    obtainSystemBundlesProviders();
  }

  // Get current shell
  public static Shell getShell() {
    return PluginUtilities.getActiveWorkbenchShell();
  }

  // Get current active FrameworksView
  public static FrameworksView getActiveInstance() {
    final IViewPart[] view = new IViewPart[1];
    IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
    if (workbenchWindows != null && workbenchWindows.length > 0) {
      final IWorkbenchWindow workbenchWindow = workbenchWindows[0];
      Display display = PlatformUI.getWorkbench().getDisplay();
      display.syncExec(new Runnable() {
        public void run() {
          IWorkbenchPage activePage = workbenchWindow.getActivePage();
          if (activePage == null) {
            return;
          }
          IViewReference[] viewRefs = activePage.getViewReferences();
          for (int i = 0; i < viewRefs.length; i++) {
            if (VIEW_ID.equals(viewRefs[i].getId())) {
              view[0] = viewRefs[i].getView(true);
              return;
            }
          }
        }
      });
    }
    return (FrameworksView) view[0];
  }

  // Get the root containing all frameworks
  public static TreeRoot getTreeRoot() {
    return FrameworksView.treeRoot;
  }

  // Gets current view tree
  public TreeViewer getTree() {
    return tree;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.IWorkbenchPart#createPartControl(Composite)
   */
  @Override
  public void createPartControl(Composite parent) {
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.verticalSpacing = 0;
    parent.setLayout(layout);

    Composite filterPanel = new Composite(parent, SWT.NONE);
    // TODO: Handle changes in the system color scheme
    filterPanel.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
    filterPanel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    FillLayout filterLayout = new FillLayout();
    filterLayout.marginHeight = 2;
    filterLayout.marginWidth = 2;
    filterPanel.setLayout(filterLayout);

    filterField = new Text(filterPanel, SWT.SINGLE | SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
    filterField.setMessage("type filter text");

    filterField.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        treeRoot.setFilter(filterField.getText());
        filterJob.cancel();
        filterJob.schedule(300);
      }
    });
    filterField.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetDefaultSelected(SelectionEvent e) {
        tree.getTree().forceFocus();
        findItem(filterField.getText().toLowerCase().trim(), tree);
      }
    });

    treeRoot.addListener(new ContentChangeListener() {
      /* (non-Javadoc)
       * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener#elementRemoved(org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent)
       */
      public void elementRemoved(ContentChangeEvent event) {
        applyFilter();
      }

      /* (non-Javadoc)
       * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener#elementChanged(org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent)
       */
      public void elementChanged(ContentChangeEvent event) {
        applyFilter();
      }

      /* (non-Javadoc)
       * @see org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeListener#elementAdded(org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ContentChangeEvent)
       */
      public void elementAdded(ContentChangeEvent event) {
        applyFilter();
      }

      private void applyFilter() {
        Display display = PlatformUI.getWorkbench().getDisplay();
        if (display != null && !display.isDisposed()) {
          display.asyncExec(new Runnable() {
            public void run() {
              if (filterField == null || filterField.isDisposed()) {
                return;
              }
              treeRoot.setFilter(filterField.getText());
            }
          });
        }

        filterJob.cancel();
        filterJob.schedule(300);
      }
    });
    filterField.setText("");

    GridData gridDataTree = new GridData(GridData.FILL_BOTH);
    tree = new TreeViewer(parent, SWT.MULTI);
    tree.getTree().setLayoutData(gridDataTree);
    tree.addDoubleClickListener(new IDoubleClickListener() {
      public void doubleClick(DoubleClickEvent event) {
        Model node = (Model) ((TreeSelection) event.getSelection()).getFirstElement();
        if (node != null) {
          if (node instanceof FrameworkImpl) {
            if (!((FrameworkImpl) node).isConnected()) {
              FrameworkConnectorFactory.connectFrameWork((FrameworkImpl) node);
            }
          }
          boolean expand = !tree.getExpandedState(node);
          tree.setExpandedState(node, expand);
        }
      }
    });

    tree.getTree().addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        boolean doSearch = e.character == Character.LINE_SEPARATOR && !filterField.getText().trim().equals("");
        if (doSearch) {
          e.doit = false;
          findItem(filterField.getText().toLowerCase().trim(), tree);
        }
        super.keyPressed(e);
      }
    });

    activePage = getSite().getPage();

    if (activeInstances == null) {
      activeInstances = new HashMap();
    }

    activeInstances.put(new Integer(activePage.hashCode()), this);

    tree.setContentProvider(new ViewContentProvider());
    tree.setLabelProvider(new ViewLabelProvider());
    tree.addFilter(filter);
    tree.setSorter(new ViewerSorter());
    tree.setInput(treeRoot);
    getViewSite().setSelectionProvider(tree);

    // Update state of bundle if there is a need! For example when a
    // developer
    // suspend framework in debug/profile mode and open category that wasn't
    // open
    // all bundles are in gray. But if he resume it and click over bundle or
    // reexpand category bundle state is updated automatically.
    tree.addSelectionChangedListener(new ISelectionChangedListener() {
      public void selectionChanged(SelectionChangedEvent event) {
        Object tmp = ((IStructuredSelection) event.getSelection()).getFirstElement();
        if (tmp instanceof Bundle) {
          Bundle b = (Bundle) tmp;
          if (b.isNeedUpdate()) {
            tree.update(b, null);
          }
        }
        updateContextMenuStates();
      }
    });
    tree.getTree().addTreeListener(new TreeAdapter() {
      public void updateItems(TreeItem parent) {
        if (!(parent.getData() instanceof Category) && !(parent.getData() instanceof BundlesCategory)) {
          return;
        }
        TreeItem items[] = null;
        items = parent.getItems();
        if (items == null) {
          return;
        }
        for (int i = 0; i < items.length; i++) {
          Bundle b = (Bundle) items[i].getData();
          if (b.isNeedUpdate()) {
            tree.update(b, null);
          }
        }
      }

      @Override
      public void treeExpanded(TreeEvent e) {
        updateItems((TreeItem) e.item);
      }
    });

    addContributions();
    createToolbarAndMenu();
    PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.FRAMEWORKS_VIEW);
    //createShortcut(FIND_COMMAND_ID, findAction, "Ctrl+F");
    // createShortcut(REFRESH_COMMAND_ID, refreshAction, "F5");
    //createShortcut(PROPERTIES_COMMAND_ID, commonPropertiesAction, "Alt+Enter");
    //createShortcut(REMOVE_COMMAND_ID, removeFrameworkAction, "DEL");
  }

  private void createToolbarAndMenu() {
    MenuManager mainMenu = (MenuManager) getViewSite().getActionBars().getMenuManager();
    mainMenu.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager manager) {
        updateContextMenuStates();
      }
    });

    mainMenu.add(showBundleIDAction);
    mainMenu.add(showBundleVersionAction);
    mainMenu.add(new Separator());
    mainMenu.add(showServPropsInTreeAction);

    ToolBarManager toolBar = (ToolBarManager) getViewSite().getActionBars().getToolBarManager();
    toolBar.add(new Separator(ContentTypeActionsProvider.GROUP_CONNECT));
    toolBar.add(new Separator(ContentTypeActionsProvider.GROUP_DEPLOYMENT));
    toolBar.add(new Separator(ContentTypeActionsProvider.GROUP_UNSIGNED));
    toolBar.add(new Separator(ContentTypeActionsProvider.GROUP_FRAMEWORK));
    toolBar.add(new Separator(ContentTypeActionsProvider.GROUP_ACTIONS));

    toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_CONNECT, connectAction);
    toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_CONNECT, disconnectAction);

    ContributionItem items[] = new ContributionItem[] {
        new ActionContributionItem(startAction), new ActionContributionItem(stopAction),
        new ActionContributionItem(updateBundleAction), new ActionContributionItem(deinstallBundleAction),
        new Separator(), new ActionContributionItem(installBundleAction)
    };
    bundlesTB = new ToolbarIMenuCreator(items, tree);
    bundlesTB.setImageDescriptor(ImageHolder.getImageDescriptor(BUNDLES_GROUP_IMAGE_PATH));
    bundlesTB.setToolTipText(Messages.BundlesAction_ToolTip);
    toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_DEPLOYMENT, bundlesTB);

    for (int i = 0; i < actionProviders.size(); i++) {
      ContentTypeActionsProvider provider = ((ActionsProviderElement) actionProviders.get(i)).getProvider();
      provider.fillToolBar(toolBar);
      Map actionsMap = provider.getCommonActions();
      if (actionsMap != null) {
        IAction action = (IAction) actionsMap.get(ContentTypeActionsProvider.PROPERTIES_ACTION);
        if (action != null) {
          commonPropertiesAction.registerAction(action);
        }
      }
    }

    toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_FRAMEWORK, addFrameworkAction);
    toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_FRAMEWORK, removeFrameworkAction);
    commonPropertiesAction.setToolTipText(Messages.property_action_label);

    toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, viewServicesAction);
    toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, viewBundlesAction);
    toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, showConsoleAction);

    refreshAction.setToolTipText(Messages.refresh_action_label);
    toolBar.add(refreshAction);

    updateContextMenuStates();
  }

  public static Set<String> getSystemBundles() {
    Set<String> systemBundles = new HashSet<String>();
    for (ISystemBundlesProvider provider : systemBundlesProviders) {
      Set<String> bundles = provider.getSystemBundlesIDs();
      if (bundles == null) {
        continue;
      }
      systemBundles.addAll(bundles);
    }
    return systemBundles;
  }

  private static void obtainSystemBundlesProviders() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registry.getExtensionPoint(SYSTEM_BUNDLES_EXT_POINT_ID);
    IConfigurationElement[] elements = extensionPoint.getConfigurationElements();
    if (elements == null) {
      return;
    }
    for (int i = 0; i < elements.length; i++) {
      try {
        systemBundlesProviders.add((ISystemBundlesProvider) elements[i].createExecutableExtension("class"));
      } catch (CoreException e) {
        FrameworkPlugin.error("Exception while intializing system bundles provider elements", e);
      }
    }
  }

  // Create custom contributions - tree popup menu
  private void addContributions() {
    IActionBars actionBars = getViewSite().getActionBars();

    addFrameworkAction = new AddAction(tree, Messages.add_action_label);
    addFrameworkAction.setImageDescriptor(ImageHolder.getImageDescriptor(ADD_ACTION_IMAGE_PATH));

    removeFrameworkAction = new RemoveAction(tree, Messages.remove_action_label);
    removeFrameworkAction.setImageDescriptor(ImageHolder.getImageDescriptor(REMOVE_ACTION_ACTION_PATH));
    actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), removeFrameworkAction);

    connectAction = new ConnectAction(tree, Messages.connect_action_label);
    connectAction.setImageDescriptor(ImageHolder.getImageDescriptor(CONNECT_ACTION_IMAGE_PATH));

    disconnectAction = new DisconnectAction(tree, Messages.disconnect_action_label);
    disconnectAction.setImageDescriptor(ImageHolder.getImageDescriptor(DISCONNECT_ACTION_IMAGE_PATH));

    installBundleAction = new InstallBundleAction(tree, Messages.install_action_label);
    installBundleAction.setImageDescriptor(ImageHolder.getImageDescriptor(INSTALL_BUNDLE_IMAGE_PATH));

    deinstallBundleAction = new DeInstallBundleAction(tree, Messages.deinstall_action_label);
    deinstallBundleAction.setImageDescriptor(ImageHolder.getImageDescriptor(UNINSTALL_BUNDLE_IMAGE_PATH));

    startAction = new StartAction(tree, Messages.start_action_label);
    startAction.setImageDescriptor(ImageHolder.getImageDescriptor(START_BUNDLE_IMAGE_PATH));

    stopAction = new StopAction(tree, Messages.stop_action_label);
    stopAction.setImageDescriptor(ImageHolder.getImageDescriptor(STOP_BUNDLE_IMAGE_PATH));

    updateBundleAction = new UpdateBundleAction(tree, Messages.update_action_label);
    updateBundleAction.setImageDescriptor(ImageHolder.getImageDescriptor(UPDATE_BUNDLE_IMAGE_PATH));

    gotoServiceAction = new GotoServiceAction(tree, Messages.goto_service_action_label);
    viewServicesAction = new ViewAction(tree, Messages.services_view_action_label, tree, FrameworkImpl.SERVICES_VIEW);
    viewServicesAction.setImageDescriptor(ImageHolder.getImageDescriptor(ViewLabelProvider.SERVICES_CATEGORY_ICON));
    viewBundlesAction = new ViewAction(tree, Messages.bundles_view_action_label, tree, FrameworkImpl.BUNDLES_VIEW);
    viewBundlesAction.setImageDescriptor(ImageHolder.getImageDescriptor(BUNDLES_GROUP_IMAGE_PATH));

    showBundleIDAction = new ShowBundleIDAction(tree, Messages.show_bundle_id_action_label, tree, getTreeRoot());
    showBundleVersionAction = new ShowBundleVersionAction(tree, Messages.show_bundle_version_action_label, tree,
        getTreeRoot());

    showServPropsInTreeAction = new ShowServicePropertiesInTree(tree, Messages.show_service_properties_in_tree);

    commonPropertiesAction = new CommonPropertiesAction(tree, Messages.property_action_label);
    commonPropertiesAction.setImageDescriptor(ImageHolder.getImageDescriptor(PROPERTIES_IMAGE_PATH));
    actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), commonPropertiesAction);

    findAction = new FindAction(tree, filterField, Messages.find_action_label);
    findAction.setImageDescriptor(ImageHolder.getImageDescriptor(SEARCH_IMAGE_PATH));
    actionBars.setGlobalActionHandler(ActionFactory.FIND.getId(), findAction);

    showConsoleAction = new ShowFrameworkConsole(tree, Messages.show_framework_console, tree);
    showConsoleAction.setImageDescriptor(ImageHolder.getImageDescriptor(CONSOLE_IMAGE_PATH));

    refreshAction = new RefreshAction(tree, Messages.refresh_action_label, tree);
    refreshAction.setImageDescriptor(ImageHolder.getImageDescriptor(REFRESH_IMAGE_PATH));
    actionBars.setGlobalActionHandler(ActionFactory.REFRESH.getId(), refreshAction);

    obtainActionProviders();
    for (int i = 0; i < actionProviders.size(); i++) {
      ContentTypeActionsProvider provider = ((ActionsProviderElement) actionProviders.get(i)).getProvider();
      provider.init(tree);
    }

    mgr = new MenuManager();
    mgr.setRemoveAllWhenShown(true);
    mgr.addMenuListener(new IMenuListener() {
      public void menuAboutToShow(IMenuManager mgr) {
        fillContextMenu(mgr);
        updateContextMenuStates();
      }
    });

    Menu menu = mgr.createContextMenu(tree.getControl());
    tree.getControl().setMenu(menu);
    getSite().registerContextMenu(mgr, tree);

    actionBars.updateActionBars();
  }

  public void updateContextMenuStates() {
    if (tree == null || tree.getControl().isDisposed()) {
      return;
    }
    IStructuredSelection selection = (IStructuredSelection) tree.getSelection();

    connectAction.updateState(selection);
    disconnectAction.updateState(selection);
    refreshAction.updateState(selection);

    startAction.updateState(selection);
    stopAction.updateState(selection);
    updateBundleAction.updateState(selection);
    deinstallBundleAction.updateState(selection);

    installBundleAction.updateState(selection);

    addFrameworkAction.setEnabled(true);
    removeFrameworkAction.updateState(selection);
    commonPropertiesAction.updateState(selection);

    viewBundlesAction.updateState(selection);
    viewServicesAction.updateState(selection);
    showConsoleAction.updateState(selection);

    DeviceConnector connector = null;
    if (selection != null) {
      Model model = (Model) selection.getFirstElement();
      if (model != null) {
        Framework fw = model.findFramework();
        if (fw != null) {
          connector = fw.getConnector();
        }
      }
    }

    for (int i = 0; i < actionProviders.size(); i++) {
      ContentTypeActionsProvider provider = ((ActionsProviderElement) actionProviders.get(i)).getProvider();
      provider.updateEnabledState(connector);
    }

    bundlesTB.setEnabled(installBundleAction.isEnabled());
  }

  // Fill context menu Actions when menu is about to show
  private void fillContextMenu(IMenuManager manager) {
    tree.setSelection(tree.getSelection());
    StructuredSelection selection = (StructuredSelection) tree.getSelection();
    boolean homogen = true;

    manager.add(new Separator(ContentTypeActionsProvider.GROUP_UNSIGNED));
    manager.add(new Separator(ContentTypeActionsProvider.GROUP_ACTIONS));
    manager.add(new Separator(ContentTypeActionsProvider.GROUP_INSTALL));
    manager.add(new Separator(ContentTypeActionsProvider.GROUP_OPTIONS));
    manager.add(new Separator(ContentTypeActionsProvider.GROUP_DEFAULT));
    manager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    manager.add(new Separator(ContentTypeActionsProvider.GROUP_PROPERTIES));

    if (selection.size() > 0) {
      Model element = (Model) selection.getFirstElement();
      Class clazz = element.getClass();

      Iterator iterator = selection.iterator();
      while (iterator.hasNext()) {
        Object sel = iterator.next();
        if (!clazz.equals(sel.getClass())) {
          homogen = false;
          break;
        }
      }

      if (homogen) {
        if (element instanceof FrameworkImpl) {
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, connectAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, disconnectAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, removeFrameworkAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, refreshAction);

          manager.appendToGroup(ContentTypeActionsProvider.GROUP_INSTALL, installBundleAction);
        }
        if (element instanceof TreeRoot) {
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_UNSIGNED, addFrameworkAction);
        }

        if (element instanceof Bundle) {
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, startAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, stopAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, updateBundleAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, installBundleAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, deinstallBundleAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, refreshAction);

          manager.appendToGroup(ContentTypeActionsProvider.GROUP_OPTIONS, showBundleIDAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_OPTIONS, showBundleVersionAction);
        }

        if (element instanceof Category) {
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, installBundleAction);
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, refreshAction);
        }

        if (element instanceof ObjectClass) {
          if (!(element.getParent() instanceof FrameworkImpl)) {
            manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, gotoServiceAction);
          }
          manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, refreshAction);
        }
        if (element instanceof SimpleNode) {
          if (element.getName().equals(Messages.bundles_node_label)) {
            manager.appendToGroup(ContentTypeActionsProvider.GROUP_INSTALL, installBundleAction);
          }
        }
      }
    } else {
      manager.appendToGroup(ContentTypeActionsProvider.GROUP_UNSIGNED, addFrameworkAction);
    }
    for (int i = 0; i < actionProviders.size(); i++) {
      ContentTypeActionsProvider provider = ((ActionsProviderElement) actionProviders.get(i)).getProvider();
      provider.menuAboutToShow(selection, manager);
    }

    // call menuAboutToShow
    manager.appendToGroup(ContentTypeActionsProvider.GROUP_DEFAULT, findAction);
    manager.appendToGroup(ContentTypeActionsProvider.GROUP_DEFAULT, showConsoleAction);
    if (selection.size() > 0 && homogen) {
      Model element = (Model) selection.getFirstElement();
      if (element instanceof FrameworkImpl || element instanceof Bundle || element instanceof ObjectClass) {
        manager.appendToGroup(ContentTypeActionsProvider.GROUP_PROPERTIES, commonPropertiesAction);
      }
    }
  }

  // Save Tree Model
  public static void saveModel() {
    FrameworkPlugin plugin = FrameworkPlugin.getDefault();
    if (plugin == null) {
      return;
    }
    final TreeRoot root = treeRoot;
    if (root == null) {
      return;
    }
    File configFile = new File(plugin.getStateLocation().toFile(), STORAGE_FILE_NAME);
    XMLMemento rootConfig = XMLMemento.createWriteRoot(MEMENTO_ROOT_TYPE);
    Model[] children = root.getChildren();
    for (int i = 0; i < root.getSize(); i++) {
      if (!((FrameworkImpl) children[i]).isAutoConnected()) {
        IMemento config = ((FrameworkImpl) children[i]).getConfig();
        if (config != null) {
          IMemento child = rootConfig.createChild(MEMENTO_TYPE);
          child.putMemento(config);
        }
      }
    }
    OutputStreamWriter writer = null;
    try {
      writer = new OutputStreamWriter(new FileOutputStream(configFile), "utf-8"); //$NON-NLS-1$
      rootConfig.save(writer);
    } catch (IOException e) {
      BrowserErrorHandler.processError(e, false);
    } finally {
      FileUtils.close(writer);
    }
  }

  // Restore Tree Model
  public static void restoreModel() {
    XMLMemento memento = null;
    try {
      File configFile = new File(FrameworkPlugin.getDefault().getStateLocation().toFile(), STORAGE_FILE_NAME);
      InputStream input = new FileInputStream(configFile);
      InputStreamReader reader = new InputStreamReader(input, "utf-8"); //$NON-NLS-1$
      memento = XMLMemento.createReadRoot(reader);
    } catch (FileNotFoundException e) {
      // do nothing
    } catch (IOException e) {
      BrowserErrorHandler.processError(e, false);
    } catch (WorkbenchException e) {
      BrowserErrorHandler.processError(e, false);
    }

    if (memento == null) {
      memento = XMLMemento.createWriteRoot(MEMENTO_ROOT_TYPE);
    }

    treeRoot = new TreeRoot(Messages.root_element_name);

    String elementName;
    FrameworkImpl element;
    IMemento[] all = memento.getChildren(MEMENTO_TYPE);
    for (int i = 0; i < all.length; i++) {
      elementName = all[i].getString(FRAMEWORK_NAME);
      if (elementName == null) {
        continue;
      }
      element = new FrameworkImpl(elementName, false);
      element.setConfig(all[i]);
      treeRoot.addElement(element);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.IWorkbenchPart#dispose()
   */
  @Override
  public void dispose() {
    super.dispose();
    activeInstances.remove(new Integer(activePage.hashCode()));

    // final dispose
    if (activeInstances.size() < 1) {
      mgr.dispose();
      tree.getTree().dispose();
      activeInstances = null;
      // treeRoot = null;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.eclipse.ui.IWorkbenchPart#setFocus()
   */
  @Override
  public void setFocus() {
    Control c = tree == null ? null : tree.getTree();
    if (c != null && !c.isDisposed() && !c.isFocusControl()) {
      c.setFocus();
    }
  }

  public static FrameworkImpl[] getFrameworks() {
    if (treeRoot == null) {
      return null;
    }
    Model fwsM[] = treeRoot.getChildren();
    FrameworkImpl fws[] = new FrameworkImpl[fwsM.length];
    System.arraycopy(fwsM, 0, fws, 0, fws.length);
    return fws;
  }

  public static FrameworkImpl[] findFramework(DeviceConnector connector) {
    if (connector == null) {
      return null;
    }
    if (treeRoot == null) {
      return null;
    }
    Model fws[] = treeRoot.getChildren();
    Vector fwVector = new Vector();
    for (int i = 0; i < fws.length; i++) {
      if (((FrameworkImpl) fws[i]).getConnector() != null && ((FrameworkImpl) fws[i]).getConnector().equals(connector)) {
        fwVector.addElement(fws[i]);
      }
    }

    FrameworkImpl fwArr[] = (FrameworkImpl[]) fwVector.toArray(new FrameworkImpl[0]);
    return fwArr;
  }

  private static List actionProviders = new ArrayList();

  public static List getActionsProviders() {
    return actionProviders;
  }

  private void obtainActionProviders() {
    IExtensionRegistry registry = Platform.getExtensionRegistry();
    IExtensionPoint extensionPoint = registry
        .getExtensionPoint("org.tigris.mtoolkit.osgimanagement.contentTypeExtensions");

    obtainActionsProviderElements(extensionPoint.getConfigurationElements(), actionProviders);
  }

  private void obtainActionsProviderElements(IConfigurationElement[] elements, List providers) {
    for (int i = 0; i < elements.length; i++) {
      if (!elements[i].getName().equals("actions")) {
        continue;
      }
      String clazz = elements[i].getAttribute("class");
      if (clazz == null) {
        continue;
      }

      ActionsProviderElement providerElement = new ActionsProviderElement(elements[i]);
      if (providers.contains(providerElement)) {
        continue;
      }

      try {
        Object provider = elements[i].createExecutableExtension("class");

        if (provider instanceof ContentTypeActionsProvider) {
          providerElement.setProvider(((ContentTypeActionsProvider) provider));
          providers.add(providerElement);
        }
      } catch (CoreException e) {
        FrameworkPlugin.error("Exception while intializing action provider elements", e);
      }
    }
  }

  private class FilterJob extends Job implements ISelectionChangedListener {
    private static final int MAX_ITEMS_TO_AUTOEXPAND = 200;
    private ISelection       selection;
    // keeps the expanded elements before applying the filter
    private Object[]         savedExpansionState;
    // keeps the elements, which have revealed on the previous filter run
    private Model[]          lastRunRevealedElements;
    private volatile boolean ignoreSelectionEvents   = false;

    private FilterJob() {
      super("Filtering...");
      setSystem(true);
    }

    // TODO: Add a status line explaining how much elements are there and
    // how much are filtered
    // TODO: Add a text when there are no elements in the filter
    // TODO: Add a text in the filter to explain the purpose of the text
    // line
    @Override
    public IStatus run(final IProgressMonitor monitor) {
      if (tree == null || tree.getControl().isDisposed()) {
        return Status.CANCEL_STATUS;
      }
      tree.addSelectionChangedListener(this);
      filterRecursively(treeRoot);
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      final Model[] allSelectedElements = treeRoot.getSelectedChildrenRecursively();
      final Model[] unrevealedElements = new Model[allSelectedElements.length];
      System.arraycopy(allSelectedElements, 0, unrevealedElements, 0, allSelectedElements.length);
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      int lastRunIdx = 0;
      if (lastRunRevealedElements != null) {
        for (int i = 0; i < unrevealedElements.length; i++) {
          if (lastRunIdx < lastRunRevealedElements.length
              && lastRunRevealedElements[lastRunIdx] == unrevealedElements[i]) {
            // remove elements which have already been expanded
            unrevealedElements[i] = null;
            lastRunIdx++;
          }
        }
      }
      if (monitor.isCanceled()) {
        return Status.CANCEL_STATUS;
      }
      final int lastRunRevealedCount = lastRunIdx;
      Display display = PlatformUI.getWorkbench().getDisplay();
      if (!display.isDisposed()) {
        display.syncExec(new Runnable() {
          public void run() {
            BusyIndicator.showWhile(null, new Runnable() {
              public void run() {
                refreshTree(allSelectedElements, unrevealedElements, lastRunRevealedCount);
              }
            });
          }
        });
      }
      return Status.OK_STATUS;
    }

    private void refreshTree(Model[] allSelectedElements, Model[] unrevealedElements, int alreadyRevealedElementsCount) {
      if (tree == null || tree.getControl().isDisposed()) {
        return;
      }
      if (selection == null) {
        selection = tree.getSelection();
      }
      int itemsToReveal = lastRunRevealedElements != null ? (allSelectedElements.length - alreadyRevealedElementsCount)
          : allSelectedElements.length;
      boolean autoExpand = itemsToReveal < MAX_ITEMS_TO_AUTOEXPAND;
      if (savedExpansionState == null) {
        savedExpansionState = tree.getExpandedElements();
      }
      ignoreSelectionEvents = true;
      try {
        tree.getTree().setRedraw(false);
        boolean refreshed = false;
        if (treeRoot.getFilter().length() > 0) {
          tree.refresh();
          refreshed = true;
          if (lastRunRevealedElements == null || alreadyRevealedElementsCount == 0) {
            // we need to expand everything
            if (autoExpand) {
              lastRunRevealedElements = allSelectedElements;
              tree.expandAll();
              return;
            }
          } else if (autoExpand) {
            for (int i = 0; i < unrevealedElements.length; i++) {
              if (unrevealedElements[i] != null) {
                if (!unrevealedElements[i].containSelectedChilds()) {
                  // expand the element if it doesn't contain
                  // selected children
                  // otherwise we will duplicate the work,
                  // which we will do for the children
                  tree.expandToLevel(unrevealedElements[i], 0);
                }
              }
            }
            lastRunRevealedElements = allSelectedElements;
            return;
          } // else restore original state, too much work otherwise
        }
        if (!refreshed) {
          tree.refresh();
        }
        tree.setExpandedElements(savedExpansionState);
        savedExpansionState = null;
        lastRunRevealedElements = null;
      } finally {
        // TODO: Change this to not set the selection on elements which
        // are out of the filter
        tree.setSelection(selection);
        tree.getTree().setRedraw(true);
        ignoreSelectionEvents = false;
      }
    }

    private void filterRecursively(Model root) {
      root.filter();
      Model[] children = root.getChildren();
      for (int i = 0; i < children.length; i++) {
        filterRecursively(children[i]);
      }
    }

    public void selectionChanged(SelectionChangedEvent event) {
      if (ignoreSelectionEvents) {
        return;
      }
      selection = event.getSelection();
    }
  }

  private void findItem(String text, TreeViewer parentView) {
    if (text.equals("")) {
      return;
    }
    if (notFoundText != null && text.indexOf(notFoundText) != -1) {
      return;
    }

    IStructuredSelection startSelection = (IStructuredSelection) parentView.getSelection();

    Model startNode = (Model) startSelection.getFirstElement();
    if (startNode == null) {
      Model children[] = FrameworksView.treeRoot.getChildren();
      if (children == null || children.length == 0) {
        return;
      }
      startNode = children[0];
    }

    Model foundNode = null;
    Model node = startNode;

    if ((foundNode = findItem(node, text, startNode)) == null) {
      Model parent = node.getParent();
      while (parent != null) {
        int startIndex = parent.indexOf(node) + 1;
        for (int i = startIndex; i < parent.getSize(); i++) {
          node = parent.getChildren()[i];
          if (isTextFound(node.getLabel().toLowerCase(), text)) {
            foundNode = node;
            break;
          }
          foundNode = findItem(node, text, startNode);
          if (foundNode != null) {
            break;
          }
        }
        if (foundNode != null) {
          break;
        }
        node = parent;
        parent = parent.getParent();
      }
    }
    if (foundNode == null && startNode != FrameworksView.treeRoot.getChildren()[0]) {
      node = FrameworksView.treeRoot;
      foundNode = findItem(node, text, startNode);
    }

    boolean itemFound = false;
    if (foundNode == startNode) {
      // if (foundNode.getName().indexOf(text) == -1) {
      // findText.setForeground(red);
      // }
    } else if (foundNode != null) {
      parentView.setSelection(new StructuredSelection(foundNode));
      // findText.setForeground(black);
      itemFound = true;
    } else {
      // findText.setForeground(red);
    }

    if (!itemFound) {
      notFoundText = text;
    } else {
      notFoundText = null;
    }
  }

  private Model findItem(Model parent, String searching, Model startNode) {
    Model children[] = parent.getChildren();
    for (int i = 0; i < children.length; i++) {
      Model child = children[i];
      if (child == startNode) {
        return child;
      }
      String text = child.getLabel();
      if (isTextFound(text.toLowerCase(), searching)) {
        return child;
      }
      Model grandChild = findItem(child, searching, startNode);
      if (grandChild != null) {
        return grandChild;
      }
    }
    return null;
  }

  private boolean isTextFound(String text, String searchFor) {
    return (text.indexOf(searchFor) != -1 && !text.equals(ServicesCategory.nodes[0]) && !text
        .equals(ServicesCategory.nodes[1]));
  }

  public class ActionsProviderElement {
    private String                     extension;
    private String                     clazz;
    private ContentTypeActionsProvider provider;
    private IConfigurationElement      confElement;

    public ActionsProviderElement(IConfigurationElement configurationElement) {
      confElement = configurationElement;
      extension = configurationElement.getAttribute("extension");
      clazz = configurationElement.getAttribute("class");
    }

    public void setProvider(ContentTypeActionsProvider provider) {
      this.provider = provider;
    }

    public IConfigurationElement getConfigurationElement() {
      return confElement;
    }

    public ContentTypeActionsProvider getProvider() {
      return provider;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return super.hashCode();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ActionsProviderElement)) {
        return false;
      }

      final ActionsProviderElement otherElement = (ActionsProviderElement) obj;

      if (clazz == null) {
        if (otherElement.clazz != null) {
          return false;
        }
      } else if (!clazz.equals(otherElement.clazz)) {
        return false;
      }

      if (extension == null) {
        if (otherElement.extension != null) {
          return false;
        }
      } else if (!extension.equals(otherElement.extension)) {
        return false;
      }

      return true;
    }
  }
}

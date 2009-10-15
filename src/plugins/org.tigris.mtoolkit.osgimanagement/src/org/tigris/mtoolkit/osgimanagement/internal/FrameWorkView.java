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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.contexts.Context;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.Scheme;
import org.eclipse.jface.bindings.keys.KeyBinding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.ParseException;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.part.ViewPart;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.osgimanagement.ContentTypeActionsProvider;
import org.tigris.mtoolkit.osgimanagement.ToolbarIMenuCreator;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.browser.model.SimpleNode;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.BrowserErrorHandler;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.FrameworkConnectorFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ListUtil;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Bundle;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.BundlesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Category;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.DeploymentPackage;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ObjectClass;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServicesCategory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.TreeRoot;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.AddAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.BundlePropertiesAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.CommonPropertiesAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ConnectAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.DPPropertiesAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.DeInstallBundleAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.DeInstallDPAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.DisconnectAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.FindAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.GotoServiceAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.InstallBundleAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.InstallDPAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.MenuFactory;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.PropertyAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.RefreshAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.RemoveAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ServicePropertiesAction;
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

public class FrameWorkView extends ViewPart implements ConstantsDistributor, KeyListener {

	public static final String VIEW_ID = FrameworkPlugin.PLUGIN_ID + ".frameworkview";

	public static final String PROPERTIES_IMAGE_PATH = "properties.gif"; //$NON-NLS-1$
	public static final String UPDATE_BUNDLE_IMAGE_PATH = "update_bundle.gif"; //$NON-NLS-1$
	public static final String STOP_BUNDLE_IMAGE_PATH = "stop_bundle.gif"; //$NON-NLS-1$
	public static final String START_BUNDLE_IMAGE_PATH = "start_bundle.gif"; //$NON-NLS-1$
	public static final String UNINSTALL_DP_IMAGE_PATH = "uninstall_dp.gif"; //$NON-NLS-1$
	public static final String UNINSTALL_BUNDLE_IMAGE_PATH = "uninstall_bundle.gif"; //$NON-NLS-1$
	public static final String INSTALL_DP_IMAGE_PATH = "install_dp.gif"; //$NON-NLS-1$
	public static final String INSTALL_BUNDLE_IMAGE_PATH = "install_bundle.gif"; //$NON-NLS-1$
	public static final String DISCONNECT_ACTION_IMAGE_PATH = "disconnect_action.gif"; //$NON-NLS-1$
	public static final String CONNECT_ACTION_IMAGE_PATH = "connect_action.gif"; //$NON-NLS-1$
	public static final String PROPERTIES_ACTION_IMAGE_PATH = "properties_action.gif"; //$NON-NLS-1$
	public static final String REMOVE_ACTION_ACTION_PATH = "remove_action.gif"; //$NON-NLS-1$
	public static final String ADD_ACTION_IMAGE_PATH = "add_action.gif"; //$NON-NLS-1$
	public static final String DP_GROUP_IMAGE_PATH = "dp_group.gif"; //$NON-NLS-1$
	public static final String BUNDLES_GROUP_IMAGE_PATH = "bundles_group.gif"; //$NON-NLS-1$
	public static final String SEARCH_IMAGE_PATH = "search_action.gif";
	public static final String REFRESH_IMAGE_PATH = "refresh_action.gif";
	public static final String CONSOLE_IMAGE_PATH = "console.gif";
	private static final String FIND_COMMAND_ID = FindAction.class.getName();
	private static final String REFRESH_COMMAND_ID = RefreshAction.class.getName();
	private static final String REMOVE_COMMAND_ID = RefreshAction.class.getName();
	
	private static final String PROPERTIES_COMMAND_ID = CommonPropertiesAction.class.getName();

	private static AddAction addAction;
	private static RemoveAction removeAction;
	private static PropertyAction propertyAction;
	private static ConnectAction connectAction;
	private static DisconnectAction disconnectAction;
	private static InstallBundleAction installBundleAction;
	private static InstallDPAction installDPAction;
	private static DeInstallBundleAction deinstallBundleAction;
	private static DeInstallDPAction deinstallDPAction;
	
	private static StartAction startAction;
	private static StopAction stopAction;
	private static UpdateBundleAction updateBundleAction;
	private static CommonPropertiesAction commonPropertiesAction;
	private static ShowServicePropertiesInTree showServPropsInTreeAction;
	private ServicePropertiesAction servicePropertiesAction;
	private GotoServiceAction gotoServiceAction;
	private static ViewAction viewServicesAction;
	private static ViewAction viewBundlesAction;
	private ShowBundleIDAction showBundleIDAction;
	private ShowBundleVersionAction showBundleVersionAction;
	private static DPPropertiesAction dpPropertiesAction;
	private static BundlePropertiesAction bundlePropertiesAction;
	private FindAction findAction;
	private static ShowFrameworkConsole showConsoleAction;
	private static RefreshAction refreshAction;
	private static Text filterField;

	public static TreeViewer tree;
	private IWorkbenchPage activePage;

	public static TreeRoot treeRoot;

	private static HashMap activeInstances;
	private MenuManager mgr;
	
	private FilterJob filterJob = new FilterJob();
	private MyViewerFilter filter = new MyViewerFilter();;
	private static String notFoundText = null;

	// Get current shell
	public static Shell getShell() {
		if (tree != null) {
			return tree.getControl().getShell();
		} else {
			return Display.getDefault().getActiveShell();
		}
	}

	// Get current active FrameWorkView
	public static FrameWorkView getActiveInstance() {
		FrameWorkView resultView = null;
		try {
			IWorkbenchPage activePage = FrameworkPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow()
					.getActivePage();
			resultView = (FrameWorkView) activeInstances.get(new Integer(activePage.hashCode()));
		} catch (NullPointerException e) {
			// do nothing
		}
		return resultView;
	}

	// Get the root containing all frameworks
	public static TreeRoot getTreeRoot() {
		return FrameWorkView.treeRoot;
	}

	// Get the TreeViewer
	public static TreeViewer[] getTreeViewers() {
		if (activeInstances == null || activeInstances.values() == null)
			return null;
		TreeViewer[] result = new TreeViewer[activeInstances.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = FrameWorkView.tree;
		}
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(Composite)
	 */
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
		filterField.setMessage("type filter here");

		filterField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				treeRoot.setFilter(filterField.getText());
				filterJob.cancel();
				filterJob.schedule(300);
			}
		});
		filterField.addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				tree.getTree().forceFocus();
				findItem(filterField.getText().trim(), tree);
			}
		});
		
		GridData gridDataTree = new GridData(GridData.FILL_BOTH);
		tree = new TreeViewer(parent, SWT.MULTI);
		tree.getTree().setLayoutData(gridDataTree);
		tree.getTree().addKeyListener(this);
		tree.addDoubleClickListener(new IDoubleClickListener() {

			public void doubleClick(DoubleClickEvent event) {
				Model node = (Model) ((TreeSelection) event.getSelection()).getFirstElement();
				if (node instanceof FrameWork) {
					FrameworkConnectorFactory.connectFrameWork((FrameWork) node);
				}					
				boolean expand = !tree.getExpandedState(node);
				tree.setExpandedState(node, expand);
			}
		});
		
		tree.getTree().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				boolean doSearch = e.character == Character.LINE_SEPARATOR && !filterField.getText().trim().equals("");
				e.doit = !doSearch;
				if (doSearch) {
					findItem(filterField.getText().trim(), tree);
				}
				
			}
		});
		
		activePage = getSite().getPage();

		if (activeInstances == null)
			activeInstances = new HashMap();

		activeInstances.put(new Integer(activePage.hashCode()), this);

		tree.setContentProvider(new ViewContentProvider());
		tree.setLabelProvider(new ViewLabelProvider());
		tree.addFilter(filter);
		tree.setSorter(ListUtil.BUNDLE_SORTER);
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
					if (b.isNeedUpdate())
						tree.update(b, null);
				}
			}
		});
		tree.getTree().addTreeListener(new TreeAdapter() {
			public void updateItems(TreeItem parent) {
				if (!(parent.getData() instanceof Category) && !(parent.getData() instanceof BundlesCategory)) {
					return;
				}
				TreeItem items[] = null;
				items = parent.getItems();
				if (items == null)
					return;
				for (int i = 0; i < items.length; i++) {
					Bundle b = (Bundle) items[i].getData();
					if (b.isNeedUpdate())
						tree.update(b, null);
				}
			}

			public void treeExpanded(TreeEvent e) {
				updateItems((TreeItem) e.item);
			}
		});

		addContributions();
		createToolbarAndMenu();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.FRAMEWORKS_VIEW);
		createShortcut(FIND_COMMAND_ID, findAction, "Ctrl+F");
		createShortcut(REFRESH_COMMAND_ID, refreshAction, "F5");
		createShortcut(PROPERTIES_COMMAND_ID, commonPropertiesAction, "Alt+Enter");
		createShortcut(REMOVE_COMMAND_ID, removeAction, "DEL");

	}

	public static String getFilter() {
		return filterField.getText();
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
				new ActionContributionItem(startAction), 
				new ActionContributionItem(stopAction), 
				new ActionContributionItem(updateBundleAction), 
				new ActionContributionItem(deinstallBundleAction),
				new ActionContributionItem(bundlePropertiesAction),
				new Separator(), 
				new ActionContributionItem(installBundleAction) };
		ToolbarIMenuCreator bundlesTB = new ToolbarIMenuCreator(items, tree);
		bundlesTB.setImageDescriptor(ImageHolder.getImageDescriptor(BUNDLES_GROUP_IMAGE_PATH));
		bundlesTB.setToolTipText(Messages.BundlesAction_ToolTip);
		toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_DEPLOYMENT, bundlesTB);

		Action[] actions = new Action[] { installDPAction, deinstallDPAction, dpPropertiesAction };
		ToolbarIMenuCreator dpTB = new ToolbarIMenuCreator(actions, tree);
		dpTB.setImageDescriptor(ImageHolder.getImageDescriptor(DP_GROUP_IMAGE_PATH));
		dpTB.setToolTipText(Messages.DPAction_ToolTip);
		toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_DEPLOYMENT, dpTB);

		for (int i = 0; i < actionProviders.size(); i++) {
			ContentTypeActionsProvider provider = ((ActionsProviderElement) actionProviders.get(i)).getProvider();
			provider.fillToolBar(toolBar);
		}

		toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_FRAMEWORK, addAction);
		toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_FRAMEWORK, removeAction);
		commonPropertiesAction.setToolTipText(Messages.property_action_label);
		toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_FRAMEWORK, commonPropertiesAction);

		toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, viewServicesAction);
		toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, viewBundlesAction);
		toolBar.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, showConsoleAction);

		refreshAction.setToolTipText(Messages.refresh_action_label);
		toolBar.add(refreshAction);

		updateContextMenuStates();
	}

	private void createShortcut(String commandName, final Action action, String shortcutCombination) {
		try {
			ICommandService commandService = (ICommandService) getSite().getService(ICommandService.class);
			IHandlerService handlerService = (IHandlerService) getSite().getService(IHandlerService.class);
			IBindingService bindingService = (IBindingService) getSite().getService(IBindingService.class);
			IContextService contextService = (IContextService) getSite().getService(IContextService.class);

			org.eclipse.core.commands.Category editCat = commandService.getCategory("org.eclipse.ui.category.edit"); //$NON-NLS-1$
			Command scmd = commandService.getCommand(commandName);
			if (!scmd.isDefined()) {
				scmd.define(Messages.find_action_label, Messages.find_action_run_string, editCat);
			}

			IHandler handler = new AbstractHandler() {
				public Object execute(ExecutionEvent event) throws ExecutionException {
					action.run();
					return null;
				}
			};

			handlerService.activateHandler(commandName, handler);

			// now set up the keybindings
			String sampleContextId = "sampleViewContext"; //$NON-NLS-1$
			String parentContextId = "org.eclipse.ui.contexts.window"; //$NON-NLS-1$

			Context sampleContext = contextService.getContext(sampleContextId);
			if (!sampleContext.isDefined()) {
				sampleContext.define(Messages.in_frameworks_view, Messages.find_in_frameworks_view, parentContextId);
			}
			contextService.activateContext(sampleContextId);

			String defaultSchemeId = "org.eclipse.ui.defaultAcceleratorConfiguration"; //$NON-NLS-1$
			Scheme defaultScheme = bindingService.getScheme(defaultSchemeId);

			ParameterizedCommand pscmd = new ParameterizedCommand(scmd, null);

			KeySequence keySequence = KeySequence.getInstance(shortcutCombination); //$NON-NLS-1$
			Binding newKey = new KeyBinding(keySequence, pscmd, defaultSchemeId, sampleContextId, null, null, null,
					Binding.USER);

			Binding[] bindings = bindingService.getBindings();
			boolean found = false;
			for (int i = 0; i < bindings.length; i++) {
				if (bindings[i].getParameterizedCommand() != null
						&& bindings[i].getParameterizedCommand().getId().equals(commandName)) {
					found = true;
					break;
				}
			}
			if (!found) {
				Binding[] newBindings = new Binding[bindings.length + 1];
				newBindings[0] = newKey;
				System.arraycopy(bindings, 0, newBindings, 1, bindings.length);
				bindingService.savePreferences(defaultScheme, newBindings);
			}
		} catch (ParseException e) {
			BrowserErrorHandler.processError(e, false);
		} catch (IOException e) {
			BrowserErrorHandler.processError(e, false);
		}
	}

	// Disposes all Frameworks
	public void clearAll() {
		Model[] children = treeRoot.getChildren();
		FrameWork child;
		for (int i = 0; i < treeRoot.getSize(); i++) {
			child = (FrameWork) children[i];
			child.dispose();
			treeRoot.removeElement(child);
		}
	}

	// Create custom contributions - tree popup menu
	protected void addContributions() {

		addAction = new AddAction(tree, Messages.add_action_label);
		addAction.setImageDescriptor(ImageHolder.getImageDescriptor(ADD_ACTION_IMAGE_PATH));

		removeAction = new RemoveAction(tree, Messages.remove_action_label);
		removeAction.setImageDescriptor(ImageHolder.getImageDescriptor(REMOVE_ACTION_ACTION_PATH));
		removeAction.setAccelerator(SWT.DEL);

		propertyAction = new PropertyAction(tree, Messages.property_action_label);
		propertyAction.setImageDescriptor(ImageHolder.getImageDescriptor(PROPERTIES_ACTION_IMAGE_PATH));

		connectAction = new ConnectAction(tree, Messages.connect_action_label);
		connectAction.setImageDescriptor(ImageHolder.getImageDescriptor(CONNECT_ACTION_IMAGE_PATH));

		disconnectAction = new DisconnectAction(tree, Messages.disconnect_action_label);
		disconnectAction.setImageDescriptor(ImageHolder.getImageDescriptor(DISCONNECT_ACTION_IMAGE_PATH));

		installBundleAction = new InstallBundleAction(tree, Messages.install_action_label);
		installBundleAction.setImageDescriptor(ImageHolder.getImageDescriptor(INSTALL_BUNDLE_IMAGE_PATH));

		installDPAction = new InstallDPAction(tree, Messages.install_dp_action_label);
		installDPAction.setImageDescriptor(ImageHolder.getImageDescriptor(INSTALL_DP_IMAGE_PATH));

		deinstallBundleAction = new DeInstallBundleAction(tree, Messages.deinstall_action_label);
		deinstallBundleAction.setImageDescriptor(ImageHolder.getImageDescriptor(UNINSTALL_BUNDLE_IMAGE_PATH));

		deinstallDPAction = new DeInstallDPAction(tree, Messages.deinstall_dp_action_label);
		deinstallDPAction.setImageDescriptor(ImageHolder.getImageDescriptor(UNINSTALL_DP_IMAGE_PATH));

		startAction = new StartAction(tree, Messages.start_action_label);
		startAction.setImageDescriptor(ImageHolder.getImageDescriptor(START_BUNDLE_IMAGE_PATH));

		stopAction = new StopAction(tree, Messages.stop_action_label);
		stopAction.setImageDescriptor(ImageHolder.getImageDescriptor(STOP_BUNDLE_IMAGE_PATH));

		updateBundleAction = new UpdateBundleAction(tree, Messages.update_action_label);
		updateBundleAction.setImageDescriptor(ImageHolder.getImageDescriptor(UPDATE_BUNDLE_IMAGE_PATH));

		servicePropertiesAction = new ServicePropertiesAction(tree, Messages.property_action_label);
		servicePropertiesAction.setImageDescriptor(ImageHolder.getImageDescriptor(PROPERTIES_IMAGE_PATH));
		
		gotoServiceAction = new GotoServiceAction(tree, Messages.goto_service_action_label);
		viewServicesAction = new ViewAction(tree, Messages.services_view_action_label, tree, FrameWork.SERVICES_VIEW);
		viewServicesAction.setImageDescriptor(ImageHolder.getImageDescriptor(ViewLabelProvider.SERVICES_CATEGORY_ICON));
		viewBundlesAction = new ViewAction(tree, Messages.bundles_view_action_label, tree, FrameWork.BUNDLES_VIEW);
		viewBundlesAction.setImageDescriptor(ImageHolder.getImageDescriptor(BUNDLES_GROUP_IMAGE_PATH));

		showBundleIDAction = new ShowBundleIDAction(tree, Messages.show_bundle_id_action_label, tree, getTreeRoot());
		showBundleVersionAction = new ShowBundleVersionAction(tree, Messages.show_bundle_version_action_label, tree, getTreeRoot());

		showServPropsInTreeAction = new ShowServicePropertiesInTree(tree, Messages.show_service_properties_in_tree);

		dpPropertiesAction = new DPPropertiesAction(tree, Messages.property_action_label);
		dpPropertiesAction.setImageDescriptor(ImageHolder.getImageDescriptor(PROPERTIES_IMAGE_PATH));

		bundlePropertiesAction = new BundlePropertiesAction(tree, Messages.property_action_label);
		bundlePropertiesAction.setImageDescriptor(ImageHolder.getImageDescriptor(PROPERTIES_IMAGE_PATH));

		commonPropertiesAction = new CommonPropertiesAction(tree, Messages.property_action_label);
		commonPropertiesAction.setImageDescriptor(ImageHolder.getImageDescriptor(PROPERTIES_ACTION_IMAGE_PATH));
		commonPropertiesAction.setAccelerator(SWT.ALT | SWT.TRAVERSE_RETURN);

		findAction = new FindAction(tree, filterField, Messages.find_action_label);
		findAction.setImageDescriptor(ImageHolder.getImageDescriptor(SEARCH_IMAGE_PATH));
		findAction.setAccelerator(SWT.CTRL | 'F');
		showConsoleAction = new ShowFrameworkConsole(tree, Messages.show_framework_console, tree);
		showConsoleAction.setImageDescriptor(ImageHolder.getImageDescriptor(CONSOLE_IMAGE_PATH));

		refreshAction = new RefreshAction(tree, Messages.refresh_action_label,
				Messages.refresh_action_label, tree);
		refreshAction.setAccelerator(SWT.F5);
		refreshAction.setImageDescriptor(ImageHolder.getImageDescriptor(REFRESH_IMAGE_PATH));

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
	}

	public static void updateContextMenuStates() {
		if (tree == null)
			return;
		IStructuredSelection selection = (IStructuredSelection) tree.getSelection();

		connectAction.updateState(selection);
		disconnectAction.updateState(selection);
		refreshAction.updateState(selection);

		startAction.updateState(selection);
		stopAction.updateState(selection);
		updateBundleAction.updateState(selection);
		deinstallBundleAction.updateState(selection);
		bundlePropertiesAction.updateState(selection);

		installBundleAction.updateState(selection);
		installDPAction.updateState(selection);

		deinstallDPAction.updateState(selection);
		dpPropertiesAction.updateState(selection);

		addAction.setEnabled(true);
		removeAction.updateState(selection);
		propertyAction.updateState(selection);
		
		viewBundlesAction.updateState(selection);
		viewServicesAction.updateState(selection);
		showConsoleAction.updateState(selection);
	}

	// Fill context menu Actions when menu is about to show
	protected void fillContextMenu(IMenuManager manager) {
		tree.setSelection(tree.getSelection());
		StructuredSelection selection = (StructuredSelection) tree.getSelection();
		boolean homogen = true;

		manager.add(new Separator(ContentTypeActionsProvider.GROUP_UNSIGNED));
		manager.add(new Separator(ContentTypeActionsProvider.GROUP_ACTIONS));
		manager.add(new Separator(ContentTypeActionsProvider.GROUP_INSTALL));
		manager.add(new Separator(ContentTypeActionsProvider.GROUP_OPTIONS));
		manager.add(new Separator(ContentTypeActionsProvider.GROUP_DEFAULT));
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
				if (element instanceof FrameWork) {
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, connectAction);
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, disconnectAction);
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, removeAction);
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, refreshAction);

					manager.appendToGroup(ContentTypeActionsProvider.GROUP_INSTALL, installBundleAction);
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_INSTALL, installDPAction);
				}
				if (element instanceof TreeRoot) {
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_UNSIGNED, addAction);
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

				if (element instanceof ObjectClass) {
					if (!(element.getParent() instanceof FrameWork)) {
						manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, gotoServiceAction);
					}
				}
				if (element instanceof SimpleNode) {
					if (element.getName().equals(Messages.bundles_node_label)) {
						manager.appendToGroup(ContentTypeActionsProvider.GROUP_INSTALL, installBundleAction);
					}
					if (element.getName().equals(Messages.dpackages_node_label)) {
						manager.appendToGroup(ContentTypeActionsProvider.GROUP_INSTALL, installDPAction);
					}
				}
				if (element instanceof DeploymentPackage) {
					manager.appendToGroup(ContentTypeActionsProvider.GROUP_ACTIONS, deinstallDPAction);
					// manager.add(dpPropertiesAction);
				}
			}
		} else {
			manager.appendToGroup(ContentTypeActionsProvider.GROUP_UNSIGNED, addAction);
		}
		for (int i = 0; i < actionProviders.size(); i++) {
			ContentTypeActionsProvider provider = ((ActionsProviderElement) actionProviders.get(i)).getProvider();
			provider.menuAboutToShow(selection, manager);
		}

		// call menuAboutToShow
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		manager.appendToGroup(ContentTypeActionsProvider.GROUP_DEFAULT, findAction);
		manager.appendToGroup(ContentTypeActionsProvider.GROUP_DEFAULT, showConsoleAction);
		if (selection.size() > 0 && homogen) {
			Model element = (Model) selection.getFirstElement();
			if (element instanceof FrameWork)
				manager.appendToGroup(ContentTypeActionsProvider.GROUP_PROPERTIES, propertyAction);
			if (element instanceof Bundle)
				manager.appendToGroup(ContentTypeActionsProvider.GROUP_PROPERTIES, bundlePropertiesAction);
			if (element instanceof DeploymentPackage)
				manager.appendToGroup(ContentTypeActionsProvider.GROUP_PROPERTIES, dpPropertiesAction);
			if (element instanceof ObjectClass)
				manager.appendToGroup(ContentTypeActionsProvider.GROUP_PROPERTIES, servicePropertiesAction);
		}
	}

	// Save Tree Model
	protected static void saveModel() {
		XMLMemento rootConfig = XMLMemento.createWriteRoot(MEMENTO_ROOT_TYPE);

		IMemento child;
		Model[] children = treeRoot.getChildren();
		for (int i = 0; i < treeRoot.getSize(); i++) {
			if (!((FrameWork) children[i]).autoConnected) {
				IMemento config = ((FrameWork) children[i]).getConfig();
				if (config != null) {
					child = rootConfig.createChild(MEMENTO_TYPE);
					child.putMemento(config);
				}
			}
		}
		try {
			if (FrameworkPlugin.getDefault() == null)
				return;
			File configFile = new File(FrameworkPlugin.getDefault().getStateLocation().toFile(), STORAGE_FILE_NAME);

			FileOutputStream stream = new FileOutputStream(configFile);
			OutputStreamWriter writer = new OutputStreamWriter(stream, "utf-8"); //$NON-NLS-1$
			rootConfig.save(writer);
			writer.close();
		} catch (IOException e) {
			BrowserErrorHandler.processError(e, false);
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
		FrameWork element;
		IMemento[] all = memento.getChildren(MEMENTO_TYPE);
		for (int i = 0; i < all.length; i++) {
			elementName = all[i].getString(FRAMEWORK_NAME);
			if (elementName == null)
				continue;
			element = new FrameWork(elementName, false);
			element.setConfig(all[i]);
			treeRoot.addElement(element);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	public void dispose() {
		super.dispose();
		activeInstances.remove(new Integer(activePage.hashCode()));

		// final dispose
		if (activeInstances.size() < 1) {
			saveModel();
			// clearAll();
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
	public void setFocus() {
	}

	public static FrameWork[] getFrameworks() {
		if (treeRoot == null)
			return null;
		Model fwsM[] = treeRoot.getChildren();
		FrameWork fws[] = new FrameWork[fwsM.length];
		System.arraycopy(fwsM, 0, fws, 0, fws.length);
		return fws;
	}

	public static FrameWork findFramework(String fwName) {
		if (treeRoot == null)
			return null;
		Model fws[] = treeRoot.getChildren();
		for (int i = 0; i < fws.length; i++) {
			if (fws[i].getName().equals(fwName)) {
				return (FrameWork) fws[i];
			}
		}
		return null;
	}
	
	public static FrameWork findFramework(DeviceConnector connector) {
		if (connector == null) return null;
		if (treeRoot == null)
			return null;
		Model fws[] = treeRoot.getChildren();
		for (int i = 0; i < fws.length; i++) {
			if (((FrameWork)fws[i]).getConnector() == connector) {
				return (FrameWork) fws[i];
			}
		}
		return null;
	}

	public void keyPressed(KeyEvent e) {
		if (e.keyCode == SWT.DEL) {
			removeAction.updateState((IStructuredSelection) tree.getSelection());
			if (removeAction.isEnabled()) {
				removeAction.run();
			}
		}
	}

	public void keyReleased(KeyEvent e) {
	}

	private List actionProviders = new ArrayList();

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
			if (providers.contains(providerElement))
				continue;

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
		private ISelection selection;
		// keeps the expanded elements before applying the filter
		private Object[] savedExpansionState;
		// keeps the elements, which have revealed on the previous filter run
		private Model[] lastRunRevealedElements;
		private int lastRunSelectedElementsCount;
		private volatile boolean ignoreSelectionEvents = false;

		private FilterJob() {
			super("Filtering...");
			setSystem(true);
		}

		// TODO: Add a status line explaining how much elements are there and how much are filtered
		// TODO: Add a text when there are no elements in the filter
		// TODO: Add a text in the filter to explain the purpose of the text line
		protected IStatus run(final IProgressMonitor monitor) {
			tree.addSelectionChangedListener(this);
			filterRecursively(treeRoot);
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			final Model[] allSelectedElements = treeRoot.getSelectedChildrenRecursively();
			final Model[] unrevealedElements = new Model[allSelectedElements.length];
			System.arraycopy(allSelectedElements, 0, unrevealedElements, 0, allSelectedElements.length);
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			int lastRunIdx = 0;
			if (lastRunRevealedElements != null) {
				for (int i = 0; i < unrevealedElements.length; i++) {
					if (lastRunIdx < lastRunRevealedElements.length && lastRunRevealedElements[lastRunIdx] == unrevealedElements[i]) {
						// remove elements which have already been expanded
						unrevealedElements[i] = null;
						lastRunIdx++;
					}
				}
			}
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			System.out.println("Filtered: lastRun: " + lastRunSelectedElementsCount + "; now: " + treeRoot.getSelectedChildren() + "(" + allSelectedElements.length + "); revealed: " + lastRunIdx);
			this.lastRunSelectedElementsCount = treeRoot.getSelectedChildren();
			final int lastRunRevealedCount = lastRunIdx;
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					BusyIndicator.showWhile(null, new Runnable() {
						public void run() {
							refreshTree(allSelectedElements, unrevealedElements, lastRunRevealedCount);
						}
					});
				}
			});
			return Status.OK_STATUS;
		}
		
		private void refreshTree(Model[] allSelectedElements, Model[] unrevealedElements, int alreadyRevealedElementsCount) {
			if (selection == null)
				selection = tree.getSelection();
			int itemsToReveal = lastRunRevealedElements != null ? (allSelectedElements.length - alreadyRevealedElementsCount) : allSelectedElements.length;
			boolean autoExpand = itemsToReveal < MAX_ITEMS_TO_AUTOEXPAND;
			System.out.println("all: " + allSelectedElements.length + "; previous: " + (lastRunRevealedElements != null) + "; expanded: " + alreadyRevealedElementsCount + "; items: " + itemsToReveal + "; auto: " + autoExpand);
			if (savedExpansionState == null) {
				savedExpansionState = tree.getExpandedElements();
			}
			ignoreSelectionEvents = true;
			tree.getTree().setRedraw(false);
			try {
				boolean refreshed = false;
				if (treeRoot.getFilter() != "") {
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
									// expand the element if it doesn't contain selected children
									// otherwise we will duplicate the work, which we will do for the children
									tree.expandToLevel(unrevealedElements[i], 0);
								}
							}
						}
						lastRunRevealedElements = allSelectedElements;
						return;
					} // else restore original state, too much work otherwise
				}
				// in the default case restore the tree state
				// collapse the tree, because the refresh is much faster
				tree.collapseAll();
				if (!refreshed)
					tree.refresh();
				tree.setExpandedElements(savedExpansionState);
				savedExpansionState = null;
				lastRunRevealedElements = null;
			} finally {
				// TODO: Change this to not set the selection on elements which are out of the filter
				tree.setSelection(selection, true);
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
			if (ignoreSelectionEvents)
				return;
			selection = event.getSelection();
		}
	}
	
	private static void findItem(String text, TreeViewer parentView) {
		if (text.equals(""))return; //$NON-NLS-1$
		if (notFoundText != null && text.indexOf(notFoundText) != -1) {
			return;
		}

		IStructuredSelection startSelection = (IStructuredSelection) parentView.getSelection();

		Model startNode = (Model) startSelection.getFirstElement();
		if (startNode == null) {
			Model children[] = FrameWorkView.treeRoot.getChildren();
			if (children == null || children.length == 0)
				return;
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
					if (isTextFound(node.getName(), text)) {
						foundNode = node;
						break;
					}
					foundNode = findItem(node, text, startNode);
					if (foundNode != null)
						break;
				}
				if (foundNode != null)
					break;
				node = parent;
				parent = parent.getParent();
			}
		}
		if (foundNode == null && startNode != FrameWorkView.treeRoot.getChildren()[0]) {
			node = FrameWorkView.treeRoot;
			foundNode = findItem(node, text, startNode);
		}

		boolean itemFound = false;
		if (foundNode == startNode) {
//			if (foundNode.getName().indexOf(text) == -1) {
//				findText.setForeground(red);
//			}
		} else if (foundNode != null) {
			parentView.setSelection(new StructuredSelection(foundNode));
//			findText.setForeground(black);
			itemFound = true;
		} else {
//			findText.setForeground(red);
		}

		if (!itemFound)
			notFoundText = text;
		else
			notFoundText = null;
	}

	private static Model findItem(Model parent, String searching, Model startNode) {
		Model children[] = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			Model child = children[i];
			if (child == startNode) {
				return child;
			}
			String text = child.getName();
			if (isTextFound(text, searching)) {
				return child;
			}
			Model grandChild = findItem(child, searching, startNode);
			if (grandChild != null) {
				return grandChild;
			}
		}
		return null;
	}

	private static boolean isTextFound(String text, String searchFor) {
		return (text.indexOf(searchFor) != -1 && !text.equals(ServicesCategory.nodes[0]) && !text.equals(ServicesCategory.nodes[1]));
	}


	public class ActionsProviderElement {
		private String extension;
		private String clazz;
		private ContentTypeActionsProvider provider;
		private IConfigurationElement confElement;

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

		public boolean equals(ActionsProviderElement otherElement) {
			if (this.clazz.equals(otherElement.clazz) && this.extension.equals(otherElement.extension))
				return true;
			return false;
		}
	}

}
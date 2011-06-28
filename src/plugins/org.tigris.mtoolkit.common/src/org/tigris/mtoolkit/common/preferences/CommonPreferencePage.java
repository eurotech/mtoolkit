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
package org.tigris.mtoolkit.common.preferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.UtilitiesPlugin;

public class CommonPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private List preferencePages;

	public static final String PREFIX = "org.tigris.mtoolkit.";
	public static final String PROPERTY_PREFERENCES = PREFIX
			+ "property_preferences_context"; //$NON-NLS-1$

	private static class PreferencePageWrapper implements Comparable {
		private IConfigurationElement element;
		private IMToolkitPreferencePage page;
		private boolean valid = true;
		private boolean indexParsed;
		private int index = Integer.MAX_VALUE;

		public PreferencePageWrapper(IConfigurationElement element) {
			Assert.isNotNull(element);
			this.element = element;
		}

		public IMToolkitPreferencePage getPage() {
			Assert.isNotNull(element);
			if (page == null && valid) {
				try {
					Object page = element.createExecutableExtension("class");
					if (page instanceof IMToolkitPreferencePage) {
						this.page = (IMToolkitPreferencePage) page;
					} else {
						// TODO: Enhance the message with the id of the
						// extension
						UtilitiesPlugin.getDefault().getLog().log(new Status(IStatus.ERROR,
							UtilitiesPlugin.PLUGIN_ID,
							"Contributed extension doesn't implement proper class"));
					}
				} catch (CoreException e) {
					UtilitiesPlugin.getDefault().getLog().log(e.getStatus());
				} finally {
					if (this.page == null) {
						this.valid = false;
					}
				}
			}
			return page;
		}

		public String getName() {
			Assert.isNotNull(element);
			String name = element.getAttribute("name");
			if (name == null)
				name = element.getAttribute("id");
			if (name == null)
				name = "Unknown";
			return name;
		}

		private int getIndex() {
			if (!indexParsed) {
				if (element != null) {
					indexParsed = true;
					String indexStr = element.getAttribute("index");
					if (indexStr != null) {
						try {
							index = Integer.parseInt(indexStr);
						} catch (NumberFormatException e) {
						}
					}
				}
			}
			return index;
		}

		public int compareTo(Object o) {
			PreferencePageWrapper wrapper = (PreferencePageWrapper) o;
			int i2 = wrapper.getIndex();
			int i1 = this.getIndex();
			if (i2 > i1)
				return -1;
			if (i2 < i1)
				return 1;
			String name2 = wrapper.getName();
			String name1 = this.getName();
			return name1.compareTo(name2);
		}
	}

	public CommonPreferencePage() {
		setPreferenceStore(UtilitiesPlugin.getDefault().getPreferenceStore());
		initializePreferencePages();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

	private void initializePreferencePages() {
		if (preferencePages == null) {
			preferencePages = new ArrayList();

			IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.tigris.mtoolkit.common.preferencesPages");
			for (int i = 0; i < elements.length; i++) {
				IConfigurationElement element = elements[i];
				preferencePages.add(new PreferencePageWrapper(element));
			}

			// sort them
			Collections.sort(preferencePages);
		}
	}

	protected Control createContents(Composite parent) {
		GridData gd;
		final Composite prefPane = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		prefPane.setLayout(layout);

		for (Iterator it = preferencePages.iterator(); it.hasNext();) {
			PreferencePageWrapper page = (PreferencePageWrapper) it.next();
			if (page.getPage() == null)
				continue;
			page.getPage().setContainer(getContainer());

			Label pageName = new Label(prefPane, SWT.NONE);
			pageName.setText(page.getName());
			final Font bold = getBoldFont(pageName.getFont());
			pageName.setFont(bold);
			parent.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					bold.dispose();
				}
			});
			gd = new GridData(GridData.FILL_HORIZONTAL);
			pageName.setLayoutData(gd);

			final Composite externalPrefPage = new Composite(prefPane, SWT.NONE);
			gd = new GridData(GridData.FILL_HORIZONTAL);
			externalPrefPage.setLayoutData(gd);
			layout = new GridLayout();
			layout.verticalSpacing = 5;
			layout.horizontalSpacing = 5;
			layout.marginHeight = 10;
			layout.marginWidth = 10;
			externalPrefPage.setLayout(layout);
			
			Control result = page.getPage().createContents(externalPrefPage);
			if (result != null)
				result.setLayoutData(new GridData(GridData.FILL_BOTH));
		}
		PlatformUI.getWorkbench().getHelpSystem()
				.setHelp(parent, PROPERTY_PREFERENCES);

		return prefPane;
	}

	public Font getBoldFont(Font font) {
		FontData data[] = font.getFontData();
		FontData boldData[] = new FontData[data.length];

		for (int i = 0; i < data.length; i++) {
			FontData fontData = data[i];
			boldData[i] = new FontData(fontData.getName(), fontData.getHeight(), fontData.getStyle() | SWT.BOLD);
		}
		return new Font(Display.getCurrent(), boldData);
	}

	public boolean isValid() {
		boolean status = true;
		for (Iterator it = preferencePages.iterator(); it.hasNext();) {
			PreferencePageWrapper page = (PreferencePageWrapper) it.next();
			if (page.getPage() == null)
				continue;
			status &= page.getPage().isValid();
		}
		return status;
	}

	public boolean performCancel() {
		boolean status = true;
		for (Iterator it = preferencePages.iterator(); it.hasNext();) {
			PreferencePageWrapper page = (PreferencePageWrapper) it.next();
			if (page.getPage() == null)
				continue;
			status &= page.getPage().performCancel();
		}
		return status;
	}

	protected void performDefaults() {
		for (Iterator it = preferencePages.iterator(); it.hasNext();) {
			PreferencePageWrapper page = (PreferencePageWrapper) it.next();
			if (page.getPage() == null)
				continue;
			page.getPage().performDefaults();
		}
	}

	public boolean performOk() {
		boolean status = true;
		for (Iterator it = preferencePages.iterator(); it.hasNext();) {
			PreferencePageWrapper page = (PreferencePageWrapper) it.next();
			if (page.getPage() == null)
				continue;
			status &= page.getPage().performOk();
		}
		return status;
	}

	public String getErrorMessage() {
		for (Iterator it = preferencePages.iterator(); it.hasNext();) {
			PreferencePageWrapper page = (PreferencePageWrapper) it.next();
			if (page.getPage() == null) {
				continue;
			}
			String errorMsg = page.getPage().getErrorMessage();
			if (errorMsg != null && errorMsg.length() > 0) {
				return errorMsg;
			}
		}
		return null;
	}

	public String getMessage() {
		IMessageProvider messageProv = getMessageProvider();
		return messageProv != null ? messageProv.getMessage() : null;
	}

	private IMessageProvider getMessageProvider() {
		IMessageProvider selectedMessageProv = null;
		int selectedMessageType = NONE;
		String selectedMessage = null;
		for (Iterator it = preferencePages.iterator(); it.hasNext();) {
			PreferencePageWrapper page = (PreferencePageWrapper) it.next();
			if (page.getPage() == null)
				continue;
			if (!(page.getPage() instanceof IMessageProvider))
				continue;
			IMessageProvider messageProv = (IMessageProvider) page.getPage();
			if (selectedMessageProv == null) {
				selectedMessageProv = messageProv;
				continue;
			}
			
			int messageType = messageProv.getMessageType();
			if (selectedMessageType < messageType) {
				selectedMessageProv = messageProv;
				selectedMessageType = messageType;
				selectedMessage = messageProv.getMessage();
			} else if (selectedMessageType == messageType) {
				String message = messageProv.getMessage();
				if ((selectedMessage == null || selectedMessage.length() == 0) 
								&& message != null && message.length() > 0) {
					selectedMessageProv = messageProv;
					selectedMessageType = messageType;
					selectedMessage = messageProv.getMessage();
				}
			}
		}
		return selectedMessageProv;
	}
	
	public int getMessageType() {
		IMessageProvider messageProv = getMessageProvider();
		return messageProv != null ? messageProv.getMessageType() : NONE;
	}
	
	
}
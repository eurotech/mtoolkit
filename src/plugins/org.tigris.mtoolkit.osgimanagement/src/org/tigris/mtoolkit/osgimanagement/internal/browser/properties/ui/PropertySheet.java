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
package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.osgimanagement.internal.IHelpContextIds;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.logic.ConstantsDistributor;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameWork;
import org.tigris.mtoolkit.osgimanagement.internal.browser.properties.logic.PropertySheetLogic;

public class PropertySheet extends Window implements ControlListener, ConstantsDistributor {

	private PropertySheetLogic logic;

	private Text textServer;
	private Text textIP;

	public Button connectButton;
	public Button okButton;
	public Button cancelButton;

	private Composite bottomButtonsHolder;
	private FrameWork fw;

	// Constructor
	public PropertySheet(TreeViewer parentView, FrameWork element, boolean firstTime) {
		super(parentView.getControl().getShell());
		logic = new PropertySheetLogic(parentView, element, firstTime, this);
		this.setShellStyle(SWT.RESIZE | SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
		fw = element;
	}

	// Create page contents
	protected Control createContents(Composite parent) {

		parent.getShell().setText(Messages.framework_properties_title);
		getShell().addControlListener(this);

		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout());
		control.setLayoutData(new GridData(GridData.FILL_BOTH));

		// Connect properties group
		Group connectPropertiesGroup = new Group(control, SWT.NONE);
		connectPropertiesGroup.setText(Messages.connect_properties_group_label);
		connectPropertiesGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
		GridLayout connectPropertiesGrid = new GridLayout();
		connectPropertiesGrid.numColumns = 3;
		connectPropertiesGrid.makeColumnsEqualWidth = true;
		connectPropertiesGroup.setLayout(connectPropertiesGrid);

		createLabel(Messages.framework_name_label, connectPropertiesGroup);
		textServer = createText(2, connectPropertiesGroup);
		createLabel(Messages.framework_ip_label, connectPropertiesGroup);
		textIP = createText(2, connectPropertiesGroup);
		textIP.setText(DEFAULT_IP);

		connectButton = createCheckboxButton(Messages.connect_button_label, control);
		connectButton.setEnabled(!fw.isConnected());

		// Bottom buttons group
		bottomButtonsHolder = new Composite(control, SWT.NONE);
		bottomButtonsHolder.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout bottomButtonsGrid = new GridLayout();
		bottomButtonsGrid.numColumns = 4;
		bottomButtonsHolder.setLayout(bottomButtonsGrid);

		okButton = createButton(Messages.ok_button_label, bottomButtonsHolder);
		cancelButton = createButton(Messages.cancel_button_label, bottomButtonsHolder);
		getShell().setDefaultButton(okButton);

		logic.sheetLoaded();

		PlatformUI.getWorkbench().getHelpSystem().setHelp(control, IHelpContextIds.PROPERTY_FRAMEWORK);

		return control;
	}

	// Initialize ui values from storage
	public void initValues(IMemento config) {
		logic.setValue(textServer, FRAMEWORK_ID);
		logic.setValue(textIP, FRAMEWORK_IP_ID);
		if (logic.setValue(connectButton, CONNECT_TO_FRAMEWORK)) {
			// connectButton.setVisible(false);
			Composite parent = connectButton.getParent();
			Control[] children = parent.getChildren();
			for (int i = 0; i < children.length; i++) {
				if (children[i].equals(connectButton)) {
					children[i].dispose();
					break;
					// children[i] = null;
				}
			}
			parent.pack();
		}
	}

	// Save ui values to storage and update target element
	public void saveValues(IMemento config) {
		config.putString(FRAMEWORK_ID, textServer.getText());
		config.putString(FRAMEWORK_IP_ID, textIP.getText());
		if (!connectButton.isDisposed())
			config.putBoolean(CONNECT_TO_FRAMEWORK, connectButton.getSelection());
	}

	// Get currently entered server name
	public String getNewName() {
		return textServer.getText();
	}

	public String getNewIP() {
		return textIP.getText();
	}

	public void setIPEditable(boolean editable) {
		textIP.setEditable(editable);
	}

	// Create Label
	private Label createLabel(String text, Composite parent) {
		Label resultLabel = new Label(parent, SWT.NONE);
		GridData grid = new GridData();

		resultLabel.setText(text);
		resultLabel.setLayoutData(grid);

		return resultLabel;
	}

	private Button createCheckboxButton(String label, Composite parent) {
		Button resultButton = new Button(parent, SWT.CHECK);
		GridData grid = new GridData(GridData.FILL_HORIZONTAL);

		resultButton.setText(label);
		resultButton.setLayoutData(grid);
		resultButton.addSelectionListener(logic);
		resultButton.setSelection(false);

		return resultButton;
	}

	// Create Button
	private Button createButton(String label, Composite parent) {
		Button resultButton = new Button(parent, SWT.PUSH);
		GridData grid = new GridData(GridData.FILL_HORIZONTAL);

		resultButton.setText(label);
		resultButton.setLayoutData(grid);
		resultButton.addSelectionListener(logic);

		return resultButton;
	}

	private Text createText(int horizSpan, Composite parent) {
		Text resultText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		GridData grid = new GridData(GridData.FILL_HORIZONTAL);

		grid.horizontalSpan = horizSpan;
		resultText.setLayoutData(grid);

		return resultText;
	}

	// Override to give the window correct size
	protected Point getInitialSize() {
		Point preferedSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		return (preferedSize);
	}

	public void controlMoved(ControlEvent e) {
		// do nothing
	}

	public void controlResized(ControlEvent e) {
		bottomButtonsHolder.layout();
	}
}
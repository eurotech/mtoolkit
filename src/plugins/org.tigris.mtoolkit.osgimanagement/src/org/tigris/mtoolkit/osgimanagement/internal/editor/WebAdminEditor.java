package org.tigris.mtoolkit.osgimanagement.internal.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

public class WebAdminEditor extends EditorPart {

	private Browser browser;

	public void doSave(IProgressMonitor monitor) {
	}

	public void doSaveAs() {
	}

	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	public boolean isDirty() {
		return false;
	}

	public boolean isSaveAsAllowed() {
		return false;
	}

	public void createPartControl(Composite parent) {
		Composite root = new Composite(parent, SWT.NONE);
		root.setLayoutData(new GridData(GridData.FILL_BOTH));
		root.setLayout(new GridLayout());
		browser = new Browser(root, 0);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		setURL(((WebAdminInput)getEditorInput()).getURL());
	}

	public void setFocus() {
	}
	
	public void setURL(String url) {
		browser.setUrl(url);
		setPartName(url);
	}
	
	public void refresh() {
		setURL(((WebAdminInput)getEditorInput()).getURL());
	}
}

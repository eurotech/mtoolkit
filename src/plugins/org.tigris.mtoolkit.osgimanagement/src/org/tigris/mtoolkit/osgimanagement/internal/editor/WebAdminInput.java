package org.tigris.mtoolkit.osgimanagement.internal.editor;

import org.eclipse.ui.internal.part.NullEditorInput;

public class WebAdminInput extends NullEditorInput {

	private String url;

	public WebAdminInput(String url) {
		this.url = url;
	}
	
	public void setURL(String url) {
		this.url = url;
	}
	
	public String getURL() {
		return url;
	}
}

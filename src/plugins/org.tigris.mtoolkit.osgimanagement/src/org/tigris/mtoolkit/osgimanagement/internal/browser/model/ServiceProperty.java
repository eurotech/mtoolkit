package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import org.tigris.mtoolkit.osgimanagement.browser.model.Model;

public class ServiceProperty extends Model {

	private String label;

	public ServiceProperty(String name, Model parent) {
		super(name, parent);
		label = name;
	}

	public String getLabel() {
		return label;
	}
}

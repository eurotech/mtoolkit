package org.tigris.mtoolkit.common;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;

public class CMultiStatus extends MultiStatus {

	public CMultiStatus(String pluginId, int code, Throwable exception) {
		super(pluginId, code, "", exception);
	}
	
	public void add(IStatus status) {
		if (status.getSeverity() > getSeverity()) {
			setMessage(status.getMessage());
		}
		super.add(status);
	}

}

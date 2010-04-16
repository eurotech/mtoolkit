package org.tigris.mtoolkit.common;

import org.eclipse.core.runtime.IStatus;

/**
 * @since 5.0
 */
public class BasePluginExporter {

	private volatile IStatus result = null;

	public BasePluginExporter() {
		super();
	}

	public IStatus getResult() {
		return result;
	}
	
	protected void setResult(IStatus result) {
		synchronized (this) {
			this.result = result;
			notifyAll();
		}
	}

	public boolean hasFinished() {
		return getResult() != null;
	}

	public IStatus join(long timeout) throws InterruptedException {
		long start = System.currentTimeMillis();
		synchronized (this) {
			while (result == null && (System.currentTimeMillis() - start < timeout)) {
				wait(timeout - System.currentTimeMillis() + start);
			}
		}
		return result;
	}

}
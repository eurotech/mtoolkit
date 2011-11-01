package org.tigris.mtoolkit.osgimanagement.installation;

import org.tigris.mtoolkit.common.installation.InstallationItem;

/**
 * Simple pair class, which can hold the processor and the installation item
 * 
 */
public class InstallationPair {

	private FrameworkProcessor processor;
	private InstallationItem item;

	public InstallationPair(FrameworkProcessor processor, InstallationItem item) {
		this.processor = processor;
		this.item = item;
	}

	public FrameworkProcessor processor() {
		return processor;
	}

	public InstallationItem item() {
		return item;
	}
}

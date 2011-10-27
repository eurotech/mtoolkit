package org.tigris.mtoolkit.osgimanagement.installation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.FrameworkImpl;

public class FrameworkInstallationItem {

	private FrameworkImpl framework;
	private FrameworkProcessor processor;
	private InstallationItem item;
	private File bundleFile;
	private InputStream input;

	/**
	 * Creates framework installation item for a {@link InstallationItem}.
	 * 
	 * @param framework
	 * @param processor
	 * @param item
	 */
	public FrameworkInstallationItem(FrameworkImpl framework, FrameworkProcessor processor, InstallationItem item) {
		this.framework = framework;
		this.processor = processor;
		this.item = item;
	}

	/**
	 * Creates framework installation item for a bundle file location.
	 * 
	 * @param framework
	 * @param processor
	 * @param item
	 */
	public FrameworkInstallationItem(FrameworkImpl framework, File bundleLocation) {
		this.framework = framework;
		this.bundleFile = bundleLocation;
	}

	public File getFile() throws IOException {
		if (bundleFile == null) {
			input = item.getInputStream();
			bundleFile = processor.saveFile(input, item.getName());
		}
		return bundleFile;
	}

	public FrameworkImpl getFramework() {
		return framework;
	}

	public void dispose() {
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
			}
		}
		if (item != null) {
			// delete the file created from the install item
			if (bundleFile != null) {
				try {
					bundleFile.delete();
				} catch (Exception e) {
				}
			}
			item.dispose();
		}

	}

}

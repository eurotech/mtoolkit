package org.tigris.mtoolkit.maven.internal.installation;

import java.io.File;

import org.eclipse.core.resources.IFile;

public class FileItem extends BaseItem {

	protected IFile pomFile;

	protected FileItem(InstallationProvider provider, IFile pomFile) {
		super(provider);
		this.pomFile = pomFile;
	}

	@Override
	public File getPomLocationAtFilesystem() {
		return new File(pomFile.getLocationURI());
	}

	@Override
	public String getDisplayName() {
		return "workspace file " + pomFile.getFullPath();
	}

}

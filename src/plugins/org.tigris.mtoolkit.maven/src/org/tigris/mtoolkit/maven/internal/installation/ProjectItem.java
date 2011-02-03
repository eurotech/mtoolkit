package org.tigris.mtoolkit.maven.internal.installation;

import java.io.File;

import org.maven.ide.eclipse.project.IMavenProjectFacade;

public class ProjectItem extends BaseItem {

	protected IMavenProjectFacade facade;

	protected ProjectItem(InstallationProvider provider, IMavenProjectFacade facade) {
		super(provider);
		this.facade = facade;
	}

	@Override
	public File getPomLocationAtFilesystem() {
		return facade.getPomFile();
	}

	@Override
	public String getDisplayName() {
		return "workspace project " + facade.getProject().getName();
	}
}

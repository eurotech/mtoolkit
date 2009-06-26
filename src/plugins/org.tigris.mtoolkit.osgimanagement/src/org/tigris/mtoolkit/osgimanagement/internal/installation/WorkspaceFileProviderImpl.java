package org.tigris.mtoolkit.osgimanagement.internal.installation;

import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.common.installation.WorkspaceFileItem;
import org.tigris.mtoolkit.common.installation.WorkspaceFileProvider;

/**
 * Generic implementation of the {@link InstallationItemProvider}, which
 * supports workspace files with specific extension.
 * <p>
 * The file extensions, handled by this provider and their MIME type are
 * specified as additional attributes in the configuration element:<br>
 * <ul>
 * <li><b>extension</b> - the extension of the files (without the leading dot).
 * Example: if you want to specify provider for *.jar files, you need to specify
 * <code>extension="jar"</code>.</li>
 * <li><b>type</b> - the MIME type of the files, handled by this provider.</li>
 * </ul>
 * </p>
 * 
 */
public class WorkspaceFileProviderImpl extends WorkspaceFileProvider {

	public InstallationItem getInstallationItem(Object resource) {
		return new WorkspaceFileItem(getFileFromGeneric(resource), mimeType);
	}

}

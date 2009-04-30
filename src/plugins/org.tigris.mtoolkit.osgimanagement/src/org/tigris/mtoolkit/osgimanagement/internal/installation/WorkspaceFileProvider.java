package org.tigris.mtoolkit.osgimanagement.internal.installation;

import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProvider;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;

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
public class WorkspaceFileProvider implements InstallationItemProvider {

	public static class WorkspaceFileItem implements InstallationItem {

		private IFile file;
		private String mimeType;

		WorkspaceFileItem(IFile file, String mimeType) {
			this.file = file;
			this.mimeType = mimeType;
		}

		public InputStream getInputStream() {
			try {
				return file.getContents();
			} catch (CoreException e) {
				FrameworkPlugin.error(NLS.bind("Failed to retrieve contents of file: {0}", file.getFullPath()), e);
				return null;
			}
		}

		public String getMimeType() {
			return mimeType;
		}

		public String getName() {
			return file.getName();
		}

		public IStatus prepare(IProgressMonitor monitor) {
			try {
				file.refreshLocal(IFile.DEPTH_ZERO, monitor);
			} catch (CoreException e) {
				return FrameworkPlugin.newStatus(IStatus.ERROR, "Failed to prepare file for installation", e);
			}
			return Status.OK_STATUS;
		}

	}

	private String extension;
	private String mimeType;

	public InstallationItem getInstallationItem(Object resource) {
		return new WorkspaceFileItem(getFileFromGeneric(resource), mimeType);
	}

	public void init(IConfigurationElement element) throws CoreException {
		extension = element.getAttribute("extension");
		if (extension == null)
			throw new CoreException(FrameworkPlugin.newStatus(IStatus.ERROR,
				"Installation item provider must specify 'extension' attribute",
				null));
		mimeType = element.getAttribute("type");
		if (mimeType == null)
			throw new CoreException(FrameworkPlugin.newStatus(IStatus.ERROR,
				"Installation item provider must specify 'type' attribute",
				null));
		// successful
	}

	public boolean isCapable(Object resource) {
		IFile file = getFileFromGeneric(resource);
		if (file != null && extension.equals(file.getFileExtension())) {
			return true;
		}
		return false;
	}

	private IFile getFileFromGeneric(Object resource) {
		if (resource instanceof IFile) {
			return (IFile) resource;
		} else if (resource instanceof IAdaptable) {
			return (IFile) ((IAdaptable) resource).getAdapter(IFile.class);
		}
		return null;
	}

}

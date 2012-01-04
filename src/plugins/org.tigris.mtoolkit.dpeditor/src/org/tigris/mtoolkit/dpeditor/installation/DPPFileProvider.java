/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.dpeditor.installation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.tigris.mtoolkit.common.android.AndroidUtils;
import org.tigris.mtoolkit.common.certificates.CertUtils;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.WorkspaceFileItem;
import org.tigris.mtoolkit.common.installation.WorkspaceFileProvider;
import org.tigris.mtoolkit.dpeditor.DPActivator;
import org.tigris.mtoolkit.dpeditor.util.DPPUtil;
import org.tigris.mtoolkit.util.DPPFile;

public final class DPPFileProvider extends WorkspaceFileProvider {

  public class DPPFileItem extends WorkspaceFileItem {
    public File dpFile;
    private boolean delete = false;

    public DPPFileItem(IFile file, String mimeType) {
      super(file, mimeType);
    }

    public void dispose() {
      super.dispose();
      if (delete) {
        dpFile.delete();
        try {
          file.getParent().refreshLocal(IResource.DEPTH_ONE, null);
        } catch (CoreException e) {
          // do nothing
        }
      }
    }

    public InputStream getInputStream() throws IOException {
      if (preparedFile != null) {
        return new FileInputStream(preparedFile);
      }
      return new FileInputStream(dpFile);
    }

    public String getLocation() {
      if (preparedFile != null) {
        return preparedFile.getAbsolutePath();
      }
      if (dpFile != null) {
        return dpFile.getAbsolutePath();
      }
      throw new IllegalStateException("Installation item wasn't initialized correctly, missing location to base file");
    }

    public void setLocation(File file) {
      if (preparedFile != null && !preparedFile.equals(file)) {
        // delete the previous prepared item
        preparedFile.delete();
      }
      preparedFile = file;
    }

    public IStatus prepare(IProgressMonitor monitor, Map properties) {
      try {
        Display dis = PlatformUI.getWorkbench().getDisplay();
        dis.syncExec(new Thread() {
          public void run() {
            PlatformUI.getWorkbench().saveAllEditors(true);
          }
        });
        DPPFile dppFile = new DPPFile(file.getLocation().toFile(), file.getProject().getLocation().toOSString());
        dpFile = new File(dppFile.getBuildInfo().getBuildLocation() + "/" + dppFile.getBuildInfo().getDpFileName());
        delete = !dpFile.exists();
        preparedFile = (delete) ? dpFile : null;
        DPPUtil.generateDeploymentPackage(dppFile, monitor, file.getProject(), DPPUtil.TYPE_QUICK_BUILD_DPP);
        if (properties != null && "Dalvik".equalsIgnoreCase((String) properties.get("jvm.name"))
            && !AndroidUtils.isDpConvertedToDex(dpFile)) {
          File convertedFile = new File(DPActivator.getDefault().getStateLocation() + "/dex/" + dpFile.getName());
          convertedFile.getParentFile().mkdirs();
          AndroidUtils.convertDpToDex(dpFile, convertedFile, monitor);
          setLocation(convertedFile);
        }
        if (monitor.isCanceled()) {
          return Status.CANCEL_STATUS;
        }
        return CertUtils.signItems(new InstallationItem[] { this }, monitor, properties);
      } catch (Exception e) {
        return new Status(Status.ERROR, DPActivator.PLUGIN_ID, "Failed to prepare file for installation");
      }
    }

  } // end of DPPFileItem

  public IStatus prepareItems(List items, Map properties, IProgressMonitor monitor) {
    if (items != null) {
      for (int i = 0; i < items.size(); i++) {
        Object item = items.get(i);
        if (item instanceof DPPFileItem) {
          ((DPPFileItem) item).prepare(monitor, properties);
        }
      }
    }
    return Status.OK_STATUS;
  }

  public String getName() {
    return "Deployment packages provider";
  }

  public InstallationItem getInstallationItem(Object resource) {
    return new DPPFileItem(getFileFromGeneric(resource), mimeType);
  }
}

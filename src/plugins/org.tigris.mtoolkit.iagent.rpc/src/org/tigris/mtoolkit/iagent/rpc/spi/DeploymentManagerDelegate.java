package org.tigris.mtoolkit.iagent.rpc.spi;

import java.io.InputStream;
import org.osgi.service.deploymentadmin.DeploymentPackage;

public interface DeploymentManagerDelegate {

  public Object installDeploymentPackage(InputStream in);

  public Object uninstallDeploymentPackage(DeploymentPackage dp, boolean force);

}

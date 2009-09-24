package org.tigris.mtoolkit.iagent.rpc.spi;

import java.io.InputStream;

import org.osgi.framework.Bundle;

public interface BundleManagerDelegate {

  public Object installBundle(String location, InputStream in);
  
  public Object uninstallBundle(Bundle bundle);
  
  public Object updateBundle(Bundle bundle, InputStream in);
  
}

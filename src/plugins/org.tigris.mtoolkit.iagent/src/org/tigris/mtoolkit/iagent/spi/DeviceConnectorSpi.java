package org.tigris.mtoolkit.iagent.spi;

import org.tigris.mtoolkit.iagent.DeviceConnector;

public interface DeviceConnectorSpi {
	
	public ConnectionManager getConnectionManager();
	
	public DeviceConnector getDeviceConnector();

}

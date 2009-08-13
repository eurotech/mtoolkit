package org.tigris.mtoolkit.iagent.spi;


public interface IAgentManager {

	public void init(DeviceConnectorSpi connector);
	
	public void dispose();
}

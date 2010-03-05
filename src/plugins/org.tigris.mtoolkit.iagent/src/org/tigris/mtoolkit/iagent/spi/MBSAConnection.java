package org.tigris.mtoolkit.iagent.spi;

import org.tigris.mtoolkit.iagent.IAgentException;

/**
 * Defines the abstraction transport layer. Isolates the implementation of the DeviceConnector and 
 * device commands from the real data transport. 
 *
 * @version 1.0
 */
public interface MBSAConnection extends AbstractConnection {
	
	public static final String PROP_MBSA_PORT = "mbsa-port";
	
  /**
   * Sends data to the device.
   * 
   * @param aCmd command on the device which should be executed
   * @param aData data arguments for the command
   * @return the response from the device
   * @throws IAgentException thrown if some error processing data or during transport occurs
   */
  public MBSAConnectionCallBack sendData(int aCmd, byte[] aData) throws IAgentException;
  
  public MBSAConnectionCallBack sendData(int aCmd, byte[] aData, boolean disconnectOnFailure) throws IAgentException;
  
  /**
   * Returns the maximum size of the data which could be sent over this transport
   * @return
   */
  public int getDataMaxSize();
  
}

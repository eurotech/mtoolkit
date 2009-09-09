package org.tigris.mtoolkit.iagent.mbsa;


public interface MBSAClient {

	public MBSAResult send(MBSACommand message) throws MBSAException;
	
	public void close();
	
}

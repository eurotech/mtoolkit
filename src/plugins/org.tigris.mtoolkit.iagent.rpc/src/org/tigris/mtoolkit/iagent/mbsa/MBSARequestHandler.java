package org.tigris.mtoolkit.iagent.mbsa;

public interface MBSARequestHandler {

	public MBSAResponse handleRequest(MBSARequest msg);
	
	public void disconnected(MBSAServer server);
	
}

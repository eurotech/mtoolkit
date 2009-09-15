package org.tigris.mtoolkit.iagent.transport;

public interface Transport {

	TransportConnection createConnection(int port);
	
}

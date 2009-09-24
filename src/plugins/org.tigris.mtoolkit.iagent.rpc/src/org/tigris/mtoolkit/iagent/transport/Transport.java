package org.tigris.mtoolkit.iagent.transport;

import java.io.IOException;

public interface Transport {

	TransportConnection createConnection(int port) throws IOException;
	
	TransportType getType();
	
	String getId();
	
	void dispose();
	
	boolean isDisposed();
}

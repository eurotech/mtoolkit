package org.tigris.mtoolkit.iagent.transport;

import java.util.List;

public interface TransportType {

	Transport openTransport(String id);
	
	List/*<String>*/ listAvailable();
	
}

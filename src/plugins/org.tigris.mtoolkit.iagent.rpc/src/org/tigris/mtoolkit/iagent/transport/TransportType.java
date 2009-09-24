package org.tigris.mtoolkit.iagent.transport;

import java.io.IOException;
import java.util.List;

public interface TransportType {

	Transport openTransport(String id) throws IOException;
	
	List/*<String>*/ listAvailable();
	
	String getTypeId();
	
}

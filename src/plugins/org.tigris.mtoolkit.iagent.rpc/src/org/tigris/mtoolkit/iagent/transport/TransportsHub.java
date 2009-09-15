package org.tigris.mtoolkit.iagent.transport;

import java.util.List;

public interface TransportsHub {

	Transport openTransport(String type, String id);
	
	List/*<TransportType>*/ listTypes();
	
}

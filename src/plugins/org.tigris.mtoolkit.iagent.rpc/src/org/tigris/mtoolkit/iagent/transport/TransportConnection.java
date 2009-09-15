package org.tigris.mtoolkit.iagent.transport;

import java.io.InputStream;
import java.io.OutputStream;

public interface TransportConnection {

	boolean isClosed();
	
	InputStream getInputStream();
	
	OutputStream getOutputStream();
	
	void close();
}

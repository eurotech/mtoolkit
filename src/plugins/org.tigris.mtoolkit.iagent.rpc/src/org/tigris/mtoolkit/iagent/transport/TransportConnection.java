package org.tigris.mtoolkit.iagent.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface TransportConnection {

	boolean isClosed();
	
	InputStream getInputStream() throws IOException;
	
	OutputStream getOutputStream() throws IOException;
	
	void close();
}

package org.tigris.mtoolkit.iagent.spi;

import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;

public interface PMPConnector {

	public PMPConnection createPMPConnection(String targetIP) throws PMPException;
	
	public void dispose();
}

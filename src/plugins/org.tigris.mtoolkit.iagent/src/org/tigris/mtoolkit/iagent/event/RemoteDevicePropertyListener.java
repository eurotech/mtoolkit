package org.tigris.mtoolkit.iagent.event;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;

/**
 * Clients interested in device properties change events must implement this
 * interface. The listeners must be added to the listeners list via
 * {@link DeviceConnector#addRemoteDevicePropertyListener(RemoteDevicePropertyListener)
 * method.
 */
public interface RemoteDevicePropertyListener {

	/**
	 * Sent when remote device properties are changed in some way (no more
	 * console is available, or eventAdmin service is registered/unregistered,
	 * etc.).
	 * 
	 * @param event
	 *            an event object containing details
	 * @throws IAgentException 
	 */
	public void devicePropertiesChanged(RemoteDevicePropertyEvent e) throws IAgentException;
}

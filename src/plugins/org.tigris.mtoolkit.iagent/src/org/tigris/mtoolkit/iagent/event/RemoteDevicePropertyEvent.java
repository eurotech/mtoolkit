package org.tigris.mtoolkit.iagent.event;

/**
 * Event object containing details about remote device properties changes.
 */
public class RemoteDevicePropertyEvent extends RemoteEvent {

	/**
	 * Constant indicating that a RemoteDeploymentAdmin service has been
	 * changed: registered or unregistered
	 */
	public final static int DP_ADMIN_CHANGED = 0;

	/**
	 * Constant indicating the RemoteEventAdmin service state has been changed:
	 * registered or unregistered
	 */
	public final static int EVENT_ADMIN_CHANGED = 1;

	/**
	 * Constant indicating that RemoteApplicationAdmin service has been changed:
	 * registered or unregistered
	 * 
	 */
	public static final int APP_ADMIN_CHANGED = 2;

	/**
	 * Constant indicating that a remote console event is received: console is
	 * supported or not supported any more.
	 */
	public final static int CONSOLE_SUPPORTED = 3;

	public boolean eventState = false;

	public RemoteDevicePropertyEvent(int type, boolean state) {
		super(type);
		this.eventState = state;
	}

	public String toString() {
		return "Remote property change event[type=" + convertType(getType()) + " is supported: " + eventState + "]";
	}

	private String convertType(int type) {
		switch (type) {
		case DP_ADMIN_CHANGED:
			return "DeploymentAdmin";
		case APP_ADMIN_CHANGED:
			return "AplicationAdmin";
		case CONSOLE_SUPPORTED:
			return "console";
		case EVENT_ADMIN_CHANGED:
			return "EventAdmin";
		default:
			return "UNKNOWN(" + type + ")";
		}
	}

	public boolean getEventState() {
		return eventState;
	}
}

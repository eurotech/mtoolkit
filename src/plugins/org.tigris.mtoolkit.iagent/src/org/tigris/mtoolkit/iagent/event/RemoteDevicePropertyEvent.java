package org.tigris.mtoolkit.iagent.event;

/**
 * Event object containing details about remote device properties changes.
 */
public class RemoteDevicePropertyEvent extends RemoteEvent {

	private static final int PROPERTY_CHANGED_TYPE = 0;

	public String property;
	public Object value;

	public RemoteDevicePropertyEvent(String property, Object value) {
		this(PROPERTY_CHANGED_TYPE, property, value);
	}

	public RemoteDevicePropertyEvent(int type, String property, Object value) {
		super(type);
		this.property = property;
		this.value = value;
	}

	public String toString() {
		return "Remote property event[type=" + convertType(getType()) + " property: " + property + " value: " + value + "]";
	}

	private String convertType(int type) {
		switch (type) {
		case PROPERTY_CHANGED_TYPE:
			return "PROPERTY_CHANGED";
		default:
			return "UNKNOWN(" + type + ")";
		}
	}

	public Object getProperty() {
		return property;
	}

	public Object getValue() {
		return value;
	}
}

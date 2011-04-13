package org.tigris.mtoolkit.common.gui;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 6.0
 */
public class ListItem {

	public Object element = null;
	public boolean checked = false;
	protected Map properties = new HashMap();

	public String toString() {
		return element != null ? element.toString() : super.toString();
	}

	/**
	 * Sets the given property.
	 * 
	 * @param key
	 * @param value
	 * @return the old value of the property
	 */
	public Object setProperty(String key, Object value) {
		return properties.put(key, value);
	}

	/**
	 * Returns the value of the property for the given key.
	 * 
	 * @param key
	 * @return the value of the property
	 */
	public Object getProperty(String key) {
		return properties.get(key);
	}

}

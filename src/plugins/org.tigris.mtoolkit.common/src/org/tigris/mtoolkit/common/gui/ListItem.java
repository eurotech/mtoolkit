package org.tigris.mtoolkit.common.gui;

/**
 * @since 6.0
 */
public class ListItem {

	public Object element = null;
	public boolean checked = false;

	public String toString() {
		return element != null ? element.toString() : super.toString();
	}

}

package org.tigris.mtoolkit.osgimanagement;

import java.util.Set;

/**
 * @since 3.0
 */
public interface ISystemBundlesProvider {
	static final String EXTENSION_POINT_NAME = "systemBundlesProvider";

	/**
	 * Returns the list of system bundles identifiers.
	 * 
	 * @return all tree elements
	 */
	Set<String> getSystemBundlesIDs();
}

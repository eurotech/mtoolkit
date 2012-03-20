package org.tigris.mtoolkit.osgimanagement.installation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.osgimanagement.ISystemBundlesProvider;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;

public class FrameworkSystemBundlesProvider implements ISystemBundlesProvider {

	private static final String SYSTEM_BUNDLES_FILE_NAME = "system_bundles.txt";
	private static final String SYSTEM_BUNDLES_RESOURCE_NAME = "recources/" + SYSTEM_BUNDLES_FILE_NAME;

	private Set loadedSymbolicNames;

	public Set<String> getSystemBundlesIDs() {
		if (loadedSymbolicNames == null) {
			BufferedReader reader = null;
			try {
				URL systemBundleURL = FrameworkPlugin.getDefault().getBundle()
						.getResource(SYSTEM_BUNDLES_RESOURCE_NAME);
				if (systemBundleURL != null) {
					reader = new BufferedReader(new InputStreamReader(systemBundleURL.openStream()));
				}
				Set symbolicNames = new HashSet();
				String line;
				while ((line = reader.readLine()) != null) {
					symbolicNames.add(line);
				}
				this.loadedSymbolicNames = symbolicNames;
			} catch (IOException e) {
				DebugUtils.error(this, "Failed to load system buindles list from the bundle resources", e);
				loadedSymbolicNames = Collections.EMPTY_SET;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		}
		return loadedSymbolicNames;
	}

}

/*******************************************************************************
 * Copyright (c) 2005, 2010 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.osgimanagement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.tigris.mtoolkit.osgimanagement.model.Model;

public class IconFetcher {

	private static HashMap instances = new HashMap();

	private FetcherThread thread;
	private List queue = new ArrayList();
	private String name;

	private IconFetcher(String name) {
		this.name = name;
	}

	/**
	 * Returns IconFetcher instance for the specified name. If name is null,
	 * then default IconFetcher is returned.
	 * 
	 * @param name
	 *            or null
	 * @return IconFetcher instance
	 */
	public static IconFetcher getInstance(String name) {
		IconFetcher fetcher;
		synchronized (instances) {
			fetcher = (IconFetcher) instances.get(name);
			if (fetcher == null) {
				fetcher = new IconFetcher(name);
				instances.put(name, fetcher);
			}
		}
		return fetcher;
	}

	public void enqueue(Model model) {
		if (model == null) {
			throw new IllegalArgumentException("model should not be null");
		}
		if (!(model instanceof IconProvider)) {
			throw new IllegalArgumentException("IconProvider instance expected");
		}
		synchronized (queue) {
			queue.add(queue.size(), model);
			if (thread == null || !thread.isRunning) {
				thread = new FetcherThread();
				thread.start();
			}
		}
	}

	private class FetcherThread extends Thread {
		private volatile boolean isRunning = false;

		public FetcherThread() {
			super("Icon Fetcher - " + (name != null ? name : "default"));
		}

		public void start() {
			isRunning = true;
			super.start();
		}

		public void run() {
			try {
				while (isRunning) {
					Model model;
					synchronized (queue) {
						if (queue.isEmpty()) {
							isRunning = false;
							return;
						}
						model = (Model) queue.remove(0);
					}

					try {
						if (((IconProvider) model).fetchIconData() != null) {
							model.updateElement();
						}
					} catch (Exception e) {
						// continue
					}
				}
			} finally {
				synchronized (instances) {
					instances.remove(name);
				}
			}
		}
	}
}

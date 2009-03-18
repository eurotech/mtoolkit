/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal.utils;

import java.util.ArrayList;
import java.util.List;

public class ThreadPool {

	private static final int MAX_WORKERS = 5;

	private volatile boolean running = true;
	private volatile int workers = 0;
	private volatile int working = 0;

	private List workUnits = new ArrayList();

	private Object lock = new Object();

	private static ThreadPool instance;
	private static int clientsCount = 0;

	public void stop() {
		running = false;
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	public synchronized static ThreadPool getPool() {
		if (instance == null)
			instance = new ThreadPool();
		clientsCount++;
		return instance;
	}

	public synchronized static void releasePool(ThreadPool pool) {
		if (pool == null)
			throw new IllegalArgumentException();
		if (instance == pool) {
			clientsCount--;
			if (clientsCount == 0) {
				instance.stop();
				instance = null;
			}
		}
	}

	public void enqueueWork(Runnable runnable) {
		if (!running)
			throw new IllegalStateException();
		if (runnable == null)
			throw new NullPointerException();
		synchronized (lock) {
			workUnits.add(runnable);
			lock.notify();
		}
		if (workers == working && workers < MAX_WORKERS) {
			// spawn new worker
			new Worker();
		}
	}

	private class Worker extends Thread {
		public Worker() {
			super();
			int workerId;
			synchronized (lock) {
				workerId = workers++;
			}
			setName("IAgent Worker #" + workerId);
			setDaemon(true);
			start();
		}

		public void run() {
			while (true) {
				Runnable unit;
				synchronized (lock) {
					while (workUnits.isEmpty() && running) {
						try {
							lock.wait(1000);
						} catch (InterruptedException e) {
						}
					}
					if (workUnits.isEmpty() /* && !running */) {
						// stop worker thread if there are no more work units
						// and the pool has been released/stopped
						workers--;
						return;
					}
					unit = (Runnable) workUnits.remove(0);
					working++;
				}
				try {
					unit.run();
				} catch (Throwable e) {
					// TODO: Log exception
					e.printStackTrace();
				} finally {
					synchronized (lock) {
						working--;
					}
				}
			}
		}
	}
}

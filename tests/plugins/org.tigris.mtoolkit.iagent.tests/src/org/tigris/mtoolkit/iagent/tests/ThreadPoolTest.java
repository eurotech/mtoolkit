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
package org.tigris.mtoolkit.iagent.tests;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.tigris.mtoolkit.iagent.internal.utils.ThreadPool;

public class ThreadPoolTest extends TestCase {

	private class Register {
		public volatile AtomicInteger started = new AtomicInteger();
		public volatile AtomicInteger done = new AtomicInteger();
		public volatile AtomicInteger created = new AtomicInteger();
	}

	private class Worker implements Runnable {
		private final int id;
		private final int duration;
		private final Register reg;

		public Worker(int id, int duration, Register reg) {
			if ((id < 0) || (id > 31))
				throw new IllegalArgumentException("id = " + id);
			if (duration < 1)
				throw new IllegalArgumentException("duration < 0");
			markBit(id, reg.created);
			this.id = id;
			this.duration = duration;
			this.reg = reg;
		}

		public void run() {
			markStarted(id, reg);
			System.out.println(this + " started.");
			sleep(duration);
			markDone(id, reg);
			System.out.println(this + " done.");
		}

		public String toString() {
			return "TestWorker #".concat(Integer.toString(id));
		}

	}

	/**
	 * Tests whether the pool can work sequentially (with length 1). The test
	 * creates a pool with length 1 and 3 work units with different lengths.
	 * After this, the test asserts that the work units are executed
	 * sequentially.
	 */
	public void testSequential() {
		Register reg = new Register();

		Worker first = new Worker(0, 1000, reg);
		Worker second = new Worker(1, 2500, reg);
		Worker third = new Worker(2, 500, reg);
		//total = 1000 + 2500 + 500;

		ThreadPool pool = new ThreadPool(1, ThreadPool.OPTION_NONE);
		pool.enqueueWork(first);
		pool.enqueueWork(second);
		pool.enqueueWork(third);

		sleep(100);

		assertTrue("First thread must be running", isStarted(0, reg));
		assertFalse("Second thread must not be running", isStarted(1, reg));
		assertFalse("Third thread must not be running", isStarted(2, reg));

		sleep(1500);

		assertTrue("First thread must be done", isDone(0, reg));
		assertTrue("Second thread must be running", isStarted(1, reg));
		assertFalse("Third thread must not be running", isStarted(2, reg));

		sleep(3000);

		assertTrue("First thread must be done", isDone(0, reg));
		assertTrue("Second thread must be done", isStarted(1, reg));
		assertTrue("Third thread must be done", isStarted(2, reg));

	}

	public void testParallelWithLimit() {
		Register reg = new Register();

		Worker first = new Worker(0, 1000, reg);
		Worker second = new Worker(1, 2500, reg);
		Worker third = new Worker(2, 500, reg);
		Worker fourth = new Worker(3, 500, reg);

		ThreadPool pool = new ThreadPool(3, ThreadPool.OPTION_AGGRESSIVE);
		pool.enqueueWork(first);
		pool.enqueueWork(second);
		pool.enqueueWork(third);
		pool.enqueueWork(fourth);

		sleep(100);
		
		assertTrue("First thread must be running", isStarted(0, reg));
		assertTrue("Second thread must be running", isStarted(1, reg));
		assertTrue("Third thread must be running", isStarted(2, reg));
		assertFalse("Fourth thread must not be running", isStarted(3, reg));

		sleep(600);

		assertFalse("First thread must not be done", isDone(0, reg));
		assertFalse("Second thread must not be done", isDone(1, reg));
		assertTrue("Third thread must be done", isDone(2, reg));
		assertTrue("Fourth thread must be running", isStarted(3, reg));

		sleep(1000);

		assertTrue("First thread must be done", isDone(0, reg));
		assertFalse("Second thread must not be done", isDone(1, reg));
		assertTrue("Fourth thread must be done", isDone(3, reg));

		sleep(1500);
		assertTrue("Second thread must be done", isDone(1, reg));
	}

	private static void sleep(int duration) {
		long start = System.currentTimeMillis();
		long elapsedTime;
		while ((elapsedTime = System.currentTimeMillis() - start) < duration) {
			try {
				Thread.sleep(duration - elapsedTime);
			} catch (InterruptedException e) {
			}
		}
	}

	private static boolean isBitMarked(int id, AtomicInteger ai) {
		return ((ai.get() & (1 << id)) != 0);
	}

	private static boolean isStarted(int id, Register r) {
		return isBitMarked(id, r.started);
	}

	private static boolean isDone(int id, Register r) {
		return isBitMarked(id, r.done);
	}

	private static void markStarted(int id, Register r) {
		markBit(id, r.started);
	}

	private static void markDone(int id, Register r) {
		markBit(id, r.done);
	}

	private static void markBit(int id, AtomicInteger ai) {
		if ((id < 0) || (id > 31))
			throw new IllegalArgumentException(id + " is not in [0, 31] interval");
		final int bitMask = 1 << id;
		do {
			int current = ai.get();
			if ((current & bitMask) != 0)
				throw new IllegalStateException("Status was already changed for id " + id);
			if (ai.compareAndSet(current, current | bitMask))
				break;
		} while (true);
	}

}

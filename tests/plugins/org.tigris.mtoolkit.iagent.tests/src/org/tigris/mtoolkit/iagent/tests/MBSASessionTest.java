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

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.tcp.MBSAConnectionImpl;
import org.tigris.mtoolkit.iagent.internal.utils.ThreadPool;
import org.tigris.mtoolkit.iagent.mbsa.MBSAClient;
import org.tigris.mtoolkit.iagent.mbsa.MBSACommand;
import org.tigris.mtoolkit.iagent.mbsa.MBSAException;
import org.tigris.mtoolkit.iagent.mbsa.MBSARequest;
import org.tigris.mtoolkit.iagent.mbsa.MBSARequestHandler;
import org.tigris.mtoolkit.iagent.mbsa.MBSAResponse;
import org.tigris.mtoolkit.iagent.mbsa.MBSAResult;
import org.tigris.mtoolkit.iagent.mbsa.MBSAServer;
import org.tigris.mtoolkit.iagent.mbsa.MBSASessionFactory;
import org.tigris.mtoolkit.iagent.spi.MBSAConnection;
import org.tigris.mtoolkit.iagent.spi.MBSAConnectionCallBack;

public class MBSASessionTest extends TestCase {

	public class TestRequestHandler implements MBSARequestHandler {
		public MBSAResponse handleRequest(MBSARequest msg) {
			MBSAResponse rsp = msg.respond(msg.getCommand());
			try {
				rsp.writeByte(msg.readByte());
				rsp.writeChar(msg.readChar());
				rsp.writeWord(msg.readWord());
				rsp.writeShort(msg.readShort());
				rsp.writeDWord(msg.readDWord());
				rsp.writeInt(msg.readInt());
				rsp.writeQWord(msg.readQWord());
				rsp.writeLong(msg.readLong());
				rsp.writeByteArray(msg.readByteArray());
				rsp.writeCharArray(msg.readCharArray());
				rsp.writeWordArray(msg.readWordArray());
				rsp.writeShortArray(msg.readShortArray());
				rsp.writeDWordArray(msg.readDWordArray());
				rsp.writeIntArray(msg.readIntArray());
				rsp.writeQWordArray(msg.readQWordArray());
				rsp.writeLongArray(msg.readLongArray());
				return rsp.done();
			} catch (IOException e) {
				e.printStackTrace();
				return ((MBSAResponse) msg.respond(1).writeString(e.toString())).done();
			}
		}

		public void disconnected(MBSAServer server) {

		}
	}
	
	public void testBaseCommunicationStandard() throws Exception {
		Object[] sessions = setupMBSASession(new TestRequestHandler());
		doBasicEchoTest((MBSAClient) sessions[0]);
	}
	
	public void testOldClient() throws Exception {
		ThreadPool pool = new ThreadPool(1, 0);
		final MBSAServer[] server = new MBSAServer[1];
		pool.enqueueWork(new Runnable() {
			public void run() {
				try {
					server[0] = MBSASessionFactory.serverListen(null, 37574, 1000000, new TestRequestHandler());
				} catch (MBSAException e) {
					fail(e.toString());
				}
			}
		});
		try {
			MBSAConnection client = new MBSAConnectionImpl("127.0.0.1", 37574) {
				boolean connected = false;

				public MBSAConnectionCallBack sendData(int aCmd, byte[] aData) throws IAgentException {
					if (!connected) {
						connect();
						connected = true;
					}
					return super.sendData(aCmd, aData);
				}
			};
			doBasicEchoTest(client);
		} finally {
			pool.join();
			if (server[0] != null)
				server[0].close();
		}
	}
	
	private void doBasicEchoTest(MBSAClient client) throws MBSAException {
		MBSACommand command = generateTestCommand();
		
		MBSAResult result = client.send(command);
		
		assertEquals(command.getCommand(), result.getStatus());
		assertEquals(command.getData(), result.getData());
	}
	
	private void doBasicEchoTest(MBSAConnection client) throws MBSAException, IAgentException {
		MBSACommand command = generateTestCommand();
		
		MBSAConnectionCallBack callback = client.sendData(command.getCommand(), command.getData());
		
		assertEquals(command.getCommand(), callback.getRspStatus());
		byte[] rspData = callback.getRspData();
		if (rspData == null)
			rspData = new byte[0];
		assertEquals(command.getData(), rspData);
	}

	private MBSACommand generateTestCommand() throws MBSAException {
		MBSACommand command = new MBSACommand(100);
		command.writeByte((byte)101);
		command.writeChar((byte)102);
		command.writeWord((short)103);
		command.writeShort((short)104);
		command.writeDWord(105);
		command.writeInt(106);
		command.writeQWord(107);
		command.writeLong(108);
		command.writeByteArray(new byte[] { 109, 110 });
		command.writeCharArray(new byte[] { 111, 112 });
		command.writeWordArray(new short[] { 113, 114 });
		command.writeShortArray(new short[] { 115, 116 });
		command.writeDWordArray(new int[] { 117, 118 });
		command.writeIntArray(new int[] { 119, 120 });
		command.writeQWordArray(new long[] { 121, 122 });
		command.writeLongArray(new long[] { 123, 124 });
		command.done();
		return command;
	}
	
	private Object[] setupMBSASession(final MBSARequestHandler handler) throws IOException {
		final PipedInputStream clientIn = new PipedInputStream();
		final PipedOutputStream clientOut = new PipedOutputStream();
		final PipedInputStream serverIn = new PipedInputStream();
		final PipedOutputStream serverOut = new PipedOutputStream();
		clientIn.connect(serverOut);
		clientOut.connect(serverIn);
		
		ThreadPool pool = new ThreadPool(5, ThreadPool.OPTION_AGGRESSIVE);
		final Object[] sessions = new Object[2];
		final Throwable[] exceptions = new Throwable[2];
		pool.enqueueWork(new Runnable() {
			public void run() {
				try {
					sessions[0] = MBSASessionFactory.wrapClient(clientIn, clientOut);
				} catch (Throwable e) {
					exceptions[0] = e;
				}
			}
		});
		pool.enqueueWork(new Runnable() {
			public void run() {
				try {
					MBSAServer server = MBSASessionFactory.wrapServer(handler, serverIn, serverOut);
					System.out.println(server);
					sessions[1] = server;
				} catch (Throwable e) {
					exceptions[1] = e;
				}
			}
		});
		pool.join();
		if (exceptions[0] != null || exceptions[1] != null) {
			if (exceptions[0] != null)
				exceptions[0].printStackTrace();
			if (exceptions[1] != null)
				exceptions[1].printStackTrace();
			throw new AssertionFailedError("Failed to setup MBSA session: " + exceptions[0] + "; " + exceptions[1]);
		}
		return sessions;
	}
	
	public void assertEquals(byte[] expected, byte[] actual) {
		if (expected == actual)
			return;
		if (expected == null || actual == null)
			throw new AssertionFailedError("expected: " + (expected != null) + "; actual: " + (actual != null));
		assertEquals("Array lengths must be equal", expected.length, actual.length);
		for (int i = 0; i < actual.length; i++)
			assertEquals("Elements at index " + i + " must be equal", expected[i], actual[i]);
	}
}

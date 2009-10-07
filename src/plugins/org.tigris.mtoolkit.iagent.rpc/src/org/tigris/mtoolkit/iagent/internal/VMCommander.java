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
package org.tigris.mtoolkit.iagent.internal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.mbsa.MBSAConstants;
import org.tigris.mtoolkit.iagent.mbsa.MBSAException;
import org.tigris.mtoolkit.iagent.mbsa.MBSARequest;
import org.tigris.mtoolkit.iagent.mbsa.MBSARequestHandler;
import org.tigris.mtoolkit.iagent.mbsa.MBSAResponse;
import org.tigris.mtoolkit.iagent.mbsa.MBSAServer;
import org.tigris.mtoolkit.iagent.mbsa.MBSASessionFactory;

public class VMCommander {

	private static final int VM_COMMAND_PORT = 7366;
	private static final int UDP_LISTENER_PORT = 7367;
	private static final int IAGENT_CMD_STOPVM = 0x00020002;
	private static final byte UDP_NOTIFICATION[] = { 0x44, 0x48, 0x54, 0x58 };

	private MBSAServer vmServer;
	private BundleContext bc;
	private UDPListener udpListener;
	private boolean shutdownOnDisconnect;

	public VMCommander(BundleContext bc, boolean shutdownOnDisconnect) {
		this.bc = bc;
		this.shutdownOnDisconnect = shutdownOnDisconnect;
		startUDPListener();
		startServer();
	}

	public void close() {
		stopServer();
		if (udpListener != null)
			udpListener.dispose();
	}

	private synchronized void startServer() {
		if (vmServer != null && !vmServer.isClosed()) {
			return;
		}
		try {
			vmServer = MBSASessionFactory.serverConnect(null, VM_COMMAND_PORT, new VMRequestHandler());
		} catch (MBSAException e) {
			// There is no active controller at the moment.
			// UDP notification shall be sent if controller is activated.
			log("[startServer] cannot connect to controller.", e);
			if (shutdownOnDisconnect && e.getCode() == MBSAException.CODE_CANNOT_CONNECT) {
				log("[startServer] Shutting down because no connection with the controller can be established.", null);
				stopVM();
			}
		}
	}

	private synchronized void stopServer() {
		if (vmServer != null) {
			// disable shutdown disconnect
			// the framework is either shutting down or we have stopped the bundle
			shutdownOnDisconnect = false;
			vmServer.close();
			vmServer = null;
		}
	}

	private void startUDPListener() {
		udpListener = new UDPListener();
		udpListener.setName("UDPListener Thread");
		udpListener.start();
	}

	private boolean stopVM() {
		log("[stopVM] stopVM command received.", null);
		try {
			bc.getBundle(0).stop();
		} catch (Exception e) {
			log("[stopVM] Cannot stop VM.", e);
			return false;
		}
		return true;
	}

	private final void log(String message, Throwable e) {
		DebugUtils.log(this, message, e);
	}

	private class UDPListener extends Thread {
		private volatile boolean listenerRunning;
		
		public UDPListener() {
			super("IAgent UDP Listener");
		}
		
		public void start() {
			listenerRunning = true;
			super.start();
		}
		
		public void dispose() {
			listenerRunning = false;
			interrupt();
		}

		public void run() {
			DatagramSocket socket = null;
			byte[] buffer = new byte[UDP_NOTIFICATION.length];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			try {
				socket = new DatagramSocket(UDP_LISTENER_PORT);
			} catch (SocketException se) {
				log("[startUDPListener] Cannot open socket for UDP notifications.", se);
				return;
			}
			while (listenerRunning) {
				try {
					socket.receive(packet);
					if (Arrays.equals(buffer, UDP_NOTIFICATION)) {
						startServer();
					} else {
						log("[UDPListener] Unknown UDP notification received.", null);
					}
				} catch (IOException e) {
					// possible timeout, nothing to receive
				}
			}
		}
	}

	private class VMRequestHandler implements MBSARequestHandler {
		public MBSAResponse handleRequest(MBSARequest msg) {
			MBSAResponse response = null;
			switch (msg.getCommand()) {
			case IAGENT_CMD_STOPVM:
				boolean res = stopVM();
				response = msg.respond((res == true) ? 0 : -1);
				response.done();
				break;
			default:
				response = msg.respond(MBSAConstants.IAGENT_RES_UNKNOWN_COMMAND);
				response.done();
				break;
			}
			return response;
		}

		public void disconnected(MBSAServer server) {
			log("[disconnected] MBSA Server got disconnected, shutdown the framework", null);
			stopVM();
		}
		
	}
}

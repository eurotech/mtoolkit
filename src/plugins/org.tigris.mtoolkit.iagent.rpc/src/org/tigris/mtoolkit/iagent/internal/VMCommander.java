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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

import org.osgi.framework.BundleContext;
import org.tigris.mtoolkit.iagent.internal.mbsa.DataFormater;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.internal.utils.ThreadUtils;
import org.tigris.mtoolkit.iagent.mbsa.MBSAConstants;
import org.tigris.mtoolkit.iagent.mbsa.MBSAException;
import org.tigris.mtoolkit.iagent.mbsa.MBSARequest;
import org.tigris.mtoolkit.iagent.mbsa.MBSARequestHandler;
import org.tigris.mtoolkit.iagent.mbsa.MBSAResponse;
import org.tigris.mtoolkit.iagent.mbsa.MBSAServer;
import org.tigris.mtoolkit.iagent.mbsa.MBSASessionFactory;
import org.tigris.mtoolkit.iagent.pmp.PMPServer;

public class VMCommander {

	private static final int VM_CONTROLLER_PORT = 7366;
	private static final int UDP_LISTENER_PORT = 7367;
	private static final int IAGENT_CMD_STOPVM = 0x00020002;
	private static final int IAGENT_CMD_GET_PMP_LISTENING_PORT = 0x0002000E;
	private static final byte UDP_NOTIFICATION[] = { 0x44, 0x48, 0x54, 0x58 };
	private static final String PROP_VM_CONTROLLER_PORT = "vm.controller.port";
	private static final String SERVER_PROP_PORT = "port";

	private MBSAServer vmServer;
	private BundleContext bc;
	private UDPListener udpListener;
	private PMPServer pmpServer;
	private boolean shutdownOnDisconnect;
	private volatile boolean closed;

	public VMCommander(BundleContext bc, PMPServer pmpServer, boolean shutdownOnDisconnect) {
		this.bc = bc;
		this.pmpServer = pmpServer;
		this.shutdownOnDisconnect = shutdownOnDisconnect;
		startUDPListener();
		startServerAsync();
	}

	public synchronized void close() {
		stopServer();
		if (udpListener != null)
			udpListener.dispose();
		closed = true;
	}

	private void startServerAsync() {
		ThreadUtils.createThread(new Runnable() {
			public void run() {
				startServer();
				if (closed) {
					// if VMCommander is closed during connection
					stopServer();
				}
			}
		}, "IAgent controller connection...").start();
	}

	private synchronized void startServer() {
		if (vmServer != null && !vmServer.isClosed()) {
			return;
		}
		try {
			vmServer = MBSASessionFactory.serverConnect(null, getControllerPort(), new VMRequestHandler());
		} catch (MBSAException e) {
			// There is no active controller at the moment.
			// UDP notification shall be sent if controller is activated.
			info("[startServer] cannot connect to controller.", e);
			if (shutdownOnDisconnect && e.getCode() == MBSAException.CODE_CANNOT_CONNECT) {
				info("[startServer] Shutting down because no connection with the controller can be established.");
				stopVM();
			}
		}
	}

	private synchronized void stopServer() {
		if (vmServer != null) {
			// disable shutdown disconnect
			// the framework is either shutting down or we have stopped the
			// bundle
			shutdownOnDisconnect = false;
			vmServer.close();
			vmServer = null;
		}
	}

	private int getControllerPort() {
		int port = VM_CONTROLLER_PORT;
		String portStr = System.getProperty(PROP_VM_CONTROLLER_PORT);
		debug("[getControllerPort] " + PROP_VM_CONTROLLER_PORT + " = " + portStr);
		if (portStr != null) {
			try {
				port = Integer.parseInt(portStr);
			} catch (NumberFormatException e) {
				info("[getControllerPort] Incorrect value of property: " + PROP_VM_CONTROLLER_PORT + ": " + portStr, e);
			}
		}
		return port;
	}

	private void startUDPListener() {
		udpListener = new UDPListener();
		udpListener.start();
	}

	private boolean stopVM() {
		debug("[stopVM] stopVM command received.");
		try {
			bc.getBundle(0).stop();
		} catch (Exception e) {
			error("[stopVM] Cannot stop VM.", e);
			return false;
		}
		return true;
	}

	private int getPmpListeningPort() {
		debug("[getPmpListeningPort] getPmpListeningPort command received.");
		if (pmpServer == null) {
			return 0;
		}
		Object port = pmpServer.getProperties().get(SERVER_PROP_PORT);
		debug("[getPmpListeningPort] port = " + port);
		if (port == null) {
			return 0;
		}
		if (port instanceof Integer) {
			return ((Integer) port).intValue();
		}
		try {
			return Integer.parseInt(port.toString());
		} catch (NumberFormatException e) {
			error("[getPmpListeningPort] unrecognized port value: " + port, e);
			return 0;
		}
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void info(String message) {
		DebugUtils.info(this, message);
	}

	private final void info(String message, Throwable t) {
		DebugUtils.info(this, message, t);
	}

	private final void error(String message, Throwable t) {
		DebugUtils.error(this, message, t);
	}

	private class UDPListener implements Runnable {
		private Thread listenerThread;
		private volatile boolean running;
		private DatagramSocket socket;

		public UDPListener() {
			listenerThread = ThreadUtils.createThread(this, "IAgent UDP Listener");
		}

		public void start() {
			running = true;
			listenerThread.start();
		}

		public void dispose() {
			if (!running) {
				return;
			}
			running = false;
			try {
				synchronized (this) {
					if (socket != null) {
						socket.close();
					}
				}
			} catch (Exception e) {
				info("[UDPListener] Error occured while closing UDP socket.", e);
			}
		}

		public void run() {
			byte[] buffer = new byte[UDP_NOTIFICATION.length];
			DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
			try {
				synchronized (this) {
					socket = new DatagramSocket(UDP_LISTENER_PORT);
				}
			} catch (SocketException se) {
				error("[startUDPListener] Cannot open socket for UDP notifications on port " + UDP_LISTENER_PORT, se);
				return;
			}
			while (running) {
				try {
					socket.receive(packet);
					if (Arrays.equals(buffer, UDP_NOTIFICATION)) {
						startServer();
					} else {
						info("[UDPListener] Unknown UDP notification received.");
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
			case IAGENT_CMD_GET_PMP_LISTENING_PORT:
				int port = getPmpListeningPort();
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				try {
					DataFormater.writeInt(bos, port);
				} catch (IOException e) {
				}
				response = msg.respond(0, bos.toByteArray());
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
			if (shutdownOnDisconnect) {
				debug("[disconnected] MBSA Server got disconnected, shutdown the framework");
				stopVM();
			}
		}

	}
}

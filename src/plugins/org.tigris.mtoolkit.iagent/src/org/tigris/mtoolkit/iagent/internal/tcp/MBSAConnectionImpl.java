/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 ****************************************************************************/
package org.tigris.mtoolkit.iagent.internal.tcp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.IAgentCommands;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MBSAConnection;
import org.tigris.mtoolkit.iagent.spi.MBSAConnectionCallBack;
import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.transport.TransportConnection;

/**
 * Implementation of Transport interface over TCP protocol
 * 
 * @author Alexander Petkov
 * @version 1.0
 */
public class MBSAConnectionImpl implements MBSAConnection, Runnable {
	private static final int DATA_MAX_SIZE = Integer.getInteger("iagent.message.size", 2048).intValue() - 15;
	// ping timeout in seconds
	private static final int PING_TIMEOUT = Integer.getInteger("iagent.ping.timeout", 15000).intValue();

	private static final int DEFAULT_MBSA_PORT = 7365;

	private int messageID = 0;
	protected boolean isClient;
	protected String deviceIP;
	protected int port;
	protected int socketTimeout;
	protected Thread pingThread;
	protected long lastCmdTime = 0;

	protected volatile boolean isClosed = false;

	protected Socket deviceSocket;
	protected TransportConnection connection;
	protected Transport transport;
	protected OutputStream os;
	protected InputStream is;

	private Object lock = new Object();
	private Thread sendingThread;
	private ByteArrayOutputStream headerBuffer;

	private ConnectionManagerImpl connManager;
	private Dictionary properties;

	public MBSAConnectionImpl(String device_ip, int port) {
		isClient = true;
		this.deviceIP = device_ip;
		this.port = port;
		headerBuffer = new ByteArrayOutputStream(15);
	}

	public MBSAConnectionImpl(int port, int timeout) {
		isClient = false;
		this.port = port;
		this.socketTimeout = timeout;
	}

	public MBSAConnectionImpl(Dictionary conProperties, ConnectionManagerImpl connManager) throws IAgentException {
		deviceIP = (String) conProperties.get(DeviceConnector.KEY_DEVICE_IP);
		this.properties = conProperties;
		if (deviceIP != null) {
			isClient = true;
			this.port = getConnectionPort();
			headerBuffer = new ByteArrayOutputStream(15);
			connect();
			this.connManager = connManager;
		} else {
			throw new IllegalArgumentException(
					"Connection properties hashtable does not contain device IP value with key DeviceConnector.KEY_DEVICE_IP!");
		}
	}

	public MBSAConnectionImpl(Transport transport, Dictionary conProperties, ConnectionManagerImpl connManager)
			throws IAgentException {
		isClient = true;
		this.properties = conProperties;
		this.port = getConnectionPort();
		headerBuffer = new ByteArrayOutputStream(15);
		connect(transport);
		this.connManager = connManager;
	}

	public void closeConnection() throws IAgentException {
		closeConnection(true);
	}

	public void closeConnection(boolean aSendEvent) throws IAgentException {
		debug("[closeConnection] start");
		boolean sendEvent = false;
		try {
			synchronized (lock) {
				if (isClosed)
					return;
				isClosed = true;
				sendEvent = aSendEvent;
				clean();
				debug("[closeConnection] socket closed!");
				lock.notifyAll();
			}
		} finally { // send event in any case
			if (connManager != null) {
				try {
					connManager.connectionClosed(this, sendEvent);
				} catch (Throwable e) {
					error("Internal failure in connection manager", e);
				}
			}
		}
		debug("[closeConnection] finish");
	}

	private void clean() {
		if (os != null) {
			try {
				os.close();
			} catch (IOException e) {
				// ignore
			}
			os = null;
		}
		debug("[closeConnection] output closed!");
		if (is != null) {
			try {
				is.close();
			} catch (IOException e) {
				// ignore
			}
			is = null;
		}
		debug("[closeConnection] input stream closed!");
		if (deviceSocket != null) {
			try {
				deviceSocket.close();
			} catch (IOException e) {
				// ignore
			}
			deviceSocket = null;
		}
	}

	public MBSAConnectionCallBack sendData(int aCmd, byte[] aData) throws IAgentException {
		return sendData(aCmd, aData, true);
	}

	public MBSAConnectionCallBack sendData(int aCmd, byte[] aData, boolean disconnectOnFailure) throws IAgentException {
		debug("[sendData] aData: " + aData + " aData.length" + (aData != null ? aData.length : 0));
		OutputStream l_os = null;
		InputStream l_is = null;
		Thread oldSendingThread;
		synchronized (lock) {
			Thread currentThread = Thread.currentThread();
			while (sendingThread != null && sendingThread != currentThread) {
				try {
					lock.wait(5000);
				} catch (InterruptedException e) {
				}
				if (isClosed) {
					break;
				}
			}
			if (isClosed)
				throw new IAgentException("Connection to the device is closed!", IAgentErrors.ERROR_DISCONNECTED);
			l_os = os;
			l_is = is;
			// save the previous value so we can properly support
			// method reentrance in the same thread
			oldSendingThread = sendingThread;
			sendingThread = currentThread;
		}
		try {
			messageID++;// increase the message ID
			// send message
			try {
				headerBuffer.reset();// reset the header buffer
				debug("[sendData] sending messageID: " + messageID);
				DataFormater.writeInt(headerBuffer, messageID);// write message
																// ID
				debug("[sendData] messageID sent: " + messageID);
				debug("[sendData] sending command: " + aCmd);
				DataFormater.writeInt(headerBuffer, aCmd);// write command
				debug("[sendData] command sent: " + aCmd);
				debug("[sendData] sending length: " + (aData != null ? aData.length : 0));
				DataFormater.writeInt(headerBuffer, (aData != null ? aData.length : 0));// write
																						// data
																						// length
				l_os.write(headerBuffer.toByteArray());// send header
				l_os.flush();
				debug("[sendData] length sent: " + (aData != null ? aData.length : 0));
				debug("[sendData] sending data: " + aData);
				if (aData != null) {// send data
					l_os.write(aData);// the data should be packed by the upper
										// command layer
					l_os.flush();
					debug("[sendData] data sent: " + aData);
				} else {
					debug("[sendData] data skipped (it is null)");
				}
			} catch (IOException e) {
				if (disconnectOnFailure || connection.isClosed())
					closeConnection();
				else if (e instanceof EOFException && !disconnectOnFailure) {
					clean();
					connect(transport);
				}
				throw new IAgentException(e.getMessage(), isClosed ? IAgentErrors.ERROR_DISCONNECTED
						: IAgentErrors.ERROR_INTERNAL_ERROR, e);
			}

			// get last cmd send time
			lastCmdTime = System.currentTimeMillis();

			// read response
			try {
				debug("[sendData] reading response");
				int rspMessageID = DataFormater.readInt(l_is);// read the
																// response
																// message id
				debug("[sendData] rspMessageID: " + rspMessageID);
				if (rspMessageID == messageID) {// check if the response is for
												// the sent message
					int rspStatus = DataFormater.readInt(l_is);// read the
																// response
																// message
					debug("[sendData] rspStatus: " + rspStatus);
					int rspDataLength = DataFormater.readInt(l_is);// read
																	// response
																	// data
																	// length
					debug("[sendData] rspDataLength: " + rspDataLength);
					byte[] rspData = rspDataLength > 0 ? new byte[rspDataLength] : null;
					debug("[sendData] rspData: " + rspData);
					if (rspDataLength > 0) {
						int readed = l_is.read(rspData);
						while (readed < rspDataLength) {
							readed += l_is.read(rspData, readed, rspDataLength - readed);
						}
					}
					MBSAConnectionCallBack tCallBack = new MBSAConnectionCallBack(rspMessageID, rspStatus, rspData);
					debug("[sendData] tCallBack: " + tCallBack);
					return tCallBack;
				} else {
					closeConnection();
					throw new IAgentException(
							"Protocol error: the send message id is different from the received one!",
							IAgentErrors.ERROR_INTERNAL_ERROR);
				}
			} catch (IOException e) {
				if (disconnectOnFailure || connection.isClosed())
					closeConnection();
				else if (e instanceof EOFException && !disconnectOnFailure) {
					clean();
					connect(transport);
				}
				throw new IAgentException(e.getMessage(), isClosed ? IAgentErrors.ERROR_DISCONNECTED
						: IAgentErrors.ERROR_INTERNAL_ERROR, e);
			}
		} finally {
			synchronized (lock) {
				sendingThread = oldSendingThread;
				lock.notifyAll();
			}
			// get last cmd send time
			lastCmdTime = System.currentTimeMillis();
		}
	}

	/**
	 * Connect to the device using specified params with which this class is
	 * created
	 * 
	 * @throws IAgentException
	 */
	protected void connect() throws IAgentException {
		if (isClient) {
			try {
				debug("[MBSAConnectionImpl][connect] >>> deviceIP: " + deviceIP + "; port: " + port);
				deviceSocket = new Socket(deviceIP, port);
			} catch (UnknownHostException e) {
				throw new IAgentException("Exception trying to establish connection!",
						IAgentErrors.ERROR_CANNOT_CONNECT, e);
			} catch (IOException e) {
				throw new IAgentException("Exception trying to establish connection!",
						IAgentErrors.ERROR_CANNOT_CONNECT, e);
			}
		} else {
			try {
				ServerSocket serverSocket = new ServerSocket(port);
				if (socketTimeout > 0)
					serverSocket.setSoTimeout(socketTimeout);
				deviceSocket = serverSocket.accept();
				try {// close the server socket after device is accepted
					serverSocket.close();
				} catch (IOException exc) {
					// ignore
				}
			} catch (IOException e) {
				closeConnection();
				throw new IAgentException("Exception trying to establish connection!",
						IAgentErrors.ERROR_CANNOT_CONNECT, e);
			}
		}
		if (deviceSocket != null) {
			try {
				os = deviceSocket.getOutputStream();
			} catch (IOException e) {
				closeConnection();
				throw new IAgentException("Exception trying to establish connection!",
						IAgentErrors.ERROR_CANNOT_CONNECT, e);
			}
			try {
				is = deviceSocket.getInputStream();
			} catch (IOException e) {
				closeConnection();
				throw new IAgentException("Exception trying to establish connection!",
						IAgentErrors.ERROR_CANNOT_CONNECT, e);
			}
			// create ping thread
			if (pingThread == null || !pingThread.isAlive()) {
				pingThread = new Thread(this, "[MBSAConnectionImpl][ping]");
				debug("[MBSAConnectionImpl][connect] Starting ping thread: " + pingThread);
				// reset last cmd send time before start thread
				lastCmdTime = System.currentTimeMillis();
				pingThread.start();
			}
			// ensure that the connection is established successfully
			sendData(IAgentCommands.IAGENT_CMD_PING, null);
		}
	}

	/**
	 * Connect to the device using specified params with which this class is
	 * created
	 * 
	 * @throws IAgentException
	 */
	protected void connect(Transport transport) throws IAgentException {
		try {
			debug("[MBSAConnectionImpl][connect] >>> " + transport);
			this.transport = transport;
			connection = transport.createConnection(port);
			os = connection.getOutputStream();
			is = connection.getInputStream();
			// create ping thread
			if (pingThread == null || !pingThread.isAlive()) {
				pingThread = new Thread(this, "[MBSAConnectionImpl][ping]");
				debug("[MBSAConnectionImpl][connect] Starting ping thread: " + pingThread);
				// reset last cmd send time before start thread
				lastCmdTime = System.currentTimeMillis();
				pingThread.start();
			}
			// ensure that the connection is established successfully
			sendData(IAgentCommands.IAGENT_CMD_PING, null);
		} catch (UnknownHostException e) {
			throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
		} catch (IOException e) {
			throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
		}
	}

	public int getDataMaxSize() {
		return DATA_MAX_SIZE;
	}

	public boolean isConnected() {
		return !isClosed;
	}

	public int getType() {
		return ConnectionManager.MBSA_CONNECTION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		debug("[MBSAConnectionImpl][run] Ping thread started: " + Thread.currentThread());

		for (;;) {
			synchronized (lock) {
				if (isClosed) {
					break;
				}
				try {
					lock.wait(PING_TIMEOUT / 5);
				} catch (InterruptedException e) {
					error("[run] Ping thread interrupted: " + Thread.currentThread(), e);
					break;
				}
			}

			long curTime = System.currentTimeMillis();
			if ((curTime - lastCmdTime) > PING_TIMEOUT) {
				try {
					sendData(IAgentCommands.IAGENT_CMD_PING, null);
				} catch (IAgentException e) {
					if (e.getErrorCode() != IAgentErrors.ERROR_DISCONNECTED)
						error("[run] Failed to send ping cmd from thread: " + Thread.currentThread(), e);
					else
						break;
				}
			}
		}

		debug("[MBSAConnectionImpl][run] Ping thread stopped: " + Thread.currentThread());
	}

	private final void debug(String message) {
		DebugUtils.debug(this, message);
	}

	private final void error(String message, Throwable e) {
		DebugUtils.error(this, message, e);
	}

	private int getConnectionPort() {
		Object conPort = properties.get(PROP_MBSA_PORT);
		int port = conPort == null ? DEFAULT_MBSA_PORT : ((Integer) conPort).intValue();
		return port;
	}

	public Object getProperty(String propertyName) {
		if (ConnectionManager.PROP_PMP_PORT.equals(propertyName)) {
			int pmpListeningPort = getPmpListeningPort();
			if (pmpListeningPort != -1) {
				return new Integer(pmpListeningPort);
			}
		}
		return null;
	}

	public int getPmpListeningPort() {
		try {
			MBSAConnectionCallBack callback = sendData(IAgentCommands.IAGENT_CMD_GET_PMP_LISTENING_PORT, null, false);
			if (callback.getRspStatus() == 0) {
				byte[] data = callback.getRspData();
				if (data != null) {
					ByteArrayInputStream bis = null;
					bis = new ByteArrayInputStream(data);
					try {
						return DataFormater.getInt(bis);
					} catch (IOException e) {
						return -1;
					}
				}
			}
		} catch (IAgentException e) {
			error("[getPmpListeningPort] Error getting PMP listening port", e);
		}
		return -1;
	}

}

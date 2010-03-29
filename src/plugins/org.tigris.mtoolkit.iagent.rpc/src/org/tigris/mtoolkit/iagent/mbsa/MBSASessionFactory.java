package org.tigris.mtoolkit.iagent.mbsa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.tigris.mtoolkit.iagent.internal.mbsa.MBSAClientImpl;
import org.tigris.mtoolkit.iagent.internal.mbsa.MBSAServerImpl;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;


public class MBSASessionFactory {

	public static MBSAClient clientConnect(String host, int port) throws MBSAException {
		try {
			Socket socket;
			if (host != null)
				socket = new Socket(host, port);
			else
				// Some environments have problem with using null as host
				socket = new Socket(InetAddress.getLocalHost(), port);
			return wrapClient(socket.getInputStream(), socket.getOutputStream());
		} catch (IOException e) {
			throw new MBSAException(MBSAException.CODE_CANNOT_CONNECT, e);
		}
	}
	
	public static MBSAServer serverConnect(String host, int port, MBSARequestHandler handler) throws MBSAException {
		try {
			Socket socket;
			if (host != null)
				socket = new Socket(host, port);
			else
				// Some environments have problem with using null as host
				socket = new Socket(InetAddress.getLocalHost(), port);
			return wrapServer(handler, socket.getInputStream(), socket.getOutputStream());
		} catch (IOException e) {
			throw new MBSAException(MBSAException.CODE_CANNOT_CONNECT, e);
		}
	}
	
	public static MBSAClient clientListen(InetAddress bindAddress, int port, int timeout) throws MBSAException {
		DebugUtils.debug(MBSASessionFactory.class, "[clientListen] address=" + bindAddress + ", port=" + port + ", timeout=" + timeout);
		ServerSocket ssocket = null;
		try {
			ssocket = new ServerSocket(port, 1, bindAddress);
			if (timeout > 0)
				ssocket.setSoTimeout(timeout);
			Socket clientSocket = ssocket.accept();
			return wrapClient(clientSocket.getInputStream(), clientSocket.getOutputStream());
		} catch (IOException e) {
			DebugUtils.debug(MBSASessionFactory.class, "[clientListen] Cannot connect: address=" + bindAddress + ", port=" + port);
			throw new MBSAException(MBSAException.CODE_CANNOT_CONNECT, e);
		} finally {
			if (ssocket != null) {
				DebugUtils.debug(MBSASessionFactory.class, "[clientListen] Close socket: address=" + bindAddress + ", port=" + port);
				try {
					ssocket.close();
					DebugUtils.debug(MBSASessionFactory.class, "[clientListen] Socket closed: address=" + bindAddress + ", port=" + port);
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * @since 3.0
	 */
	public static MBSAClient clientListen(ServerSocket ssocket) throws MBSAException {
		DebugUtils.debug(MBSASessionFactory.class, "[clientListen] ssocket = " + ssocket);
		try {
			Socket clientSocket = ssocket.accept();
			return wrapClient(clientSocket.getInputStream(), clientSocket.getOutputStream());
		} catch (IOException e) {
			DebugUtils.debug(MBSASessionFactory.class, "[clientListen] Cannot connect: ssocket = " + ssocket);
			throw new MBSAException(MBSAException.CODE_CANNOT_CONNECT, e);
		} finally {
			if (ssocket != null) {
				DebugUtils.debug(MBSASessionFactory.class, "[clientListen] Close socket: ssocket = " + ssocket);
				try {
					ssocket.close();
					DebugUtils.debug(MBSASessionFactory.class, "[clientListen] Socket closed: ssocket = " + ssocket);
				} catch (IOException e) {
				}
			}
		}
	}

	/**
	 * @since 3.0
	 */
	public static ServerSocket getServerSocketOnFreePort(InetAddress bindAddress, int defaultPort, int backlog,
			int timeout) throws IOException {
		ServerSocket ssocket = null;
		try {
			ssocket = new ServerSocket(defaultPort, backlog, bindAddress);
		} catch (IOException e) {
			// the default port is in use, try random free port
			ssocket = new ServerSocket(0, backlog, bindAddress);
		}
		if (timeout > 0)
			ssocket.setSoTimeout(timeout);
		return ssocket;
	}

	public static MBSAServer serverListen(InetAddress bindAddress, int port, int timeout, MBSARequestHandler handler) throws MBSAException {
		try {
			ServerSocket ssocket = new ServerSocket(port, 1, bindAddress);
			if (timeout > 0)
				ssocket.setSoTimeout(timeout);
			Socket clientSocket = ssocket.accept();
			return wrapServer(handler, clientSocket.getInputStream(), clientSocket.getOutputStream());
		} catch (IOException e) {
			throw new MBSAException(MBSAException.CODE_CANNOT_CONNECT, e);
		}
	}
	
	public static MBSAClient wrapClient(InputStream input, OutputStream output) throws MBSAException {
		return new MBSAClientImpl(input, output);
	}

	public static MBSAServer wrapServer(MBSARequestHandler handler, InputStream input, OutputStream output) throws MBSAException {
		return new MBSAServerImpl(handler, input, output);
	}
}

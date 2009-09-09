package org.tigris.mtoolkit.iagent.mbsa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.tigris.mtoolkit.iagent.internal.mbsa.MBSAClientImpl;
import org.tigris.mtoolkit.iagent.internal.mbsa.MBSAServerImpl;


public class MBSASessionFactory {

	public static MBSAClient clientConnect(String host, int port) throws MBSAException {
		try {
			Socket socket = new Socket(host, port);
			return wrapClient(socket.getInputStream(), socket.getOutputStream());
		} catch (IOException e) {
			throw new MBSAException(MBSAException.CODE_CANNOT_CONNECT, e);
		}
	}
	
	public static MBSAServer serverConnect(String host, int port, MBSARequestHandler handler) throws MBSAException {
		try {
			Socket socket = new Socket(host, port);
			return wrapServer(handler, socket.getInputStream(), socket.getOutputStream());
		} catch (IOException e) {
			throw new MBSAException(MBSAException.CODE_CANNOT_CONNECT, e);
		}
	}
	
	public static MBSAClient clientListen(InetAddress bindAddress, int port, int timeout) throws MBSAException {
		try {
			ServerSocket ssocket = new ServerSocket(port, 1, bindAddress);
			if (timeout > 0)
				ssocket.setSoTimeout(timeout);
			Socket clientSocket = ssocket.accept();
			return wrapClient(clientSocket.getInputStream(), clientSocket.getOutputStream());
		} catch (IOException e) {
			throw new MBSAException(MBSAException.CODE_CANNOT_CONNECT, e);
		}
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

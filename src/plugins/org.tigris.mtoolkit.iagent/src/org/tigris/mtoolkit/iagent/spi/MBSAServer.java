package org.tigris.mtoolkit.iagent.spi;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class MBSAServer {

	private class MBSAMessage {
		public int commandId;
		// any fields which are required
	}
	
	private class MBSAResponse {
		// any fields which are required
	}
	
	public void run() {
		ServerSocket server = null;
		try {
			server = new ServerSocket();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// open socket
		// accept
		Socket acceptedSocket = null;
		try {
			acceptedSocket = server.accept();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		handleClient(acceptedSocket);
	}
	
	private void handleClient(Socket socket) {
		// do any initial handshake if the protocol requires
		MBSAMessage message = null;
		try {
			message = readNextMessage(socket.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		handleMessage(message);
	}
	
	private MBSAResponse handleMessage(MBSAMessage message) {
		// I don't know whether the protocol is synchronous or asynchronous
		// check it
		switch (message.commandId) {
		case 1: break;
		case 2: break;
		}
		return new MBSAResponse();
	}
	
	private MBSAMessage readNextMessage(InputStream input) {
		// parse next message
		return new MBSAMessage();
	}
}

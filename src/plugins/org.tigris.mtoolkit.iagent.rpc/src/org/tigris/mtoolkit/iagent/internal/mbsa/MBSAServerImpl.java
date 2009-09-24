package org.tigris.mtoolkit.iagent.internal.mbsa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.mbsa.MBSAConstants;
import org.tigris.mtoolkit.iagent.mbsa.MBSAException;
import org.tigris.mtoolkit.iagent.mbsa.MBSARequest;
import org.tigris.mtoolkit.iagent.mbsa.MBSARequestHandler;
import org.tigris.mtoolkit.iagent.mbsa.MBSAResponse;
import org.tigris.mtoolkit.iagent.mbsa.MBSAServer;

public class MBSAServerImpl implements MBSAServer, Runnable {

	private static final byte SUPPORTED_VERSION = 2;

	private volatile boolean running;
	private Thread serverThread;
	private MBSARequestHandler handler;
	private InputStream input;
	private OutputStream output;
	private volatile boolean closed;
	private byte protocolVersion = 1;

	public MBSAServerImpl(MBSARequestHandler handler, InputStream input, OutputStream output) throws MBSAException {
		if (handler == null || input == null || output == null)
			throw new NullPointerException("one of the arguments is null");
		this.handler = handler;
		this.input = input;
		this.output = output;

		serverThread = new Thread(this);
		serverThread.setName("MBSA Session");
		start();
	}

	private void start() throws MBSAException {
		if (running || closed)
			// if we are already running || session is closed
			return;
		running = true;
		serverThread.start();
	}

	public void run() {
		while (running || !closed) {
			try {
				MBSARequest request = readRequest(input);
				MBSAResponse rsp = handleRequest(request);
				if (!running)
					// check that we are still running
					// request handling might be long operation
					break;
				if (rsp != null)
					sendRsp(rsp, output);
				// else the handler is currently handling the request, it will
				// send the response when ready
			} catch (IOException e) {
				close();
			}
		}
	}

	private MBSAResponse handleRequest(MBSARequest request) throws IOException {
		if (request.getCommand() == MBSAConstants.IAGENT_CMD_PING) {
			respondToPing(request);
			return null;
		}
		MBSAResponse rsp = handler.handleRequest(request);
		if (rsp == null)
			rsp = request.respond(MBSAConstants.IAGENT_RES_INTERNAL_ERROR);
		return rsp;
	}

	private void respondToPing(MBSARequest request) throws IOException {
		MBSAResponse rsp = request.respond();
		byte protocolVersion = 1;
		if (request.available() > 0) {
			protocolVersion = request.readByte();
			log("[pingResponse] Clients supported version: " + protocolVersion);
			rsp.writeByte(SUPPORTED_VERSION);
		}
		rsp.done();
		sendRsp(rsp, output);
		this.protocolVersion = protocolVersion;
	}

	private MBSARequest readRequest(InputStream is) throws IOException {
		log("[readRequest] >>> is: " + is);
		synchronized (is) {
			int msgId = DataFormater.readInt(is);
			int cmdId = DataFormater.readInt(is);
			int cmdLength = DataFormater.readInt(is);
			byte[] data = new byte[cmdLength];
			int readed = is.read(data);
			while (readed < cmdLength) {
				readed += is.read(data, readed, cmdLength - readed);
			}
			MBSARequest request = new MBSARequest(msgId, cmdId, data);
			if (DebugUtils.DEBUG)
				log("[readRequest] <<< " + request);
			return request;
		}
	}

	private void sendRsp(MBSAResponse rsp, OutputStream os) throws IOException {
		if (DebugUtils.DEBUG)
			log("[sendRsp] Send response >>> " + rsp);
		if (protocolVersion > 1)
			try {
				rsp = new MBSAResponse(rsp.getId() | MBSAConstants.IAGENT_FLAGS_RESULT, rsp.getStatus(), rsp.getData())
						.done();
			} catch (MBSAException e) {
				log("[sendRsp] Failed to send response", e);
				rsp = new MBSAResponse(rsp.getId(), MBSAConstants.IAGENT_RES_INTERNAL_ERROR);
				rsp.writeString(e.toString());
				rsp.done();
			}
		rsp.writeTo(os);
		os.flush();
	}

	public void close() {
		running = false;
		closed = true;
		try {
			input.close();
		} catch (IOException e) {
		}

		try {
			output.close();
		} catch (IOException e) {
		}
	}

	private final void log(String message) {
		log(message, null);
	}

	private final void log(String message, Throwable t) {
		if (DebugUtils.DEBUG)
			DebugUtils.log(this, message, t);
	}

	public boolean isClosed() {
		return closed;
	}
}

package org.tigris.mtoolkit.iagent.internal.mbsa;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.mbsa.MBSAClient;
import org.tigris.mtoolkit.iagent.mbsa.MBSACommand;
import org.tigris.mtoolkit.iagent.mbsa.MBSAConstants;
import org.tigris.mtoolkit.iagent.mbsa.MBSAException;
import org.tigris.mtoolkit.iagent.mbsa.MBSAResult;

public class MBSAClientImpl implements MBSAClient {

	private static final byte SUPPORTED_VERSION = 2;

	private InputStream input;
	private OutputStream output;
	private byte protocolVersion = 1;
	private int msgId = 1;

	public MBSAClientImpl(InputStream input, OutputStream output) throws MBSAException {
		this.input = input;
		this.output = output;
		pingWithVersion();
	}

	public void close() {
		try {
			input.close();
		} catch (IOException e) {
		}

		try {
			output.close();
		} catch (IOException e) {
		}
	}

	private void pingWithVersion() throws MBSAException {
		log("[pingWithVersion] Sending an extended ping command");
		MBSACommand cmd = new MBSACommand(MBSAConstants.IAGENT_CMD_PING);
		cmd.writeByte(SUPPORTED_VERSION);
		cmd.done();
		try {
			MBSAResult result = send(cmd);
			// we check that the ping response is 0 (OK)
			if (result.getStatus() == 0 && result.available() > 0) {
				// the server supports version > 2
				protocolVersion = result.readByte();
				log("[pingWithVersion] Remote peer supports protocol " + protocolVersion);
			}
		} catch (IOException e) {
			close();
			log("[pingWithVersion] Failed to do the initial handshake", e);
			throw new MBSAException(MBSAException.CODE_INITIAL_HANDSHAKE, "MBSA session initial handshake failed", e);
		}
	}

	public MBSAResult send(MBSACommand command) throws MBSAException {
		if (DebugUtils.DEBUG)
			log("[send] >>> " + command);
		try {
			return sendAndReceive(command);
		} catch (IOException e) {
			close();
			throw new MBSAException(MBSAException.CODE_PROTOCOL_ERROR, "Command execution failed", e);
		} catch (MBSAException e) {
			if (e.getCode() == MBSAException.CODE_PROTOCOL_ERROR)
				close();
			throw e;
		}
	}

	private MBSAResult sendAndReceive(MBSACommand command) throws IOException, MBSAException {
		int msgId = nextMessageId();
		sendCmd(command, output, msgId);
		// we don't have a server thread, which reads the messages
		// read the result as request and match it against the command
		MBSAResult result = readResult(input);
		validateResult(result, msgId);
		return result;
	}

	private void validateResult(MBSAResult req, int msgId) throws MBSAException {
		if (DebugUtils.DEBUG)
			log("[matchResult] >>> msgId: " + msgId + "; req: " + req + "; version: " + protocolVersion);
		switch (protocolVersion) {
		case 1:
			if (req.getId() == msgId)
				break; // everything is OK
			// fall through the protocol version 2
		case 2:
		default:
			if ((req.getId() & (~MBSAConstants.IAGENT_FLAGS_RESULT)) != msgId)
				throw new MBSAException(MBSAException.CODE_PROTOCOL_ERROR,
						"Protocol error: received response is not expected");
			if ((req.getId() & MBSAConstants.IAGENT_FLAGS_RESULT) == 0)
				throw new MBSAException(MBSAException.CODE_PROTOCOL_ERROR,
						"Protocol error: expected response but received command");
			break; // it's OK
		}
	}

	private void sendCmd(MBSACommand cmd, OutputStream os, int msgId) throws IOException {
		cmd.validate();
		if (DebugUtils.DEBUG)
			log("[sendCmd] Send command msgId: " + msgId + " >>> " + cmd);
		DataFormater.writeInt(os, msgId);
		cmd.writeTo(os);
		os.flush();
	}

	private MBSAResult readResult(InputStream is) throws IOException {
		log("[readResult] >>> is: " + is);
		int msgId = DataFormater.readInt(is);
		int cmdId = DataFormater.readInt(is);
		int cmdLength = DataFormater.readInt(is);
		byte[] data = new byte[cmdLength];
		int readed = is.read(data);
		while (readed < cmdLength) {
			readed += is.read(data, readed, cmdLength - readed);
		}
		MBSAResult result = new MBSAResult(msgId, cmdId, data);
		if (DebugUtils.DEBUG)
			log("[readResult] <<< " + result);
		return result;
	}

	private synchronized int nextMessageId() {
		return msgId++;
	}

	private final void log(String message) {
		log(message, null);
	}

	private final void log(String message, Throwable t) {
		if (DebugUtils.DEBUG)
			DebugUtils.log(this, message, t);
	}
}

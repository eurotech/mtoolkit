package org.tigris.mtoolkit.iagent.mbsa;

import java.io.IOException;
import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.internal.mbsa.DataFormater;

public class MBSAResponse extends MBSAWriter {

	private int msgId;
	private int status;

	public MBSAResponse(int msgId, int status) {
		this.msgId = msgId;
		this.status = status;
	}

	public MBSAResponse(int msgId, int status, byte[] data) {
		super(data);
		this.msgId = msgId;
		this.status = status;
	}
	
	public int getId() {
		return msgId;
	}
	
	public int getStatus() {
		return status;
	}

	public void writeTo(OutputStream os) throws IOException {
		validate();
		DataFormater.writeInt(os, msgId);
		DataFormater.writeInt(os, status);
		super.writeTo(os);
	}

	public MBSAResponse done() {
		flush();
		return this;
	}

	public String toString() {
		String dataLength = Integer.toString(getData().length);
		return "MBSAResponse[msgId=" + msgId + ";status=" + status + ";data.length=" + dataLength + "]";
	}
}

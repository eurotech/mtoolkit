package org.tigris.mtoolkit.iagent.mbsa;


public class MBSARequest extends MBSAReader {

	private int msgId;
	private int cmdId;

	public MBSARequest(int msgId, int cmdId, byte[] data) {
		super(data);
		this.msgId = msgId;
		this.cmdId = cmdId;
	}

	public int getCommand() {
		return cmdId;
	}
	
	public int getId() {
		return msgId;
	}

	public MBSAResponse respond() {
		return respond(MBSAConstants.IAGENT_RES_OK, null);
	}

	public MBSAResponse respond(int status) {
		return respond(status, null);
	}

	public MBSAResponse respond(int status, byte[] data) {
		return new MBSAResponse(msgId, status, data);
	}

	public String toString() {
		return "MBSARequest[msgId=" + msgId + ";cmdId=" + cmdId + ";data.length=" + getData().length + "]";
	}
	
}

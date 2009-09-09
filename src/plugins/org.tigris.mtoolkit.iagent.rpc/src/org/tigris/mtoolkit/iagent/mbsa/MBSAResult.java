package org.tigris.mtoolkit.iagent.mbsa;

public class MBSAResult extends MBSAReader {

	private int msgId;
	private int status;

	public MBSAResult(int msgId, int status, byte[] data) {
		super(data);
		this.status = status;
		this.msgId = msgId;
	}
	
	public int getId() {
		return msgId;
	}

	public int getStatus() {
		return status;
	}
	
	public String toString() {
		return "MBSAResult[msgId=" + msgId + ";status=" + status + ";data.length=" + getData().length + "]";
	}
	
}

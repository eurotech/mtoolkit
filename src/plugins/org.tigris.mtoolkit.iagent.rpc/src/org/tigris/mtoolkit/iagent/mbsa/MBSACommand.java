package org.tigris.mtoolkit.iagent.mbsa;

import java.io.IOException;
import java.io.OutputStream;

import org.tigris.mtoolkit.iagent.internal.mbsa.DataFormater;

public class MBSACommand extends MBSAWriter {

	private int cmdId;

	public MBSACommand(int cmdId) {
		this.cmdId = cmdId;
	}

	public MBSACommand(int cmdId, byte[] data) {
		super(data);
		this.cmdId = cmdId;
	}
	
	public int getCommand() {
		return cmdId;
	}
	
	public MBSACommand done() throws MBSAException {
		flush();
		return this;
	}
	
	public void writeTo(OutputStream os) throws IOException {
		validate();
		DataFormater.writeInt(os, cmdId);
		super.writeTo(os);
	}
	
	public String toString() {
		String dataLength;
		try {
			dataLength = Integer.toString(getData().length);
		} catch (MBSAException e) {
			dataLength = "(not flushed)";
		}
		return "MBSACommand[cmdId=" + cmdId + ";data.length=" + dataLength + "]";
	}
	
}

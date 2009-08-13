package org.tigris.mtoolkit.iagent.spi;

/**
 * Holds the data received from the device as command response.
 * 
 * @version 1.0
 */
public class MBSAConnectionCallBack {
  protected int messageID;
  protected int rspStatus;
  protected byte rspData[];
  
  /**
   * Creates new response callback 
   * 
   * @param aMessageID
   * @param aRspStatus
   * @param aRspData
   */
  public MBSAConnectionCallBack(int aMessageID, int aRspStatus, byte[] aRspData) {
    this.messageID = aMessageID;
    this.rspStatus = aRspStatus;
    this.rspData = aRspData;
  }

  /**
   * Returns the message id of this response needed only for the transport layer
   * 
   * @return
   */
  public int getMessageID() {
    return messageID;
  }

  /**
   * Returns the response status code.
   * 
   * @return
   */
  public int getRspStatus() {
    return rspStatus;
  }

  /**
   * Returns the response data
   * 
   * @return
   */
  public byte[] getRspData() {
    return rspData;
  }
  
  /**
   * Return the response data length
   * 
   * @return
   */
  public int getRspLength() {
    return rspData != null ? rspData.length : 0;
  }
  
  public String toString() {
    String result = "\n[TransportCallBack]>>>" +
                    "\n  messageID: " + messageID +
                    "\n  rspStatus: " + rspStatus +
                    "\n  rspData: " + rspData +
                    "\n  rspData.length: " + (rspData != null ? rspData.length : 0) + 
                    "\n<<<[TransportCallBack]";
    return result;
  }
}

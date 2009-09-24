package org.tigris.mtoolkit.iagent.internal.tcp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.internal.IAgentCommands;
import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.spi.ConnectionManager;
import org.tigris.mtoolkit.iagent.spi.MBSAConnection;
import org.tigris.mtoolkit.iagent.spi.MBSAConnectionCallBack;
import org.tigris.mtoolkit.iagent.transport.Transport;
import org.tigris.mtoolkit.iagent.transport.TransportConnection;

/**
 * Implementation of Transport interface over TCP protocol 
 *
 * @author Alexander Petkov
 * @version 1.0
 */
public class MBSAConnectionImpl implements MBSAConnection, Runnable {
  private static final int DATA_MAX_SIZE = Integer.getInteger("iagent.message.size", 2048).intValue() - 15;
  // ping timeout in seconds
  private static final int PING_TIMEOUT = Integer.getInteger("iagent.ping.timeout", 15000).intValue(); 
  private int messageID = 0;
  protected boolean isClient;
  protected String deviceIP;
  protected int port;
  protected int socketTimeout;
  protected Thread pingThread;
  protected long lastCmdTime = 0;

  protected volatile boolean isClosed = false;

  protected Socket deviceSocket;
  protected TransportConnection connection;
  protected OutputStream os;
  protected InputStream is;

  private Object lock = new Object();
  private boolean isSending = false;
  private ByteArrayOutputStream headerBuffer;

  private ConnectionManagerImpl connManager;

  public MBSAConnectionImpl(String device_ip, int port) {
    isClient = true;
    this.deviceIP = device_ip;
    this.port = port;
    headerBuffer = new ByteArrayOutputStream(15);
  }

  public MBSAConnectionImpl(int port, int timeout) {
    isClient = false;
    this.port = port;
    this.socketTimeout = timeout;
  }

  public MBSAConnectionImpl(Dictionary conProperties, ConnectionManagerImpl connManager) throws IAgentException {
    deviceIP = (String) conProperties.get(DeviceConnector.KEY_DEVICE_IP);
    if ( deviceIP != null ) {
      isClient = true;
      this.port = 7365;
      headerBuffer = new ByteArrayOutputStream(15);
      connect();
      this.connManager = connManager;
    } else {
      throw new IllegalArgumentException("Connection properties hashtable does not contain device IP value with key DeviceConnector.KEY_DEVICE_IP!");
    }    
  }

  public MBSAConnectionImpl(Transport transport, Dictionary conProperties, ConnectionManagerImpl connManager) throws IAgentException {
    isClient = true;
    this.port = 7365;
    headerBuffer = new ByteArrayOutputStream(15);
    connect(transport);
    this.connManager = connManager;
  }

  public void closeConnection() throws IAgentException {
    log("[closeConnection] start");
    boolean sendEvent = false;
    try {
      synchronized (lock) {
        if ( isClosed )
          return;
        isClosed = true;
        sendEvent = true;
        if ( os != null ) {
          try {
            os.close();
          } catch (IOException e) {
            // ignore
          }
          os = null;
        }
        log("[closeConnection] output closed!");
        if ( is != null ) {
          try {
            is.close();
          } catch (IOException e) {
            // ignore
          }
          is = null;
        }
        log("[closeConnection] input stream closed!");
        if ( deviceSocket != null ) {
          try {
            deviceSocket.close();
          } catch (IOException e) {
            // ignore
          }
          deviceSocket = null;
        }
        log("[closeConnection] socket closed!");
        lock.notifyAll();
      }
    } finally { // send event in any case
      if (sendEvent && connManager != null) {
        try {
          connManager.connectionClosed(this);
        } catch (Throwable e) {
          log("Internal failure in connection manager", e);
        }
      }
    }
    log("[closeConnection] finish");
  }

  public MBSAConnectionCallBack sendData(int aCmd, byte[] aData) throws IAgentException {
    log("[sendData] aData: " + aData + " aData.length" + ( aData != null ? aData.length : 0));
    OutputStream l_os = null;
    InputStream l_is = null;
    synchronized (lock) {
      while(isSending) {
        try {
          lock.wait(5000);
        } catch (InterruptedException e) {
        }
        if (isClosed) {
          break;
        }
      }
      if ( isClosed )
        throw new IAgentException("Connection to the device is closed!", IAgentErrors.ERROR_DISCONNECTED);
      l_os = os;
      l_is = is;
      isSending = true;
    }
    try {
      messageID++;//increase the message ID
      //send message
      try {
        headerBuffer.reset();//reset the header buffer
        log("[sendData] sending messageID: " + messageID);
        DataFormater.writeInt(headerBuffer, messageID);//write message ID
        log("[sendData] messageID sent: " + messageID);
        log("[sendData] sending command: " + aCmd);
        DataFormater.writeInt(headerBuffer, aCmd);//write command
        log("[sendData] command sent: " + aCmd);
        log("[sendData] sending length: " + (aData != null ? aData.length : 0));
        DataFormater.writeInt(headerBuffer, (aData != null ? aData.length : 0));//write data length
        l_os.write(headerBuffer.toByteArray());//send header
        l_os.flush();
        log("[sendData] length sent: " + (aData != null ? aData.length : 0));
        log("[sendData] sending data: " + aData);
        if ( aData != null ) {//send data
          l_os.write(aData);//the data should be packed by the upper command layer
          l_os.flush();
          log("[sendData] data sent: " + aData);
        } else {
          log("[sendData] data skipped (it is null)");
        }
      } catch (IOException e) {
        closeConnection();
        throw new IAgentException(e.getMessage(), IAgentErrors.ERROR_INTERNAL_ERROR, e);
      }
      
      // get last cmd send time
      lastCmdTime = System.currentTimeMillis();
      
      //read response
      try {
          log("[sendData] reading response");
        int rspMessageID = DataFormater.readInt(l_is);//read the response message id
        log("[sendData] rspMessageID: " + rspMessageID);
        if ( rspMessageID == messageID ) {//check if the response is for the sent message
          int rspStatus = DataFormater.readInt(l_is);//read the response message
          log("[sendData] rspStatus: " + rspStatus);
          int rspDataLength = DataFormater.readInt(l_is);//read response data length
          log("[sendData] rspDataLength: " + rspDataLength);
          byte[] rspData = rspDataLength > 0 ? new byte[rspDataLength] : null;
          log("[sendData] rspData: " + rspData);
          if ( rspDataLength > 0 ) {
            int readed = l_is.read(rspData);
            while ( readed < rspDataLength ) {
              readed += l_is.read(rspData, readed, rspDataLength - readed);
            }
          }
          MBSAConnectionCallBack tCallBack = new MBSAConnectionCallBack(rspMessageID, rspStatus, rspData);
          log("[sendData] tCallBack: " + tCallBack);
          return tCallBack;
        } else {
          closeConnection();
          throw new IAgentException("Protocol error: the send message id is different from the received one!", IAgentErrors.ERROR_INTERNAL_ERROR);
        }
      } catch (IOException e) {
        closeConnection();
        throw new IAgentException(e.getMessage(), IAgentErrors.ERROR_INTERNAL_ERROR, e);
      }
    } finally {
      synchronized (lock) {
        isSending = false;
        lock.notifyAll();
      }
      // get last cmd send time
      lastCmdTime = System.currentTimeMillis(); 
    }
  }

  /**
   * Connect to the device using specified params with which this class is created
   * 
   * @throws IAgentException
   */
  protected void connect() throws IAgentException {
    if ( isClient ) {
      try {
        log("[MBSAConnectionImpl][connect] >>> deviceIP: " + deviceIP + "; port: " + port);
        deviceSocket = new Socket(deviceIP, port);
      } catch (UnknownHostException e) {
        throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
      } catch (IOException e) {
        throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
      }
    } else {
      try {
        ServerSocket serverSocket = new ServerSocket(port);
        if ( socketTimeout > 0 )
          serverSocket.setSoTimeout(socketTimeout);
        deviceSocket = serverSocket.accept();
        try {//close the server socket after device is accepted
          serverSocket.close();
        } catch (IOException exc) {
          // ignore
        }
      } catch (IOException e) {
        closeConnection();
        throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
      }
    }
    if ( deviceSocket != null ) {
      try {
        os = deviceSocket.getOutputStream();
      } catch (IOException e) {
        closeConnection();
        throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
      }
      try {
        is = deviceSocket.getInputStream();
      } catch (IOException e) {
        closeConnection();
        throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
      }
      // create ping thread
      if (pingThread == null || !pingThread.isAlive()) {
        pingThread = new Thread(this, "[MBSAConnectionImpl][ping]");
        log("[MBSAConnectionImpl][connect] Starting ping thread: " + pingThread);
        // reset last cmd send time before start thread
        lastCmdTime = System.currentTimeMillis();              
        pingThread.start();
      }
    }
  }

  /**
   * Connect to the device using specified params with which this class is created
   * 
   * @throws IAgentException
   */
  protected void connect(Transport transport) throws IAgentException {
    try {
  	  log("[MBSAConnectionImpl][connect] >>> " + transport);
      connection = transport.createConnection(port);
      os = connection.getOutputStream();
      is = connection.getInputStream();
	  // create ping thread
      if (pingThread == null || !pingThread.isAlive()) {
        pingThread = new Thread(this, "[MBSAConnectionImpl][ping]");
        log("[MBSAConnectionImpl][connect] Starting ping thread: " + pingThread);
        // reset last cmd send time before start thread
        lastCmdTime = System.currentTimeMillis();              
        pingThread.start();
      }
    } catch (UnknownHostException e) {
      throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
    } catch (IOException e) {
      throw new IAgentException("Exception trying to establish connection!", IAgentErrors.ERROR_CANNOT_CONNECT, e);
    }
  }

  public int getDataMaxSize() {
    return DATA_MAX_SIZE;
  }

  public boolean isConnected() {
    return !isClosed;
  }

  public int getType() {
    return ConnectionManager.MBSA_CONNECTION;
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  public void run() {
    log("[MBSAConnectionImpl][run] Ping thread started: " + Thread.currentThread());
    
    for(;;) {
      synchronized (lock) {
        if (isClosed) {
          break;
        }
        try {
          lock.wait(PING_TIMEOUT/5);
        } catch (InterruptedException e) {
          log("[MBSAConnectionImpl][run] Ping thread interrupted: " + Thread.currentThread(), e);
          break;
        }
      }
      
      long curTime = System.currentTimeMillis();
      if ((curTime - lastCmdTime) > PING_TIMEOUT) {
        try {
          sendData(IAgentCommands.IAGENT_CMD_PING, null);
        } catch (Exception e) {
          log("[MBSAConnectionImpl][run] Failed to send ping cmd from thread: " + Thread.currentThread(), e);
          break;          
        }
      }
    }
    
    log("[MBSAConnectionImpl][run] Ping thread stopped: " + Thread.currentThread());
  }
  
  private final void log(String message) {
		log(message, null);
	}

	private final void log(String message, Throwable e) {
		DebugUtils.log(this, message, e);
	}

}

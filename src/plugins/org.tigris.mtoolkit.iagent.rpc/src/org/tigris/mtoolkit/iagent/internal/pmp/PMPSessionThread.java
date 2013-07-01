/*******************************************************************************
 * Copyright (c) 2005, 2009 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal.pmp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import org.tigris.mtoolkit.iagent.internal.utils.DebugUtils;
import org.tigris.mtoolkit.iagent.internal.utils.ThreadPool;
import org.tigris.mtoolkit.iagent.internal.utils.ThreadUtils;
import org.tigris.mtoolkit.iagent.transport.TransportConnection;

public class PMPSessionThread implements Runnable {
  protected PMPOutputStream     os;
  protected PMPInputStream      is;
  protected String              url;

  protected boolean             running;

  private Connection            connection;
  private Socket                socket;
  private TransportConnection   transportConnection;

  /**
   * Indicates that the initial handshake has been passed successfully
   */
  boolean                       connected                      = false;

  /** current message id & operation id */
  private short                 msgID;
  private int                   opID;
  private PMPAnswer             answer;

  protected int                 maxS                           = -1;
  protected int                 maxA                           = -1;

  protected PMPPeerImpl         peer;
  protected String              sessionID;

  private Vector                eventTypes;

  /** the object id that the nest registered remote object will receive */

  private static int            objID                          = 1;

  private Map                   objects;

  protected static final int    PING_REQ_OP                    = 0;
  protected static final byte[] PING                           = {
                                                                 (byte) PING_REQ_OP
                                                               };                              // 00000000
  protected static final int    PING_REPLY_OP                  = 144;
  protected static final byte[] PING_REPLY                     = {
                                                                 (byte) PING_REPLY_OP
                                                               };                              // - 16
  // 10000110

  protected static final int    CONNECT_REPLY_OP               = 1;
  protected static final byte[] CONNECT_REPLY                  = {
                                                                 (byte) CONNECT_REPLY_OP
                                                               };                              // 00000001
  protected static final int    REFERENCE_REPLY_OP             = 2;
  protected static final byte[] REFERENCE                      = {
                                                                 (byte) REFERENCE_REPLY_OP
                                                               };                              // 00000010
  protected static final int    SERIALIZED_OBJ_REPLY_OP        = 4;
  protected static final byte[] SERIALIZED                     = {
                                                                 (byte) SERIALIZED_OBJ_REPLY_OP
                                                               };                              // 00000100
  protected static final int    EVENT_LISTENER_REPLY_OP        = 8;
  protected static final byte[] EV_REPLY                       = {
                                                                 (byte) EVENT_LISTENER_REPLY_OP
                                                               };                              // 00001000
  protected static final int    EVENT_LISTENER_FAILED_REPLY_OP = 24;
  protected static final byte[] EV_FAILED                      = {
                                                                 (byte) EVENT_LISTENER_FAILED_REPLY_OP
                                                               };                              // 8+16
  // 00011000
  protected static final int    NEW_EVENT_REQ_OP               = 16;
  protected static final byte[] EVENT                          = {
                                                                 (byte) NEW_EVENT_REQ_OP
                                                               };                              // 00010000

  protected static final int    GET_METHODS_REPLY_OP           = 32;
  protected static final byte[] METHODS                        = {
                                                                 (byte) GET_METHODS_REPLY_OP
                                                               };                              // 00100000
  protected static final int    GET_METHOD_REPLY_OP            = 96;
  protected static final byte[] METHOD                         = {
                                                                 (byte) GET_METHOD_REPLY_OP
                                                               };                              // 32+64
  // 01100000
  protected static final int    DISCONNECT_REQ_OP              = 64;
  protected static final byte[] DISCONNECT                     = {
                                                                 (byte) DISCONNECT_REQ_OP
                                                               };                              // 01000000

  protected static final int    CONNECT_REQ_OP                 = 129;
  protected static final byte[] CONNECT                        = {
                                                                 (byte) CONNECT_REQ_OP
                                                               };                              // -1
  // 10000001
  protected static final int    GET_REFERENCE_REQ_OP           = 130;
  protected static final byte[] GET_REF                        = {
                                                                 (byte) GET_REFERENCE_REQ_OP
                                                               };                              // -2
  // 10000010
  protected static final int    GET_REFERENCE_BY_ID_REQ_OP     = 134;
  protected static final byte[] GET_REF_BY_ID                  = {
                                                                 (byte) GET_REFERENCE_BY_ID_REQ_OP
                                                               };                              // -(2+4)
  // 10000110

  protected static final int    INVOKE_METHOD_REQ_OP           = 132;
  protected static final byte[] INVOKE                         = {
                                                                 (byte) INVOKE_METHOD_REQ_OP
                                                               };                              // -4
  // 10000100
  protected static final int    INVOKE_METHOD_WITH_REFS_REQ_OP = 140;
  protected static final byte[] INVOKE_R                       = {
                                                                 (byte) INVOKE_METHOD_WITH_REFS_REQ_OP
                                                               };                              // -
  // (4+8)
  // 10001100

  protected static final int    ADD_LISTENER_REQ_OP            = 136;
  protected static final byte[] ADD_LS                         = {
                                                                 (byte) ADD_LISTENER_REQ_OP
                                                               };                              // -8
  // 10001000
  protected static final int    REMOVE_LISTENER_REQ_OP         = 152;
  protected static final byte[] REMOVE_LS                      = {
                                                                 (byte) REMOVE_LISTENER_REQ_OP
                                                               };                              // -(8+16)
  // 10011000

  protected static final int    GET_METHOD_REQ_OP              = 160;
  protected static final byte[] GET_METHOD                     = {
                                                                 (byte) GET_METHOD_REQ_OP
                                                               };                              // -32
  // 10100000

  protected static final int    DISPOSE_REQ_OP                 = 192;
  protected static final byte[] DISPOSE                        = {
                                                                 (byte) DISPOSE_REQ_OP
                                                               };                              // -64
  // 11000000

  private static final String   ERRMSG1                        = "Protocol Error";
  protected static final String ERRMSG2                        = "Write Error";
  private static final String   ERRMSG3                        = "Read Error";
  private static final String   DSCMSG1                        = "Normal Disconnect Received";
  private static final String   DSCMSG2                        = "Error Disconnect Received: ";

  protected ThreadPool          pool;
  private Thread                sessionThread;

  public PMPSessionThread(PMPPeerImpl peer, Socket socket, String sessionID, String host) throws IOException {
    sessionThread = ThreadUtils.createThread(this, "PMP " + peer.getRole() + " Thread [" + host + "]"); //$NON-NLS-1$
    this.url = host;
    this.socket = socket;
    this.sessionID = sessionID;

    os = new PMPOutputStream(socket.getOutputStream(), this);
    is = new PMPInputStream(socket.getInputStream(), this);
    running = true;

    connection = new Connection(is, os, this);
    objects = new Hashtable();
    this.peer = peer;
    this.pool = peer.pool;
    maxS = peer.maxStringLength;
    maxA = peer.maxArrayLength;
    sessionThread.start();
  }

  public PMPSessionThread(PMPPeerImpl peer, TransportConnection tc, String sessionID) throws IOException {
    sessionThread = ThreadUtils.createThread(this, "PMP " + peer.getRole() + " Thread [" + tc + "]"); //$NON-NLS-1$
    this.transportConnection = tc;
    this.sessionID = sessionID;

    os = new PMPOutputStream(tc.getOutputStream(), this);
    is = new PMPInputStream(tc.getInputStream(), this);
    running = true;

    connection = new Connection(is, os, this);
    objects = new Hashtable();
    this.peer = peer;
    this.pool = peer.pool;
    maxS = peer.maxStringLength;
    maxA = peer.maxArrayLength;
    sessionThread.start();
  }

  public void run() {
    try {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "Thread started.");
      }
      while (running) {
        try {
          msgID = is.nextMessage();
          if (DebugUtils.DEBUG_ENABLED) {
            DebugUtils.debug(this, "MID " + msgID + " : " + this + " : " + this.hashCode());
          }
          opID = is.read();
          if (DebugUtils.DEBUG_ENABLED) {
            DebugUtils.debug(this, "Operation: " + opID);
          }
          switch (opID) {
          case -1:
            DebugUtils.error(this, ERRMSG1, null);
            try {
              os.begin((short) -1);
              os.write(DISCONNECT);
              PMPData.writeString(ERRMSG1, os);
              os.end(false);
            } catch (Throwable exc) {
            }
            disconnect(ERRMSG1, false);
            return;
          case PING_REPLY_OP:
            // received ping replay;
            os.ping = false;
            break;
          case PING_REQ_OP:
            // //ping request;
            if (connected) {
              ping_repl();
            } else {
              disconnect("Not Connected", true);
            }
            break;
          case DISCONNECT_REQ_OP:
            // DISCONNECT request
            String errMsg = PMPData.readString(is, maxS);
            if (errMsg == null || errMsg.length() == 0) {
              // normal disconnect
              if (DebugUtils.DEBUG_ENABLED) {
                DebugUtils.debug(this, DSCMSG1);
              }
            } else {
              // error disconnect
              if (DebugUtils.DEBUG_ENABLED) {
                DebugUtils.debug(this, DSCMSG2 + errMsg);
              }
            }
            disconnect(errMsg, false);
            break;
          case CONNECT_REQ_OP:
            connect();
            connected = true;
            connection.connected = true;
            break;
          case GET_REFERENCE_REQ_OP:
          case GET_REFERENCE_BY_ID_REQ_OP:
            getReference(opID);
            break;
          case INVOKE_METHOD_REQ_OP:
          case INVOKE_METHOD_WITH_REFS_REQ_OP:
            invokeMethod();
            break;
          case ADD_LISTENER_REQ_OP:
          case REMOVE_LISTENER_REQ_OP:
            remoteListener(opID);
            break;
          case GET_METHOD_REQ_OP:
            getMethod();
            break;
          case DISPOSE_REQ_OP:
            dispose();
            break;
          case CONNECT_REPLY_OP:
            connectReplay();
            break;
          case REFERENCE_REPLY_OP:
            readReference();
            break;
          case SERIALIZED_OBJ_REPLY_OP:
            getObject();
            break;
          case EVENT_LISTENER_REPLY_OP:
          case EVENT_LISTENER_FAILED_REPLY_OP:
            eventReply(opID);
            break;
          case NEW_EVENT_REQ_OP:
            readEvent(opID);
            break;
          case GET_METHOD_REPLY_OP:
          case GET_METHODS_REPLY_OP:
            readMethods(opID);
            break;
          }
        } catch (IOException exc) {
          if (!isConBroken(exc)) {
            DebugUtils.error(this, "An unexpected error occurred: " + exc.toString(), exc);
          }
          disconnect("Disconnecting...", false);
        } catch (Exception exc) {
          if (DebugUtils.DEBUG_ENABLED) {
            DebugUtils.debug(this, "Runtime Exception " + exc);
          }
        }
      }
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "Thread ended.");
      }
    } finally {
      disconnect("Closing Connection ", true);
    }
  }

  private boolean prepareAnswerReply() {
    synchronized (os.answers) {
      answer = (PMPAnswer) os.answers.remove(new Short(msgID));
    }
    if (answer == null) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "No Such Message ID " + msgID);
      }
      return false;
    }
    return true;
  }

  /** Called to assign object id to a new remote object */
  protected int addRemoteObject(Object obj, Class[] interfaces, Object context) {
    synchronized (objects) {
      ObjectInfo info = new ObjectInfo(obj, interfaces, context);
      return addRemoteObject(info);
    }
  }

  protected int addRemoteObject(Object obj, Class[] interfaces) {
    synchronized (objects) {
      ObjectInfo info = new ObjectInfo(obj, interfaces);
      return addRemoteObject(info);
    }
  }

  protected int addRemoteObject(ObjectInfo info) {
    synchronized (objects) {
      do {
        if (++objID < 0) {
          objID = 1;
        }
        if (objects.get(new Integer(objID)) == null) {
          break;
        }
      } while (true);
      objects.put(new Integer(objID), info);
      return objID;
    }
  }

  protected void disconnect(String errMsg, boolean toAnswer) {
    if (!running) {
      return;
    }
    running = false;
    sessionThread.interrupt();
    connection.disconnected(errMsg);
    peer.removeElement(this);
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "Disconnecting Client " + ((url != null) ? url : transportConnection.toString()));
    }

    peer.fireConnectionEvent(false, this);
    if (toAnswer && connected) {
      try {
        os.begin((short) -1);
        os.write(DISCONNECT);
        PMPData.writeString(errMsg, os);
        os.end(false);
      } catch (Throwable exc) {
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "Can't Send Disconnect: " + exc.toString());
        }
      }
    }

    try {
      os.close();
      is.close();
    } catch (Exception exc) {
      DebugUtils.error(this, "Close Error", exc);
    }
    if (toAnswer) {
      try {
        // old connection method compatibility
        if (socket != null) {
          socket.close();
        } else {
          transportConnection.close();
        }
      } catch (Exception exc) {
      }
    }
    if (eventTypes != null) {
      peer.removeListeners(eventTypes, this);
    }

    synchronized (objects) {
      for (Iterator it = objects.keySet().iterator(); it.hasNext();) {
        try {
          ObjectInfo info = (ObjectInfo) it.next();
          it.remove();
          if (info != null) {
            info.freeInfo();
          }
        } catch (Exception exc) {
        }
      }
    }
    if (answer != null) {
      answer.errMsg = errMsg;
      answer.finish();
    }
    objects.clear();
  }

  private void connect() throws IOException {
    try {
      is.timeout = PMPData.readInt(is);
    } catch (Exception ioExc) {
      if (running) {
        os.begin(msgID);
        try {
          os.write(CONNECT_REPLY);
          os.write(1); // success
          PMPData.writeString(ioExc.toString(), os);
          os.end(false);
        } catch (Exception exc) {
          DebugUtils.error(this, ERRMSG2, exc);
          os.unlock();
        }
      }
      if (ioExc instanceof IOException) {
        throw (IOException) ioExc;
      }
      throw new IOException(ioExc.getMessage());
    }
    try {
      os.begin(msgID);
      os.write(CONNECT_REPLY);
      os.write(0); // failure
      os.end(false);
    } catch (Exception exc) {
      DebugUtils.error(this, ERRMSG2, exc);
      os.unlock();
    }
    peer.fireConnectionEvent(true, this);
  }

  private void getReference(int opID) throws IOException {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    String clazz = null;
    String filter = null;
    long bid = 0;
    try {
      clazz = PMPData.readString(is, maxS);
      filter = PMPData.readString(is, maxS);
      if (filter.length() == 0) {
        filter = null;
      }
      bid = ((opID == 6)) ? PMPData.readLong(is) : 0;
    } catch (IOException ioExc) {
      if (running) {
        writeRef(0, ioExc.toString());
      }
      throw ioExc;
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "Getting Service " + clazz + " " + filter + ((bid == 0) ? "" : " " + bid));
    }
    String errMsg = null;
    int objID = 0;
    ObjectInfo newInfo = peer.getService(clazz, filter);
    if (newInfo != null) {
      objID = addRemoteObject(newInfo);
    } else {
      errMsg = "Requested service " + clazz + " [" + filter + "] is not available.";
    }
    writeRef(objID, errMsg);
  }

  private void writeRef(int objID, String errMsg) {
    try {
      os.begin(msgID);
      os.write(REFERENCE);
      PMPData.writeInt(objID, os);
      if (errMsg != null) {
        PMPData.writeString(errMsg, os);
      }
      os.end(false);
    } catch (Exception exc) {
      DebugUtils.error(this, ERRMSG2, exc);
      os.unlock();
    }
  }

  private void invokeMethod() throws IOException {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "Method Invocation Request ...");
    }
    int objID = 0;
    int methodID = 0;
    try {
      objID = PMPData.readInt(is);
      methodID = PMPData.readInt(is);
    } catch (IOException ioExc) {
      if (running) {
        writeInvocationError(ioExc.toString(), msgID);
      }
      throw ioExc;
    }
    ObjectInfo info = (ObjectInfo) objects.get(new Integer(objID));
    if (info == null) {
      writeInvocationError("No Remote Object With ID: " + objID + " On Server", msgID);
      return;
    }
    Method m = null;
    if (methodID < 0 || methodID > info.methods.size()) { // optimizataion
      writeInvocationError("No Method With ID: " + methodID + " Associated With This Remote Object", msgID);
      return;
    }
    try {
      m = (Method) info.methods.elementAt(methodID - 1);
    } catch (Exception exc) {
      writeInvocationError("No Method With ID: " + methodID + " Associated With This Remote Object", msgID);
      return;
    }
    int ser = 0;
    try {
      ser = is.read();
    } catch (IOException ioExc) {
      if (running) {
        writeInvocationError(ioExc.toString(), msgID);
      }
      throw ioExc;
    }
    if (ser == -1) {
      writeInvocationError(ERRMSG3, msgID);
      throw new IOException(ERRMSG3);
    }
    boolean serflag = (ser == 0);
    Class[] argTypes = m.getParameterTypes();
    Object[] args = new Object[argTypes.length];
    try {
      boolean refs = (opID == 12);
      for (int i = 0; i < args.length; i++) {
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "Reading Argument " + i + " : ");
        }
        boolean readReal = true;
        if (refs) {
          readReal = (is.read() == 1);
        }
        if (readReal) {
          args[i] = PMPData.readObject(null, info.obj.getClass().getClassLoader(), is, new String(), maxA, -1, null);
          // check that if the arguments is InputStream and it is not
          // the last argument we need to load it entirely in the memory
          if ((i != (args.length - 1)) && (args[i] instanceof InputStream)) {
            byte[] buf = new byte[256];
            int read;
            InputStream input = (InputStream) args[i];
            ByteArrayOutputStream bos = new ByteArrayOutputStream(4096);
            while ((read = input.read(buf)) != -1) {
              bos.write(buf, 0, read);
            }
            // close the old input stream
            input.close();
            // replace the args[i] with ByteArrayInputStream
            args[i] = new ByteArrayInputStream(bos.toByteArray());
          }
        } else {
          int tempID = PMPData.readInt(is);
          args[i] = ((ObjectInfo) objects.get(new Integer(tempID))).obj;
          if (args[i] == null) {
            throw new IOException("No Remote Object With ID: " + tempID + " On Server");
          }
        }
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "Argument " + i + " Is: " + args[i]);
        }
      }
    } catch (Exception ioExc) {
      if (running) {
        writeInvocationError(ioExc.toString(), msgID);
      }
      throw new IOException(ioExc.getMessage());
    }
    new InvocationThread(this, m, info.obj, serflag, info.context, args, msgID);
  }

  protected void writeInvocationError(String errMsg, short msgId) {
    DebugUtils.error(this, errMsg, null);
    try {
      os.begin(msgId);
      os.write(SERIALIZED);
      os.write(0);
      PMPData.writeString(errMsg, os);
      os.end(false);
    } catch (Exception exc) {
      DebugUtils.error(this, ERRMSG2, exc);
      os.unlock();
    }
  }

  private void getMethod() throws IOException {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    int objID = -1;
    String methodName = null;
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "Get Method Request ...");
    }
    try {
      objID = PMPData.readInt(is);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "Requested Object ID: " + objID);
      }
      methodName = PMPData.readString(is, maxS);
    } catch (IOException ioExc) {
      if (running) {
        writeGetMethodError(ioExc.toString());
      }
      throw ioExc;
    }

    boolean all = methodName.equals(""); //$NON-NLS-1$
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, (all ? "All Methods Requested" : "Requested Method Name: " + methodName));
    }
    ObjectInfo info = (ObjectInfo) objects.get(new Integer(objID));
    if (info == null) {
      writeGetMethodError("No Remote Object With ID: " + objID + " On Server");
    } else if (all) {
      // all methods
      Vector methods = info.getMethods();
      int size = methods.size();
      os.begin(msgID);
      try {
        os.write(METHODS);
        PMPData.writeInt(size, os);
        for (int i = 0; i < size; i++) {
          Method method = (Method) methods.elementAt(i);
          PMPData.writeString(method.getName(), os);
          PMPData.writeString(method.getReturnType().getName(), os);
          Class[] argTypes = method.getParameterTypes();
          PMPData.writeInt(argTypes.length, os);
          for (int j = 0; j < argTypes.length; j++) {
            PMPData.writeString(argTypes[j].getName(), os);
          }
        }
        os.end(false);
      } catch (Exception exc) {
        DebugUtils.error(this, ERRMSG2, exc);
        os.unlock();
      }
    } else {
      // single method
      int length = 0;
      String[] argTypes = null;
      try {
        length = PMPData.readInt(is);
        argTypes = new String[length];
        for (int i = 0; i < length; i++) {
          argTypes[i] = PMPData.readString(is, maxS);
        }
      } catch (IOException ioExc) {
        if (running) {
          writeGetMethodError(ioExc.toString());
        }
        throw ioExc;
      }
      Method[] methods = new Method[1];
      int methodID = 0;
      try {
        methodID = info.getMethod(methodName, argTypes, methods);
      } catch (Exception exc) {
        writeGetMethodError("Error Getting Method Of " + info.obj + " : " + exc.toString());
        return;
      }
      os.begin(msgID);
      try {
        os.write(METHOD);
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "Remote Method ID: " + methodID);
        }
        PMPData.writeInt(1, os);
        PMPData.writeInt(methodID, os);
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "Method To Return: " + methods[0]);
        }
        PMPData.writeString(methods[0].getReturnType().getName(), os);
        os.end(false);
      } catch (Exception exc) {
        DebugUtils.error(this, ERRMSG2, exc);
        os.unlock();
      }
    }
  }

  private void writeGetMethodError(String errMsg) {
    DebugUtils.info(this, errMsg);
    try {
      os.begin(msgID);
      os.write(METHOD);
      PMPData.writeInt(1, os);
      PMPData.writeInt(0, os);
      PMPData.writeString(errMsg, os);
      os.end(false);
    } catch (Exception exc) {
      DebugUtils.error(this, ERRMSG1, exc);
      os.unlock();
    }
  }

  private void remoteListener(int opID) throws IOException {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    if ((opID & 16) != 0) {
      // remove
      String evType = PMPData.readString(is, maxS);
      byte res = peer.removeListener(evType, this);
      if (res == 2) {
        eventTypes.removeElement(evType);
      }
    } else {
      // add
      String evType = null;
      try {
        evType = PMPData.readString(is, maxS);
      } catch (Exception exc) {
        writeEventFailed(exc.toString());
        return;
      }
      byte res = peer.addListener(evType, this);
      if (res == 2) {
        if (eventTypes == null) {
          eventTypes = new Vector();
        }
        eventTypes.addElement(evType);
      }
    }
    os.begin(msgID);
    try {
      os.write(EV_REPLY);
      os.end(false);
    } catch (Exception exc) {
      DebugUtils.error(this, ERRMSG1, exc);
      os.unlock();
    }
  }

  private void dispose() {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    int objID;
    try {
      objID = PMPData.readInt(is);
    } catch (IOException exc) {
      DebugUtils.error(this, ERRMSG3, exc);
      return;
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "Disposing objID " + objID);
    }
    synchronized (objects) {
      ObjectInfo info = (ObjectInfo) objects.remove(new Integer(objID));
      if (info != null) {
        info.freeInfo();
      }
    }
  }

  public void unregisterService(Object context) {
    synchronized (objects) {
      for (Iterator it = objects.values().iterator(); it.hasNext();) {
        ObjectInfo info = (ObjectInfo) it.next();
        if (info != null && context.equals(info.context)) {
          it.remove();
          peer.ungetService(info);
          info.freeInfo();
        }
      }
    }
  }

  protected void event(Object ev, String eventType) {
    try {
      os.begin((short) -1);
      os.write(EVENT);
      PMPData.writeString(eventType, os);
      PMPData.writeObject(ev, os, true);
      os.end(false);
    } catch (Exception exc) {
      os.unlock();
    }
  }

  public Connection getConnection() {
    return connection;
  }

  public void postEvent(Object event, String eventType) {
    event(event, eventType);
  }

  private void writeEventFailed(String errMsg) {
    DebugUtils.error(this, errMsg, null);
    try {
      os.begin(msgID);
      os.write(EV_FAILED);
      PMPData.writeString(errMsg, os);
      os.end(false);
    } catch (Exception exc) {
      DebugUtils.error(this, ERRMSG1, exc);
      os.unlock();
    }

  }

  protected void ping_repl() {
    try {
      os.begin(null);
      os.write(PING_REPLY);
      os.end(true);
    } catch (Exception exc) {
      os.unlock();
    }
  }

  protected void ping() {
    try {
      os.begin((short) -1);
      os.write(PING);
      os.end(false);
    } catch (Exception exc) {
      os.unlock();
    }
  }

  /** ** client methods ** */

  private void connectReplay() {
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "answer " + answer);
    }
    if (!prepareAnswerReply()) {
      return;
    }
    int success = 1;
    try {
      success = is.read();
      if (success == -1) {
        answer.errMsg = "Read Error";
      } else if (success == 1) {
        answer.errMsg = PMPData.readString(is, maxS);
        if (answer.errMsg == null) {
          answer.errMsg = "Read Error";
        }
      }
    } catch (IOException ioExc) {
      answer.errMsg = ioExc.toString();
      answer.errCause = ioExc;
    }
    synchronized (answer) {
      answer.received = true;
      if (success == 0) {
        answer.connected = true;
        connected = true;
        peer.fireConnectionEvent(true, this);
      }
      if (answer.waiting) {
        answer.notify();
      }
    }
  }

  private void readReference() {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    if (!prepareAnswerReply()) {
      return;
    }
    try {
      answer.objID = PMPData.readInt(is);
      if (answer.objID == 0) {
        answer.errMsg = PMPData.readString(is, maxS);
      }
    } catch (Exception exc) {
      answer.errMsg = exc.toString();
      answer.errCause = exc;
    }
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "objID: " + answer.objID);
    }
    synchronized (answer) {
      answer.received = true;
      if (answer.waiting) {
        answer.notify();
      }
    }
  }

  private void getObject() {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    if (!prepareAnswerReply()) {
      return;
    }
    try {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "method result");
      }
      int type = is.read();
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "type: " + type);
      }
      if (type == -1) {
        answer.errMsg = "Read Error";
      } else if (type == 0) {
        // exception
        answer.errMsg = PMPData.readString(is, maxS);
        if (answer.errMsg == null) {
          answer.errMsg = "Read Error";
        }
      } else if (type == 1) {
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, "return expected: " + answer.expectsReturn);
        }
        if (answer.expectsReturn) {
          try {
            answer.obj = PMPData.readObject(null, answer.loader, is, answer.returnType, maxA, -1, null);
          } catch (Exception exc) {
            answer.errMsg = exc.toString();
            answer.errCause = exc;
          }
        }
      }
    } catch (Exception ioExc) {
      answer.errMsg = ioExc.toString();
      answer.errCause = ioExc;
    }
    answer.finish();
  }

  private void readMethods(int opID) {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    if (!prepareAnswerReply()) {
      return;
    }
    try {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "get methods ...");
      }
      int length;
      length = PMPData.readInt(is);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "methods length: " + length);
      }
      if (length == 1) {
        if ((opID & 64) != 0) {
          // single method
          answer.methodID = PMPData.readInt(is);
          answer.returnType = PMPData.readString(is, maxS);
          if (DebugUtils.DEBUG_ENABLED) {
            DebugUtils.debug(this, "method id: " + answer.methodID + "; return type: " + answer.returnType);
          }
          if (answer.returnType == null) {
            answer.errMsg = "Read Error";
          } else if (answer.methodID <= 0) {
            answer.errMsg = answer.returnType;
          }
        } else {
          answer.methods = new RemoteMethodImpl[1];
          answer.errMsg = readMethod(answer.methods, 0);
        }
      } else {
        answer.methods = new RemoteMethodImpl[length];
        for (int i = 0; i < length; i++) {
          answer.errMsg = readMethod(answer.methods, i);
          if (answer.errMsg != null) {
            break;
          }
        }
      }
    } catch (Exception ioExc) {
      answer.errMsg = ioExc.toString();
      answer.errCause = ioExc;
    }
    answer.finish();
  }

  private String readMethod(RemoteMethodImpl[] methods, int pos) {
    try {
      String name = PMPData.readString(is, maxS);
      if (name == null) {
        return "Read Error";
      }
      String returnType = PMPData.readString(is, maxS);
      if (returnType == null) {
        return "Read Error";
      }
      int params = PMPData.readInt(is);
      String[] argTypes = new String[params];
      for (int i = 0; i < params; i++) {
        argTypes[i] = PMPData.readString(is, maxS);
        if (argTypes[i] == null) {
          return "Read Error";
        }
      }
      methods[pos] = new RemoteMethodImpl(name, returnType, argTypes, answer.connection, pos + 1, answer.requestingRObj);
      return null;
    } catch (IOException ioExc) {
      return ioExc.toString();
    }
  }

  private void eventReply(int opID) throws Exception {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    if (!prepareAnswerReply()) {
      return;
    }
    try {
      if ((opID & 16) != 0) {
        answer.errMsg = PMPData.readString(is, maxS);
        if (answer.errMsg == null) {
          answer.errMsg = "Read Error";
        }
        answer.success = false;
      } else {
        answer.success = true;

      }
    } catch (IOException ioExc) {
      answer.errMsg = ioExc.toString();
      answer.errCause = ioExc;
    }
    answer.finish();

  }

  private void readEvent(int opID) throws Exception {
    if (!connected) {
      disconnect("Handshake hasn't finished", true);
      return;
    }
    try {
      if (connection.evMngr != null) {
        String sEvType = PMPData.readString(is, maxS);
        if (DebugUtils.DEBUG_ENABLED) {
          DebugUtils.debug(this, sEvType);
        }
        ClassLoader loader = connection.evMngr.getClassLoader(sEvType);
        Object event = PMPData.readObject(null, loader, is, new String(), maxA, -1, null);
        connection.evMngr.postEvent(sEvType, event);
      }
    } catch (IOException ioExc) {
      DebugUtils.error(this, "error receiving event", ioExc);
    }
  }

  private static boolean isConBroken(Throwable aThrow) {
    if (!(aThrow instanceof IOException)) {
      return false;
    }
    String exmsg = aThrow.getLocalizedMessage();
    if (exmsg == null) {
      return false;
    }
    exmsg = exmsg.toLowerCase();
    return exmsg.startsWith("connection reset") //$NON-NLS-1$
        || exmsg.startsWith("connection aborted") //$NON-NLS-1$
        || exmsg.startsWith("software caused connection abort") //$NON-NLS-1$
        || exmsg.startsWith("broken pipe") //$NON-NLS-1$
        || exmsg.startsWith("socket closed") //$NON-NLS-1$
        || exmsg.startsWith("bad socket") //$NON-NLS-1$
        || exmsg.startsWith("read error"); //$NON-NLS-1$
  }
}

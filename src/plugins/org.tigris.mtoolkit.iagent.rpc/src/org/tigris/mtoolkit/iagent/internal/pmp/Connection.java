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

import org.tigris.mtoolkit.iagent.pmp.EventListener;
import org.tigris.mtoolkit.iagent.pmp.PMPConnection;
import org.tigris.mtoolkit.iagent.pmp.PMPException;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

/**
 * Contains the methods necessary for connecting to the PMP Service and for
 * getting references to the services registered in the Framework.
 */
class Connection implements PMPConnection {
  private static final String EMPTY_STRING = new String();

  protected volatile boolean  connected    = false;

  protected PMPInputStream    is;
  protected PMPOutputStream   os;
  protected PMPSessionThread  reader;

  protected PMPEventsManager  evMngr;

  public Connection(PMPInputStream is, PMPOutputStream os, PMPSessionThread reader) {
    this.is = is;
    this.os = os;
    this.reader = reader;
  }

  public void connect() throws PMPException {
    PMPAnswer answer = new PMPAnswer(reader);
    try {
      os.begin(answer);
      os.write(PMPSessionThread.CONNECT);
      PMPData.writeInt(is.timeout, os);
      os.end(true);
      answer.get((is.timeout == 0) ? 10000 : is.timeout);
    } catch (Exception ex) {
      ex.printStackTrace();
      throw new PMPException(ex.getMessage(), ex);
    }
    try {
      if (!answer.connected) {
        String errMsg = "Can't Connect To Server: " + answer.errMsg;
        disconnect(errMsg);
        throw PMPAnswer.createException(errMsg, answer.errCause);
      } else {
        connected = true;
      }
    } finally {
      answer.free();
    }
  }

  /**
   * Ends the session. Frees all allocated resources.
   *
   * @param errMsg
   *          the error message
   */
  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPConnection#disconnect(java.lang.String)
   */
  public void disconnect(String errMsg) {
    if (!connected) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "already disconnected ...");
      }
      return;
    }
    reader.disconnect(errMsg, true);
  }

  protected void disconnected(String errMsg) {
    errMsg = errMsg == null ? EMPTY_STRING : errMsg;
    if (DebugUtils.DEBUG_ENABLED) {
      DebugUtils.debug(this, "Connection: disconnecting " + errMsg);
    }
    connected = false;
    postDisconnectedEvent(errMsg);
    evMngr = null;
  }

  protected void postDisconnectedEvent(String errMsg) {
    if (evMngr != null) {
      evMngr.postEvent(PMPConnection.FRAMEWORK_DISCONNECTED, this);
      evMngr.stopEvents();
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(reader, "Connection: Events manager stopped.");
      }
    }
  }

  /**
   * Gets reference to a service registered in the Framework.
   *
   * @param clazz
   *          Specifies the interface under which the service was regisered.
   * @param filter
   *          Specifies the search filter (exactly as
   *          <code>BundleContext.getService</code> filter)
   * @return a reference to the service.
   * @exception PMPException
   *              If an IOException or protocol error occured, if the user does
   *              not have access rigths for this service, or if there was no
   *              such service registered in the Framework.
   * @see RemoteObject
   */
  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPConnection#getReference(java.lang.String, java.lang.String)
   */
  public RemoteObject getReference(String clazz, String filter) throws PMPException {
    PMPAnswer answer = new PMPAnswer(reader);
    try {
      os.begin(answer);
      os.write(PMPSessionThread.GET_REF);
      PMPData.writeString(clazz, os);
      PMPData.writeString(filter, os);
      os.end(true);
      answer.get(is.timeout);
    } catch (Exception ioExc) {
      os.unlock();
      throw new PMPException(ioExc.toString(), ioExc);
    }
    if (answer.errMsg != null) {
      String errMsg = answer.errMsg;
      Throwable cause = answer.errCause;
      answer.free();
      throw PMPAnswer.createException(errMsg, cause);
    }
    RemoteObjectImpl toReturn = new RemoteObjectImpl(answer.objID, this);
    answer.free();
    return toReturn;
  }

  /**
   * Gets reference to a service registered in the Framework.
   *
   * @param clazz
   *          Specifies the interface under which the service was registered.
   * @param filter
   *          Specifies the search filter (exactly as
   *          <code>BundleContext getService</code> filter)
   * @param bid
   *          The id of the bundle that registered the service.
   * @return a reference to the service.
   * @exception PMPException
   *              If an IOException or protocol error occurred, if the user does
   *              not have access rigths for this service, or if there was no
   *              such service registered in the Framework.
   * @see RemoteObject
   */
  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPConnection#getReference(java.lang.String, java.lang.String, long)
   */
  public RemoteObject getReference(String clazz, String filter, long bid) throws PMPException {
    PMPAnswer answer = new PMPAnswer(reader);
    short msgID = 0;
    try {
      msgID = os.begin(answer);
      os.write(PMPSessionThread.GET_REF_BY_ID);
      PMPData.writeString(clazz, os);
      PMPData.writeString(filter, os);
      PMPData.writeLong(bid, os);
      os.end(true);
      answer.get(is.timeout);
    } catch (Exception ioExc) {
      if (msgID >= 100) {
        os.unlock();
      }
      throw new PMPException(ioExc.toString(), ioExc);
    }
    if (answer.errMsg != null) {
      String errMsg = answer.errMsg;
      Throwable cause = answer.errCause;
      answer.free();
      throw PMPAnswer.createException(errMsg, cause);
    }
    RemoteObjectImpl toReturn = new RemoteObjectImpl(answer.objID, this);
    answer.free();
    return toReturn;

  }

  protected RemoteMethodImpl[] getMethods(RemoteObjectImpl ro) throws PMPException {
    PMPAnswer answer = new PMPAnswer(reader);
    answer.connection = ro.c;
    short msgID = 0;
    try {
      answer.requestingRObj = ro;
      msgID = os.begin(answer);
      os.write(PMPSessionThread.GET_METHOD);
      PMPData.writeInt(ro.IOR, os);
      PMPData.writeString(EMPTY_STRING, os);
      os.end(true);
      answer.get(is.timeout);
      if (answer.errMsg != null) {
        throw PMPAnswer.createException(answer.errMsg, answer.errCause);
      }
      return answer.methods;
    } catch (Exception exc) {
      if (msgID >= 100) {
        os.unlock();
      }
      throw (exc instanceof PMPException) ? (PMPException) exc : new PMPException(exc.toString(), exc);
    } finally {
      answer.free();
    }
  }

  protected RemoteMethodImpl getMethod(RemoteObjectImpl ro, String name, String[] argTypes) throws PMPException {
    PMPAnswer answer = new PMPAnswer(reader);
    short msgID = 0;
    if (argTypes == null) {
      argTypes = new String[0];
    }
    try {
      msgID = os.begin(answer);
      os.write(PMPSessionThread.GET_METHOD);
      PMPData.writeInt(ro.IOR, os);
      PMPData.writeString(name, os);
      PMPData.writeInt(argTypes.length, os);
      for (int i = 0; i < argTypes.length; i++) {
        PMPData.writeString(argTypes[i], os);
      }
      os.end(true);
      answer.get(is.timeout);
    } catch (Exception exc) {
      if (msgID >= 100) {
        os.unlock();
      }
      answer.free();
      throw new PMPException(exc.toString(), exc);
    }
    if (answer.errMsg != null) {
      String errMsg = answer.errMsg;
      Throwable cause = answer.errCause;
      answer.free();
      throw PMPAnswer.createException(errMsg, cause);
    }
    try {
      return new RemoteMethodImpl(name, answer.returnType, argTypes, ro.c, answer.methodID, ro);
    } finally {
      answer.free();
    }
  }

  protected boolean dispose(int objID) {
    boolean locked = false;
    try {
      os.begin(null);
      locked = true;
      os.write(PMPSessionThread.DISPOSE);
      PMPData.writeInt(objID, os);
      os.end(true);
      return true;
    } catch (Exception exc) {
      if (locked) {
        os.unlock();
      }
      return false;
    }
  }

  protected Object invoke(Object[] args, String[] argTypes, boolean serflag, int objID, int methodID,
      String expReturnType, ClassLoader loader, boolean changed, Connection cr) throws PMPException {
    PMPAnswer answer = new PMPAnswer(reader);
    answer.loader = loader;
    if (changed) {
      answer.returnType = expReturnType;
    } else {
      answer.returnType = EMPTY_STRING;
    }
    short msgID = 0;
    try {
      answer.expectsReturn = !expReturnType.equals(PMPData.TYPES1[8]) && !expReturnType.equals(PMPData.TYPES2[8]);
      msgID = os.begin(answer);
      boolean haveRefs = false;
      if (args == null) {
        args = new Object[argTypes.length];
      }
      for (int i = 0; i < args.length; i++) {
        if (args[i] instanceof RemoteObjectImpl) {
          haveRefs = true;
          break;
        }
      }
      if (haveRefs) {
        os.write(PMPSessionThread.INVOKE_R);
      } else {
        os.write(PMPSessionThread.INVOKE);
      }
      PMPData.writeInt(objID, os);
      PMPData.writeInt(methodID, os);
      os.write(serflag ? 0 : 1);
      if (args.length != argTypes.length) {
        throw new PMPException("Incorrect arguments");
      }
      for (int i = 0; i < args.length; i++) {
        String className = null;
        try {
          int pos = argTypes[i].indexOf('.');
          if (pos == -1) {
            className = getClassName(argTypes[i]);
          }
          if (className == null) {
            className = argTypes[i];
          }
        } catch (Exception exc) {
          os.unlock();
          msgID = 0;
          throw new PMPException("Can't Load Argument Type: " + argTypes[i]);
        }
        if (className.equals("org.tigris.mtoolkit.iagent.pmp.RemoteObject")) { //$NON-NLS-1$
          int tmpID = addRemoteObject(args[i]);
          RemoteObjectImpl objImpl = new RemoteObjectImpl(tmpID, null);
          if (haveRefs) {
            os.write(1);
          }
          PMPData.writeObject(objImpl, os, true);
        } else if (args[i] instanceof RemoteObjectImpl) {
          if (haveRefs) {
            os.write(0);
          }
          PMPData.writeInt(((RemoteObjectImpl) args[i]).IOR, os);
        } else {
          if (haveRefs) {
            os.write(1);
          }
          PMPData.writeObject(args[i], os, true);
        }
      }
      os.end(true);
      // if (answer.expectsReturn) {
      answer.get(is.timeout);
      if (answer.errMsg != null) {
        throw PMPAnswer.createException("Error Invoking Method: " + answer.errMsg, answer.errCause);
      }
      if (!answer.expectsReturn) {
        return null;
      }
      return (serflag) ? answer.obj : (answer.objID > 0) ? new RemoteObjectImpl(answer.objID, cr) : null;
      // }
      // else return null;
    } catch (Exception exc) {
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(this, "", exc);
      }
      if (msgID >= 100) {
        os.unlock();
      }
      throw (exc instanceof PMPException) ? (PMPException) exc : new PMPException(exc.toString(), exc);
    } finally {
      answer.free();
    }
  }

  private String getClassName(String name) {
    return name.equals(PMPData.TYPES1[0]) ? PMPData.TYPES2[0] : name.equals(PMPData.TYPES1[4]) ? PMPData.TYPES2[4]
        : name.equals(PMPData.TYPES1[3]) ? PMPData.TYPES2[3] : name.equals(PMPData.TYPES1[1]) ? PMPData.TYPES2[1]
            : name.equals(PMPData.TYPES1[2]) ? PMPData.TYPES2[2] : name.equals(PMPData.TYPES1[5]) ? PMPData.TYPES2[5]
                : name.equals(PMPData.TYPES1[6]) ? PMPData.TYPES2[6] : name.equals(PMPData.TYPES1[8])
                    ? PMPData.TYPES2[8] : name.equals(PMPData.TYPES1[7]) ? PMPData.TYPES2[7] : null;
  }

  /** Called to assign object id to a new remote object */
  private int addRemoteObject(Object obj) {
    Class[] interfaces = {
      obj.getClass()
    };
    return reader.addRemoteObject(obj, interfaces);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPConnection#addEventListener(org.tigris.mtoolkit.iagent.pmp.EventListener, java.lang.String[])
   */
  public void addEventListener(EventListener el, String[] eventTypes) throws IllegalArgumentException {
    if (el == null) {
      throw new IllegalArgumentException("Can't add null listener");
    }
    if (evMngr == null) {
      evMngr = new PMPEventsManager(reader);
      if (DebugUtils.DEBUG_ENABLED) {
        DebugUtils.debug(reader, "Connection: Start events manager.");
      }
      evMngr.start();
    }
    if (eventTypes != null && eventTypes.length != 0) {
      for (int i = 0; i < eventTypes.length; i++) {
        evMngr.addEventListener(el, eventTypes[i]);
      }
    } else {
      throw new IllegalArgumentException("Incorrect event types");
    }

  }

  /**
   * Unregisters an EventListener
   *
   * @param el
   *          the EventListener
   */

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPConnection#removeEventListener(org.tigris.mtoolkit.iagent.pmp.EventListener, java.lang.String[])
   */
  public void removeEventListener(EventListener el, String[] eventTypes) throws IllegalArgumentException {
    if (el == null) {
      throw new IllegalArgumentException("Can't remove null listener");
    }
    if (evMngr == null) {
      return;
    }
    if (eventTypes != null && eventTypes.length != 0) {
      for (int i = 0; i < eventTypes.length; i++) {
        evMngr.removeEventListener(el, eventTypes[i]);
      }
    } else {
      throw new IllegalArgumentException("Incorrect event types");
    }

  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPConnection#getSessionID()
   */
  public String getSessionID() {
    return reader.sessionID;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.pmp.PMPConnection#isConnected()
   */
  public boolean isConnected() {
    return connected;
  }
}

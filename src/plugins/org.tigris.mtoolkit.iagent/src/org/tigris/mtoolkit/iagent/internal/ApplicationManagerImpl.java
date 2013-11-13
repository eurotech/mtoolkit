/*******************************************************************************
 * Copyright (c) 2005, 2010 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal;

import java.util.Dictionary;
import java.util.EventListener;
import java.util.Map;

import org.tigris.mtoolkit.iagent.ApplicationManager;
import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteApplication;
import org.tigris.mtoolkit.iagent.event.RemoteApplicationEvent;
import org.tigris.mtoolkit.iagent.event.RemoteApplicationListener;
import org.tigris.mtoolkit.iagent.spi.AbstractManager;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.PMPConnection;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public final class ApplicationManagerImpl extends AbstractManager implements ApplicationManager {
  private static final String   EVENT_APPLICATION_ID_KEY = "application_id";
  private static final String   EVENT_TYPE_KEY           = "type";

  private final MethodSignature GET_APPLICATIONS         = new MethodSignature("getApplications");
  private final MethodSignature START                    = new MethodSignature("start", new Class[] {
      String.class, Map.class
                                                         });

  private final MethodSignature STOP                     = new MethodSignature("stop", String.class);
  private final MethodSignature GET_STATE                = new MethodSignature("getState", String.class);
  private final MethodSignature GET_PROPERTIES           = new MethodSignature("getProperties", String.class);

  /**
   * @param remoteAdmin
   */
  public ApplicationManagerImpl() {
    super(PMPConnection.REMOTE_APPLICATION_ADMIN_NAME, "synch_application_event");
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.ApplicationManager#addRemoteApplicationListener(org.tigris.mtoolkit.iagent.event.RemoteApplicationListener)
   */
  public void addRemoteApplicationListener(RemoteApplicationListener listener) throws IAgentException {
    DebugUtils.debug(this, "[addRemoteApplicaionListener] >>> listener: " + listener);
    addRemoteEventListener(listener);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.ApplicationManager#removeRemoteApplicationListener(org.tigris.mtoolkit.iagent.event.RemoteApplicationListener)
   */
  public void removeRemoteApplicationListener(RemoteApplicationListener listener) throws IAgentException {
    DebugUtils.debug(this, "[removeApplicationListener] >>> listener: " + listener);
    removeRemoteEventListener(listener);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.ApplicationManager#listApplications()
   */
  public RemoteApplication[] listApplications() throws IAgentException {
    DebugUtils.debug(this, "[listApplications] >>>");
    String[] applicationIDs = (String[]) GET_APPLICATIONS.call(getRemoteAdmin());
    DebugUtils.debug(this,
        "[listApplications] Returned applications list: " + DebugUtils.convertForDebug(applicationIDs));
    int appsLength = (applicationIDs != null) ? applicationIDs.length : 0;
    RemoteApplication[] applications = new RemoteApplication[appsLength];
    if (applicationIDs != null) {
      for (int i = 0; i < applicationIDs.length; i++) {
        applications[i] = new RemoteApplicationImpl(this, applicationIDs[i]);
      }
    }
    return applications;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.spi.AbstractManager#remoteEvent(java.util.Dictionary)
   */
  protected void remoteEvent(Dictionary eventProps) {
    int type = ((Integer) eventProps.get(EVENT_TYPE_KEY)).intValue();
    String applicationId = (String) eventProps.get(EVENT_APPLICATION_ID_KEY);
    fireApplicationEvent(applicationId, type);
  }

  void startApplication(String applicationID, Map properties) throws IAgentException {
    Object result = START.call(getRemoteAdmin(), new Object[] {
        applicationID, properties
    });
    if (result instanceof Error) {
      Error err = (Error) result;
      throw new IAgentException("Failed to start application"
          + (err.getMessage() != null ? ": " + err.getMessage() : ""), err.getCode());
    }
  }

  void stopApplication(String applicationID) throws IAgentException {
    Object result = STOP.call(getRemoteAdmin(), applicationID);
    if (result instanceof Error) {
      Error err = (Error) result;
      throw new IAgentException("Failed to stop application"
          + (err.getMessage() != null ? ": " + err.getMessage() : ""), err.getCode());
    }
  }

  String getApplicationState(String applicationID) throws IAgentException {
    return (String) GET_STATE.call(getRemoteAdmin(), applicationID);
  }

  Map getApplicationProperties(String applicationId) throws IAgentException {
    Object result = GET_PROPERTIES.call(getRemoteAdmin(), applicationId);
    if (result instanceof Error) {
      throw new IAgentException((Error) result);
    }
    return (Map) result;
  }

  private void fireApplicationEvent(String applicationID, int type) {
    DebugUtils.debug(this, "[fireApplicationEvent] >>> applicationId: " + applicationID + "; type: " + type);
    EventListener[] listeners = getEventListeners();
    if (listeners == null) {
      return;
    }
    RemoteApplication application = new RemoteApplicationImpl(this, applicationID);
    RemoteApplicationEvent event = new RemoteApplicationEvent(application, type);
    DebugUtils.debug(this, "[fireApplicationEvent] " + listeners.length + " listeners found.");
    for (int i = 0; i < listeners.length; i++) {
      RemoteApplicationListener listener = (RemoteApplicationListener) listeners[i];
      try {
        DebugUtils.debug(this, "[fireApplicationEvent] deliver event: " + event + " to listener: " + listener);
        listener.applicationChanged(event);
      } catch (Throwable e) {
        DebugUtils.error(this, "[fireApplicationEvent] Failed to deliver event to " + listener, e);
      }
    }
  }
}

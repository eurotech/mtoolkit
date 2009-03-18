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
package org.tigris.mtoolkit.iagent.internal.utils;

import org.tigris.mtoolkit.iagent.IAgentErrors;

public class ExceptionCodeHelper {

  public static int toDeploymentExceptionCode(int code) {
    if (code < IAgentErrors.ERROR_DEPLOYMENT_UNKNOWN && code > -9000) {
      return IAgentErrors.ERROR_DEPLOYMENT_UNKNOWN - code;
    } else {
      return 0;
    }
  }

  public static int toApplicationExceptionCode(int code) {
    if (code < IAgentErrors.ERROR_APPLICATION_UNKNOWN && code > IAgentErrors.ERROR_APPLICATION_NO_APPLICATION_FOUND) {
      return IAgentErrors.ERROR_APPLICATION_UNKNOWN - code;
    } else {
      return 0;
    }
  }

  public static int fromApplicationExceptionCode(int code) {
    if (code < 1) {
      return IAgentErrors.ERROR_APPLICATION_UNKNOWN;
    } else {
      return IAgentErrors.ERROR_APPLICATION_UNKNOWN - code;
    }
  }

  public static int fromDeploymentExceptionCode(int code) {
    if (code < 1) {
      return IAgentErrors.ERROR_DEPLOYMENT_UNKNOWN;
    } else {
      return IAgentErrors.ERROR_DEPLOYMENT_UNKNOWN - code;
    }
  }

}

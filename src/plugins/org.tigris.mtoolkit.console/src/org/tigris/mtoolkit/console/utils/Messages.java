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

package org.tigris.mtoolkit.console.utils;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
  private static final String BUNDLE_NAME = "org.tigris.mtoolkit.console.utils.ConsoleMessages"; //$NON-NLS-1$

  public static String        redirect_console_output;

  // org.tigris.mtoolkit.console.internal.RemoveAllConsoleAction
  public static String        RemoveAllConsoleAction_Remove_All_Disconnected;

  // org.tigris.mtoolkit.console.internal.RemoveConsoleAction
  public static String        RemoveConsoleAction_Remove_Console;

  // org.tigris.mtoolkit.console.internal.ConsoleReader
  public static String        ConsoleReader_Unsupported_Encoding;
  public static String        ConsoleReader_Remote_Console;
  public static String        ConsoleReader_Error_Reading_User_Input;
  public static String        ConsoleReader_Command_Execution_Failed;

  // org.tigris.mtoolkit.console.internal.RemoteConsole
  public static String        RemoteConsole_Redirection_Failed;
  public static String        RemoteConsole_FW_Redirection_Failed;
  public static String        RemoteConsole_Console_Write_Failed;
  public static String        RemoteConsole_Disconnecting_Console;
  public static String        RemoteConsole_FW_Out_Reset_Failed;
  public static String        RemoteConsole_Disconnected;
  public static String        RemoteConsole_Remote_Console_Name;
  public static String        RemoteConsole_Remote_Framework_Name;

  private Messages() {
  }

  static {
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}

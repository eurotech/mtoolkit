/*******************************************************************************
 * Copyright (c) 2005, 2012 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal;

/**
 * Holds the constants used to identify different commands that are sent to
 * device
 *
 * @author Alexander Petkov
 * @version 1.1
 */
public interface IAgentCommands {
  /**
   * InstAgent ping command.
   */
  public static final int IAGENT_CMD_PING                   = 0x12121212;

  /**
   * InstAgent command to create file.
   */
  public static final int IAGENT_CMD_CREATEFILE             = 0x00010001;

  /**
   * InstAgent command to create file.
   */
  public static final int IAGENT_CMD_OPENFILE               = 0x00010002;

  /**
   * InstAgent command to read file.
   */
  public static final int IAGENT_CMD_READFILE               = 0x00010003;

  /**
   * InstAgent command to write file.
   */
  public static final int IAGENT_CMD_WRITEFILE              = 0x00010004;

  /**
   * InstAgent command to close file.
   */
  public static final int IAGENT_CMD_CLOSEFILE              = 0x00010005;

  /**
   * InstAgent command to delete file.
   */
  public static final int IAGENT_CMD_DELETEFILE             = 0x00010006;

  /**
   * InstAgent command to make directory.
   */
  public static final int IAGENT_CMD_MAKEDIR                = 0x00010007;

  /**
   * InstAgent command to list directory.
   */
  public static final int IAGENT_CMD_LISTDIR                = 0x00010008;

  /**
   * InstAgent command to remove directory.
   */
  public static final int IAGENT_CMD_REMOVEDIR              = 0x00010009;

  // Runtime commands
  /**
   * InstAgent command for starting VM.
   */
  public static final int IAGENT_CMD_STARTVM                = 0x00020001;

  /**
   * InstAgent command for stopping VM.
   */
  public static final int IAGENT_CMD_STOPVM                 = 0x00020002;

  /**
   * InstAgent command for setting output.
   */
  public static final int IAGENT_CMD_SETOUTPUT              = 0x00020003;

  /**
   * InstAgent command for add library.
   */
  public static final int IAGENT_CMD_ADDLIBRARY             = 0x00020004;

  /**
   * InstAgent command for remove library.
   */
  public static final int IAGENT_CMD_REMOVELIBRARY          = 0x00020005;

  /**
   * InstAgent command for enabling micro analyzer.
   */
  public static final int IAGENT_CMD_SETMICROANALYZER       = 0x00020006;

  /**
   * InstAgent command for enabling -Xdebug.
   */
  public static final int IAGENT_CMD_SETXDEBUG              = 0x00020007;

  /**
   * InstAgent command for listing raw arguments
   */
  public static final int IAGENT_CMD_LISTRAWARGS            = 0x00020008;

  /**
   * InstAgent command for resetting all arguments.
   */
  public static final int IAGENT_CMD_RESETARGS              = 0x00020009;

  /**
   * InstAgent command to add raw argument.
   */
  public static final int IAGENT_CMD_ADDRAWARGUMENT         = 0x0002000A;

  /**
   * InstAgent command to remove raw argument.
   */
  public static final int IAGENT_CMD_REMOVERAWARGUMENT      = 0x0002000B;

  // System commands
  /**
   * Create process by given executable name.
   */
  public static final int IAGENT_CMD_CREATEPROCESS          = 0x00030001;

  /**
   * Kill process by PID.
   */
  public static final int IAGENT_CMD_KILLPROCESS            = 0x00030002;

  /**
   * Start application which handles given document.
   */
  public static final int IAGENT_CMD_STARTDOCUMENT          = 0x00030003;

  /**
   * Start application by caption name.
   */
  public static final int IAGENT_CMD_STARTAPPLICATION       = 0x00030004;

  /**
   * Check if application is running.
   */
  public static final int IAGENT_CMD_ISAPPRUNNING           = 0x00030005;

  /**
   * Stop application by its UID.
   */
  public static final int IAGENT_CMD_STOPAPPLICATION        = 0x00030006;

  /**
   * Get device language.
   */
  public static final int IAGENT_CMD_GETDEVICELANGUAGE      = 0x00030007;

  /**
   * InstAgent command to get PMP listening port.
   */
  public static final int IAGENT_CMD_GET_PMP_LISTENING_PORT = 0x0002000E;

  /**
   * InstAgent command to get server process ID
   */
  public static final int IAGENT_CMD_GET_PID                = 0x0002FFFF;
}

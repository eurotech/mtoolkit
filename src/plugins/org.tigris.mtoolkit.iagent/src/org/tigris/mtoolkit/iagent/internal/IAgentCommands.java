package org.tigris.mtoolkit.iagent.internal;

/**
 * Holds the constants used to identify different commands that are sent to device
 *
 * @author Alexander Petkov
 * @version 1.1
 */
public interface IAgentCommands {
  /**
   * InstAgent ping command.
   */
  public static final int IAGENT_CMD_PING = 0x12121212;
  
  /**
   * InstAgent command for starting VM.
   */
  public static final int IAGENT_CMD_STARTVM = 0x00020001;
  
  /**
   * InstAgent command for stopping VM.
   */
  public static final int IAGENT_CMD_STOPVM = 0x00020002;

  
  /**
   * InstAgent command for listing raw arguments
   */
  public static final int IAGENT_CMD_LISTRAWARGS = 0x00020008;
  
  /**
   * InstAgent command for resetting all arguments.
   */
  public static final int IAGENT_CMD_RESETARGS = 0x00020009;
  
  /**
   * InstAgent command to add raw argument.
   */
  public static final int IAGENT_CMD_ADDRAWARGUMENT = 0x0002000A;

  /**
   * InstAgent command to remove raw argument.
   */
  public static final int IAGENT_CMD_REMOVERAWARGUMENT = 0x0002000B;
  
  /**
   * InstAgent command to return platform properties.
   */
  public static final int IAGENT_CMD_GETPLATFORMPROPERTIES = 0x0002000C;

  /**
   * InstAgent command to return system bundles names.
   */
  public static final int IAGENT_CMD_GET_SYSTEM_BUNDLES = 0x0002000D;
  
  /**
   * InstAgent command to get PMP listening port.
   */
  public static final int IAGENT_CMD_GET_PMP_LISTENING_PORT = 0x0002000E;

}

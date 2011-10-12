package org.tigris.mtoolkit.common;

import org.eclipse.core.runtime.Platform;

/**
 * @since 6.1
 */
public class PlatformUtils {

  public static boolean isWindows() {
    return Platform.OS_WIN32.equals(Platform.getOS());
  }

  public static boolean isLinux() {
    return Platform.OS_LINUX.equals(Platform.getOS());
  }

  public static boolean isMacOSX() {
    return Platform.OS_MACOSX.equals(Platform.getOS());
  }

}

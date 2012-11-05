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
package org.tigris.mtoolkit.common;

import org.eclipse.core.runtime.Platform;

/**
 * @since 6.1
 */
public final class PlatformUtils {
  private PlatformUtils() {
  }

  public static boolean isWindows() { // NO_UCD
    return Platform.OS_WIN32.equals(Platform.getOS());
  }

  public static boolean isLinux() { // NO_UCD
    return Platform.OS_LINUX.equals(Platform.getOS());
  }

  public static boolean isMacOSX() { // NO_UCD
    return Platform.OS_MACOSX.equals(Platform.getOS());
  }
}

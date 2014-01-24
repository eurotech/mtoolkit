/*******************************************************************************
 * Copyright (c) 2011 ProSyst Software GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     ProSyst Software GmbH - initial API and implementation
 *******************************************************************************/
package org.tigris.mtoolkit.iagent.internal.utils;

import java.lang.reflect.Constructor;
import java.text.MessageFormat;

import org.tigris.mtoolkit.iagent.internal.rpc.Messages;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

/**
 * @since 3.1
 */
public class ThreadUtils {
  private static final String PROP_IA_THREADS_STACK_SIZE  = "iagent.threads.stackSize"; //$NON-NLS-1$
  private static final String PROP_MBS_THREADS_STACK_SIZE = "mbs.threads.stacksize";   //$NON-NLS-1$

  private static Constructor  tssConstructor;

  static {
    try {
      tssConstructor = Thread.class.getConstructor(new Class[] {
      ThreadGroup.class, Runnable.class, String.class, long.class
      });
    } catch (Throwable t) {
      DebugUtils.info(ThreadUtils.class, Messages.getString("ThreadUtils.CannotSetStackSize"), t); //$NON-NLS-1$
    }
  }

  private ThreadUtils() {
  }

  public static Thread createThread(Runnable runnable, String name) {
    Thread thread = createThread(runnable, getThreadsStackSize());
    thread.setName(name);
    return thread;
  }

  public static Thread createThread(Runnable runnable, long threadStackSize) {
    if (tssConstructor != null) {
      try {
        return (Thread) tssConstructor.newInstance(new Object[] {
            null, runnable, Messages.getString("ThreadUtils.WorkerName"), new Long(threadStackSize) //$NON-NLS-1$
            });
      } catch (Throwable t) {
        DebugUtils.error(ThreadUtils.class, Messages.getString("ThreadUtils.ThreadCreateFailed"), t); //$NON-NLS-1$
        // ignore the request if failed
      }
    }
    return new Thread(runnable);
  }

  private static long getThreadsStackSize() {
    String stackSizeOption = System.getProperty(PROP_IA_THREADS_STACK_SIZE);
    if (stackSizeOption != null) {
      try {
        return new Long(stackSizeOption).longValue();
      } catch (NumberFormatException e) {
        DebugUtils.error(ThreadUtils.class,
            MessageFormat.format(Messages.getString("ThreadUtils.InvalidStackSize"), new Object[] { stackSizeOption})); //$NON-NLS-1$
      }
    }
    stackSizeOption = System.getProperty(PROP_MBS_THREADS_STACK_SIZE);
    if (stackSizeOption != null) {
      try {
        return new Long(stackSizeOption).longValue();
      } catch (NumberFormatException e) {
        DebugUtils.error(ThreadUtils.class,
            MessageFormat.format(Messages.getString("ThreadUtils.InvalidStackSize"), new Object[] { stackSizeOption})); //$NON-NLS-1$
      }
    }
    return 0;
  }
}

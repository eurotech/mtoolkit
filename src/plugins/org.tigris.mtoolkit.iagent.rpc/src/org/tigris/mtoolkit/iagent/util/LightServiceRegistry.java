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
package org.tigris.mtoolkit.iagent.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * A simple service registry, which relies on a properties file to describe the
 * available services.
 *
 * <p>
 * Registry will search for all files with a specific name, which can be found
 * on the classpath, i.e. {@link ClassLoader#getResources(String)} method is
 * used. All these files must be standard Java properties, where each pair
 * describes a single service: the name of the property is the name of the
 * service and the value is the implementation class.
 * </p>
 * <p>
 * The implementation class must conform to the following conditions:
 * <ul>
 * <li>must be visible to the class loader used by the registry. This usually is
 * not a problem, because if the file can be found, the class should be
 * available as well (as long as the class is distributed in the same JAR as the
 * definition file)</li>
 * <li>must have default public constructor (without arguments)</li>
 * </ul>
 * </p>
 * <p>
 * The registry doesn't interpret the interface of the returned class, it is up
 * to the caller to properly cast the result. The registry provides only a
 * mapping between a name and an class name. The mapping is described with
 * properties file with specific filename, located on the classpath.
 * </p>
 * <p>
 * This registry can be used in both OSGi and non-OSGi environment. In OSGi
 * environment, it works only in the scope of a bundle and its fragments. In
 * non-OSGi environment (standard Java environment), it works for all definition
 * files, which are visible to the class loader, used by the registry.
 * </p>
 *
 */
public final class LightServiceRegistry {
  /**
   * Default name of the definition file, parsed by the registry.
   */
  public static final String DEFAULT_REGISTRY_FILE = "extenders.properties";

  private static class ExtenderHandle {

    private static final Object  EXTENDER_LOAD_FAILED = new Object();
    private String               className;
    private Object               extender;
    private LightServiceRegistry registry;

    // used to create linked list
    public ExtenderHandle        next;

    public ExtenderHandle(LightServiceRegistry registry, String className) {
      this.registry = registry;
      this.className = className;
    }

    public Object get() {
      create();
      if (extender == EXTENDER_LOAD_FAILED) {
        return null;
      }
      return extender;
    }

    private void create() {
      if (extender == null) {
        try {
          Class cl = registry.cloader.loadClass(className);
          extender = cl.newInstance();
        } catch (ClassNotFoundException e) {
          error("Cannot load specified implementation class: " + className, e);
          extender = EXTENDER_LOAD_FAILED;
        } catch (InstantiationException e) {
          error("Cannot instantiate specified implementation class: " + className, e);
          extender = EXTENDER_LOAD_FAILED;
        } catch (IllegalAccessException e) {
          error("Implementation class constructor visibility of class " + className + " is too restrictive.", e);
          extender = EXTENDER_LOAD_FAILED;
        }
      }
    }
  }

  private Map         registry = new HashMap();
  private ClassLoader cloader;
  private String      filename;
  private boolean     DEBUG    = Boolean.getBoolean("iagent.extender.registry");

  /**
   * Default constructor, which creates a registry, which uses the
   * {@link #DEFAULT_REGISTRY_FILE} filename and the class loader, which has
   * loaded {@link LightServiceRegistry} class.
   */
  public LightServiceRegistry() {
    this(DEFAULT_REGISTRY_FILE);
  }

  /**
   * Constructor, which creates a new registry using the passed
   * <code>filename</code> for the definition files.
   *
   * @param filename
   *          the name of the definition files
   */
  public LightServiceRegistry(String filename) {
    this(filename, LightServiceRegistry.class.getClassLoader());
  }

  public LightServiceRegistry(ClassLoader loader) {
    this(DEFAULT_REGISTRY_FILE, loader);
  }

  /**
   * Constructor, which creates a new registry with specified definition files'
   * name and class loader.
   *
   * @param filename
   *          the name of the definition files
   * @param loader
   *          the class loader to be searched for definition files and
   *          subsequent class loads
   */
  public LightServiceRegistry(String filename, ClassLoader loader) {
    this.filename = filename;
    this.cloader = loader;
    init();
  }

  private void init() {
    Enumeration en;
    try {
      en = cloader.getResources(filename);
    } catch (IOException e) {
      error("Failed to search classpath for " + filename, e);
      // failed to initialize, stop
      return;
    }
    if (!en.hasMoreElements()) {
      warning("No registry definition files found!", null);
      return;
    }
    while (en.hasMoreElements()) {
      try {
        URL registryFile = (URL) en.nextElement();
        if (DEBUG) {
          log("Loading " + registryFile.toString());
        }
        Properties registryProps = new Properties();
        registryProps.load(registryFile.openStream());
        for (Iterator i = registryProps.entrySet().iterator(); i.hasNext();) {
          Map.Entry entry = (Entry) i.next();
          if (DEBUG) {
            log("\t" + entry.getKey() + " = " + entry.getValue());
          }
          addExtender((String) entry.getKey(), (String) entry.getValue());
        }
      } catch (IOException e) {
        error("Failed to properly load a definition file", e);
        // ignore and continue loading
      }
    }
    if (DEBUG) {
      log("Initialization done.");
    }
  }

  private void addExtender(String name, String implementation) {
    ExtenderHandle newHandle = new ExtenderHandle(this, implementation);
    ExtenderHandle existingHandle = (ExtenderHandle) registry.get(name);
    // cycle until we reach the end of the list (existingHandle.next == null)
    while (existingHandle != null && existingHandle.next != null) {
      existingHandle = existingHandle.next;
    }
    if (existingHandle != null) {
      existingHandle.next = newHandle;
    } else {
      registry.put(name, newHandle);
    }
  }

  /**
   * Queries the registry for a service with given name. If more than one
   * implementation are provided, only the first one is returned.
   *
   * @param name
   *          the name of the service
   * @return an implementation of the service, if found. If no implementations
   *         are registered, null will be returned.
   */
  public Object get(String name) {
    Object[] all = getAll(name);
    if (all.length > 0) {
      return all[0];
    }
    return null;
  }

  /**
   * Queries the registry for all implementations of a service with given name.
   *
   * @param name
   *          the name of the service
   * @return an array containing all implementations, defined for this service
   *         name. An empty array will be returned, if no implementations are
   *         defined.
   */
  public Object[] getAll(String name) {
    synchronized (registry) {
      List result = new ArrayList(1);
      ExtenderHandle handle = (ExtenderHandle) registry.get(name);
      while (handle != null) {
        Object extender = handle.get();
        if (extender != null) {
          result.add(extender);
        }
        handle = handle.next;
      }
      return result.toArray(new Object[result.size()]);
    }
  }

  /**
   * Queries the registry for all service names.
   *
   * @return an array containing all service names. An empty array will be
   *         returned, if no services are defined.
   */
  public String[] getAllServiceNames() {
    synchronized (registry) {
      Set names = registry.keySet();
      return (String[]) names.toArray(new String[names.size()]);
    }
  }

  public Object[] getAllServices() {
    synchronized (registry) {
      Collection services = registry.values();
      return services.toArray();
    }
  }

  private static final void log(String message) {
    log(message, null);
  }

  private static final void log(String message, Throwable t) {
    log("INFO", message, t);
  }

  private static final void warning(String message, Throwable t) {
    log("WARNING", message, t);
  }

  private static final void error(String message, Throwable t) {
    log("ERROR", message, t);
  }

  private static final void log(String severity, String message, Throwable t) {
    System.out.println("[Registry][" + severity + "] " + (message != null ? message : "No message"));
    if (t != null) {
      t.printStackTrace(System.out);
    }
  }

}

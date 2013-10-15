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
package org.tigris.mtoolkit.iagent.internal;

import java.io.InputStream;
import java.util.Dictionary;

import org.tigris.mtoolkit.iagent.Error;
import org.tigris.mtoolkit.iagent.IAgentErrors;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;
import org.tigris.mtoolkit.iagent.RemoteService;
import org.tigris.mtoolkit.iagent.pmp.RemoteObject;
import org.tigris.mtoolkit.iagent.spi.MethodSignature;
import org.tigris.mtoolkit.iagent.spi.Utils;
import org.tigris.mtoolkit.iagent.util.DebugUtils;

public final class RemoteBundleImpl implements RemoteBundle {
  private static MethodSignature GET_SIGNER_CERTIFICATES_METHOD  = new MethodSignature("getSignerCertificates",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature IS_SIGNER_TRUSTED_METHOD        = new MethodSignature("isSignerTrusted",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_BUNDLE_STATE_METHOD         = new MethodSignature("getBundleState",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_BUNDLE_LAST_MODIFIED_METHOD = new MethodSignature("getBundleLastModified",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_BUNDLE_HEADERS_METHOD       = new MethodSignature("getBundleHeaders",
                                                                     new String[] {
      "long", MethodSignature.STRING_TYPE
                                                                     }, true);
  private static MethodSignature GET_BUNDLE_LOCATION_METHOD      = new MethodSignature("getBundleLocation",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature START_BUNDLE_METHOD             = new MethodSignature("startBundle", new String[] {
      "long", "int"
                                                                 }, true);
  private static MethodSignature STOP_BUNDLE_METHOD              = new MethodSignature("stopBundle", new String[] {
      "long", "int"
                                                                 }, true);
  private static MethodSignature UPDATE_BUNDLE_METHOD            = new MethodSignature("updateBundle", new String[] {
      "long", MethodSignature.INPUT_STREAM_TYPE
                                                                 }, true);
  private static MethodSignature UNINSTALL_BUNDLE_METHOD         = new MethodSignature("uninstallBundle",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_BUNDLE_NAME_METHOD          = new MethodSignature("getBundleSymbolicName",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature RESOLVE_BUNDLES_METHOD          = new MethodSignature("resolveBundles", new String[] {
                                                                   long[].class.getName()
                                                                 }, true);
  private static MethodSignature GET_REGISTERED_SERVICES_METHOD  = new MethodSignature("getRegisteredServices",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_USING_SERVICES_METHOD       = new MethodSignature("getUsingServices",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_FRAGMENT_BUNDLES_METHOD     = new MethodSignature("getFragmentBundles",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_HOST_BUNDLES_METHOD         = new MethodSignature("getHostBundles",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_BUNDLE_TYPE_METHOD          = new MethodSignature("getBundleType",
                                                                     MethodSignature.BID_ARGS, true);
  private static MethodSignature GET_BUNDLE_HEADER_METHOD        = new MethodSignature("getBundleHeader", new String[] {
      "long", MethodSignature.STRING_TYPE, MethodSignature.STRING_TYPE
                                                                 }, true);
  private static MethodSignature GET_BUNDLE_START_LEVEL_METHOD   = new MethodSignature("getBundleStartLevel",
                                                                     new String[] {
                                                                       "long"
                                                                     }, true);
  // should not serialize because resource could be big and sending will block the communication
  private static MethodSignature GET_BUNDLE_RESOURCE_METHOD      = new MethodSignature("getBundleResource",
                                                                     new String[] {
      "long", MethodSignature.STRING_TYPE, Dictionary.class.getName()
                                                                     }, false);
  private static MethodSignature IS_BUNDLE_SIGNED_METHOD         = new MethodSignature("isBundleSigned",
                                                                     MethodSignature.BID_ARGS, true);

  private Long                   id;
  private String                 location;
  private boolean                uninstalled                     = false;
  private DeploymentManagerImpl  commands;

  public RemoteBundleImpl(DeploymentManagerImpl deploymentCommands, Long id) {
    this(deploymentCommands, id, null);
  }

  public RemoteBundleImpl(DeploymentManagerImpl deploymentCommands, Long id, String location) {
    DebugUtils.debug(this, "[Constructor] >>> Creating new RemoteBundle: manager: " + deploymentCommands + "; id " + id
        + "; location " + location);
    this.commands = deploymentCommands;
    this.id = id;
    this.location = location;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getBundleId()
   */
  public long getBundleId() {
    return id.longValue();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getHeaders(java.lang.String)
   */
  public Dictionary getHeaders(String locale) throws IAgentException {
    DebugUtils.debug(this, "[getHeaders] >>> locale: " + locale);
    checkBundleState();
    Dictionary headers = (Dictionary) GET_BUNDLE_HEADERS_METHOD.call(getBundleAdmin(), new Object[] {
        id, locale
    });
    if (headers == null) {
      DebugUtils.debug(this, "[getHeaders] Bundle cannot be found on the remote site. Assuming it is uninstalled");
      uninstalled = true;
      checkBundleState(); // throw illegal state exception
    }
    DebugUtils.debug(this, "[getHeaders] result: " + DebugUtils.convertForDebug(headers));
    return headers;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getHeader(java.lang.String,
   * java.lang.String)
   */
  public String getHeader(String headerName, String locale) throws IAgentException {
    DebugUtils.debug(this, "[getHeader] >>> headerName: " + headerName + "; locale: " + locale);
    checkBundleState();
    Object result = GET_BUNDLE_HEADER_METHOD.call(getBundleAdmin(), new Object[] {
        id, headerName, locale
    });
    if (result == null) {
      DebugUtils.debug(this, "[getHeader] No header with given method found");
      return null;
    } else if (result instanceof Error) {
      Error error = (Error) result;
      DebugUtils.info(this, "[getHeader] Failed to get header: " + error);
      checkBundleErrorResult(error);
      return null;
    } else {
      DebugUtils.debug(this, "[getHeader] Header value: " + result);
      return (String) result;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#isBundleSigned()
   */
  public boolean isBundleSigned() throws IAgentException {
    DebugUtils.debug(this, "[isSigned] >>>");
    boolean isSigned = false;
    checkBundleState();
    RemoteObject admin = getBundleAdmin();
    if (Utils.isRemoteMethodDefined(admin, IS_BUNDLE_SIGNED_METHOD)) {
      Boolean isSignedResult = (Boolean) IS_BUNDLE_SIGNED_METHOD.call(admin, new Object[] {
        id
      });
      DebugUtils.debug(this, "[isSigned] Bundle signed: " + isSigned);
      isSigned = isSignedResult.booleanValue();
    } else {
      DebugUtils.debug(this, "[method not found on iagent] >>>");
    }
    return isSigned;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getLocation()
   */
  public String getLocation() throws IAgentException {
    DebugUtils.debug(this, "[getLocation] >>>");
    checkBundleState();
    if (location == null) {
      location = (String) GET_BUNDLE_LOCATION_METHOD.call(getBundleAdmin(), new Object[] {
        id
      });
      if (location == null) {
        DebugUtils.debug(this, "[getLocation] Bundle cannot be found on the remote site. Assuming it is uninstalled");
        uninstalled = true;
        checkBundleState();
        return null;
      }
    }
    DebugUtils.debug(this, "[getLocation] Bundle location: " + location);
    return location;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getState()
   */
  public int getState() throws IAgentException {
    if (uninstalled) {
      DebugUtils.debug(this, "[getState] bundle state: " + UNINSTALLED);
      return UNINSTALLED;
    }
    Integer state = (Integer) GET_BUNDLE_STATE_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    if (state.intValue() == UNINSTALLED) {
      uninstalled = true;
    }
    DebugUtils.debug(this, "[getState] bundle state: " + state);
    return state.intValue();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getSymbolicName()
   */
  public String getSymbolicName() throws IAgentException {
    DebugUtils.debug(this, "[getSymbolicName] >>>");
    checkBundleState();
    String symbolicName = (String) GET_BUNDLE_NAME_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    if (symbolicName == null) { // the bundle is uninstalled
      DebugUtils
          .debug(this, "[getSymbolicName] Bundle cannot be found on the remote site. Assuming it was uninstalled");
      uninstalled = true;
      checkBundleState();
      return null;
    } else if (symbolicName.length() == 0) { // the bundle is R3
      DebugUtils.info(this, "[getSymbolicName] symbolic name: null");
      return null;
    } else { // everything is normal
      DebugUtils.debug(this, "[getSymbolicName] symbolic name: " + symbolicName);
      return symbolicName;
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getVersion()
   */
  public String getVersion() throws IAgentException {
    DebugUtils.debug(this, "[getVersion] >>>");
    checkBundleState();
    String headerValue = getHeader("Bundle-Version", "");
    DebugUtils.debug(this, "[getVersion] bundle version: " + headerValue);
    return headerValue != null ? headerValue.trim() : null;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#resolve()
   */
  public boolean resolve() throws IAgentException {
    DebugUtils.debug(this, "[resolve] >>> Trying to resolve bundle...");
    if (!uninstalled && getState() == UNINSTALLED) {
      uninstalled = true; // check for uninstall before call
    }
    checkBundleState();
    boolean resolvingResult = ((Boolean) RESOLVE_BUNDLES_METHOD.call(getBundleAdmin(), new Object[] {
      new long[] {
        id.longValue()
      }
    })).booleanValue();
    DebugUtils.debug(this, "[resolve] resolve status: " + resolvingResult);
    return resolvingResult;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#start(int)
   */
  public void start(int flags) throws IAgentException {
    DebugUtils.debug(this, "[start] >>> flags: " + flags);
    checkBundleState();
    Error error = (Error) START_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] {
        id, new Integer(flags)
    });
    DebugUtils.debug(this, "[start] Bundle start result: " + error);
    checkBundleErrorResult(error);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#stop(int)
   */
  public void stop(int flags) throws IAgentException {
    DebugUtils.debug(this, "[stop] flags: " + flags);
    checkBundleState();
    Error error = (Error) STOP_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] {
        id, new Integer(flags)
    });
    DebugUtils.debug(this, "[stop] Bundle stop result: " + error);
    checkBundleErrorResult(error);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#update(java.io.InputStream)
   */
  public void update(InputStream in) throws IAgentException {
    DebugUtils.debug(this, "[update] >>> in: " + in);
    if (in == null) {
      throw new IllegalArgumentException();
    }
    checkBundleState();
    Error err = (Error) UPDATE_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] {
        id, in
    });
    DebugUtils.debug(this, "[update] Bundle update result: " + err);
    checkBundleErrorResult(err);
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getLastModified()
   */
  public long getLastModified() throws IAgentException {
    DebugUtils.debug(this, "[getLastModified] >>>");
    checkBundleState();
    Long lastModified = (Long) GET_BUNDLE_LAST_MODIFIED_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    if (lastModified.longValue() == -2) {
      DebugUtils.debug(this, "[getLastModified] remote call result: " + lastModified + " -> bundle is uninstalled");
      uninstalled = true;
      checkBundleState();
      return 0; // unreachable
    }
    DebugUtils.debug(this, "[getLastModified] bundle last modified: " + lastModified);
    return lastModified.longValue();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getRegisteredServices()
   */
  public RemoteService[] getRegisteredServices() throws IAgentException {
    DebugUtils.debug(this, "[getRegisteredServices] >>>");
    checkBundleState();
    Dictionary[] servicesProps = (Dictionary[]) GET_REGISTERED_SERVICES_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    if (servicesProps == null) {
      DebugUtils.debug(this, "[getRegisteredServices] remote call result is null -> bundle is uninstalled");
      uninstalled = true;
      checkBundleState();
      return new RemoteService[0];
    }
    RemoteService[] services = new RemoteService[servicesProps.length];
    for (int i = 0; i < servicesProps.length; i++) {
      services[i] = new RemoteServiceImpl((ServiceManagerImpl) commands.getDeviceConnector().getServiceManager(),
          servicesProps[i]);
    }
    DebugUtils.debug(this, "[getRegisteredServices] Registered services: " + DebugUtils.convertForDebug(services));
    return services;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getServicesInUse()
   */
  public RemoteService[] getServicesInUse() throws IAgentException {
    DebugUtils.debug(this, "[getServicesInUse] >>>");
    checkBundleState();
    Dictionary[] servicesProps = (Dictionary[]) GET_USING_SERVICES_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    if (servicesProps == null) {
      DebugUtils.debug(this, "[getServicesInUse] remote call result is null -> bundle is uninstalled");
      uninstalled = true;
      checkBundleState();
      return new RemoteService[0];
    }
    RemoteService[] services = new RemoteService[servicesProps.length];
    for (int i = 0; i < servicesProps.length; i++) {
      services[i] = new RemoteServiceImpl((ServiceManagerImpl) commands.getDeviceConnector().getServiceManager(),
          servicesProps[i]);
    }
    DebugUtils.debug(this, "[getServicesInUse] In use services: " + DebugUtils.convertForDebug(services));
    return services;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getFragments()
   */
  public RemoteBundle[] getFragments() throws IAgentException {
    DebugUtils.debug(this, "[getFragments] >>>");
    checkBundleState();
    long[] fragmentBundleIDs = (long[]) GET_FRAGMENT_BUNDLES_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    if (fragmentBundleIDs == null) {
      DebugUtils.debug(this, "[getFragments] remote call result is null -> bundle is uninstalled");
      uninstalled = true;
      checkBundleState();
      return null;
    }
    if (fragmentBundleIDs.length == 0) {
      DebugUtils.debug(this, "[getFragments] No fragment bundles");
      return null;
    }
    RemoteBundle[] fragmentRemoteBundles = new RemoteBundle[fragmentBundleIDs.length];
    for (int i = 0; i < fragmentRemoteBundles.length; i++) {
      fragmentRemoteBundles[i] = commands.getBundle(fragmentBundleIDs[i]);
    }
    DebugUtils.debug(this, "[getFragments] Attached fragments: " + DebugUtils.convertForDebug(fragmentRemoteBundles));
    return fragmentRemoteBundles;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getHosts()
   */
  public RemoteBundle[] getHosts() throws IAgentException {
    DebugUtils.debug(this, "[getHosts] >>>");
    checkBundleState();
    long[] hostBundleIDs = (long[]) GET_HOST_BUNDLES_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    if (hostBundleIDs == null) {
      DebugUtils.debug(this, "[getHosts] remote call result is null -> bundle is uninstalled");
      uninstalled = true;
      checkBundleState();
      return null;
    }
    if (hostBundleIDs.length == 0) {
      DebugUtils.debug(this, "[getHosts] No host bundles");
      return null;
    }
    RemoteBundle[] hostRemoteBundles = new RemoteBundle[hostBundleIDs.length];
    for (int i = 0; i < hostRemoteBundles.length; i++) {
      hostRemoteBundles[i] = commands.getBundle(hostBundleIDs[i]);
    }
    DebugUtils.debug(this, "[getHosts] Hosts attached to: " + DebugUtils.convertForDebug(hostRemoteBundles));
    return hostRemoteBundles;
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getType()
   */
  public int getType() throws IAgentException {
    DebugUtils.debug(this, "[getType] >>>");
    checkBundleState();
    Integer bundleType = (Integer) GET_BUNDLE_TYPE_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    if (bundleType.intValue() == -1) {
      DebugUtils.debug(this, "[getType] remote call result is: " + bundleType + " -> bundle is uninstalled");
      uninstalled = true;
      checkBundleState();
    } else if (bundleType.intValue() == -2) {
      DebugUtils.info(this, "[getType] PackageAdmin is not available on the remote site");
      throw new IAgentException("PackageAdmin is not available on the remote site", 0);
    }
    DebugUtils.debug(this, "[getType] bundle type: " + bundleType);
    return bundleType.intValue();
  }

  /*
   * (non-Javadoc)
   *
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getBundleStartLevel()
   */
  public int getBundleStartLevel() throws IAgentException {
    DebugUtils.debug(this, "[getBundleStartLevel] >>>");
    checkBundleState();
    Integer bundleStartLevel = (Integer) GET_BUNDLE_START_LEVEL_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    return bundleStartLevel.intValue();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.tigris.mtoolkit.iagent.RemoteBundle#getResource(java.lang.String)
   */
  public InputStream getResource(String name) throws IAgentException {
    if (name == null) {
      throw new IllegalArgumentException();
    }
    if (!GET_BUNDLE_RESOURCE_METHOD.isDefined(getBundleAdmin())) {
      return null;
    }
    Object res = GET_BUNDLE_RESOURCE_METHOD.call(getBundleAdmin(), new Object[] {
        id, name, null
    });
    if (res instanceof Error) {
      checkBundleErrorResult((Error) res);
    } else if (res instanceof InputStream) {
      return (InputStream) res;
    } else if (res instanceof RemoteObject) {
      return new RemoteReader((RemoteObject) res);
    }
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * org.tigris.mtoolkit.iagent.RemotePackage#uninstall(java.util.Dictionary)
   */
  public void uninstall(Dictionary params) throws IAgentException {
    DebugUtils.debug(this, "[uninstall] >>>");
    checkBundleState();
    Error err = (Error) UNINSTALL_BUNDLE_METHOD.call(getBundleAdmin(), new Object[] {
      id
    });
    DebugUtils.debug(this, "[uninstall] Bundle uninstallation result: " + err);
    if (err == null) {
      uninstalled = true;
      return;
    }
    checkBundleErrorResult(err);
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#refreshPackages()
   */
  public void refreshPackages() throws IAgentException {
    commands.refreshPackages();
  }


  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#getSignerCertificates()
   */
  //public Dictionary getSignerCertificates() throws IAgentException {
  public Dictionary getSignerCertificates() throws IAgentException {
    DebugUtils.debug(this, "[getSignerCertificates] >>>");
    checkBundleState();
    RemoteObject admin = getBundleAdmin();
    Dictionary signerCertificates = null;
    if (Utils.isRemoteMethodDefined(admin, GET_SIGNER_CERTIFICATES_METHOD)) {
      signerCertificates = (Dictionary) GET_SIGNER_CERTIFICATES_METHOD.call(admin, new Object[] {
        id
      });
      DebugUtils.debug(this, "[getSignerCertificates] Bundle sertificates: " + signerCertificates);
    } else {
      DebugUtils.debug(this, "[getSignerCertificates] method not found on iagent>>>");
    }
    return signerCertificates;
  }

  /* (non-Javadoc)
   * @see org.tigris.mtoolkit.iagent.RemoteBundle#isSignerTrusted()
   */
  public boolean isSignerTrusted() throws IAgentException {
    DebugUtils.debug(this, "[isSignerTrusted] >>>");
    boolean isSignerTrusted = false;
    checkBundleState();
    RemoteObject admin = getBundleAdmin();
    if (Utils.isRemoteMethodDefined(admin, IS_SIGNER_TRUSTED_METHOD)) {
      Boolean isSignerTrustedResult = (Boolean) IS_SIGNER_TRUSTED_METHOD.call(admin, new Object[] {
        id
      });
      isSignerTrusted = isSignerTrustedResult.booleanValue();
      DebugUtils.debug(this, "[isSignerTrusted] Bundle signer trusted: " + isSignerTrusted);
    } else {
      DebugUtils.debug(this, "[isSignerTrusted] Method not found on iagent >>>");
    }
    return isSignerTrusted;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "RemoteBundle@" + Integer.toHexString(System.identityHashCode(this)) + "[" + id + "][" + location + "]";
  }

  private void checkBundleErrorResult(Error err) throws IAgentException {
    if (err == null) {
      return;
    } else if (err.getCode() == Error.BUNDLE_UNINSTALLED_CODE) {
      uninstalled = true;
      checkBundleState();
    } else {
      throw new IAgentException(err);
    }
  }

  private void checkBundleState() throws IAgentException {
    if (uninstalled) {
      DebugUtils.debug(this, "[checkBundleState] Remote bundle has been uninstalled");
      throw new IAgentException("Remote bundle has been uninstalled", IAgentErrors.ERROR_BUNDLE_UNINSTALLED);
    }
  }

  private RemoteObject getBundleAdmin() throws IAgentException {
    return commands.getBundleAdmin();
  }
}
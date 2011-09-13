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
package org.tigris.mtoolkit.common.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.tigris.mtoolkit.common.UtilitiesPlugin;
import org.tigris.mtoolkit.common.gui.SWTUserInteraction;

public class InteractiveConnectionHandler extends SSLSocketFactory implements HostnameVerifier {


	private static final UserInteractionWrapper interactionWrapper = new UserInteractionWrapper();
	private static final Object syncObject = new Object();

	private static volatile UserInteraction userInteraction;
	private static TemporaryStore tempStore = new TemporaryStore();

	private SSLSocketFactory privateFactory;
	private SSLSocketFactory defaultFactory;
	private HostnameVerifier privateVerifier;
	private HostnameVerifier defaultVerifier;

	private static final String MPRM_HTTP_TRANSPORT_THREAD_ELEMENT = "com.prosyst.mprm.net.impl.http.client.HttpClientTransportConnection";

	private List threads = new ArrayList(2);

	private InteractiveConnectionHandler(	SSLSocketFactory privateFactory,
											SSLSocketFactory defaultFactory,
											HostnameVerifier privateVerifier,
											HostnameVerifier defaultVerifier) {
		this.privateFactory = privateFactory;
		this.defaultFactory = defaultFactory;
		this.privateVerifier = privateVerifier;
		this.defaultVerifier = defaultVerifier;
	}

	public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
		return chooseSocketFactory(host).createSocket(s, host, port, autoClose);
	}

	public String[] getDefaultCipherSuites() {
		return chooseSocketFactory(null).getDefaultCipherSuites();
	}

	public String[] getSupportedCipherSuites() {
		return chooseSocketFactory(null).getDefaultCipherSuites();
	}

	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return chooseSocketFactory(host).createSocket(host, port);
	}

	public Socket createSocket(InetAddress host, int port) throws IOException {
		return chooseSocketFactory(host.toString()).createSocket(host, port);
	}

	public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException,
					UnknownHostException {
		return chooseSocketFactory(host).createSocket(host, port, localHost, localPort);
	}

	public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort)
					throws IOException {
		return chooseSocketFactory(address.toString()).createSocket(address, port, localAddress, localPort);
	}

	public boolean verify(String host, SSLSession session) {
		return chooseHostnameVerifier().verify(host, session);
	}

	private SSLSocketFactory chooseSocketFactory(String host) {
		synchronized (this) {
			try {
				if (threads.contains(Thread.currentThread()) || isMprmHttpTransportThread())
					return host != null ? createInteractiveSocketFactory(host) : privateFactory;
			} catch (GeneralSecurityException e) {
				// TODO: logging
				e.printStackTrace();
			}
		}
		Thread.dumpStack();
		return defaultFactory;
	}

	private boolean isMprmHttpTransportThread() {
		StackTraceElement[] stackTrace = new Exception().getStackTrace();
		for (int i = stackTrace.length - 1; i >= 0; i--) {
			if (MPRM_HTTP_TRANSPORT_THREAD_ELEMENT.equals(stackTrace[i].getClassName())) {
				return true;
			}
		}
		return false;
	}

	private HostnameVerifier chooseHostnameVerifier() {
		synchronized (this) {
			if (threads.contains(Thread.currentThread()))
				return privateVerifier;
			if (isMprmHttpTransportThread())
				return privateVerifier;
		}
		Thread.dumpStack();
		return defaultVerifier;
	}

	public static Object executeWithInteraction(Operation operation) throws Exception {
		if (userInteraction == null)
			userInteraction = new SWTUserInteraction();
		InteractiveConnectionHandler interactiveFactory = null;
		synchronized (syncObject) {
			interactiveFactory = installInteractiveFactory();
			interactiveFactory.markThread();
		}
		try {
			return operation.execute();
		} finally {
			synchronized (syncObject) {
				if (interactiveFactory.unmarkThread())
					// if the factory is no longer in use, uninstall it
					uninstallInteractiveFactory(interactiveFactory);
			}
		}
	}

	private void markThread() {
		synchronized (threads) {
			threads.add(Thread.currentThread());
		}
	}

	private boolean unmarkThread() {
		synchronized (threads) {
			threads.remove(Thread.currentThread());
		}
		return threads.size() == 0;
	}

	private static InteractiveConnectionHandler installInteractiveFactory() throws GeneralSecurityException {
		SSLSocketFactory oldFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
		InteractiveConnectionHandler handler;
		if (oldFactory instanceof InteractiveConnectionHandler) {
			handler = (InteractiveConnectionHandler) oldFactory;
			if (HttpsURLConnection.getDefaultHostnameVerifier() != oldFactory) {
				// the ssl socket factory is OK, but the hostname verifier has been replaced
				// update the ssl socket factory and reset it
				handler.defaultVerifier = findOriginalHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
			}
		} else {
			// just in case, get the original verifier, not some version of our left behind
			HostnameVerifier originalVerifier = findOriginalHostnameVerifier(HttpsURLConnection.getDefaultHostnameVerifier());
			handler = createInteractiveConnectionHandler(oldFactory, originalVerifier);
			HttpsURLConnection.setDefaultSSLSocketFactory(handler);
			HttpsURLConnection.setDefaultHostnameVerifier(handler);
		}
		return handler;
	}

	private static HostnameVerifier findOriginalHostnameVerifier(HostnameVerifier current) {
		HostnameVerifier next = current;
		while (next instanceof InteractiveConnectionHandler) {
			next = ((InteractiveConnectionHandler) next).defaultVerifier;
		}
		return next;
	}

	private static void uninstallInteractiveFactory(InteractiveConnectionHandler handler) {
		// uninstall the factory only in case nobody has replaced it, otherwise we cannot do anything
		// NOTE: This replacement of the default ssl socket factory is not thread-safe
		// but we cannot synchronize with external parties
		if (HttpsURLConnection.getDefaultSSLSocketFactory() == handler)
			HttpsURLConnection.setDefaultSSLSocketFactory(handler.defaultFactory);
		if (HttpsURLConnection.getDefaultHostnameVerifier() == handler)
			HttpsURLConnection.setDefaultHostnameVerifier(handler.defaultVerifier);
	}

	private static InteractiveConnectionHandler createInteractiveConnectionHandler(SSLSocketFactory oldSocketFactory,
					HostnameVerifier oldVerifier) throws GeneralSecurityException {
		InteractiveConnectionHandler factory = new InteractiveConnectionHandler(createInteractiveSocketFactory(null),
			oldSocketFactory,
			getHostnameVerifier(),
			oldVerifier);
		return factory;
	}

	private static SSLSocketFactory createInteractiveSocketFactory(String host) throws GeneralSecurityException {
		SSLContext ctx = SSLContext.getInstance("SSLv3");
		ctx.init(null, new TrustManager[] { getTrustManager(host) }, null);
		return ctx.getSocketFactory();
	}

  private static InteractiveTrustManager getTrustManager(String host) {
		return new InteractiveTrustManager(null, interactionWrapper, tempStore, host);
	}

  private static InteractiveHostnameVerifier getHostnameVerifier() {
		return new InteractiveHostnameVerifier(interactionWrapper, tempStore);
	}

	// @NotThreadSafe
	public static void setUserInteraction(UserInteraction aUserInteraction) {
		userInteraction = aUserInteraction;
	}

	private static class UserInteractionWrapper implements UserInteraction {

		public boolean confirmConnectionTrust(int validationResult, String message, String host,
						X509Certificate[] certChain) {
			UserInteraction _userInteraction = userInteraction;
			if (_userInteraction != null)
				return _userInteraction.confirmConnectionTrust(validationResult, message, host, certChain);
			// TODO: Log that the user interaction is not initialized yet
			UtilitiesPlugin.getDefault().getLog().log(new Status(IStatus.WARNING,
				UtilitiesPlugin.PLUGIN_ID,
				"SSL certificate requested, but the dialog failed to show",
				new Exception()));
			return false;
		}
	}
	
	public static TemporaryStore getTemporaryStore() {
		return tempStore;
	}
}

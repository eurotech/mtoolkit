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
package org.tigris.mtoolkit.iagent.internal.rpc.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.StringTokenizer;

import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.Bundle;

public class EquinoxCommandInterpreter implements CommandInterpreter {

	private static final String WS_DELIM = " \t\n\r\f"; //$NON-NLS-1$

	/** The command line in StringTokenizer form */
	private StringTokenizer tok;
	
	private boolean firstCommand = true;
	
	private Object[] commandProviders;
	
	private RemoteConsoleServiceBase console;
	
	public EquinoxCommandInterpreter(String cmd, Object[] commandProviders, RemoteConsoleServiceBase console) {
		tok = new StringTokenizer(cmd);
		this.commandProviders = commandProviders;
		this.console = console;
	}

	public Object execute(String cmd) {
		if (!firstCommand)
			return innerExecute(cmd);
		firstCommand = false;
		Object retval = null;
		Class[] parameterTypes = new Class[] {CommandInterpreter.class};
		Object[] parameters = new Object[] {this};
		boolean executed = false;
		int size = commandProviders.length;
		for (int i = 0; !executed && (i < size); i++) {
			try {
				Object target = commandProviders[i];
				Method method = target.getClass().getMethod("_" + cmd, parameterTypes); //$NON-NLS-1$
				retval = method.invoke(target, parameters);
				executed = true; // stop after the command has been found
			} catch (NoSuchMethodException ite) {
				// keep going - maybe another command provider will be able to execute this command
			} catch (InvocationTargetException ite) {
				executed = true; // don't want to keep trying - we found the method but got an error
				printStackTrace(ite.getTargetException());
			} catch (Exception ee) {
				executed = true; // don't want to keep trying - we got an error we don't understand
				printStackTrace(ee);
			}
		}
		// if no command was found to execute, display help for all registered command providers
		if (!executed) {
			for (int i = 0; i < size; i++) {
				try {
					CommandProvider commandProvider = (CommandProvider) commandProviders[i];
					console.print(commandProvider.getHelp());
				} catch (Exception ee) {
					printStackTrace(ee);
				}
			}
		}
		return retval;
	}
	
	private Object innerExecute(String cmd) {
		if (cmd != null && cmd.length() > 0) {
			CommandInterpreter intcp = new EquinoxCommandInterpreter(cmd, commandProviders, console);
			String command = intcp.nextArgument();
			if (command != null)
				return intcp.execute(command);
		}
		return null;
	}

	public String nextArgument() {
		if (tok == null || !tok.hasMoreElements())
			return null;

		String arg = tok.nextToken();
		if (arg.startsWith("\"")) { //$NON-NLS-1$
			if (arg.endsWith("\"")) { //$NON-NLS-1$
				if (arg.length() >= 2)
					// strip the beginning and ending quotes
					return arg.substring(1, arg.length() - 1);
			}
			String remainingArg = tok.nextToken("\""); //$NON-NLS-1$
			arg = arg.substring(1) + remainingArg;
			// skip to next whitespace separated token
			tok.nextToken(WS_DELIM);
		} else if (arg.startsWith("'")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (arg.endsWith("'")) { //$NON-NLS-1$
				if (arg.length() >= 2)
					// strip the beginning and ending quotes
					return arg.substring(1, arg.length() - 1);
			}
			String remainingArg = tok.nextToken("'"); //$NON-NLS-1$
			arg = arg.substring(1) + remainingArg;
			// skip to next whitespace separated token
			tok.nextToken(WS_DELIM);
		}
		return arg;
	}

	public void print(Object o) {
		console.print(String.valueOf(o));
	}

	public void printBundleResource(Bundle bundle, String resource) {
		URL entry = null;
		entry = bundle.getEntry(resource);
		if (entry != null) {
			try {
				println(resource);
				InputStream in = entry.openStream();
				byte[] buffer = new byte[1024];
				int read = 0;
				try {
					while ((read = in.read(buffer)) != -1)
						print(new String(buffer, 0, read));
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
						}
					}
				}
			} catch (Exception e) {
				printStackTrace(e);
			}
		} else {
			println(resource + " not found in " + bundle.toString());
		}
	}

	public void printDictionary(Dictionary dic, String title) {
		if (dic == null)
			return;

		int count = dic.size();
		String[] keys = new String[count];
		Enumeration keysEnum = dic.keys();
		int i = 0;
		while (keysEnum.hasMoreElements()) {
			keys[i++] = (String) keysEnum.nextElement();
		}
		Arrays.sort(keys);

		if (title != null) {
			println(title);
		}
		for (i = 0; i < count; i++) {
			println(" " + keys[i] + " = " + dic.get(keys[i])); //$NON-NLS-1$//$NON-NLS-2$
		}
		println();
	}

	public void printStackTrace(Throwable t) {
		console.print(generateStackTrace(t));
	}
	
	private String generateStackTrace(Throwable t) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter out = new PrintWriter(stringWriter);
		
		t.printStackTrace(out);

		Method[] methods = t.getClass().getMethods();

		int size = methods.length;
		Class throwable = Throwable.class;

		for (int i = 0; i < size; i++) {
			Method method = methods[i];

			if (Modifier.isPublic(method.getModifiers()) && method.getName().startsWith("get") && throwable.isAssignableFrom(method.getReturnType()) && (method.getParameterTypes().length == 0)) { //$NON-NLS-1$
				try {
					Throwable nested = (Throwable) method.invoke(t, null);

					if ((nested != null) && (nested != t)) {
						out.println("Nested exceptions:");
						out.print(generateStackTrace(nested));
					}
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
				}
			}
		}
		return stringWriter.toString();
	}

	public void println() {
		// TODO: Handle correctly line separators
		console.print(System.getProperty("line.separator"));
	}

	public void println(Object o) {
		print(o);
		println();
	}

}

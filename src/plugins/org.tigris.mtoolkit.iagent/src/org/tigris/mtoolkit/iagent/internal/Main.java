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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.tigris.mtoolkit.iagent.DeploymentManager;
import org.tigris.mtoolkit.iagent.DeviceConnector;
import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.iagent.RemoteBundle;

public class Main {
	private static Map ERROR_MESSAGES = new HashMap();

	static {
		ERROR_MESSAGES.put(new Integer(-5006), "Cannot connect to the device.");
		ERROR_MESSAGES.put(new Integer(-5007), "Connection to the device was closed by external entity.");
		ERROR_MESSAGES.put(new Integer(-5008), "Some internal error occurred while communicating with the device.");
		ERROR_MESSAGES.put(new Integer(-3009), "Timeout waiting for the VM to start.");
		ERROR_MESSAGES.put(new Integer(-3010), "An error ocurred while starting the VM.");
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.out.println("Invalid number of arguments are specified.");
			usage();
			return;
		}
		String command = args[0];
		String ip = args[1];
		try {
			if ("start".equals(command)) {
				String[] vmArgs = new String[args.length - 2];
				System.arraycopy(args, 2, vmArgs, 0, vmArgs.length);
				startVM(ip, vmArgs);
				return;
			}
			if ("stop".equals(command)) {
				stopVM(ip);
				return;
			}
			if ("status".equals(command)) {
				statusVM(ip);
				return;
			}
			if ("ls".equals(command)) {
				listContent(ip);
				return;
			}
			System.out.println("Invalid command specified: " + command);
			usage();
			return;
		} catch (IAgentException e) {
			e.printStackTrace();
			System.out.println(e.getErrorCode() + " : " + e.getMessage());
			if (ERROR_MESSAGES.get(new Integer(e.getErrorCode())) != null) {
        System.out.println("Details: " + ERROR_MESSAGES.get(new Integer(e.getErrorCode())));
      }
		}
	}

	private static void statusVM(String ip) throws IAgentException {
		DeviceConnector connector = connectVM(ip);
		try {
			if (connector.getVMManager().isVMActive()) {
				System.out.println("Remote OSGi framework is active.");
			} else {
				System.out.println("Remote OSGi framework is not active.");
			}
		} finally {
			connector.closeConnection();
		}
	}

	private static void stopVM(String ip) throws IAgentException {
		DeviceConnector connector = connectVM(ip);
		try {
			connector.getVMManager().stopVM();
			System.out.println("Remote virtual machine successfully stopped.");
		} finally {
			connector.closeConnection();
		}
	}

	private static void listContent(String ip) throws IAgentException {
		DeviceConnector connector = connectVM(ip, false);
		try {
			if (!connector.getVMManager().isVMActive()) {
				System.out.println("Remote OSGi framework is not active. Cannot list its content.");
				return;
			}
			DeploymentManager mng = connector.getDeploymentManager();
			RemoteBundle bundles[] = mng.listBundles();
			for (int i = 0; i < bundles.length; i++) {
				System.out.println(bundles[i].getBundleId() + "\t" + getState(bundles[i].getState()) + "\t"
						+ bundles[i].getSymbolicName() + " [" + bundles[i].getVersion());
			}
		} finally {
			connector.closeConnection();
		}
	}

	private static String getState(int state) {
		switch (state) {
		case Bundle.ACTIVE:
			return "ACTIVE";
		case Bundle.INSTALLED:
			return "INSTALLED";
		case Bundle.RESOLVED:
			return "RESOLVED";
		case Bundle.STARTING:
			return "STARTING";
		case Bundle.STOPPING:
			return "STOPPING";
		case Bundle.UNINSTALLED:
			return "UNINSTALLED";
		}
		return "UNKNOWN(" + state + ")";
	}

	private static void startVM(String ip, String[] vmArgs) throws IAgentException {
		DeviceConnector connector = connectVM(ip);
		try {
			System.out.println("Warning: If the remote VM is already active, starting it again will not result in any additional action.");
			connector.getVMManager().resetArgs();
			System.out.print("Command line: ");
			for (int i = 0; i < vmArgs.length; ++i) {
				System.out.print(vmArgs[i]);
				System.out.print(' ');
				connector.getVMManager().addRawArgument(vmArgs[i]);
			}
			System.out.println();
			connector.getVMManager().startVM();
			System.out.print("Remote virtual machine successfully started.");
		} finally {
			connector.closeConnection();
		}
	}

	private static DeviceConnector connectVM(String ip) throws IAgentException {
		Hashtable props = new Hashtable();
		props.put("framework-connection-ip", ip);
		return DeviceConnector.connect("socket", ip, props, null);
	}

	private static DeviceConnector connectVM(String ip, boolean immediate) throws IAgentException {
		Hashtable props = new Hashtable();
		props.put("framework-connection-ip", ip);
		props.put("framework-connection-immediate", Boolean.valueOf(immediate));
		return DeviceConnector.connect("socket", ip, props, null);
	}

	private static void usage() {
		System.out.println("USAGE: iagent <command> <IP> [args]");
		System.out.println("\t<command> - instructs the iagent what to do. The following commands can be used:");
		System.out.println("\t\t\tstart - starts the VM (if not running). The command will wait until the framework is fully started before returning.");
		System.out.println("\t\t\tstop - stops the VM (if running). The command will wait until the framework is fully stopped before returning.");
		System.out.println("\t\t\tstatus - prints the status of the VM. It can be either running or not running.");
		System.out.println("\t\t\tls - prints the list of the bundles on the remote framework.");
		System.out.println("\t<IP> - the IP of the device. It must be specified immediately after the <command>.");
		System.out.println("\t[args] - any arguments that are used for starting the VM. These arguments are considered only when launching the VM and ignored for any other command");
	}
}
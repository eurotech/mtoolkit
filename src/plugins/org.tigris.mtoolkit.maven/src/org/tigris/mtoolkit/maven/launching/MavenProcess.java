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
package org.tigris.mtoolkit.maven.launching;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.tigris.mtoolkit.maven.MavenConstants;
import org.tigris.mtoolkit.maven.internal.MavenCorePlugin;


public class MavenProcess {

	private static boolean isWindows = File.separatorChar == '\\';

	public static void launchDefaultBuild(File location, IProgressMonitor monitor) throws CoreException {
		ILaunchManager launchManager = DebugPlugin.getDefault()
				.getLaunchManager();
		ILaunchConfigurationType launchConfigurationType = launchManager
				.getLaunchConfigurationType("org.eclipse.m2e.Maven2LaunchConfigurationType");

		if (launchConfigurationType != null) {
			// String goals = "clean install";
			String goals = "";
			for (String goal : MavenConstants.DEFAULT_GOALS) {
				goals += goal + " ";
			}

			String loc = location.getPath();
			loc = loc.replace('\\', '-');
			loc = loc.replace('/', '-');
			ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType
					.newInstance(null, //
							"Executing " + goals + " in " + loc);
			workingCopy.setAttribute(
					"org.eclipse.jdt.launching.WORKING_DIRECTORY",
					location.getPath());
			workingCopy.setAttribute("M2_GOALS", goals);
			workingCopy.setAttribute(
					"org.eclipse.debug.core.ATTR_REFRESH_SCOPE", "${project}");
			workingCopy.setAttribute(
					"org.eclipse.debug.core.ATTR_REFRESH_RECURSIVE", true);

			ILaunch launch = workingCopy.launch(ILaunchManager.RUN_MODE,
					monitor);

			IProcess[] processes = launch.getProcesses();
			if (processes == null || processes.length != 1) {
				throw new CoreException(new Status(IStatus.ERROR,
						MavenCorePlugin.PLUGIN_ID,
						"Could not start maven install process!"));
			}
			IProcess process = processes[0];
			while (!process.isTerminated()) {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
			int exitValue = process.getExitValue();
			if (exitValue != 0) {
				throw new CoreException(new Status(IStatus.ERROR,
						MavenCorePlugin.PLUGIN_ID,
						"Install process exit value is: " + exitValue));
			}
		} else {
			launchGoals(location, MavenConstants.DEFAULT_GOALS, monitor);
		}
	}

	public static void launchGoals(File location, String[] goals, IProgressMonitor monitor) throws CoreException {
		String[] cmdLine = generateMavenCmdLine(goals);
		try {
			Process mvnProcess = executeProcess(cmdLine, location);
			waitForMavenBuild(mvnProcess, monitor);
		} catch (IOException e) {
			throw MavenCorePlugin.newException(IStatus.ERROR,
					"Cannot launch Maven build process at " + location.getAbsolutePath(), e);
		}
	}

	private static String[] generateMavenCmdLine(String[] goals) {
		List<String> cmdLine = new ArrayList<String>();
		if (isWindows) {
			cmdLine.add("cmd.exe");
			cmdLine.add("/c");
			cmdLine.add("mvn");
		} else {
			cmdLine.add("mvn");
		}
		if (goals != null) {
			for (String goal : goals) {
				cmdLine.add(goal);
			}
		}
		return cmdLine.toArray(new String[cmdLine.size()]);
	}

	private static Process executeProcess(String[] cmdLine, File workingDir) throws IOException {
		return Runtime.getRuntime().exec(cmdLine, null, workingDir);
	}

	private static void captureOutput(Process process, OutputStream collectOutput, OutputStream collectError) {
		new CaptureOutputJob(process, process.getInputStream(), collectOutput, process.toString() + "[stdout]").schedule();
		new CaptureOutputJob(process, process.getErrorStream(), collectError, process.toString() + "[stderr]").schedule();
	}

	private static void redirectOutputToStdout(Process process) {
		captureOutput(process, System.out, System.err);
	}

	private static void waitForMavenBuild(Process process, IProgressMonitor monitor) throws CoreException {
		// TODO: Redirect the maven output, so we can handle client requests
		redirectOutputToStdout(process);
		MonitorProcessJob job = new MonitorProcessJob(process.toString(), process);
		job.schedule();
		try {
			Job.getJobManager().join(process, monitor);
		} catch (OperationCanceledException e) {
		} catch (InterruptedException e) {
		}
		if (job.getExitValue() != 0)
			throw MavenCorePlugin.newException(IStatus.ERROR,
					"Maven build failed with non-zero exit value: " + job.getExitValue(), null);
	}

	private static class CaptureOutputJob extends Job {
		private Process process;
		private InputStream input;
		private OutputStream output;

		public CaptureOutputJob(Process process, InputStream input, OutputStream output, String processName) {
			super("Capturing output of " + processName);
			this.input = input;
			this.output = output;
			this.process = process;
			setSystem(true);
			setUser(false);
		}

		@Override
		public boolean belongsTo(Object family) {
			return process != null && process.equals(family);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			byte[] buffer = new byte[2048];
			int read;
			try {
				while ((read = input.read(buffer)) != -1) {
					output.write(buffer, 0, read);
					if (monitor.isCanceled())
						break;
				}
			} catch (IOException e) {
				if (!monitor.isCanceled())
					return MavenCorePlugin.newStatus(IStatus.ERROR, "IOException while capturing external process output", e);
			}
			if (monitor.isCanceled())
				return Status.CANCEL_STATUS;
			return Status.OK_STATUS;
		}
	}

	private static class MonitorProcessJob extends Job {
		private Process process;
		private int exitValue = -1;

		public MonitorProcessJob(String name, Process process) {
			super("Monitoring " + name);
			this.process = process;
			setSystem(true);
			setUser(false);
		}

		@Override
		public boolean belongsTo(Object family) {
			return process != null && process.equals(family);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try {
				exitValue = process.waitFor();
				return Status.OK_STATUS;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return Status.CANCEL_STATUS;
			}
		}

		public int getExitValue() {
			return exitValue;
		}
	}
}

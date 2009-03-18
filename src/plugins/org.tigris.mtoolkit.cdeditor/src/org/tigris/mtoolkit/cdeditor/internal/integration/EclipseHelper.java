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
package org.tigris.mtoolkit.cdeditor.internal.integration;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;

/**
 * Provides common methods for use in eclipse environment.
 */
public class EclipseHelper {

	public static void enableDescriptionValidation(IProject project, boolean enable) {
		if (project == null)
			return;
		try {
			enableBuilder(project, enable);
			if (!enable)
				project.deleteMarkers(ComponentDescriptionBuilder.MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			CDEditorPlugin.log(e);
		}
	}
	
	public static boolean isDescriptionValidationEnabled(IProject project) {
		return findComponentDescriptionBuilder(project) != -1;
	}
	
	private static int findComponentDescriptionBuilder(IProject project) {
		try {
			IProjectDescription desc = project.getDescription();
			ICommand[] commands = desc.getBuildSpec();

			for (int i = 0; i < commands.length; ++i) {
				if (commands[i].getBuilderName().equals(ComponentDescriptionBuilder.BUILDER_ID))
					return i;
			}
		} catch (CoreException e) {
			CDEditorPlugin.log(e);
		}
		return -1;
	}

	private static boolean enableBuilder(IProject project, boolean enable) throws CoreException {
		IProjectDescription desc = project.getDescription();
		ICommand[] commands = desc.getBuildSpec();

		int idx = findComponentDescriptionBuilder(project);
		boolean found = idx != -1;

		ICommand[] newCommands;
		if (found) {
			if (enable)
				return true;
			newCommands = new ICommand[commands.length - 1];
			System.arraycopy(commands, 0, newCommands, 0, idx);
			if (idx < commands.length - 1)
				System.arraycopy(commands, idx + 1, newCommands, idx, commands.length - idx - 1);
		} else {
			if (!enable)
				return false;
			newCommands = new ICommand[commands.length + 1];
			System.arraycopy(commands, 0, newCommands, 0, commands.length);
			ICommand command = desc.newCommand();
			command.setBuilderName(ComponentDescriptionBuilder.BUILDER_ID);
			newCommands[newCommands.length - 1] = command;
		}
		desc.setBuildSpec(newCommands);
		project.setDescription(desc, null);
		return enable;
	}

}

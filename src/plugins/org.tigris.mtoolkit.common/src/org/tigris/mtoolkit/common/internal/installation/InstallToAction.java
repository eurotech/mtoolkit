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
package org.tigris.mtoolkit.common.internal.installation;

import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.tigris.mtoolkit.common.installation.InstallationItem;
import org.tigris.mtoolkit.common.installation.InstallationItemProcessor;
import org.tigris.mtoolkit.common.installation.InstallationTarget;

public class InstallToAction extends Action {
	private InstallationItemProcessor processor;
	private InstallationTarget target;
	private List items;

	public InstallToAction(InstallationItemProcessor processor, InstallationTarget target, List items) {
		super(target.getName());
		this.processor = processor;
		this.target = target;
		this.items = items;
	}

	public void run() {
		Job job = new Job("Installing to " + target.getName()) {
			public IStatus run(IProgressMonitor monitor) {
				InstallationHistory.getDefault().promoteHistory(target, processor);
				InstallationHistory.getDefault().saveHistory();

				SubMonitor subMonitor = SubMonitor.convert(monitor, items.size());
				Iterator iterator = items.iterator();
				while (iterator.hasNext()) {
					SubMonitor mon = subMonitor.newChild(1);
					processor.processInstallationItem((InstallationItem) iterator.next(), target, mon);
					if (monitor.isCanceled()) {
						break;
					}
				}

				monitor.done();
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
}

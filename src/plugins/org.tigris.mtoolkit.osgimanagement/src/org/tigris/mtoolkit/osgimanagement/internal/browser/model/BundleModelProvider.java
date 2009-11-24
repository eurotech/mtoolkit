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
package org.tigris.mtoolkit.osgimanagement.internal.browser.model;

import org.tigris.mtoolkit.iagent.IAgentException;
import org.tigris.mtoolkit.osgimanagement.MimeTypeContentProvider;
import org.tigris.mtoolkit.osgimanagement.browser.model.Framework;
import org.tigris.mtoolkit.osgimanagement.browser.model.Model;

public class BundleModelProvider implements MimeTypeContentProvider {

	public Model getResource(String id, String version, Framework framework) throws IAgentException {
		FrameworkImpl fw = ((FrameworkImpl)framework);
		Bundle master = fw.findBundle(new Long(id));
		Bundle slave = new Bundle(master);
		
		
		Model children[] = master.getChildren();
		if (children != null && children.length > 0) {
			Model regServ[] = children[0].getChildren();
			if (regServ != null) {
				Model servCategory = fw.getServiceCategoryNode(slave, ServicesCategory.REGISTERED_SERVICES, true);
				for (int i = 0; i < regServ.length; i++) {
					ObjectClass oc = new ObjectClass(regServ[i].getName(),
						new Long(((ObjectClass) regServ[i]).getService().getServiceId()),
						((ObjectClass) regServ[i]).getService());
					servCategory.addElement(oc);
					if (fw != null &&  fw.isShownServicePropertiss()) {
						try {
							fw.addServicePropertiesNodes(oc);
						} catch (IAgentException e) {
							e.printStackTrace();
						}
					}
				}
			}

			Model usedServ[] = children[1].getChildren();
			if (usedServ != null) {
				Model servCategory = fw.getServiceCategoryNode(slave, ServicesCategory.USED_SERVICES, true);
				for (int i = 0; i < usedServ.length; i++) {
					ObjectClass oc = new ObjectClass(usedServ[i].getName(),
						new Long(((ObjectClass) usedServ[i]).getService().getServiceId()),
						((ObjectClass) usedServ[i]).getService());
					servCategory.addElement(oc);
					if (fw != null &&  fw.isShownServicePropertiss()) {
						try {
							fw.addServicePropertiesNodes(oc);
						} catch (IAgentException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		
		return slave;
	}

	public String[] getSupportedMimeTypes() {
		return new String[] {MimeTypeContentProvider.MIME_TYPE_BUNDLE};
	}

}

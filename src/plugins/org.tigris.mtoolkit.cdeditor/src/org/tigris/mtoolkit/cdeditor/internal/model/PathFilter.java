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
package org.tigris.mtoolkit.cdeditor.internal.model;

import java.util.Dictionary;
import java.util.Hashtable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.tigris.mtoolkit.cdeditor.internal.CDEditorPlugin;


// TODO: This class handles paths to files only. It should be made more general
public class PathFilter {
	private IPath path;
	private boolean valid;
	private Filter filter = null;
	private Dictionary properties;

	public PathFilter(String path) {
		this(new Path(path));
	}
	
	public PathFilter(IPath path) {
		this.path = path;
		this.valid = this.path.segmentCount() != 0 && !this.path.hasTrailingSeparator();
		if (valid) {
			if (this.path.lastSegment().indexOf('*') != -1) {
				try {
					this.filter = CDEditorPlugin.getBundleContext().createFilter("(filename=" + this.path.lastSegment() + ")");
				} catch (InvalidSyntaxException e) {
				}
			}
		}
	}

	public boolean matchPath(IPath otherPath) {
		if (!valid)
			return false;
		if (path.segmentCount() != otherPath.segmentCount())
			return false;
		if (path.matchingFirstSegments(otherPath) < path.segmentCount() - 1)
			return false;
		if (filter != null) {
			if (properties == null)
				properties = new Hashtable(2);
			properties.put("filename", otherPath.lastSegment());
			return filter.matchCase(properties);
		} else {
			// try to match directly
			return path.lastSegment().equals(otherPath.lastSegment());
		}
	}

	public boolean isValid() {
		return valid;
	}

	public boolean isWildcard() {
		return filter != null;
	}

	public IPath getPath() {
		return path;
	}

	public String toString() {
		return path.toString();
	}

	
}
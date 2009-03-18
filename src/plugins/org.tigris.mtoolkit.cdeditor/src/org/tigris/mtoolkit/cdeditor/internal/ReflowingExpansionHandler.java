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
/**
 * 
 */
package org.tigris.mtoolkit.cdeditor.internal;

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.events.IExpansionListener;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;

/**
 * <p>
 * There is a bug which requires reflow() to be called twice, otherwise the
 * composite doesn't account the newly showed scrollbar and places an ugly
 * horizontal scrollbar when it is not needed.
 * </p>
 * <p>
 * When added as expansion listener to a given section, it will cause additional
 * call to reflow() of the section's container, which will accumodate the showed
 * scrollbar.
 * </p>
 * <p>
 * Eclipse bug: <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=242258">bug 242258</a>
 * </p>
 * 
 */
public class ReflowingExpansionHandler implements IExpansionListener {
	
	public static final IExpansionListener INSTANCE = new ReflowingExpansionHandler();
	
	private ReflowingExpansionHandler() {
	}
	
	public void expansionStateChanged(ExpansionEvent e) {
		Control c = (Control) e.getSource();
		while (c != null) {
			c = c.getParent();
			if (c instanceof SharedScrolledComposite)
				((SharedScrolledComposite) c).reflow(true);
		}
	}

	public void expansionStateChanging(ExpansionEvent e) {
	}
	
}
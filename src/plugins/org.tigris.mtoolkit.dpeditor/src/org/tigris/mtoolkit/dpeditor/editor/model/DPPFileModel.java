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
package org.tigris.mtoolkit.dpeditor.editor.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.swt.widgets.Shell;
import org.tigris.mtoolkit.dpeditor.util.DPPErrorHandler;
import org.tigris.mtoolkit.util.DPPFile;
import org.tigris.mtoolkit.util.DPPUtilities;
import org.tigris.mtoolkit.util.InconsistentDataException;

/**
 * 
 */
public class DPPFileModel {

	/** The instance of Deployment package file object */
	private DPPFile dppFile;
	/** The file from which deployment package file object will be created */
	private IFile file;
	/** The flag which shows if the model is changed */
	protected boolean dirty = false;
	/** The value of last modification of the file */
	private long lastModified;
	/** The <code>Vector</code> of all listeners that will be added */
	private Vector listeners = new Vector();

	/**
	 * Constructor of Deployment package file model, that used <code>null</code>
	 * for the file.
	 */
	public DPPFileModel() {
		this((IFile) null);
	}

	/**
	 * Constructor which sets given <code>IFile</code> and creates from this
	 * file deployment package file object.
	 * 
	 * @param file
	 *            the <code>IFile</code> which be used to create Deployment
	 *            package file object
	 */
	public DPPFileModel(IFile file) {
		this.file = file;
		try {
			dppFile = new DPPFile(file.getLocation().toFile(), file.getProject().getLocation().toOSString());
		} catch (IOException e) {
			DPPErrorHandler.processError(e, "Error while create DPPFile");
		}
	}

	/**
	 * Returns the <code>IFile</code> that used to create
	 * <code>DPPFile<code> object
	 */
	public IFile getFile() {
		return file;
	}

	/**
	 * Sets new <code>IFile</code> and sets this file as a file in the
	 * deployment package file object
	 * 
	 * @param newFile
	 *            the new file which be set
	 */
	public void setFile(IFile newFile) {
		file = newFile;
		dppFile.setFile(file.getLocation().toFile());
	}

	/**
	 * Returns the deployment package file object
	 */
	public DPPFile getDPPFile() {
		return dppFile;
	}

	/**
	 * Writes the contents of the existed
	 * <code>DPPFile<code> to a <code>PrintWriter<code>
	 * 
	 * @param writer
	 *            the <code>PrintWriter</code> to write the file to the contents
	 *            of the <code>DPPFile</code>
	 * @throws IOException
	 *             if there is problem writing
	 * @throws InconsistentDataException
	 *             if the data in the <code>DPPFile</code> object is
	 *             inconsistent
	 */
	public void save(PrintWriter writer) throws IOException, InconsistentDataException {
		try {
			dppFile.write(writer);
		} catch (IOException e) {
			throw e;
		} catch (InconsistentDataException e) {
			throw e;
		}
		dirty = false;
	}

	/**
	 * Returns the active shell
	 */
	public Shell getShell() {
		return DPPErrorHandler.getShell();
	}

	/**
	 * Creates the output stream and saves the data contained in the
	 * <code>DPPFile</code> to the given output stream
	 */
	public String getContents() {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			DPPUtilities.debug("Internal save to source started");
			dppFile.save(bos);
			return bos.toString();
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InconsistentDataException e1) {
			e1.printStackTrace();
		}
		return "";
	}

	/**
	 * Adds the given <code>IModelChangedListener</code> in to the
	 * <code>Vector</code> of all listeners
	 * 
	 * @param listener
	 *            the listener will be added
	 */
	public void addModelChangedListener(IModelChangedListener listener) {
		listeners.add(listener);
	}

	/**
	 * Removes the given <code>IModelChangedListener</code> from the
	 * <code>Vector</code> of all listeners
	 * 
	 * @param listener
	 *            the listener will be removed
	 */
	public void removeModelChangedListener(IModelChangedListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Notifies all existing <code>IModelChangedListener</code>'s of a change of
	 * the model.
	 * 
	 * @param event
	 *            a change event that describes the kind of the model change
	 */
	public void fireModelChanged(IModelChangedEvent event) {
		dirty = true;
		for (Iterator iter = listeners.iterator(); iter.hasNext();) {
			IModelChangedListener listener = (IModelChangedListener) iter.next();
			listener.modelChanged(event);
		}
	}

	/**
	 * Creates the <code>ModelChangedEvent</code> for the given object and
	 * property and notifies all existing <code>IModelChangedListener</code>'s
	 * of a change of the model.
	 * 
	 * @param object
	 *            the changed <code>Object</code>
	 * @param property
	 *            the changed property
	 */
	public void fireModelObjectChanged(Object object, String property) {
		fireModelChanged(new ModelChangedEvent(IModelChangedEvent.CHANGE, new Object[] { object }, property));
	}

	/**
	 * Returns the last modification of the changed file which this model
	 * depends of
	 * 
	 * @return A <code>long</code> value representing the time the file was last
	 *         modified
	 */
	public long getModelModified() {
		return lastModified;
	}

	/**
	 * Returns if the model is changed.
	 * 
	 * @return <code>true</code> if the model is changed, <code>false</code>
	 *         otherwise
	 */
	public boolean isDirty() {
		return dirty;
	}

	/**
	 * Sets the model changed value
	 * 
	 * @param dirty
	 *            <code>true</code> if the model is changed, <code>false</code>
	 *            otherwise
	 */
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

	public void updateLastModified(IFile file) {
		lastModified = file.getLocation().toFile().lastModified();
	}
}

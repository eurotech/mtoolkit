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
package org.tigris.mtoolkit.cdeditor.internal.text;

import org.eclipse.core.filebuffers.IDocumentSetupParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.ui.editors.text.ForwardingDocumentProvider;
import org.eclipse.ui.editors.text.TextFileDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProvider;

public class ComponentDescriptionDocumentProvider extends
		TextFileDocumentProvider {

	public ComponentDescriptionDocumentProvider() {
		/*
		 * dnachev: This piece of code was inspired by JDT source editor The
		 * first TextFileDocumentProvider provides access to the file. It
		 * automatically finds its content, when the IEditorInput is passed to
		 * it. The ForwardingDocumentProvider insures that the document is
		 * partitioned with our partitioner (and all others which are
		 * registered) The setup participant sets up the partitioner
		 */
		IDocumentProvider provider = new TextFileDocumentProvider();
		provider = new ForwardingDocumentProvider(CDPartitionScanner.PARTITIONER_TYPE, new ComponentDescriptionSetupParticipant(), provider);
		setParentDocumentProvider(provider);

	}

	protected FileInfo createFileInfo(Object element) throws CoreException {
		// return always null, so the setup participant will be called
		// otherwise, only the setup participants registered in the registry
		// will be called
		return null;
	}

	protected DocumentProviderOperation createSaveOperation(Object element, IDocument document, boolean overwrite) throws CoreException {
		/*
		 * return null, so the parent provider is asked to save the document
		 * otherwise, the text buffer is directly replaced in the file rather
		 * than committing only the changed parts. This cause the caret to be
		 * moved to the beginning of the file when saving, which is annoying
		 */
		return null;
	}

	public class ComponentDescriptionSetupParticipant implements
			IDocumentSetupParticipant {
		public void setup(IDocument document) {
			FastPartitioner cdPartitioner = new FastPartitioner(new CDPartitionScanner(), CDPartitionScanner.VALID_CONTENT_TYPES);
			if (document instanceof IDocumentExtension3)
				((IDocumentExtension3) document).setDocumentPartitioner(CDPartitionScanner.PARTITIONER_TYPE, cdPartitioner);
			else
				document.setDocumentPartitioner(cdPartitioner);
			cdPartitioner.connect(document);
		}
	}

}

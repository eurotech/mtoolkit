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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;

public class CDSourceViewerConfiguration extends SourceViewerConfiguration {

	private StyleManager styleManager;

	public CDSourceViewerConfiguration(StyleManager styleManager) {
		super();
		this.styleManager = styleManager;
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { CDPartitionScanner.XML_TAG, CDPartitionScanner.XML_PROC_INSTR, CDPartitionScanner.XML_COMMENT, IDocument.DEFAULT_CONTENT_TYPE };
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(CDPartitionScanner.PARTITIONER_TYPE);

		FullPartitionDamager fullDamager = new FullPartitionDamager();

		reconciler.setDamager(fullDamager, CDPartitionScanner.XML_TAG);
		reconciler.setRepairer(new DefaultDamagerRepairer(getXMLTagScanner()), CDPartitionScanner.XML_TAG);

		reconciler.setDamager(fullDamager, CDPartitionScanner.XML_PROC_INSTR);
		reconciler.setRepairer(getPresentationRepairer(StyleManager.TYPE_XML_PROC_INSTR), CDPartitionScanner.XML_PROC_INSTR);

		reconciler.setDamager(fullDamager, CDPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(getPresentationRepairer(StyleManager.TYPE_XML_COMMENT), CDPartitionScanner.XML_COMMENT);

		reconciler.setDamager(fullDamager, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(getPresentationRepairer(StyleManager.TYPE_DEFAULT), IDocument.DEFAULT_CONTENT_TYPE);

		return reconciler;
	}

	private IPresentationRepairer getPresentationRepairer(String style) {
		return new FullPresentationRepairer(styleManager.getStyle(style));
	}

	private ITokenScanner getXMLTagScanner() {
		return new CDXmlTagScanner(styleManager);
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return CDPartitionScanner.PARTITIONER_TYPE;
	}

	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new MarkerAnnotationHover();
	}

}

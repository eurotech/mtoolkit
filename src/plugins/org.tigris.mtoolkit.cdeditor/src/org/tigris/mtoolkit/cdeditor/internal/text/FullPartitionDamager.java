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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPartitioningException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.presentation.IPresentationDamager;

/**
 * This damager repairer calculates damage, so that the damage region doesn't
 * contain parts of a partition. The region borders are aligned with document
 * partitions.
 * 
 */
public class FullPartitionDamager implements IPresentationDamager {

	private IDocument fDocument;

	public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent e, boolean documentPartitioningChanged) {
		if (!documentPartitioningChanged) {
			IDocumentExtension3 document = (IDocumentExtension3) fDocument;
			try {
				int length = e.getText() == null ? 0 : e.getText().length();
				IRegion beginPart = document.getPartition(CDPartitionScanner.PARTITIONER_TYPE, e.getOffset(), false);
				IRegion endPart = document.getPartition(CDPartitionScanner.PARTITIONER_TYPE, e.getOffset() + length, false);
				if (beginPart.equals(endPart))
					return partition;
				int damageStart = beginPart.getOffset();
				return new Region(damageStart, endPart.getOffset() + endPart.getLength() - damageStart);
			} catch (BadLocationException ex) {
			} catch (BadPartitioningException ex) {
			}
		}
		return partition;
	}

	public void setDocument(IDocument document) {
		this.fDocument = document;
	}

}

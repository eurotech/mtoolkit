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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.rules.IPredicateRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.MultiLineRule;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.eclipse.jface.text.rules.Token;

public class CDPartitionScanner extends RuleBasedPartitionScanner {

	public static final String XML_TAG = "__xml_tag";
	public static final String XML_PROC_INSTR = "__xml_proc_instr";
	public static final String XML_COMMENT = "__xml_comment";

	public static final String[] VALID_CONTENT_TYPES = new String[] { XML_TAG, XML_PROC_INSTR, XML_COMMENT };
	public static final String PARTITIONER_TYPE = "org.tigris.mtoolkit.cdeditor.partitioner";

	public CDPartitionScanner() {
		super();

		IToken xmlComment = new Token(XML_COMMENT);
		IToken xmlTag = new Token(XML_TAG);
		IToken xmlProcInstr = new Token(XML_PROC_INSTR);

		List rules = new ArrayList();

		rules.add(new TagRule(xmlTag));
		rules.add(new MultiLineRule("<!--", "-->", xmlComment));
		rules.add(new MultiLineRule("<?", "?>", xmlProcInstr));

		setPredicateRules((IPredicateRule[]) rules.toArray(new IPredicateRule[rules.size()]));
	}

}

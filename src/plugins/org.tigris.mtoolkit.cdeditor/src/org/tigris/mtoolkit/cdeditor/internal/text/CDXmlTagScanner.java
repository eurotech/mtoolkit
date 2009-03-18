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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.rules.Token;

public class CDXmlTagScanner implements ITokenScanner {

	private static final int STATE_BEGINNING = 0;
	private static final int STATE_AFTER_TAGNAME = 1;

	private IDocument document;

	private int rangeEnd;

	private int currentOffset;

	private IToken tagName;
	private IToken tagAttrName;
	private IToken tagAttrValue;
	private IToken defaultToken;

	private int lastTokenOffset = 0;

	private int state = STATE_BEGINNING;

	private char[] delimiters = new char[] { '=', '\'', '"', '<', '>', '!', '?', '/' };

	public CDXmlTagScanner(StyleManager manager) {
		Assert.isNotNull(manager);

		tagName = new Token(manager.getStyle(StyleManager.TYPE_XML_TAG_NAME));
		tagAttrName = new Token(manager.getStyle(StyleManager.TYPE_XML_TAG_ATTR_NAME));
		tagAttrValue = new Token(manager.getStyle(StyleManager.TYPE_XML_TAG_ATTR_VALUE));
		defaultToken = new Token(manager.getStyle(StyleManager.TYPE_DEFAULT));
	}

	public int getTokenLength() {
		if (currentOffset < rangeEnd)
			return currentOffset - getTokenOffset();
		return rangeEnd - getTokenOffset();
	}

	public int getTokenOffset() {
		return lastTokenOffset;
	}

	public IToken nextToken() {
		lastTokenOffset = currentOffset;

		if (state == STATE_BEGINNING) {
			int ch = read();
			if (ch == '<') {
				ch = read();
				if (ch != '/') // if the range doesn't continue with '/',
								// restore the character
					unread();
				return defaultToken;
			} else
				unread();
		}

		int ch = read();
		// check for EOF
		if (ch == -1)
			return Token.EOF;
		else
			unread();

		skipWhitespace(); // always skip whitespace

		ch = read();
		if (ch == '\'' || ch == '"') { // the token seems to be attribute value
			readQuotedString(ch);
			state = STATE_AFTER_TAGNAME; // after we have found string value,
			// there is no chance we are before
			// tagname
			return tagAttrValue;
		}
		if (isWordStart(ch)) {
			readWord();
			switch (state) {
			case STATE_BEGINNING:
				state = STATE_AFTER_TAGNAME;
				return tagName;
			case STATE_AFTER_TAGNAME:
				return tagAttrName;
			}
		}
		return defaultToken;
	}

	private void skipWhitespace() {
		while (Character.isWhitespace((char) read()))
			;
		unread(); // rewind the last non-whitespace character
	}

	private boolean isWordStart(int ch) {
		return !isDelimiter(ch);
	}

	private void readQuotedString(int ch) {
		int next;
		next = read();
		while (next != ch && next != -1) {
			if (next == '\\')
				read(); // skip the next character
			next = read();
		}
	}

	private void readWord() {
		int ch;
		while (!isDelimiter(ch = read()) && ch != -1)
			; // while we don't have whitespace
		unread(); // restore the whitespace character
	}

	private boolean isDelimiter(int ch) {
		if (Character.isWhitespace((char) ch))
			return true;
		for (int i = 0; i < delimiters.length; i++)
			if (delimiters[i] == ch)
				return true;
		return false;
	}

	private int read() {
		try {
			if (currentOffset < rangeEnd) {
				try {
					return document.getChar(currentOffset);
				} catch (BadLocationException e) {
				}
			}
			return -1; // EOF
		} finally {
			currentOffset++; // increase on every read, although it may returns
								// -1
		}
	}

	private void unread() {
		currentOffset--;
	}

	public void setRange(IDocument document, int offset, int length) {
		Assert.isNotNull(document);
		Assert.isTrue(offset > -1);
		Assert.isTrue(length > -1);
		Assert.isTrue(offset + length <= document.getLength());

		this.document = document;
		currentOffset = offset;
		rangeEnd = offset + length;
		state = STATE_BEGINNING;
	}

}

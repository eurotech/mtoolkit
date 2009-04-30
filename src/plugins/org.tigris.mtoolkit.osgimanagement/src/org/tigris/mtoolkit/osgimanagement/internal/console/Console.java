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
package org.tigris.mtoolkit.osgimanagement.internal.console;

import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.StoreConstants;

public class Console extends Composite implements VerifyKeyListener, VerifyListener, MouseListener, MouseMoveListener {

	public static final int COMMAND_MODE = 1;
	public static final int MARK_MODE = 2;
	private int state = COMMAND_MODE;

	private int tagLength = 0;
	private int caretPosition = 0;

	private int bufferIndex = 0;
	private int maxBufferSize = 50;
	private Vector buffer = new Vector();

	private ConsoleManager cManager;

	private VerifyListener vlist;

	private ConsolePopupMenu popupMenu;

	private IPreferenceStore pstore;
	private StyledText styledText;
	String textToAppend = ""; //$NON-NLS-1$

	String textToStore = ""; //$NON-NLS-1$

	private TextViewer viewer;
	private IDocument document;

	public Console(Composite parent, int style, IActionBars aBars) {
		super(parent, SWT.NONE);
		GridLayout compositeLayout = new GridLayout();
		compositeLayout.marginHeight = 0;
		compositeLayout.marginWidth = 0;
		setLayout(compositeLayout);
		setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		viewer = new TextViewer(this, SWT.V_SCROLL + SWT.H_SCROLL + SWT.MULTI);
		if (document == null) {
			document = new Document();
		}
		GridData data = new GridData(GridData.FILL_BOTH);
		viewer.setEditable(false);
		viewer.getControl().setLayoutData(data);
		viewer.setDocument(document);
		viewer.getTextWidget().setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));

		styledText = viewer.getTextWidget();

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				try {
					textToStore = getText();
				} catch (Throwable t) {
				}
			}

		});

		styledText.addVerifyKeyListener(this);
		styledText.addVerifyListener(this);
		vlist = this;
		styledText.addMouseListener(this);
		styledText.addMouseMoveListener(this);
		popupMenu = new ConsolePopupMenu(this, aBars);
		styledText.setMenu(popupMenu.getMenu());

		pstore = FrameworkPlugin.getDefault().getPreferenceStore();
	}

	public String getStoredText() {
		return textToStore;
	}

	public void clearStoredText() {
		textToStore = ""; //$NON-NLS-1$
	}

	public void setConsoleManager(ConsoleManager cm) {
		cManager = cm;
	}

	public void setState(int newState) {
		if (state == newState)
			return;
		if (newState != COMMAND_MODE || newState != MARK_MODE)
			throw new IllegalArgumentException("illegal state: " + newState); //$NON-NLS-1$

		state = newState;
		if (state == COMMAND_MODE) {
			checkCaret();
		}
	}

	public void switchState() {
		switch (state) {
		case COMMAND_MODE:
			state = MARK_MODE;
			break;
		default:
			state = COMMAND_MODE;
			checkCaret();
		}
	}

	public int getState() {
		return state;
	}

	public void clear() {
		setText(""); //$NON-NLS-1$
		if (cManager != null) {
			executeCommand(""); //$NON-NLS-1$
		}
	}

	public boolean hasSelection() {
		return styledText.getSelectionText().length() > 0;
	}

	private void upPressed() {
		if (bufferIndex > 0) {
			String text = getCurrentLineText();
			if (text.length() == 0) {
				bufferIndex--;
				String newText = (String) buffer.elementAt(bufferIndex);
				styledText.insert(newText);
				styledText.setCaretOffset(getText().length());
			} else if (((bufferIndex < buffer.size()) && text.equals(buffer.elementAt(bufferIndex)))
							|| ((bufferIndex == buffer.size()) && text.equals(buffer.elementAt(0)))) {
				bufferIndex--;
				String newText = (String) buffer.elementAt(bufferIndex);
				styledText.replaceTextRange(getStartOfLine(), getCurrentLineText().length(), newText);
				styledText.setCaretOffset(getText().length());
			}
		} else {
			bufferIndex = buffer.size();
			if (bufferIndex > 0) {
				upPressed();
			}
		}
	}

	private void downPressed() {
		if ((bufferIndex > 0) || (buffer.size() > 0)) {
			String text = getCurrentLineText();
			int bufferSize = buffer.size();
			if (text.length() == 0) {
				if ((bufferIndex == bufferSize) || (bufferIndex == bufferSize - 1)) {
					bufferIndex = 0;
				} else {
					bufferIndex++;
				}
				String newText = (String) buffer.elementAt(bufferIndex);
				styledText.insert(newText);
				styledText.setCaretOffset(getText().length());
			} else if ((bufferIndex < bufferSize) && text.equals(buffer.elementAt(bufferIndex))) {
				if (bufferIndex == bufferSize - 1) {
					bufferIndex = 0;
				} else {
					bufferIndex++;
				}
				String newText = (String) buffer.elementAt(bufferIndex);
				styledText.replaceTextRange(getStartOfLine(), getCurrentLineText().length(), newText);
				styledText.setCaretOffset(getText().length());
			}
		}
	}

	private int getPositionOnLine() {
		int lineDelimIndex = getText().lastIndexOf(getLineDelimiter());
		if (lineDelimIndex < 0) {
			return styledText.getCaretOffset();
		}
		String beforeText = getText().substring(0, lineDelimIndex + getLineDelimiter().length());

		return styledText.getCaretOffset() - beforeText.length();
	}

	private String getCurrentLineText() {
		int lineDelimIndex = getText().lastIndexOf(getLineDelimiter());
		if (lineDelimIndex < 0) {
			return getText().substring(tagLength);
		}
		String text = getText().substring(lineDelimIndex + getLineDelimiter().length());
		if (text.length() < tagLength)
			return ""; //$NON-NLS-1$
		return text.substring(tagLength);
	}

	private String getAllCurrentLineText() {
		int lineDelimIndex = getText().lastIndexOf(getLineDelimiter());
		if (lineDelimIndex < 0) {
			return getText();
		}
		String text = getText().substring(lineDelimIndex + getLineDelimiter().length());
		return text;
	}

	private int getStartOfLine() {
		int lineDelimIndex = getText().lastIndexOf(getLineDelimiter());
		if (lineDelimIndex < 0) {
			return tagLength;
		}
		String beforeText = getText().substring(0, lineDelimIndex + getLineDelimiter().length());

		return beforeText.length() + tagLength;
	}

	public void verifyKey(VerifyEvent e) {
		if (state == MARK_MODE || !isEditable()) {
			return;
		}
		char ch = e.character;
		int code = e.keyCode;
		if ((code == SWT.ARROW_LEFT) || (ch == SWT.BS) || (code == SWT.ARROW_UP) || (code == SWT.ARROW_DOWN)) {

			if (((code == SWT.ARROW_LEFT) || (ch == SWT.BS && styledText.getSelectionCount() == 0))
							&& (getPositionOnLine() <= tagLength)) {
				e.doit = false;
			} else if (code == SWT.ARROW_UP) {
				e.doit = false;
				upPressed();
			} else if (code == SWT.ARROW_DOWN) {
				e.doit = false;
				downPressed();
			}
		} else if (ch == SWT.CR) {
			styledText.setCaretOffset(getText().length());
			executeCommand(getCurrentLineText());
		} else {
			switch (code) {
			case SWT.HOME:
				styledText.setCaretOffset(getStartOfLine());
			case SWT.PAGE_UP:
			case SWT.PAGE_DOWN:
				e.doit = false;
			}
		}
		caretPosition = styledText.getCaretOffset();
	}

	public void verifyText(VerifyEvent e) {
		if (state == MARK_MODE) {
			e.doit = false;
			return;
		}
		styledText.removeVerifyListener(this);
		String text = e.text;
		if (text.indexOf(getLineDelimiter()) < 0) {
			e.doit = true;
		} else if (text.endsWith(getLineDelimiter())) {
			e.doit = false;
			styledText.append(text);
			styledText.setCaretOffset(getText().length());
			StringTokenizer sTok = new StringTokenizer(text, getLineDelimiter());
			while (sTok.hasMoreTokens()) {
				executeCommand(sTok.nextToken());
			}
		} else {
			e.doit = false;
			StringTokenizer sTok = new StringTokenizer(text, getLineDelimiter());
			Vector temp = new Vector();
			int num = sTok.countTokens();
			for (int i = 0; i < num - 1; i++) {
				String tok = sTok.nextToken();
				styledText.append(tok + getLineDelimiter());
				temp.addElement(tok);
			}
			styledText.setCaretOffset(getText().length());
			for (int i = 0; i < temp.size(); i++) {
				executeCommand((String) temp.elementAt(i));
			}
			styledText.append(sTok.nextToken());
			styledText.setCaretOffset(getText().length());
		}
		styledText.addVerifyListener(this);
		caretPosition = styledText.getCaretOffset();
	}

	public synchronized void insertReply(final String text) {
		if (!isDisposed()) {
			boolean hasToAddEvent = textToAppend.equals(""); //$NON-NLS-1$
			textToAppend = textToAppend + text;
			if (hasToAddEvent) {
				getDisplay().asyncExec(new Runnable() {
					public void run() {
						String theText = getTextToAppend();
						if (!theText.equals("")) { //$NON-NLS-1$
							appendText(theText);
							tagLength = getAllCurrentLineText().length();
						}
					}
				});
			}
		}
	}

	synchronized String getTextToAppend() {
		String result = textToAppend;
		textToAppend = ""; //$NON-NLS-1$
		return result;
	}

	public void appendText(String text) {
		styledText.removeVerifyListener(vlist);
		styledText.append(text);
		styledText.setCaretOffset(getText().length());
		styledText.getVerticalBar().setSelection(styledText.getVerticalBar().getMaximum());
		styledText.showSelection();
		styledText.addVerifyListener(vlist);
		caretPosition = styledText.getCaretOffset();

		if (pstore.getBoolean(StoreConstants.MAX_NUMBER_OF_ROWS_CHECK_KEY)) {
			int maxLines = pstore.getInt(StoreConstants.MAX_NUMBER_OF_ROWS_KEY);
			if (styledText.getLineCount() > maxLines) {
				int diff = styledText.getLineCount() - maxLines;
				int len = styledText.getOffsetAtLine(diff);
				styledText.replaceTextRange(0, len, ""); //$NON-NLS-1$
			}
		}
	}

	private void addTextToBuffer(String text) {
		if (text.length() > 0) {
			if (bufferIndex >= 0 && bufferIndex < buffer.size() && text.equals(buffer.elementAt(bufferIndex))) {
				bufferIndex++;
			} else if (maxBufferSize > 0) {
				if (buffer.size() == maxBufferSize) {
					buffer.removeElementAt(0);
				}
				buffer.addElement(text);
				bufferIndex = buffer.size();
			}
		} else {
			bufferIndex = buffer.size();
		}
	}

	private void executeCommand(final String s) {
		addTextToBuffer(s);
		new Thread() {
			public void run() {
				cManager.execute(s);
			}
		}.start();
	}

	public void mouseDoubleClick(MouseEvent e) {
	}

	public void mouseUp(MouseEvent e) {
	}

	public void mouseDown(MouseEvent e) {
		checkCaret();
	}

	public void mouseMove(MouseEvent e) {
		checkCaret();
	}

	private void checkCaret() {
		if (state == COMMAND_MODE) {
			if (styledText.getCaretOffset() < getStartOfLine()) {
				styledText.setCaretOffset(caretPosition);
			} else {
				caretPosition = styledText.getCaretOffset();
			}
		}
	}

	public void setEditable(boolean isEditable) {
		styledText.setEditable(isEditable);
	}

	public boolean isEditable() {
		return styledText.getEditable();
	}

	public String getLineDelimiter() {
		return styledText.getLineDelimiter();
	}

	public void setText(String txt) {
		styledText.setText(txt);
	}

	public String getText() {
		return styledText.getText();
	}

	public StyledText getStyledText() {
		return styledText;
	}
}
package org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.MessageBox;
import org.tigris.mtoolkit.osgimanagement.internal.FrameworkPlugin;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.console.Console;

public class SaveAsConsoleAction extends Action {

	private Console console;
	private FileDialog fdialog;
	private MessageBox mbox;

	public SaveAsConsoleAction(String label, Console console) {
		super(label);
		this.console = console;
	}

	public void run() {
		saveasAction();
	}

	void saveasAction() {
		String consoleText = console.getText();
		if (consoleText.trim().length() == 0) {
			return;
		}
		initFileDialog();
		fdialog.setFileName("console.txt"); //$NON-NLS-1$
		fdialog.setFilterPath(FrameworkPlugin.fileDialogLastSelection);
		String file = fdialog.open();
		if (file != null) {
			File theFile = new File(file);
			FrameworkPlugin.fileDialogLastSelection = theFile.getAbsolutePath();
			if (theFile.exists()) {
				initMessageBox();
				if (mbox.open() != SWT.YES)
					return;
			}
			try {
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(consoleText.getBytes());
				fos.close();
			} catch (IOException _) {
			}
		}
	}

	private void initFileDialog() {
		if (fdialog == null) {
			fdialog = new FileDialog(console.getShell(), SWT.SAVE);
			fdialog.setFilterExtensions(new String[] { "*.txt", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
			fdialog.setFilterNames(new String[] { Messages.text_files_filter_label, Messages.all_files_filter_label });
		}
	}

	private void initMessageBox() {
		if (mbox == null) {
			mbox = new MessageBox(console.getShell(), SWT.ICON_QUESTION + SWT.YES | SWT.NO);
			mbox.setMessage(Messages.overwriting_file_confirmation_text);
			mbox.setText(Messages.question_dialog_title);
		}
	}
}

package org.tigris.mtoolkit.osgimanagement.internal.console;

import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.SharedImages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.ClearConsoleAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.PrintConsoleAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.SaveAsConsoleAction;
import org.tigris.mtoolkit.osgimanagement.internal.browser.treeviewer.action.StateConsoleAction;
import org.tigris.mtoolkit.osgimanagement.internal.images.ImageHolder;

public class ConsoleToolbarMenu {

	private ToolBarManager toolBar;
	private StateConsoleAction stateAction;

	public ConsoleToolbarMenu(final Console console, IActionBars aBars) {
		toolBar = (ToolBarManager) aBars.getToolBarManager();

		ClearConsoleAction clearAction = new ClearConsoleAction("Clear console", console);
		clearAction.setImageDescriptor(ImageHolder.getImageDescriptor("clear_action.gif"));
		toolBar.add(clearAction);
		toolBar.add(new Separator());

		SaveAsConsoleAction saveAction = new SaveAsConsoleAction("Save as", console);
		saveAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(SharedImages.IMG_ETOOL_SAVEAS_EDIT));
		toolBar.add(saveAction);

		PrintConsoleAction printAction = new PrintConsoleAction("Print", console);
		printAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(SharedImages.IMG_ETOOL_PRINT_EDIT));
		toolBar.add(printAction);
		toolBar.add(new Separator());

		stateAction = new StateConsoleAction("Mark Type", console);
		stateAction.setChecked(console.getState() == Console.COMMAND_MODE);

		toolBar.add(stateAction);
	}

	public void updateCheck(int state) {
		stateAction.setChecked(state == Console.COMMAND_MODE);
	}

}

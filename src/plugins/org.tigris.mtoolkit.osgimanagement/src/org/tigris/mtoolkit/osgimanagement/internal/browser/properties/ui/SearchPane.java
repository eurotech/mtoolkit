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
package org.tigris.mtoolkit.osgimanagement.internal.browser.properties.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.tigris.mtoolkit.osgimanagement.internal.FrameWorkView;
import org.tigris.mtoolkit.osgimanagement.internal.Messages;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.Model;
import org.tigris.mtoolkit.osgimanagement.internal.browser.model.ServicesCategory;


public class SearchPane extends Composite implements SelectionListener, ModifyListener, KeyListener, PaintListener, MouseTrackListener, MouseListener, MouseMoveListener {

  private Text findText;
  private Button findButton;
  private Composite closeButton;
  private TreeViewer parentView;
  private String text = ""; //$NON-NLS-1$
  private Color red;
  private Color black;
  
  private final int NORMAL = 0;
  private final int HOT = 1;
  private final int SELECTED = 2;
  private int closeImageState = 0;
  boolean in = false;
  boolean down = false;


  public SearchPane(Composite parent, int style, TreeViewer parentView) {
    super(parent, style);
    this.parentView = parentView;

    setLayout(new GridLayout());
    setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    Group group = new Group(this, SWT.NONE);

    group.setText(Messages.find_text);
    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    group.setLayout(new GridLayout(3, false));
    GridLayout connectPropertiesGrid = new GridLayout();
    connectPropertiesGrid.numColumns = 3;
    connectPropertiesGrid.makeColumnsEqualWidth = false;
    group.setLayout(connectPropertiesGrid);
    
    closeButton = new Composite(group, SWT.NONE);
    GridData gridDataCloseButton = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
    closeButton.setLayoutData(gridDataCloseButton);
    gridDataCloseButton.widthHint = gridDataCloseButton.heightHint = 15;
    
    findText = new Text(group, SWT.SINGLE | SWT.BORDER);
    GridData gridDataFindText = new GridData(GridData.FILL_HORIZONTAL);
    findText.setLayoutData(gridDataFindText);

    
    GC gc = new GC(parent);
    gc.setFont(parent.getFont());
    FontMetrics fontMetrics = gc.getFontMetrics();
    gc.dispose();

    findButton = new Button(group, SWT.PUSH);
    GridData gridDataFindButton = new GridData(GridData.HORIZONTAL_ALIGN_END);

    findButton.setText(Messages.find_button_label);
    findButton.setLayoutData(gridDataFindButton);
    gridDataFindButton.widthHint = Dialog.convertHorizontalDLUsToPixels(fontMetrics, IDialogConstants.BUTTON_WIDTH);

    getShell().setDefaultButton(findButton);
    
    findText.setText(text);
    findText.setSelection(0, text.length());
    findText.addModifyListener(this);
    
    red = new Color(getShell().getDisplay(), 255, 0, 0);
    black = new Color(getShell().getDisplay(), 0, 0, 0);
    
    findButton.addSelectionListener(this);
    addKeyListener(this);
    findButton.addKeyListener(this);
    findText.addKeyListener(this);
    closeButton.addKeyListener(this);
    closeButton.addMouseTrackListener(this);
    closeButton.addMouseListener(this);
    closeButton.addMouseMoveListener(this);
    closeButton.setDragDetect(true);
    closeButton.addPaintListener(this);
    hide();
  }
  
  
  public void widgetDefaultSelected(SelectionEvent e) {
  }

  public void widgetSelected(SelectionEvent e) {
    if (e.getSource() == closeButton) {
      hide();
    }
    if (e.getSource() == findButton) {
      findItem();
    }
  }
  
  public void show() {
    setVisible(true);
    ((GridData)getLayoutData()).exclude = false;
    getParent().layout(true);
    findText.forceFocus();
    findText.selectAll();
    closeImageState = NORMAL;
  }
  
  public void hide() {
    setVisible(false);
    ((GridData)getLayoutData()).exclude = true;
    getParent().layout(true);
    closeImageState = NORMAL;
  }

  protected void findItem() {
    text = findText.getText();
    if (text.equals("")) return; //$NON-NLS-1$
    IStructuredSelection startSelection = (IStructuredSelection) parentView.getSelection();
    
    Model startNode = (Model) startSelection.getFirstElement();
    if (startNode == null) {
      Model children[] = FrameWorkView.treeRoot.getChildren();
      if (children == null || children.length == 0) return;
      startNode = children[0];
    }
    
    Model foundNode = null;
    Model node = startNode;
    
    if ((foundNode = findItem(node, text, startNode)) == null) {
      Model parent = node.getParent();
      while (parent != null) {
        int startIndex = parent.indexOf(node) + 1;
        for (int i=startIndex; i<parent.getSize(); i++) {
          node = parent.getChildren()[i];
          if (isTextFound(node.getName(), text)) {
            foundNode = node;
            break;
          }
          foundNode =  findItem(node, text, startNode);
          if (foundNode != null) break;
        }
        if (foundNode != null) break;
        node = parent;
        parent = parent.getParent();
      }
    }
    if (foundNode == null && startNode != FrameWorkView.treeRoot.getChildren()[0]) {
      node = FrameWorkView.treeRoot;
      foundNode = findItem(node, text, startNode);
    }
    
    if (foundNode == startNode) {
      if (foundNode.getName().indexOf(text) == -1) {
        findText.setForeground(red);
      }
    } else if (foundNode != null) {
    	parentView.setSelection(new StructuredSelection(foundNode));
    	findText.setForeground(black);
    } else {
      findText.setForeground(red);
    }
  }
  
  
  public Model findItem(Model parent, String searching, Model startNode) {
    Model children[] = parent.getChildren();
    for (int i=0; i<children.length; i++) {
      Model child = children[i]; 
      if (child == startNode) {
        return child;
      }
      String text = child.getName();
      if (isTextFound(text, searching)) {
        return child;
      }
      Model grandChild = findItem(child, searching, startNode);
      if (grandChild != null) {
        return grandChild;
      }
    }
    return null;
  }
  
  private boolean isTextFound(String text, String searchFor) {
    return (text.indexOf(searchFor) != -1 && !text.equals(ServicesCategory.nodes[0]) && !text.equals(ServicesCategory.nodes[1]));
  }

  public void modifyText(ModifyEvent e) {
    String text = findText.getText();
    TreeItem treeItems[] = parentView.getTree().getSelection();
    if (treeItems != null && treeItems.length > 0) {
      if (treeItems[0].getText().indexOf(text) != -1) {
        findText.setForeground(black);
        return;
      }
    }
    findItem();
  }

  public void keyPressed(KeyEvent e) {
    if (e.character == SWT.ESC) {
      hide();
    } else if (e.character == SWT.KEYPAD_CR || e.character == 13) {
      findItem();
    }
  }

  public void keyReleased(KeyEvent e) {
  }

  public void paintControl(PaintEvent e) {
    GC gc = e.gc;
    int x = 2, y = 2;

    Color closeBorder = getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
    Color normalFill = getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
    RGB closeFill = new RGB(252, 160, 160);
    switch (closeImageState) {
      case NORMAL: {
        int[] shape = new int[] {x,y, x+2,y, x+4,y+2, x+5,y+2, x+7,y, x+9,y, 
            x+9,y+2, x+7,y+4, x+7,y+5, x+9,y+7, x+9,y+9,
            x+7,y+9, x+5,y+7, x+4,y+7, x+2,y+9, x,y+9,
            x,y+7, x+2,y+5, x+2,y+4, x,y+2};
        gc.setBackground(normalFill);
        gc.fillPolygon(shape);
        gc.setForeground(closeBorder);
        gc.drawPolygon(shape);
        break;
      }
      case HOT: {
        int[] shape = new int[] {x,y, x+2,y, x+4,y+2, x+5,y+2, x+7,y, x+9,y, 
            x+9,y+2, x+7,y+4, x+7,y+5, x+9,y+7, x+9,y+9,
            x+7,y+9, x+5,y+7, x+4,y+7, x+2,y+9, x,y+9,
            x,y+7, x+2,y+5, x+2,y+4, x,y+2};
        Color fill = new Color(getDisplay(), closeFill);
        gc.setBackground(fill);
        gc.fillPolygon(shape);
        fill.dispose();
        gc.setForeground(closeBorder);
        gc.drawPolygon(shape);
        break;
      }
      case SELECTED: {
        int[] shape = new int[] {x+1,y+1, x+3,y+1, x+5,y+3, x+6,y+3, x+8,y+1, x+10,y+1, 
            x+10,y+3, x+8,y+5, x+8,y+6, x+10,y+8, x+10,y+10,
            x+8,y+10, x+6,y+8, x+5,y+8, x+3,y+10, x+1,y+10,
            x+1,y+8, x+3,y+6, x+3,y+5, x+1,y+3};
        Color fill = new Color(getDisplay(), closeFill);
        gc.setBackground(fill);
        gc.fillPolygon(shape);
        fill.dispose();
        gc.setForeground(closeBorder);
        gc.drawPolygon(shape);
        break;
      }
    }
  }

  public void mouseEnter(MouseEvent e) {
    in = true;
    closeImageState = HOT;
    closeButton.redraw();
  }

  public void mouseExit(MouseEvent e) {
    in = false;
    down = false;
    closeImageState = NORMAL;
    closeButton.redraw();
  }

  public void mouseHover(MouseEvent e) {
  }

  public void mouseDoubleClick(MouseEvent e) {
  }

  public void mouseDown(MouseEvent e) {
    down = true;
    if (in) {
      closeImageState = SELECTED;
      closeButton.redraw();
    }
  }

  public void mouseUp(MouseEvent e) {
    if (!down) {
      down = false;
      closeImageState = NORMAL;
      closeButton.redraw();
    } else {
      down = false;
      hide();
    }
  }

  public void mouseMove(MouseEvent e) {
    if(!inside(e) && closeImageState != NORMAL) {
      in = false;
      down = false;
      closeImageState = NORMAL;
      closeButton.redraw();
    } else if (inside(e) && closeImageState != HOT && !down) {
      in = true;
      closeImageState = HOT;
      closeButton.redraw();
    }
  }
  
  // check if mouse event coordinates are inside the "close button"
  private boolean inside(MouseEvent e) {
    if (e.x > 0 && e.y > 0 && e.x < 15 && e.y < 15) return true;
    return false;
  }

}

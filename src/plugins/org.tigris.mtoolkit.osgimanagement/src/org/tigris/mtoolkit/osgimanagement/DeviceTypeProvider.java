package org.tigris.mtoolkit.osgimanagement;

import java.util.Dictionary;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IMemento;

public interface DeviceTypeProvider {

	public Control createPanel(Composite parent);
	
	public void setProperties(IMemento config);

	public boolean validate();
	
	public String getTransportID();
	
	public Dictionary load(IMemento config);

	public void save(IMemento config);

	public String getTransportType();
	
	public void setEditable(boolean editable);
}

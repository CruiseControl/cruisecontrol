/*
 * Created on Dec 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import net.sourceforge.cruisecontrol.gui.ProjectBrowser;

import org.jdom.Element;

/**
 * @author windows
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface EditorPanel {

	public void setElement( Element anElement );
	
	public void setProjectBrowser( ProjectBrowser aProjectBrowser );
}

/*
 * Created on Jan 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import net.sourceforge.cruisecontrol.gui.ProjectBrowser;

import org.arch4j.ui.components.PropertiesPanel;
import org.jdom.Element;

/**
 * @author alwick
 */
public class BaseElementPanel  extends PropertiesPanel implements EditorPanel {
	
	private Element element;
	private ProjectBrowser browser;
	
	/* (non-Javadoc)
	 * @see net.sourceforge.cruisecontrol.gui.panels.EditorPanel#setElement(org.jdom.Element)
	 */
	public void setElement(Element anElement) {
		
		element = anElement;
	}
	
	/* (non-Javadoc)
	 * @see net.sourceforge.cruisecontrol.gui.panels.EditorPanel#setProjectBrowser(net.sourceforge.cruisecontrol.gui.ProjectBrowser)
	 */
	public void setProjectBrowser(ProjectBrowser aProjectBrowser) {
		
		browser = aProjectBrowser;
	}
	
	/* (non-Javadoc)
	 * @see org.arch4j.ui.components.PropertiesPanel#addComponents()
	 */
	public void addComponents() {
		
	}
	/**
	 * @return Returns the browser.
	 */
	public ProjectBrowser getBrowser() {
		return browser;
	}
	/**
	 * @param browser The browser to set.
	 */
	public void setBrowser(ProjectBrowser browser) {
		this.browser = browser;
	}
	/**
	 * @return Returns the element.
	 */
	public Element getElement() {
		return element;
	}
	
	public boolean getBooleanAttributeValue( Element anElement, String anAttributeName ) {
		
		String theElementValue = anElement.getAttributeValue( anAttributeName );
		
		boolean theValue = true;
		if ( theElementValue != null ) {
			theValue = Boolean.valueOf( theElementValue ).booleanValue();
		}
		
		return theValue;
	}
}

/*
 * Created on Dec 30, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

import net.sourceforge.cruisecontrol.gui.ProjectBrowser;

import org.arch4j.ui.components.PropertiesPanel;
import org.jdom.Element;

/**
 * This panel is used to edit the attributs on the Threads element.
 * 
 * @author Allan Wick
 */
public class ThreadsPanel extends PropertiesPanel implements EditorPanel {

	private Element threadsElement;
	
	private JTextField countField;
	private ProjectBrowser browser;
	
	public void addComponents() {
		
		super.addComponents();
		
		countField = addTextField( "Count", null);
		countField.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						threadsElement.setAttribute( "count", 
								                     countField.getText() );						
					}
				});
	}
	
	/**
	 * Set the owning browser class
	 */
	public void setProjectBrowser( ProjectBrowser aBrowser ) {
		
		browser = aBrowser;
	}
	public void setElement( Element anElement ) {
		
		threadsElement = anElement;
		
		countField.setText( anElement.getAttributeValue("threads") );
	}
	
	/**
	 * The panel title to display above the fields.
	 */
	public String getTitle() {
		
		return "Threads Element";
	}
}

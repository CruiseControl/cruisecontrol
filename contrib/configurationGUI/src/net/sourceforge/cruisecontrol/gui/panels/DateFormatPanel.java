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
 * This panel is used to edit the attributs on the DateFormat element.
 * 
 * @author Allan Wick
 */
public class DateFormatPanel extends PropertiesPanel implements EditorPanel {

	private Element dateFormatElement;
	
	private JTextField dateFormat;
	private ProjectBrowser browser;
	
	public void addComponents() {
		
		super.addComponents();
		
		dateFormat = addTextField( "Format", null);
		dateFormat.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						dateFormatElement.setAttribute( "format", 
								                        dateFormat.getText() );						
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
		
		dateFormatElement = anElement;
		
		dateFormat.setText( anElement.getAttributeValue("format") );
	}
	
	/**
	 * The panel title to display above the fields.
	 */
	public String getTitle() {
		
		return "Date Format Element";
	}
}

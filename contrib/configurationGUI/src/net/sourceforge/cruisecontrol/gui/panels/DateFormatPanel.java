/*
 * Created on Dec 30, 2004
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

import org.jdom.Element;

/**
 * This panel is used to edit the attributs on the DateFormat element.
 * 
 * @author Allan Wick
 */
public class DateFormatPanel extends BaseElementPanel implements EditorPanel {
	
	private JTextField dateFormat;
	
	public void addComponents() {
		
		super.addComponents();
		
		dateFormat = addTextField( "Format", null);
		dateFormat.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "format", 
						              dateFormat.getText() );						
					}
				});
	}

	public void setElement( Element anElement ) {
		
		super.setElement( anElement );
		
		dateFormat.setText( anElement.getAttributeValue("format") );
	}
	
	/**
	 * The panel title to display above the fields.
	 */
	public String getTitle() {
		
		return "Date Format Element";
	}
}

/*
 * Created on Dec 30, 2004
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

import org.jdom.Element;

/**
 * This panel is used to edit the attributs on the Threads element.
 * 
 * @author Allan Wick
 */
public class ThreadsPanel extends BaseElementPanel implements EditorPanel {
	
	private JTextField countField;
	
	public void addComponents() {
		
		super.addComponents();
		
		countField = addTextField( "Count", null);
		countField.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "count", 
						              countField.getText() );						
					}
				});
	}

	public void setElement( Element anElement ) {
		
		super.setElement( anElement );
		
		countField.setText( anElement.getAttributeValue("threads") );
	}
	
	/**
	 * The panel title to display above the fields.
	 */
	public String getTitle() {
		
		return "Threads Element";
	}
}

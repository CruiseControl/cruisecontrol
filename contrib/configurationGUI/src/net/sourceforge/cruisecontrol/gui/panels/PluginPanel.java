/*
 * Created on Dec 7, 2004
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdom.Element;

/**
 * @author Allan Wick
 */
public class PluginPanel extends BaseElementPanel implements EditorPanel {

	private JTextField pluginNameText;
	private JTextField classnameText;
	
	/* (non-Javadoc)
	 * @see org.arch4j.ui.components.PropertiesPanel#addComponents()
	 */
	public void addComponents() {
		super.addComponents();
		
		JLabel theLabel = new JLabel( "Name" );
		pluginNameText = addTextField( theLabel, null );
		
		pluginNameText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "name", pluginNameText.getText() );
						
						getBrowser().updateNodeText( pluginNameText.getText() );
					}
				});
		
		theLabel = new JLabel( "Classname" );
		
		classnameText = addTextField( theLabel, null );
		
		classnameText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "classname", classnameText.getText() );
					}
				});
	}
	
	public void setElement( Element anElement ) {
		
		super.setElement( anElement );
		
		pluginNameText.setText( anElement.getAttributeValue("name") );
	}
	
	/* (non-Javadoc)
	 * @see org.arch4j.ui.components.PropertiesPanel#getTitle()
	 */
	public String getTitle() {
		
		return "Plugin Element";
	}
}

/*
 * Created on Dec 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sourceforge.cruisecontrol.gui.ProjectBrowser;

import org.arch4j.ui.components.PropertiesPanel;
import org.jdom.Element;

/**
 * @author Allan Wick
 */
public class PluginPanel extends PropertiesPanel implements EditorPanel {

	private Element element;
	private JTextField pluginNameText;
	private JTextField classnameText;
	private ProjectBrowser browser;
	
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
						element.setAttribute( "name", pluginNameText.getText() );
						
						browser.updateNodeText( pluginNameText.getText() );
					}
				});
		
		theLabel = new JLabel( "Classname" );
		
		classnameText = addTextField( theLabel, null );
		
		classnameText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						element.setAttribute( "classname", classnameText.getText() );
					}
				});
	}
	
	public void setProjectBrowser( ProjectBrowser aBrowser ) {
		
		browser = aBrowser;
	}
	
	public void setElement( Element anElement ) {
		
		element = anElement;
		
		pluginNameText.setText( anElement.getAttributeValue("name") );
	}
	
	/* (non-Javadoc)
	 * @see org.arch4j.ui.components.PropertiesPanel#getTitle()
	 */
	public String getTitle() {
		
		return "Plugin Element";
	}
}

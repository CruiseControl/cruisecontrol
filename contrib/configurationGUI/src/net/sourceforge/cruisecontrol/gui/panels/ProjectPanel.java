/*
 * Created on Dec 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sourceforge.cruisecontrol.gui.ProjectBrowser;

import org.arch4j.ui.components.PropertiesPanel;
import org.jdom.Element;

/**
 * This panel is used to edit the Project element properties in the cruisecontrol
 * configuration file.
 * 
 * @author Allan Wick
 */
public class ProjectPanel extends PropertiesPanel implements EditorPanel {

	private Element element;
	private JTextField projectText;
	private JCheckBox buildAfterFailedBox;
	private ProjectBrowser browser;
	
	/**
	 * Add all the widgets to the panel
	 */
	public void addComponents() {
		super.addComponents();
		
		JLabel theLabel = new JLabel( "Project Name" );
		projectText = addTextField( theLabel, null );
		
		projectText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						element.setAttribute( "name", projectText.getText() );
						
						browser.updateNodeText( projectText.getText() );
					}
				});
		
		theLabel = new JLabel( "Build After Failed" );
		
		buildAfterFailedBox = addCheckBox( theLabel );
		buildAfterFailedBox.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						element.setAttribute( "buildAfterFailed", 
								              String.valueOf( buildAfterFailedBox.isSelected() ) );						
					}
				});

		JButton theButton = new JButton( "New Listener...");
		theButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent anEvent ) {
				
				browser.addListenerType();
			}
		});
		
		addComponent( theButton, 0, 150 );
		
		theButton = new JButton( "New Bootstrapper...");
		theButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent anEvent ) {
				
				browser.addBootstrapper();
			}
		});
		
		addComponent( theButton, 5, 150 );
		
		theButton = new JButton( "New Modification Set Type...");
		theButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent anEvent ) {
				
				browser.addModificationSetType();
			}
		});
		
		addComponent( theButton, 5, 200 );
		
		theButton = new JButton( "New Publisher...");
		theButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent anEvent ) {
				
				browser.addPublisherType();
			}
		});
		
		addComponent( theButton, 5, 150 );
	}
	
	public void setProjectBrowser( ProjectBrowser aBrowser ) {
		
		browser = aBrowser;
	}
	
	public void setElement( Element anElement ) {
		
		element = anElement;
		
		projectText.setText( anElement.getAttributeValue("name") );
		String theBuildAfterFailed = anElement.getAttributeValue( "buildAfterFailed" );
		
		boolean theValue = true;
		if ( theBuildAfterFailed != null ) {
			theValue = Boolean.valueOf( theBuildAfterFailed ).booleanValue();
		}
		
		buildAfterFailedBox.setSelected( theValue );
	}
	
	/**
	 * The panel title to display above the fields.
	 */
	public String getTitle() {
		
		return "Project Element";
	}
}

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

import org.jdom.Element;

/**
 * This panel is used to edit the Project element properties in the cruisecontrol
 * configuration file.
 * 
 * @author Allan Wick
 */
public class ProjectPanel extends BaseElementPanel implements EditorPanel {

	private JTextField projectText;
	private JCheckBox buildAfterFailedBox;
	
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
						setAttribute( "name", projectText.getText() );
						
						getBrowser().updateNodeText( projectText.getText() );
					}
				});
		
		theLabel = new JLabel( "Build After Failed" );
		
		buildAfterFailedBox = addCheckBox( theLabel );
		buildAfterFailedBox.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "buildAfterFailed", 
						              buildAfterFailedBox.isSelected() );						
					}
				});

		JButton theButton = new JButton( "New Listener...");
		theButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent anEvent ) {
				
				getBrowser().addListenerType();
			}
		});
		
		addComponent( theButton, 0, 150 );
		
		theButton = new JButton( "New Bootstrapper...");
		theButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent anEvent ) {
				
				getBrowser().addBootstrapper();
			}
		});
		
		addComponent( theButton, 5, 150 );
		
		theButton = new JButton( "New Modification Set Type...");
		theButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent anEvent ) {
				
				getBrowser().addModificationSetType();
			}
		});
		
		addComponent( theButton, 5, 200 );
		
		theButton = new JButton( "New Publisher...");
		theButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent anEvent ) {
				
				getBrowser().addPublisherType();
			}
		});
		
		addComponent( theButton, 5, 150 );
	}
	
	public void setElement( Element anElement ) {
		
		super.setElement( anElement );
		
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

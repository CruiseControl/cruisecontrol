/*
 * Created on Jan 22, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdom.Element;

/**
 * @author alwick
 */
public class MavenSnapshotDependencyPanel extends BaseElementPanel {

	private JTextField projectFileText;
	private JTextField userText;
	private JTextField localRepositoryText;
	private JTextField propertyText;
	
	/* (non-Javadoc)
	 * @see org.arch4j.ui.components.PropertiesPanel#addComponents()
	 */
	public void addComponents() {

		super.addComponents();
		
		JLabel theLabel = new JLabel( "POM File" );
		projectFileText = addTextField( theLabel, null );
		
		projectFileText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "projectFile", projectFileText.getText() );
					}
				});
		
		theLabel = new JLabel( "User" );
		userText = addTextField( theLabel, null );
		
		userText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "user", userText.getText() );
					}
				});
		
		theLabel = new JLabel( "Local Repository" );
		localRepositoryText = addTextField( theLabel, null );
		
		localRepositoryText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "localRepository", localRepositoryText.getText() );
					}
				});
		
		theLabel = new JLabel( "Property" );
		projectFileText = addTextField( theLabel, null );
		
		projectFileText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "property", projectFileText.getText() );
					}
				});		
	}
	
	/* (non-Javadoc)
	 * @see net.sourceforge.cruisecontrol.gui.panels.EditorPanel#setElement(org.jdom.Element)
	 */
	public void setElement(Element anElement) {
		
		super.setElement(anElement);
		
		projectFileText.setText( anElement.getAttributeValue( "projectFile" ));
		userText.setText( anElement.getAttributeValue( "user" ));
		localRepositoryText.setText( anElement.getAttributeValue( "localRepository" ));
		//propertyText.setText( anElement.getAttributeValue( "property" ));
		
	}
}

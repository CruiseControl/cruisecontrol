/*
 * Created on Jan 22, 2005
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdom.Element;

/**
 * @author alwick
 */
public class SametimeAnnouncementPublisherPanel extends BaseElementPanel {

	private JTextField buildResultsUrlText;
	private JTextField hostText;
	private JTextField usernameText;
	private JTextField passwordText;
	private JTextField communityText;
	private JCheckBox resolveUsersBox;
	private JCheckBox resolveGroupsBox;
	private JCheckBox useGroupContentBox;
	private JComboBox handleResolveConflicsBox;
	private JComboBox handleResolveFailsBox;
	private JComboBox handleQueryGroupContentFailsBox;
	
	public void addComponents() {

		super.addComponents();
		
		JLabel theLabel = new JLabel( "Build Results URL" );
		buildResultsUrlText = addTextField( theLabel, null );
		
		buildResultsUrlText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "buildresultsurl", buildResultsUrlText.getText() );
					}
				});
		
		theLabel = new JLabel( "Host" );
		hostText = addTextField( theLabel, null );
		
		hostText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "host", hostText.getText() );
					}
				});
		
		theLabel = new JLabel( "Username" );
		usernameText = addTextField( theLabel, null );
		
		usernameText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "username", usernameText.getText() );
					}
				});
		
		theLabel = new JLabel( "Password" );
		passwordText = addTextField( theLabel, null );
		
		passwordText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "password", passwordText.getText() );
					}
				});
		
		theLabel = new JLabel( "Community" );
		communityText = addTextField( theLabel, null );
		
		communityText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "community", communityText.getText() );
					}
				});
		
		theLabel = new JLabel( "Resolve Users" );
		
		resolveUsersBox = addCheckBox( theLabel );
		resolveUsersBox.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "resolveusers", 
								      resolveUsersBox.isSelected() );						
					}
				});
		
		theLabel = new JLabel( "Resolve Groups" );
		
		resolveGroupsBox = addCheckBox( theLabel );
		resolveGroupsBox.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "resovegroups", 
							          resolveGroupsBox.isSelected() );						
					}
				});
		
		theLabel = new JLabel( "Use Group Content" );
		
		useGroupContentBox = addCheckBox( theLabel );
		useGroupContentBox.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "usegroupcontent", 
						              useGroupContentBox.isSelected() );						
					}
				});	
		
		theLabel = new JLabel( "Handle Resolve Conflicts" );
		
		handleResolveConflicsBox = addComboBox( theLabel, 
				                                getHandleResolveConflictsValues(),
												"recipient" );
		
		handleResolveConflicsBox.addFocusListener (
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "handleresolveconflicts", 
								      (String) handleResolveConflicsBox.getSelectedItem() );						
					}
				});
		
		theLabel = new JLabel( "Handle Resolve Fails" );
		
		handleResolveFailsBox = addComboBox( theLabel,
				                             getHandleResolveFailsValues(),
											 "error" );
		
		handleResolveFailsBox.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "handleresolvefails", 
						              (String) handleResolveFailsBox.getSelectedItem() );						
					}
				});
		
		theLabel = new JLabel( "Handle Qry Group Content Fails" );
		
		handleQueryGroupContentFailsBox = addComboBox( theLabel,
				                                       getHandleQueryGroupContentFailsValues(),
													   "error" );
		handleQueryGroupContentFailsBox.addFocusListener(
				new FocusAdapter() {
					
					public void focusLost(FocusEvent anEvent ) {
						
						setAttribute( "handlequerygroupcontentfails", 
						             (String) handleQueryGroupContentFailsBox.getSelectedItem() );						
					}
				});
		
	}

	public void setElement(Element anElement) {

		super.setElement(anElement);
		
		buildResultsUrlText.setText( anElement.getAttributeValue( "buildresultsurl" ));
		hostText.setText( anElement.getAttributeValue( "host" ));
		usernameText.setText( anElement.getAttributeValue( "username" ));
		passwordText.setText( anElement.getAttributeValue( "password" ));
		communityText.setText( anElement.getAttributeValue( "community" ));
		resolveUsersBox.setSelected( getBooleanAttributeValue( anElement, "resolveusers" ));
		resolveGroupsBox.setSelected( getBooleanAttributeValue( anElement, "resolvegroups" ));
		useGroupContentBox.setSelected( getBooleanAttributeValue( anElement, "usegroupcontent" ));
		handleResolveConflicsBox.setSelectedItem( anElement.getAttributeValue( "handleresolveconflicts" ));
		handleResolveFailsBox.setSelectedItem( anElement.getAttributeValue("handleresolvefailures" ));
		handleQueryGroupContentFailsBox.setSelectedItem( anElement.getAttributeValue( "handlequerygroupcontentfails" ));
	}
	
	private Vector getHandleResolveConflictsValues() {
		
		Vector theValues = new Vector();
		
		theValues.add( "error" );
		theValues.add( "warn" );
		theValues.add( "ignore" );
		theValues.add( "recipient" );
		
		return theValues;
	}
	
	private Vector getHandleResolveFailsValues() {
		
		Vector theValues = new Vector();
		
		theValues.add( "error" );
		theValues.add( "warn" );
		theValues.add( "ignore" );
		
		return theValues;
	}
	
	private Vector getHandleQueryGroupContentFailsValues() {
		
		Vector theValues = new Vector();
		
		theValues.add( "error" );
		theValues.add( "warn" );
		theValues.add( "ignore" );
		
		return theValues;
	}
}

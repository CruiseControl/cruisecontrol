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
public class FtpCommonPanel extends BaseElementPanel  {

	private JTextField targetHostText;
	private JTextField targetUserText;
	private JTextField targetPasswdText;
	private JTextField targetPortText;
	private JTextField targetDirText;
	private JTextField targetSeparatorText;
	
	public void addComponents() {
		
		super.addComponents();
		
		JLabel theLabel = new JLabel( "Target Host" );
		
		targetHostText = addTextField( theLabel, null );
		
		targetHostText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "targetHost", targetHostText.getText() );
						
						getBrowser().updateNodeText( targetHostText.getText() );
					}
				});

		theLabel = new JLabel( "Target User" );
		
		targetUserText = addTextField( theLabel, null );
		
		targetUserText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "targetUser", targetUserText.getText() );
					}
				});
		
		theLabel = new JLabel( "Target Password" );
		
		targetPasswdText = addTextField( theLabel, null );
		
		targetPasswdText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "targetPasswd", targetPasswdText.getText() );
					}
				});
		
		theLabel = new JLabel( "Target Port" );
		
		targetPortText = addTextField( theLabel, null );
		
		targetPortText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "targetPort", targetPortText.getText() );
					}
				});
		
		theLabel = new JLabel( "Target Dir" );
		
		targetDirText = addTextField( theLabel, null );
		
		targetDirText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "targetDir", targetDirText.getText() );
					}
				});
		
		theLabel = new JLabel( "Target Separator" );
		
		targetSeparatorText = addTextField( theLabel, null );
		
		targetSeparatorText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						setAttribute( "targetSeparator", targetSeparatorText.getText() );
					}
				});
		
	}
	
	
	public void setElement( Element anElement ) {
		
		super.setElement( anElement );
		
		targetHostText.setText( anElement.getAttributeValue( "targetHost" ));
		targetPortText.setText( anElement.getAttributeValue( "targetPort" ));
		targetUserText.setText( anElement.getAttributeValue( "targetUser" ));
		targetPasswdText.setText( anElement.getAttributeValue( "targetPasswd" ));
		targetDirText.setText( anElement.getAttributeValue( "targetDir" ));
		targetSeparatorText.setText( anElement.getAttributeValue( "targetSeparator" ));
	}
}

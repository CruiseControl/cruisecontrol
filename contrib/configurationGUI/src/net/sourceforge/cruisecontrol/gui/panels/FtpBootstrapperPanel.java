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
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class FtpBootstrapperPanel extends FtpCommonPanel {

	private JTextField fileText;
	private JTextField destDirText;
	
	/* (non-Javadoc)
	 * @see org.arch4j.ui.components.PropertiesPanel#addComponents()
	 */
	public void addComponents() {

		super.addComponents();
		
		JLabel theLabel = new JLabel( "File" );
		fileText = addTextField( theLabel, null );
		
		fileText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						getElement().setAttribute( "file", fileText.getText() );
					}
				});

		theLabel = new JLabel( "Destination Dir" );
		destDirText = addTextField( theLabel, null );
		
		destDirText.addFocusListener(
				new FocusAdapter() {
					public void focusLost(FocusEvent e) {
						getElement().setAttribute( "destDir", destDirText.getText() );
					}
				});
	}
	
	public void setElement( Element anElement ) {
		
		super.setElement( anElement );
		
		fileText.setText( anElement.getAttributeValue( "file" ));
		destDirText.setText( anElement.getAttributeValue( "destDir" ));
	}
}

/*
 * Created on Dec 29, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.awt.CardLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

import org.arch4j.ui.components.PropertiesPanel;
import org.arch4j.ui.layout.FrameConstraint;

/**
 * This panel represents the right side panel that has both 
 * the card panel of element panels as well as the help panel.
 * 
 * @author Allan Wick
 */
public class ElementDetailsPanel extends PropertiesPanel {

	private JPanel cardPanel;
	
	private HelpPanel helpPanel;
	
	public void addComponents() {
		
		cardPanel = new JPanel();
		cardPanel.setLayout( new CardLayout() );
		
		addComponentRelativeToBottom( cardPanel, 350 );
		
		helpPanel = new HelpPanel();
		helpPanel.setBorder( new TitledBorder( "Help" ));
        layout.setConstraint( helpPanel,
                new FrameConstraint( 0.0, x,
                                     1.0, -340,
                                     1.0, rightIndent,
                                     1.0, -5 ));

        add( helpPanel );
	}
	
	public void addPanel( JPanel aPanel, String aKey ) {
		
		cardPanel.add( new JScrollPane( aPanel ), aKey );
	}
	
	public void showPanel( String aPanelKey ) {
		
		CardLayout theLayout = (CardLayout)cardPanel.getLayout();
        theLayout.show(cardPanel,aPanelKey);
        cardPanel.validate();
        
	}
	
	public void scrollToAnchor( String anAnchorName ) {
		
		if ( anAnchorName.equals( "nullEditor") ) {
			return;
		}
		
		helpPanel.scrollToAnchor( "#" + anAnchorName );
	}
}

/*
 * Created on Dec 15, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import javax.swing.JButton;
import javax.swing.JScrollPane;

import org.arch4j.ui.components.PropertiesPanel;
import org.arch4j.ui.layout.FrameConstraint;
import org.arch4j.ui.tree.TreeBrowser;

/**
 * @author alwick
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class DefaultNavigationPanel extends PropertiesPanel {
    private PropertiesPanel detailsPanel;
    
    private TreeBrowser browser;
    
    /**
     * Refeneces to the navigation buttons
     */
    private JButton nextButton, previousButton;
    
    public DefaultNavigationPanel( TreeBrowser aBrowser, PropertiesPanel aDetailsPanel ) {

        browser = aBrowser;        
        detailsPanel = aDetailsPanel;
        
        layoutComponents();
    }
    
    public void addComponents() {

    }

    private void layoutComponents() {
        
        JScrollPane scrollPane = new JScrollPane( detailsPanel );
        scrollPane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED );
        scrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED );
        
        layout.setConstraint( scrollPane, 
                              new FrameConstraint( 0.0, x, 
                                                   0.0, y, 
                                                   1.0, 0,
                                                   1.0, -yIncrement ) );
        add( scrollPane );
    
        // create the add button and listen for pressed.
        nextButton = new JButton( "Next" );
        nextButton.setActionCommand( "nextNode" );
        nextButton.addActionListener( this );
        
        //layout in the lower right corner of the panel
        layout.setConstraint( nextButton, 
                              new FrameConstraint( 1.0, -100, 
                                                   1.0, -yIncrement+boxOffset, 
                                                   1.0, -5,
                                                   1.0, 0-boxOffset ) );        
        
        add( nextButton );
        
        // create the add button and listen for pressed.
        previousButton = new JButton( "Previous" );
        previousButton.setActionCommand( "previousNode" );
        previousButton.addActionListener( this );
        
        //layout in the lower right corner of the panel
        layout.setConstraint( previousButton, 
                              new FrameConstraint( 1.0, -200, 
                                                   1.0, -yIncrement+boxOffset, 
                                                   1.0, -105,
                                                   1.0, 0-boxOffset ) );        
        
        add( previousButton );
    }
        
	/**
	 * Returns the detailsPanel.
	 * @return PropertiesPanel
	 */
	public PropertiesPanel getDetailsPanel() {
		return detailsPanel;
	}

	/**
	 * Sets the detailsPanel.
	 * @param detailsPanel The detailsPanel to set
	 */
	public void setDetailsPanel(PropertiesPanel detailsPanel) {
		this.detailsPanel = detailsPanel;
	}
    
    /**
     * Invoked when previous button pressed.
     */
    public void previousNode() {
        browser.previousNode();
    }
        
    /**
     * Invoked when next button pressed.
     */
    public void nextNode() {      

        browser.nextNode();
    }
}

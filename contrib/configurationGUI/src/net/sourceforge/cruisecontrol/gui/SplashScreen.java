/*
 * SplashScreen.java
 *
 * Created on March 4, 2002, 10:54 AM
 */

package net.sourceforge.cruisecontrol.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.arch4j.ui.AbstractSplashScreen;

/**
 *
 * @author  awick
 */
public class SplashScreen extends AbstractSplashScreen {
    
    private JProgressBar progress;
    
    private int currentStep = 0;
    
    /** Creates a new instance of SplashScreen */
    public SplashScreen( Frame aFrame ) {
        
        super( aFrame );
        
        setProductName( SystemInformation.SYSTEM_NAME );
        setProductVersion( SystemInformation.VERSION_NUMBER );
    }
    
    public String getApplicationImageKey() {
    	
        return SystemInformation.ICON_NAME;
    }
    
    /**
     * Gets the default panel to display on the splash screen.
     * 
     * @return the default panel to display on the splash screen.
     */
    protected JPanel getMainPanel() {
     
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout( new BorderLayout() );
        
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout( new BorderLayout() );
        
        // add the image
        JPanel imagePanel = new JPanel();
        JLabel theImageLabel = new JLabel( getApplicationImage() );
        centerPanel.setPreferredSize( new Dimension( 300, 300 ) );
        imagePanel.add( theImageLabel );
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout( new GridLayout( 3, 1 ) );
        
        productName = new JLabel();
        productName.setHorizontalAlignment( SwingConstants.CENTER );
		productName.setAlignmentX( SwingConstants.CENTER );
        infoPanel.add( productName );
        
        productVersion = new JLabel();
        productVersion.setHorizontalAlignment( SwingConstants.CENTER );
		productVersion.setAlignmentX( SwingConstants.CENTER );
        infoPanel.add( productVersion );

		progress = new JProgressBar(0, MainWindow.NUMBER_OF_STARTUP_STEPS );
    	progress.setValue(0);
    	progress.setStringPainted(true);
    	infoPanel.add( progress );
    
        centerPanel.add( imagePanel, BorderLayout.CENTER );
        centerPanel.add( infoPanel, BorderLayout.SOUTH );
        
        mainPanel.add( centerPanel, BorderLayout.CENTER );
        
        //setup the status label
        JPanel statusPanel = new JPanel();
        status = new JLabel();
        
        statusPanel.add( status );
        
        mainPanel.add( statusPanel, BorderLayout.SOUTH );
        
        return mainPanel;
    }

    /**
     * Overriden to provide update of progress bar.
     */
    public void setStatus(String aString) {
        super.setStatus(aString);
        
        progress.setValue( ++currentStep );
    }

}

/*
 * Created on Dec 16, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.arch4j.ui.components.PropertiesPanel;

/**
 * Display the help text.
 * 
 * @author Allan Wick
 */
public class HelpPanel extends PropertiesPanel
					   implements HyperlinkListener {

	private static final String MAIN_URL = "http://cruisecontrol.sourceforge.net/main/configxml.html";
	JEditorPane htmlPane;
	private static String helpDocument;

	private static String getLocalDoc() throws IOException {
		
		if ( helpDocument == null ) {
			StringBuffer theInput = new StringBuffer();
			
			InputStream theHelpTextStream = HelpPanel.class.getClassLoader().getResourceAsStream( "configxml.html" );
			BufferedReader theInputReader = new BufferedReader( new InputStreamReader ( theHelpTextStream ) );
			
			String theNextLine = theInputReader.readLine();
			
			while ( theNextLine != null ) {
				
				theInput.append( theNextLine );
				
				theNextLine = theInputReader.readLine();
			}
			
			helpDocument = theInput.toString();
		}
		
		return helpDocument;
	}
	
	public void addComponents() {
		
		// make up for border
		y += 15;
		
		try {
		    //htmlPane = new JEditorPane(MAIN_URL);
			htmlPane = new JEditorPane( "text/html", getLocalDoc() );
		    htmlPane.setEditable(false);
		    htmlPane.addHyperlinkListener(this);
		    
		    JScrollPane theScroller = new JScrollPane( htmlPane );
		    addComponentRelativeToBottom( theScroller, 10 );
		}
		catch ( IOException ioe ) {
			addLabel( "Unable to connect to documentation..." ); 
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent event) {
		
		if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
			try {
				htmlPane.setPage(event.getURL());
			} catch (IOException ioe) {
				displayWarning( "Can't follow link to "
						        + event.getURL().toExternalForm() + ": " + ioe,
								"Unable to follow link" );
			}
		}
	}
	
	public void scrollToAnchor( String anAnchor ) {
		
		try {
			htmlPane.setPage( new URL( MAIN_URL + anAnchor ) );
		}
		catch (IOException ioe ) {
			
			// for developers only
			ioe.printStackTrace();
			System.out.println( "Anchor: " + anAnchor );
		}
	}
}
/*
 * Created on Dec 7, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui.panels;

import java.util.Properties;


import org.jdom.Element;

/**
 * @author Allan Wick
 */
public class ElementPanelGenerator {
	
	private static final String PROPERTIES_FILENAME = "element_panels.properties";
	
	private Properties panelClasses;
	
	public ElementPanelGenerator() {
		
		loadProperties();
	}
	
	public EditorPanel getPanelFor( Element anElement ) throws IllegalAccessException, InstantiationException, ClassNotFoundException {
		
		String thePanelClassname = panelClasses.getProperty( anElement.getName() );
		
		// if we don't know about this element return with null
		if ( thePanelClassname == null ) {
			
			return null;
		}
		
		return (EditorPanel) Class.forName( thePanelClassname ).newInstance();
	}
	
	private void loadProperties() {
		
		panelClasses = new Properties();
		
		try {
			panelClasses.load( getClass().getClassLoader().getResourceAsStream( PROPERTIES_FILENAME ) );
		}
		catch( Exception e ) {
			
			System.out.println( "Problem loading Panel classses: " + e.toString() );
		}
	}
}

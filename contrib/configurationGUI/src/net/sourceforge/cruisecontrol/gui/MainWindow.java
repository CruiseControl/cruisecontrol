/*
 * Created on Dec 1, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.plaf.ColorUIResource;

import org.arch4j.ui.AbstractSplashScreen;
import org.arch4j.ui.JAppCloser;
import org.arch4j.ui.ResourceManager;
import org.arch4j.ui.XMLResourceBuilder;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;

/**
 * The main class for the gui.
 * 
 * @author alwick
 */
public class MainWindow extends CruiseControlApplication {
	
	public static final int NUMBER_OF_STARTUP_STEPS = 1;
	
    private static MainWindow window;
    
    static {
		// set the properties directory for the property manager
		System.setProperty( "arch4j.suppress.constants.message", "true" );
		
		// setup our look and feel
	    initializeTheLookAndFeel();
    }
    
	/**
	 * Method initializeTheLookAndFeel.
	 * 
	 * Good url: http://home-1.tiscali.nl/~bmc88/java/sbook/061.html
	 */
    private static void initializeTheLookAndFeel() {

    	try {

            // Get LookAndFeel setting
    	    KunststoffLookAndFeel theLookAndFeel = new KunststoffLookAndFeel();
    	    //KunststoffLookAndFeel.setCurrentTheme( new CmicTheme() );
            
            // set the look and feel and override some colors.
			UIManager.setLookAndFeel( theLookAndFeel );
			UIManager.put( "Tree.background", Color.lightGray );
			UIManager.put( "Tree.textBackground", Color.lightGray );
			UIManager.put( "Tree.selectionBackground", Color.white );
			UIManager.put( "Tree.selectionForeground", Color.blue );
			UIManager.put( "Tree.drawsFocusBorderAroundIcon", new Boolean(true) );
			
			Font theFont = (Font) UIManager.get( "Label.font" );
			UIManager.put( "Tree.font", theFont );

			// highlighting green
			Color highlightColor = Color.green;

			UIManager.put( "TextField.caretForeground", new ColorUIResource( highlightColor ) );
			UIManager.put( "TextField.selectionBackground", highlightColor );

			UIManager.put( "TextArea.caretForeground", new ColorUIResource( highlightColor ) );
			UIManager.put( "TextArea.selectionBackground", highlightColor );
			
			// Menu Items
			UIManager.put( "Menu.selectionBackground", new ColorUIResource( Color.orange ) );
			
			// hack for webstart to tell it to use our classpath when loading the look and feel classes
    	    UIManager.getLookAndFeelDefaults().put( "ClassLoader", MainWindow.class.getClassLoader() );
    	    
    	    // make tooltip faster
    	    ToolTipManager.sharedInstance().setInitialDelay(100);
			ToolTipManager.sharedInstance().setReshowDelay(500);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void main( String args[] ) {
        try {
            JFrame frame = new JFrame();
            
            // sets the window variable in constructor
            window = new MainWindow( new SplashScreen(frame) );

            window.setFrame(frame);
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            frame.setTitle(window.getTitle());

            ImageIcon theIcon = window.getIcon();

            if (theIcon != null) {
                frame.setIconImage(theIcon.getImage());
            }

            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add("Center", window);
			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new JAppCloser( window ) );
            
            setWindowSize( frame );
            
            frame.show();
            frame.setCursor(Cursor.getDefaultCursor());
            window.pullToFront(); // pop to the top!
        } catch (Throwable t) {
            System.out.println("uncaught exception: " + t);
            t.printStackTrace();
        }
    }
    
    public MainWindow( AbstractSplashScreen aSplashScreen ) {
    	
    	super( aSplashScreen );
		
    	aSplashScreen.dispose();
    	
    	disableAction( "saveConfiguration" );
    }

    /**
     * Initialize the Windows look & feel.
     */
    protected void initializeLookAndFeel( ) {

    	// already done in main panel
    }

    /**
     * Initialize the contentPanel. The contentPanel will contain
     * other panels that define the content of the application.
     */
    protected void initializeContentPanel() {

        contentPanel = new ProjectBrowser( commandMgr, resourceMgr, this );
    }
    
    /**
     * Create an instance of the ResourceManager. Subclasses will override
     * this to populate the resource manager differently.
     */
    protected void initializeResourceManager() {
        showStartupStatus( "Initializing resources..." );
        resourceMgr = new ResourceManager();
        new XMLResourceBuilder(resourceMgr, "main_resources.xml").populateResources();
    }
    
	public void pullToFront() {
		
		if ( getFrame() != null ) {
			getFrame().setState(JFrame.NORMAL);
			getFrame().toFront();
		}
	}
	
	public void openConfiguration() {
		
		getProjectBrowser().openConfiguration();
		
		if (getProjectBrowser().hasConfigurationOpen() ) {
			enableAction( "saveConfiguration" );
		}
	}
	
	public void newConfiguration() {
		
		getProjectBrowser().newConfiguration();
		
		enableAction( "saveConfiguration" );
	}
    
	public void cut() {
		getProjectBrowser().cut();
	}
    
	public void copy() {
		getProjectBrowser().copy();
	}
    
	public void paste() {
		getProjectBrowser().paste();
	}
    
	public void delete() {
		getProjectBrowser().delete();
	}

	public void addPlugin() {
		getProjectBrowser().addPlugin();
    }

	public void addDateFormat() {
		getProjectBrowser().addDateFormat();
    }

	public void addLabelIncrementer() {
		getProjectBrowser().addLabelIncrementer();
    }

	public void addListeners() {
		getProjectBrowser().addListeners();
    }
	
	public void addListenerType() {
		getProjectBrowser().addListenerType();
    }
	
	public void addBootstrappers() {
		getProjectBrowser().addBootstrappers();
    }
	
	public void addBootstrapper() {
		getProjectBrowser().addBootstrapper();
    }

	public void addModificationSet() {
		getProjectBrowser().addModificationSet();
    }
	
	public void addModificationSetType() {
		getProjectBrowser().addModificationSetType();
    }

	public void addSchedule() {
		getProjectBrowser().addSchedule();
    }

	public void addScheduleType() {
		getProjectBrowser().addScheduleType();
    }
	
	public void addLog() {
		getProjectBrowser().addLog();
    }
	
	public void addMerge() {
		getProjectBrowser().addMerge();
    }
	
	public void addPublishers() {
		getProjectBrowser().addPublishers();
    }
	
	public void addPublisherType() {
		getProjectBrowser().addPublisherType();
    }
	
	public void saveConfiguration() {
		
		getProjectBrowser().saveConfiguration();
	}
	
	private ProjectBrowser getProjectBrowser() {
		
		return (ProjectBrowser) contentPanel;
	}
}

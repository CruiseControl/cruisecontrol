package net.sourceforge.cruisecontrol.gui;

import java.util.ArrayList;

import net.sourceforge.cruisecontrol.PluginRegistry;

import org.jdom.Document;
import org.jdom.Element;

/**
 * This class is used to map the elements, nesting, etc.
 * 
 * @author Allan Wick
 */
public class ConfigurationFileMapper {

	private static ConfigurationFileMapper instance;
	
	private static final String PLUGIN = "plugin";
	private static final String DATEFORMAT = "dateformat";
	private static final String LABEL_INCREMENTER = "labelincrementer";
	
	private static final String LISTENERS = "listeners";
	private static final String[] LISTENER_TYPES = { "CurrentBuildStatus" };
	
	private static final String BOOTSTRAPPERS = "bootstrappers";
	private static final String[] BOOTSTRAPPER_TYPES = { "Clearcase", 
			                                             "CurrentBuildStatus", 
														 "CurrentBuildStatusFtp",
														 "CVS", 
														 "P4",
														 "Snapshot",
														 "Starteam",
														 "Surround",
														 "SVN",
														 "VSS" };

	private static final String MODIFICATION_SET = "modificationset";
	private static final String[] MODIFICATION_SET_TYPES = { "AlwaysBuild", 
			                                                 "BuildStatus", 
														     "Clearcase",
															 "Compound",
															 "CVS",
															 "Filesystem",
															 "ForceOnly",
															 "HttpFile",
															 "MavenSnapshotDependency",
															 "MKS",
															 "P4",
															 "PVCS",
															 "SnapshotCM",
															 "Starteam",
															 "Surround",
															 "SVN",
															 "VSS",
															 "VSSJournal" };
	
	private static final String SCHEDULE = "schedule";
	private static final String[] SCHEDULE_TYPES = { "Ant", 
	                                                 "Maven",
													 "Nant",
												     "Pause" };
	
	private static final String LOG = "log";
	private static final String MERGE = "merge";
	
	private static final String PUBLISHERS = "publishers";
	private static final String[] PUBLISHER_TYPES = { "ArtifactsPublisher", 
													  "CurrentBuildStatusPublisher", 
													  "CurrentBuildStatusFTPPublisher",
													  "Email",
													  "Execute",
													  "FtpPublisher",
													  "Jabber",
													  "HtmlEmail",
													  "SametimeAnnouncement",
													  "SCP",
													  "X10",
													  "XsltLogPublisher" };
	
	private PluginRegistry registry;
	
	public static ConfigurationFileMapper getInstance() {
		
		if ( instance == null ) {
			instance = new ConfigurationFileMapper();
		}
		
		return instance;
	}
	
	private ConfigurationFileMapper() {
		
		registry = PluginRegistry.createRegistry();
	}
	
	/**
	 * Gets the plugin registry for the configuration application.
	 * 
	 * @return
	 */
	public PluginRegistry getRegistry() {
    	
    	return registry;
    }
    
	public Element createNewConfiguration() {
		
		Element theRootElement = new Element("cruisecontrol");
		Document theDocument = new Document( theRootElement );
		theRootElement.addContent( createNewSystem() );
		theRootElement.addContent( createNewPlugin() );
		theRootElement.addContent( createNewProject() );
		
		return theRootElement;
	}
	
	public Element createNewSystem() {
		
		Element theSystemElement = new Element( "system" );
		Element theConfigurationElement = new Element( "configuration");
		Element theThreadsElement = new Element( "threads" );
		theThreadsElement.setAttribute( "count", "1" );
		theConfigurationElement.addContent( theThreadsElement );
		theSystemElement.addContent( theConfigurationElement );
		
		return theSystemElement;
	}
	
	/**
	 * Create a new project element with all sub-elements.
	 * 
	 * @return A new project element with all sub-elements.
	 */
	public Element createNewProject() {
		
		Element theProjectElement = new Element( "project" );
		theProjectElement.setAttribute( "name", "Default name" );
		theProjectElement.addContent( createNewDateFormat() );
		theProjectElement.addContent( createNewLabelIncrementer() );
		theProjectElement.addContent( createNewListeners() );
		theProjectElement.addContent( createNewBootstrappers() );
		theProjectElement.addContent( createNewModificationSet() );
		theProjectElement.addContent( createNewLog() );
		theProjectElement.addContent( createNewSchedule() );
		theProjectElement.addContent( createNewPublishers() );
		
		return theProjectElement;
	}

	public Element createNewPlugin() {
		
		return new Element( PLUGIN );
	}
	
	public Element createNewDateFormat() {
		
		return new Element( DATEFORMAT );
	}
	
	public Element createNewLabelIncrementer() {
		
		return new Element( LABEL_INCREMENTER );
	}
	
	public Element createNewListeners() {
		
		return new Element( LISTENERS );
	}
	
	public Element createNewBootstrappers() {
		
		return new Element( BOOTSTRAPPERS );
	}
	
	public Element createNewModificationSet() {

		return new Element( MODIFICATION_SET );
	}
	
	public Element createNewSchedule() {
		
		return new Element( SCHEDULE );
	}
	
	public Element createNewMerge() {
		
		return new Element( MERGE );
	}
	
	public Element createNewLog() {
		
		return new Element( LOG );
	}
	
	public Element createNewPublishers() {
			
		return new Element( PUBLISHERS );
	}
	
	/**
	 * Get the listener types.  
	 *  
	 * @return
	 */
	public ArrayList getListenerTypes() {
		
		ArrayList theTypes = new ArrayList();
		
		for ( int i = 0; i < LISTENER_TYPES.length;i++ ) {
			
			// add types that have plugin class
			// TODO is there a better way to find out this info?
			try {
				getRegistry().getPluginClass( LISTENER_TYPES[i] );
				theTypes.add( LISTENER_TYPES[i] );
			}
			catch (Throwable e ) {
				
				// we don't care
			}
		}
		
		return theTypes;
	}
	
	/**
	 * Get the bootstrapper types.  
	 *  
	 * @return
	 */
	public ArrayList getBootstrapperTypes() {
		
		ArrayList theTypes = new ArrayList();
		
		for ( int i = 0; i < BOOTSTRAPPER_TYPES.length;i++ ) {
			
			// add types that have plugin class
			// TODO is there a better way to find out this info?
			try {
				getRegistry().getPluginClass( BOOTSTRAPPER_TYPES[i] );
				theTypes.add( BOOTSTRAPPER_TYPES[i] );
			}
			catch (Throwable e ) {
				
				// we don't care
			}
		}
		
		return theTypes;
	}

	/**
	 * Get the modification set types.  
	 *  
	 * @return
	 */
	public ArrayList getModificationSetTypes() {
		
		ArrayList theTypes = new ArrayList();
		
		for ( int i = 0; i < MODIFICATION_SET_TYPES.length;i++ ) {
			
			// add types that have plugin class
			// TODO is there a better way to find out this info?
			try {
				getRegistry().getPluginClass( MODIFICATION_SET_TYPES[i] );
				theTypes.add( MODIFICATION_SET_TYPES[i] );
			}
			catch (Throwable e ) {
				
				// we don't care
			}
		}
		
		return theTypes;
	}

	/**
	 * Get the modification set types.  
	 *  
	 * @return
	 */
	public ArrayList getScheduleTypes() {
		
		ArrayList theTypes = new ArrayList();
		
		for ( int i = 0; i < SCHEDULE_TYPES.length;i++ ) {
			
			// add types that have plugin class
			// TODO is there a better way to find out this info?
			try {
				getRegistry().getPluginClass( SCHEDULE_TYPES[i] );
				theTypes.add( SCHEDULE_TYPES[i] );
			}
			catch (Throwable e ) {
				
				// we don't care
			}
		}
		
		return theTypes;
	}

	/**
	 * Get the publisher types.  
	 *  
	 * @return
	 */
	public ArrayList getPublisherTypes() {
		
		ArrayList theTypes = new ArrayList();
		
		for ( int i = 0; i < PUBLISHER_TYPES.length;i++ ) {
			
			// add types that have plugin class
			// TODO is there a better way to find out this info?
			try {
				getRegistry().getPluginClass( PUBLISHER_TYPES[i] );
				theTypes.add( PUBLISHER_TYPES[i] );
			}
			catch (Throwable e ) {
				
				// we don't care
			}
		}
		
		return theTypes;
	}
	
	/**
	 * Create the bootstrapper element for the project of the given type.
	 * 
	 * @param projectElement
	 * @param aType
	 * @return
	 */
	public Element createListenerElementFor( Element projectElement, String aType ) {
		
		// first check for grouping element 
		Element theListenersElement = projectElement.getChild( LISTENERS );
		
		// check to see if this is the listeners
		if ( theListenersElement == null ) {
			if( projectElement.getName().equals( LISTENERS )) {
				theListenersElement = projectElement;
			}
		}
		
		// if not found create it...
		if ( theListenersElement == null ) {
			
			theListenersElement = new Element( LISTENERS );
			projectElement.addContent( theListenersElement );
		}
		
		// create and add the new element for the type passed in
		Element theListenerElement = new Element( aType.toLowerCase() + "listener" );
		theListenersElement.addContent( theListenerElement );
		
		return theListenerElement;
	}
	
	/**
	 * Create the bootstrapper element for the project of the given type.
	 * 
	 * @param projectElement
	 * @param aType
	 * @return
	 */
	public Element createBootstrapperElementFor( Element projectElement, String aType ) {
		
		// first check for grouping element 
		Element theBootstrappersElement = projectElement.getChild( BOOTSTRAPPERS );
		
		// check to see if this is the boostrapper
		if ( theBootstrappersElement == null ) {
			if( projectElement.getName().equals( BOOTSTRAPPERS )) {
				theBootstrappersElement = projectElement;
			}
		}
		
		// if not found create it...
		if ( theBootstrappersElement == null ) {
			
			theBootstrappersElement = new Element( BOOTSTRAPPERS );
			projectElement.addContent( theBootstrappersElement );
		}
		
		// create and add the new element for the type passed in
		Element theBootstrapperElement = new Element( aType.toLowerCase() + "bootstrapper" );
		theBootstrappersElement.addContent( theBootstrapperElement );
		
		return theBootstrapperElement;
	}
	
	/**
	 * Create the modification set element for the project of the given type.
	 * 
	 * @param projectElement
	 * @param aType
	 * @return
	 */
	public Element createModificationSetElementFor( Element projectElement, String aType ) {
		
		// first check for grouping element 
		Element theModificationSetElement = projectElement.getChild( MODIFICATION_SET );
		
		// check to see if this is the modificationset
		if ( theModificationSetElement == null ) {
			if( projectElement.getName().equals( MODIFICATION_SET )) {
				theModificationSetElement = projectElement;
			}
		}
		
		// if not found create it...
		if ( theModificationSetElement == null ) {
			
			theModificationSetElement = new Element( MODIFICATION_SET );
			projectElement.addContent( theModificationSetElement );
		}
		
		// create and add the new element for the type passed in
		Element theNewModificationSetElement = new Element( aType.toLowerCase() );
		theModificationSetElement.addContent( theNewModificationSetElement );
		
		return theNewModificationSetElement;
	}
	
	/**
	 * Create the schedule element for the project of the given type.
	 * 
	 * @param projectElement
	 * @param aType
	 * @return
	 */
	public Element createScheduleElementFor( Element projectElement, String aType ) {
		
		// first check for grouping element 
		Element theScheduleElement = projectElement.getChild( SCHEDULE );
		
		// check to see if this is the schedule
		if ( theScheduleElement == null ) {
			if( projectElement.getName().equals( SCHEDULE )) {
				theScheduleElement = projectElement;
			}
		}
		
		// if not found create it...
		if ( theScheduleElement == null ) {
			
			theScheduleElement = new Element( SCHEDULE );
			projectElement.addContent( theScheduleElement );
		}
		
		// create and add the new element for the type passed in
		Element theNewScheduleElement = new Element( aType.toLowerCase() );
		theScheduleElement.addContent( theNewScheduleElement );
		
		return theNewScheduleElement;
	}
	
	/**
	 * Create the schedule element for the project of the given type.
	 * 
	 * @param projectElement
	 * @param aType
	 * @return
	 */
	public Element createPublisherElementFor( Element projectElement, String aType ) {
		
		// first check for grouping element 
		Element thePublishersElement = projectElement.getChild( PUBLISHERS );
		
		// check to see if this is the publishers
		if ( thePublishersElement == null ) {
			if( projectElement.getName().equals( PUBLISHERS )) {
				thePublishersElement = projectElement;
			}
		}
		
		// if not found create it...
		if ( thePublishersElement == null ) {
			
			thePublishersElement = createNewPublishers();
			projectElement.addContent( thePublishersElement );
		}
		
		// create and add the new element for the type passed in
		Element theNewPublisherElement = new Element( aType.toLowerCase() );
		thePublishersElement.addContent( theNewPublisherElement );
		
		return theNewPublisherElement;
	}
}

/*
 * Created on Dec 1, 2004
 */
package net.sourceforge.cruisecontrol.gui;

import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.PluginRegistry;
import net.sourceforge.cruisecontrol.config.ConfigurationContext;
import net.sourceforge.cruisecontrol.gui.panels.DefaultElementPanel;
import net.sourceforge.cruisecontrol.gui.panels.EditorPanel;
import net.sourceforge.cruisecontrol.gui.panels.ElementDetailsPanel;
import net.sourceforge.cruisecontrol.gui.panels.ElementPanelGenerator;
import net.sourceforge.cruisecontrol.util.Util;

import org.arch4j.ui.CommandManager;
import org.arch4j.ui.ResourceManager;
import org.arch4j.ui.components.NullPanel;
import org.arch4j.ui.tree.BrowserTreeNode;
import org.arch4j.ui.tree.TreeBrowser;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;

/**
 * @author alwick
 */
public class ProjectBrowser extends TreeBrowser {
    
	private ConfigurationContext context = new ConfigurationContext();
    
	private MainWindow builder;

	private HashMap builtPanels = new HashMap();
	
	private ElementPanelGenerator panelGenerator = new ElementPanelGenerator();
	private ElementDetailsPanel detailsPanel;
	
    public ProjectBrowser( CommandManager aCommandManager, ResourceManager aResourceManager, MainWindow aBuilder ) {

        commandMgr = aCommandManager;
        resourceMgr = aResourceManager;
        builder = aBuilder;
    }
    
    /**
     * This method was created by a SmartGuide.
     */
    public void createSplitPane( ) {
        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setMinimumSize(new Dimension(200, 200));
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, scrollPane, cardPanel);
        splitPane.setDividerLocation( getDividerLocation() );
    }

    /**
     * This method was created by a SmartGuide.
     */
    public void createCardPanel( ) {

    	detailsPanel = new ElementDetailsPanel();
        cardPanel = detailsPanel;
        
        // add default panel
        NullPanel thePanel = new NullPanel();

        detailsPanel.addPanel( thePanel, "nullPanel" );

    }
    
    public void showControllerPanel() {
    	
    }
   
    public void addPlugin() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewPlugin();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }
    
    public void addDateFormat() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewDateFormat();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }
    
    public void addLabelIncrementer() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewLabelIncrementer();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }

    public void addListeners() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewListeners();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }

    public void addBootstrappers() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewBootstrappers();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }
    
    public void addModificationSet() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewModificationSet();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }
    
    public void addSchedule() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewSchedule();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }
    
    public void addLog() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewLog();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }
    
    public void addMerge() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewMerge();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }
    
    public void addPublishers() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	Element theNewElement = ConfigurationFileMapper.getInstance().createNewPublishers();
    	
    	theSelectedElement.addContent( theNewElement );
    	updateTree( theNewElement );
    }
    
    public void addListenerType() {
    	
    	// get the valid types to display
    	ArrayList theOptions = ConfigurationFileMapper.getInstance().getListenerTypes();
    	
    	// allow user to select
    	SelectionPanel theSelectionPanel = new SelectionPanel( theOptions );
    	theSelectionPanel.showDialog( builder, "Select Publisher type" );
    	
    	if ( !theSelectionPanel.wasCancelled() ) {
	    	// use selected type to build element
	    	Element theProjectElement = (Element) getSelectedObject();
	    	Element theNewListener = 
	    		ConfigurationFileMapper.getInstance().createListenerElementFor( theProjectElement, 
	    	                                                                    theSelectionPanel.getSelectedType() );
	    	
	    	updateTree( theNewListener );
    	}
    }
    
    public void addBootstrapper() {
    	
    	// get the valid types to display
    	ArrayList theOptions = ConfigurationFileMapper.getInstance().getBootstrapperTypes();
    	
    	// allow user to select
    	SelectionPanel theSelectionPanel = new SelectionPanel( theOptions );
    	theSelectionPanel.showDialog( builder, "Select Bootstrapper type" );
    	
    	if ( !theSelectionPanel.wasCancelled() ) {
	    	// use selected type to build element
	    	Element theProjectElement = (Element) getSelectedObject();
	    	Element theNewBoostrapper = 
	    		ConfigurationFileMapper.getInstance().createBootstrapperElementFor( theProjectElement, 
	    	                                                                        theSelectionPanel.getSelectedType() );
	    	
	    	updateTree( theNewBoostrapper );
    	}
    }
    
    public void addModificationSetType() {
    	
    	// get the valid types to display
    	ArrayList theOptions = ConfigurationFileMapper.getInstance().getModificationSetTypes();
    	
    	// allow user to select
    	SelectionPanel theSelectionPanel = new SelectionPanel( theOptions );
    	theSelectionPanel.showDialog( builder, "Select Modification Set type" );
    	
    	if ( !theSelectionPanel.wasCancelled() ) {
	    	// use selected type to build element
	    	Element theProjectElement = (Element) getSelectedObject();
	    	Element theNewModificationSet = 
	    		ConfigurationFileMapper.getInstance().createModificationSetElementFor( theProjectElement, 
	    	                                                                           theSelectionPanel.getSelectedType() );
	    	
	    	updateTree( theNewModificationSet );
    	}
    }
    
    public void addScheduleType() {
    	
    	// get the valid types to display
    	ArrayList theOptions = ConfigurationFileMapper.getInstance().getScheduleTypes();
    	
    	// allow user to select
    	SelectionPanel theSelectionPanel = new SelectionPanel( theOptions );
    	theSelectionPanel.showDialog( builder, "Select Schedule type" );
    	
    	if ( !theSelectionPanel.wasCancelled() ) {
	    	// use selected type to build element
	    	Element theProjectElement = (Element) getSelectedObject();
	    	Element theNewSchedule = 
	    		ConfigurationFileMapper.getInstance().createScheduleElementFor( theProjectElement, 
	    	                                                                    theSelectionPanel.getSelectedType() );
	    	
	    	updateTree( theNewSchedule );
    	}
    }
    
    public void addPublisherType() {
    	
    	// get the valid types to display
    	ArrayList theOptions = ConfigurationFileMapper.getInstance().getPublisherTypes();
    	
    	// allow user to select
    	SelectionPanel theSelectionPanel = new SelectionPanel( theOptions );
    	theSelectionPanel.showDialog( builder, "Select Publisher type" );
    	
    	if ( !theSelectionPanel.wasCancelled() ) {
	    	// use selected type to build element
	    	Element theProjectElement = (Element) getSelectedObject();
	    	Element theNewModificationSet = 
	    		ConfigurationFileMapper.getInstance().createPublisherElementFor( theProjectElement, 
	    	                                                                     theSelectionPanel.getSelectedType() );
	    	
	    	updateTree( theNewModificationSet );
    	}
    }
    
    private boolean validateNewConfiguration() {
    	
    	// if nodes in the tree, validate the configuration.
    	BrowserTreeNode theRootNode = (BrowserTreeNode) getTreeModel().getRoot();
    	
    	if ( theRootNode.getChildCount() > 0 ) {
    		
    		return displayConfirm( "There is a configuration open, are you sure you want to edit another file?", 
    				               "Open Configuration" );
    	}
    	
    	return true;
    }
    
    public void openConfiguration() {
    	
    	if ( validateNewConfiguration() ) {
	        JFileChooser chooser = new JFileChooser();
	        int returnValue = chooser.showOpenDialog(builder);
	        
	        if (returnValue == JFileChooser.APPROVE_OPTION) {
	            System.out.println("User selected a file.");
	            File chosenFile = chooser.getSelectedFile();
	            System.out.println("The file was: " + chosenFile.getAbsolutePath());
	            
	            context.setConfigurationFile(chosenFile);
	            
	            try {
		            Element rootElement = Util.loadConfigFile(context.getConfigurationFile());
		            
		            refreshTree( rootElement );
		            
	        	} catch (CruiseControlException e) {
	                e.printStackTrace();
	                
	                displayError( "Unable to open file: " + chosenFile.getName(), 
	                		      "Error Opening File" );
	            }
	        } else {
	            System.out.println("User cancelled...");
	        }
    	}        
    }
    
    public void saveConfiguration() {
    	
    	try {
    		File chosenFile = context.getConfigurationFile();

    		// ask for the file if not found
    		// TODO share this code with open configuration...
    		if ( chosenFile == null ) {
    	        JFileChooser chooser = new JFileChooser();
    	        int returnValue = chooser.showSaveDialog(builder);
    	        
    	        if (returnValue == JFileChooser.APPROVE_OPTION) {
    	            System.out.println("User selected a file.");
    	            chosenFile = chooser.getSelectedFile();
    	            System.out.println("The file was: " + chosenFile.getAbsolutePath());
    	            
    	            context.setConfigurationFile(chosenFile);
    	        }
    		}
    		
    		Element selectedElement = (Element) getSelectedNode().getUserObject();
    		Element rootElement = selectedElement.getDocument().getRootElement();
    		
    		new XMLOutputter().output( rootElement, new FileWriter( chosenFile ) );
    		
    		displayInformation( "Configuration Updated successfully", "Saved Configuration" );
    	}
    	catch ( Exception e ) {
    		System.out.println( "Error saving configuration" + e );
    	}
    }
    
    public void delete() {
    	
    	if ( displayConfirm( "Do you really want to remove this node?", "Delete Node" )) {
	    	Element theCutElement = (Element) getSelectedObject();
	    	theCutElement.getParentElement().removeContent( theCutElement );
	    	
	    	BrowserTreeNode theParent = (BrowserTreeNode) getSelectedNode().getParent();
	    	
	    	getTreeModel().removeNodeFromParent( getSelectedNode() );
	    	
	    	if ( theParent == null ) {
	    		showNullEditor();
	    	}
	    	else {
	    		selectNode( theParent );
	    	}
    	}
    }

    public void paste() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	BrowserTreeNode theSelectedNode = getSelectedNode();
    	
    	if ( theSelectedElement == null ) {
    		
    		displayError( "You must have node selected when pasting.", "Paste Error" );
    		return;
    	}
    	
    	if ( copyBuffer.isEmpty() ) {
    		displayError( "You must copy or cut something before pasting.", "Paste Error" );
    		return;    		
    	}
    	
    	if ( theSelectedElement.getName().equals( "project" ) ) {
    		
    		theSelectedElement = theSelectedElement.getDocument().getRootElement();
    		theSelectedNode = (BrowserTreeNode) getTreeModel().getRoot();
    	}
    	
    	BrowserTreeNode theCutNode = (BrowserTreeNode) copyBuffer.iterator().next();
    	Element theCutElement = (Element) theCutNode.getUserObject();
    	
    	Element theNewElement = (Element) theCutElement.clone();
    	theSelectedElement.addContent( theNewElement );
    	
    	BrowserTreeNode theNewNode = createNodeFor( theNewElement );
    	addChildrenToTree( theNewElement, theNewNode );
    	
    	insertNode( theNewNode, theSelectedNode );
    	getTreeModel().nodeChanged( theSelectedNode );
    	
    	selectNode( theNewNode );
    	
    }
    
    public void copyPaste() {
    	
    	Element theSelectedElement = (Element) getSelectedObject();
    	BrowserTreeNode theSelectedNode = getSelectedNode();
    	
    	if ( theSelectedElement == null ) {
    		
    		displayError( "You must have node selected when pasting.", "Paste Error" );
    		return;
    	}
    	
    	if ( theSelectedElement.getName().equals( "project" ) ) {
    		
    		theSelectedElement = theSelectedElement.getDocument().getRootElement();
    		theSelectedNode = (BrowserTreeNode) getTreeModel().getRoot();
    	}
    	
    	BrowserTreeNode theCutNode = (BrowserTreeNode) getSelectedNode();
    	Element theCutElement = (Element) theCutNode.getUserObject();
    	
    	Element theNewElement = (Element) theCutElement.clone();
    		
    	theSelectedElement.addContent( theNewElement );
    	
    	addChildrenToTree ( theNewElement, theSelectedNode );
    	
    	getTreeModel().nodeChanged( theSelectedNode );
    }
    
    public void newConfiguration() {
    	
    	if ( validateNewConfiguration() ) {
	    	context = new ConfigurationContext();
	    	
	    	//create basics
			Element rootElement = ConfigurationFileMapper.getInstance().createNewConfiguration();
			
			refreshTree( rootElement );
    	}
    }
    
    public boolean hasConfigurationOpen() {
    	
    	return context.getConfigurationFile() != null;
    }
    
    private void refreshTree( Element rootElement ) {
        
    	// create new root node and set it on the model
		BrowserTreeNode top = new BrowserTreeNode();
		getTreeModel().setRoot( top );
		
		// add new nodes back
		addChildrenToTree(rootElement, top);
		
		getTreeModel().nodeChanged( top );
		
        // select the root node.
        selectNode( (BrowserTreeNode) top.children().nextElement() );
        tree.expandRow(0);
    }
    
    private void updateTree( Element anElementToSelect ) {
    	
    	// get the current node
    	BrowserTreeNode theParentNode = getSelectedNode();
    	
    	//find the correct parent tree node.
    	Element theParentElement = anElementToSelect.getParentElement();
    	
    	if ( theParentElement != (Element) theParentNode.getUserObject() ) {
    		
    		for ( int index = 0; index < theParentNode.getChildCount();index++ ) {
    			
    			BrowserTreeNode theChildNode = (BrowserTreeNode) theParentNode.getChildAt( index );
    			
    			Element theChildElement = (Element) theChildNode.getUserObject();
    			
    			if ( theChildElement == theParentElement ) {
    				
    				theParentNode = theChildNode;
    				break;
    			}
    		}
    		
    	}
    	
    	// create new child node
    	BrowserTreeNode theChildNode = createNodeFor( anElementToSelect );
    	
    	// insert the node
    	insertNode( theChildNode, theParentNode );
    	
    	// select the new child
    	selectNode( theChildNode );
    }
    
    /**
     * Show the panel associated to the selected node.  If the panel for the element doesn't exist, add 
     * to the card panel on right.
     */
    public void showElementPanel() {
    	
    	BrowserTreeNode theSelectedNode = getSelectedNode();
    	Element theSelectedElement = (Element) theSelectedNode.getUserObject();
    	
    	try {
    		String thePanelKey = theSelectedElement.getName();

    		EditorPanel theEditorPanel = (EditorPanel) builtPanels.get( thePanelKey );
    		
    		// if not found, create panel
    		if ( theEditorPanel == null ) {
	    		// try the generator
    			theEditorPanel = panelGenerator.getPanelFor( theSelectedElement );
	    		
	    		// if none found, use default
	    		if ( theEditorPanel == null ) {
	    			
	    			// get the cc plugin class
	    			Class theBuilderClass = getRegistry().getPluginClass( thePanelKey );
	    			
	    			// if no builder class, just show the default panel
	    			if ( theBuilderClass == null ) {
	    				showNullEditor();
	    				detailsPanel.scrollToAnchor( theSelectedElement.getName() );
	    				return;
	    			}
	    			
	    			theEditorPanel = new DefaultElementPanel( theBuilderClass );
	    		}
	    		else {
	    			theEditorPanel.setProjectBrowser( this );
	    		}
	    		
	    		builtPanels.put( thePanelKey, theEditorPanel );
	    		
	    		// wrap with standard navigation panel
	    		//DefaultNavigationPanel theNavigationPanel = new DefaultNavigationPanel(this, (PropertiesPanel) theEditorPanel );
	    		
	    		addPanel( (JPanel) theEditorPanel, thePanelKey );
    		}
    		
	    	theEditorPanel.setElement( theSelectedElement );
	    	showPanel( thePanelKey );
	    	detailsPanel.scrollToAnchor( theSelectedElement.getName() );
    	}
    	catch ( Exception e ) {
    		
    		e.printStackTrace();
    		
    		showNullEditor();
    	}
    }
    
    protected void showNullEditor() {
    	
    	detailsPanel.showPanel( "nullPanel" );
    }
    
    protected void showPanel( String aPanelKey ) {
    	
    	ElementDetailsPanel theDetailsPanel = (ElementDetailsPanel) cardPanel;
    	theDetailsPanel.showPanel( aPanelKey );
    }
    
    private void addPanel( JPanel aPanel, String aPanelKey ) {
    	
    	ElementDetailsPanel theDetailsPanel = (ElementDetailsPanel) cardPanel;
    	
    	theDetailsPanel.addPanel( aPanel, aPanelKey );
    }
    
    public void updateNodeText( String aNodeText ) {
    	
    	BrowserTreeNode theSelectedNode = getSelectedNode();
    	theSelectedNode.setText( aNodeText );
    	
    	getTreeModel().nodeChanged( theSelectedNode );
    	
    }
    
    private PluginRegistry getRegistry() {
    	
    	return ConfigurationFileMapper.getInstance().getRegistry();
    }
    
    private BrowserTreeNode addChildrenToTree( Element aRootElement, BrowserTreeNode topTreeNode ) {
    	
        //Traverse the elements and build tree nodes
        List children = aRootElement.getChildren();
        BrowserTreeNode theRootNode = null;
        
        for (int i = 0; i < children.size(); i++) {
            Element nextElem = (Element) children.get(i);
            
            BrowserTreeNode theNode = createNodeFor( nextElem );
            theRootNode = theNode;
            insertNode( theNode, topTreeNode );
            
            addChildrenToTree( nextElem, theNode );
        }
        
        return theRootNode;
    }
    
    private BrowserTreeNode createNodeFor( Element anElement ) {

        String elemName = anElement.getName();
        String menuName = elemName + "Menu";
        
        // we want to show the project name to distinquish projects in the tree.
        if ( elemName.equals("project") ||
        	 elemName.equals( "plugin" )) {
        	
        	elemName = anElement.getAttributeValue( "name" );
        	
        	if ( elemName == null ) {
        		elemName = anElement.getName();
        	}
        }
        
        BrowserTreeNode theNode = new BrowserTreeNode();
        theNode.setText( elemName );
        theNode.setIconAccessor("folderc");
        theNode.setAction("showElementPanel");
        theNode.setUserObject(anElement);
        theNode.setName("element");
        
        // set the menu if found
        JPopupMenu theNodeMenu = getCommandManager().createPopupMenu( menuName );
        
        // if no menu found, use the default one.
        if ( theNodeMenu == null ) {
        	theNode.setMenu( getCommandManager().createPopupMenu( "nodeMenu" ));
        }
        else {
        	theNode.setMenu( theNodeMenu );
        }
        
        theNode.setEnabled(true);
        
        return theNode;
    }
}

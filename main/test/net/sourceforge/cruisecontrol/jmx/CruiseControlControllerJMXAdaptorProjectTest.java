package net.sourceforge.cruisecontrol.jmx;

import java.util.NoSuchElementException;
import java.io.File;
import java.io.FileWriter;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.CruiseControlSettings;
import net.sourceforge.cruisecontrol.gendoc.PluginInfo;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

/**
 * Tests the CruiseControlControllerJMXAdaptor with a dummy configuration
 * containing a project.
 * @author pollens@msoe.edu
 */
public class CruiseControlControllerJMXAdaptorProjectTest extends TestCase {

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    private CruiseControlControllerJMXAdaptor adaptor;

    @Override
    protected void setUp() throws Exception {
        CruiseControlSettings.getInstance(this);
        // Generate a temporary config.xml file.
        File configFile = File.createTempFile("config", ".xml");
        filesToDelete.add(configFile);
        
        FileWriter configFileOut = new FileWriter(configFile);
        configFileOut.write("<?xml version=\"1.0\" ?>\n");
        configFileOut.write("<cruisecontrol>\n");
        configFileOut.write("<project name='proj'>\n");
        configFileOut.write("  <plugin name=\"mynewplugin\"");
        configFileOut.write("    classname=\"net.sourceforge.cruisecontrol.gendoc.DummySourceControl\" />");
        configFileOut.write("  <modificationset><alwaysbuild /></modificationset>\n");
        configFileOut.write("  <schedule interval=\"1000\"><ant /></schedule>\n");
        configFileOut.write("</project>\n");
        configFileOut.write("</cruisecontrol>");
        configFileOut.close();
        
        // Use the temporary config.xml to create a new controller and adaptor.
        CruiseControlController ccController = new CruiseControlController();
        ccController.setConfigFile(configFile);
        
        adaptor = new CruiseControlControllerJMXAdaptor(ccController);
    }

    @Override
    protected void tearDown() throws Exception {
        CruiseControlSettings.delInstance(this);
        filesToDelete.delete();
    }
    
    public void testGetPluginInfo() {
        // Get the root project plugin tree. Make sure the project-specific
        // plugin isn't there.
        PluginInfo rootRoot = adaptor.getPluginInfo(null);
        assertEquals("cruisecontrol", rootRoot.getName());
        assertNull(rootRoot
                .getChildPluginByName("project")
                .getChildPluginByName("modificationset")
                .getChildPluginByName("mynewplugin"));
        
        // Try getting plugins from a bogus project.
        try {
            adaptor.getPluginInfo("bogus");
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) { }
        
        // Get a legitimate project's plugins. Make sure the specially
        // configured plugin is there.
        PluginInfo projRoot = adaptor.getPluginInfo("proj");
        assertEquals("cruisecontrol", projRoot.getName());
        assertNotNull(projRoot
                .getChildPluginByName("project")
                .getChildPluginByName("modificationset")
                .getChildPluginByName("mynewplugin"));
    }
    
    public void testGetPluginHtml() {
        // Get the root project HTML.
        String rootHtml = adaptor.getPluginHTML(null).trim();
        assertTrue(rootHtml.length() > 10000);
        assertTrue(rootHtml.startsWith("<!DOCTYPE html PUBLIC"));
        
        // Try getting plugins from a bogus project.
        try {
            adaptor.getPluginHTML("bogus");
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException e) { }
        
        // Get a legitimate project's HTML.
        String projHtml = adaptor.getPluginHTML("proj").trim();
        assertTrue(projHtml.length() > rootHtml.length()); // There should be extra content for the extra plugin.
        assertTrue(projHtml.startsWith("<!DOCTYPE html PUBLIC"));
    }

}

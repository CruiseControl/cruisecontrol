/*
 * Created on Oct 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.jmx;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlConfig;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.CruiseControlOptions;
import net.sourceforge.cruisecontrol.gendoc.PluginInfo;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

/**
 * @author pollens@msoe.edu
 */
public class CruiseControlControllerJMXAdaptorGendocTest extends TestCase {

    private final TestUtil.FilesToDelete filesToDelete = new TestUtil.FilesToDelete();

    private CruiseControlControllerJMXAdaptor adaptor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CruiseControlOptions.getInstance(this);

        // Create a dummy config file.
        File configFile = File.createTempFile("cruisecontrol-test", ".xml");
        filesToDelete.add(configFile);
        PrintWriter writer = new PrintWriter(configFile);
        writer.write(
                "<cruisecontrol>" +
                "  <project name='proj'>" +
                "    <schedule>" +
                "      <ant/>" +
                "    </schedule>" +
                "  </project>" +
                "</cruisecontrol>");
        writer.close();

        CruiseControlController controller = new CruiseControlController();
        controller.setConfigFile(configFile);

        adaptor = new CruiseControlControllerJMXAdaptor(controller);
    }

    @Override
    protected void tearDown() throws Exception {
        filesToDelete.delete();
        CruiseControlOptions.delInstance(this);
    }

    public void testGetPluginCSS() {
        String css = adaptor.getPluginCSS().trim();
        assertTrue("CSS not loaded (if this fails in your IDE you simply need to run a command line build first in order to generate gendoc.css)",
                css.length() > 1000);
    }

    public void testGetPluginInfo() {
        for (PluginInfo info : new PluginInfo[] {
                adaptor.getPluginInfo(null),
                adaptor.getPluginInfo("proj")}) {

            assertEquals("cruisecontrol", info.getName());
            assertEquals(CruiseControlConfig.class.getName(), info.getClassName());
            assertTrue(info.getChildren().size() > 0);
        }
    }

    public void testGetAllPlugins() {
        List<PluginInfo> infos = adaptor.getAllPlugins(null);
        assertTrue(infos.size() > 10);

        // Make sure the <cruisecontrol> plugin is in there.
        boolean found = false;
        for (PluginInfo info : infos) {
            found = info.getName().equals("cruisecontrol");
            if (found) {
                break;
            }
        }
        if (!found) {
            fail("Expected to find <cruisecontrol> plugin in list");
        }
    }

    public void testGetPluginHTML() {
        String html = adaptor.getPluginHTML(null);
        assertTrue("HTML not generated", html.length() > 1000);
        assertTrue(html.trim().startsWith("<!DOCTYPE html PUBLIC"));
    }

}

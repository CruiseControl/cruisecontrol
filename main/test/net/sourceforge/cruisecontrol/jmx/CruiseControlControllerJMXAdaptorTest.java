/*
 * Created on Oct 1, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sourceforge.cruisecontrol.jmx;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlController;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.bootstrappers.BootstrapperDetail;
import net.sourceforge.cruisecontrol.publishers.PublisherDetail;
import net.sourceforge.cruisecontrol.sourcecontrols.SourceControlDetail;

/**
 * @author alwick
 */
public class CruiseControlControllerJMXAdaptorTest extends TestCase {

    private CruiseControlControllerJMXAdaptor adaptor;

    protected void setUp() throws Exception {
        super.setUp();
        
        adaptor = new CruiseControlControllerJMXAdaptor(new CruiseControlController());
    }

    public void testInvalid() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("<cruisecontrol>");
            sb.append("<project name=\"test\" foo=\"foo\"></project>");
            sb.append("</cruisecontrol>");
            
            adaptor.validateConfig(sb.toString());
            fail("No exception found");
        } catch (CruiseControlException cce) {
            // expected
        }
    }

    public void testValid() throws Exception {
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("<cruisecontrol>");
            sb.append("<project name=\"test\">");
            sb.append("<modificationset><cvs localworkingcopy=\".\"/></modificationset>");
            sb.append("<schedule><ant/></schedule>");
            sb.append("</project>");
            sb.append("</cruisecontrol>");
            
            adaptor.validateConfig(sb.toString());

        } catch (CruiseControlException cce) {
            fail("Validation failed on valid config, reason: " + cce.getMessage());
        }
    }
    
    public void testShouldGetAvailableBootstrappers() {
        BootstrapperDetail[] bootstrappers = adaptor.getAvailableBootstrappers();
        assertEquals(12, bootstrappers.length);
        assertEquals("accurevbootstrapper", bootstrappers[0].getPluginName());
        assertEquals("cvsbootstrapper", bootstrappers[7].getPluginName());
        assertEquals("svnbootstrapper", bootstrappers[10].getPluginName());
    }

    public void testShouldGetAvailablePublishers() {
        PublisherDetail[] publishers = adaptor.getAvailablePublishers();
        assertEquals(17, publishers.length);
    }
    
    public void testShouldGetAvailableSourceControls() {
        SourceControlDetail[] srcControls = adaptor.getAvailableSourceControls();
        assertEquals(18, srcControls.length);
        assertEquals("accurev", srcControls[0].getPluginName());
        assertEquals("cvs", srcControls[7].getPluginName());
        assertEquals("svn", srcControls[15].getPluginName());
    }
    
    public void testShouldGetAllAvailablePlugins() {
        assertEquals(47, adaptor.getAvailablePlugins().length);
    }
}

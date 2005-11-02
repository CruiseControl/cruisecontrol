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

/**
 * @author alwick
 */
public class CruiseControlControllerJMXAdaptorTest extends TestCase {

    public void testInvalid() throws Exception {
        
        CruiseControlControllerJMXAdaptor theTestAdaptor = 
            new CruiseControlControllerJMXAdaptor(new CruiseControlController());
        
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("<cruisecontrol>");
            sb.append("<project name=\"test\" foo=\"foo\"></project>");
            sb.append("</cruisecontrol>");
            
            theTestAdaptor.validateConfig(sb.toString());
            fail("No exception found");
        } catch (CruiseControlException cce) {
            // expected
        }
    }

    public void testValid() throws Exception {
        
        CruiseControlControllerJMXAdaptor theTestAdaptor = 
            new CruiseControlControllerJMXAdaptor(new CruiseControlController());
        
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("<cruisecontrol>");
            sb.append("<project name=\"test\">");
            sb.append("<modificationset><cvs localworkingcopy=\".\"/></modificationset>");
            sb.append("<schedule><ant/></schedule>");
            sb.append("</project>");
            sb.append("</cruisecontrol>");
            
            theTestAdaptor.validateConfig(sb.toString());

        } catch (CruiseControlException cce) {
            fail("Validation failed on valid config, reason: " + cce.getMessage());
        }
    }
}

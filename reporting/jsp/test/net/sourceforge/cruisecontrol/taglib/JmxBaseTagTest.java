package net.sourceforge.cruisecontrol.taglib;

import junit.framework.TestCase;

import java.net.URL;

import net.sourceforge.cruisecontrol.mock.MockServletRequest;
import net.sourceforge.cruisecontrol.mock.MockPageContext;
import net.sourceforge.cruisecontrol.mock.MockServletContext;

/**
 * @author Dan Rollo
 * Date: Apr 23, 2007
 * Time: 1:17:22 PM
 */
public class JmxBaseTagTest extends TestCase {

    private JmxBaseTag jmxBaseTag;
    private MockPageContext pageContext;

    public void setUp() {
        jmxBaseTag = new JmxBaseTag();
        pageContext = new MockPageContext();
        jmxBaseTag.setPageContext(pageContext);
        MockServletRequest request = new MockServletRequest("context", "servlet");
        pageContext.setHttpServletRequest(request);
    }


    public void testCreateJmxUrlPort() throws Exception {
        final URL url = jmxBaseTag.createJmxUrl();
        assertNotNull(url);
        assertEquals("Wrong jmx port", JmxBaseTag.DEFAULT_JMX_PORT, url.getPort());
    }

    public void testCreateJmxUrlPortOverride() throws Exception {
        // set a JMXPORT param
        final int testJmxPort = 8888;
        ((MockServletContext) pageContext.getServletContext()).setInitParameter(JmxBaseTag.JMX_PORT, testJmxPort + "");
        assertEquals("Wrong jmx port", testJmxPort, jmxBaseTag.createJmxUrl().getPort());
    }

    public void testCreateJmxUrlPortUnresolved() throws Exception {
        // set a bad (unresolved) JMXPORT param
        ((MockServletContext) pageContext.getServletContext()).setInitParameter(JmxBaseTag.JMX_PORT, "@JMXPORT");
        assertEquals("Wrong jmx port", JmxBaseTag.DEFAULT_JMX_PORT, jmxBaseTag.createJmxUrl().getPort());
    }
}

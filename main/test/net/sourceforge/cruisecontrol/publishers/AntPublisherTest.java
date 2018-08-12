/*
 * Created on Mar 16, 2005
 */
package net.sourceforge.cruisecontrol.publishers;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.jdom2.Element;


/**
 * @author Jeffrey Fredrick
 */
public class AntPublisherTest extends TestCase {

    public void testValidate() throws Exception {
        AntPublisher publisher = new AntPublisher();
        publisher.validate();
    }

    public void testPopulatePropertiesForAntBuilder() {
        final Element successfulBuild = TestUtil.createElement(true, true);
        final Element failedBuild = TestUtil.createElement(false, false);
        final Map<String, String> properties = new HashMap<String, String>();
        final AntPublisher publisher = new AntPublisher();
        
        publisher.populatePropertesForAntBuilder(successfulBuild, properties);
        assertTrue("true".equals(properties.get("thisbuildsuccessful")));

        publisher.populatePropertesForAntBuilder(failedBuild, properties);
        assertTrue("false".equals(properties.get("thisbuildsuccessful")));
    }
    
}

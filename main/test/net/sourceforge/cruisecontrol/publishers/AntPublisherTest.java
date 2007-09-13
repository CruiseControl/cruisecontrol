/*
 * Created on Mar 16, 2005
 */
package net.sourceforge.cruisecontrol.publishers;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import org.jdom.Element;


/**
 * @author Jeffrey Fredrick
 */
public class AntPublisherTest extends TestCase {

    public void testValidate() throws Exception {
        AntPublisher publisher = new AntPublisher();
        publisher.validate();
    }

    public void testPopulatePropertiesForAntBuilder() {
        Element successfulBuild = TestUtil.createElement(true, true);
        Element failedBuild = TestUtil.createElement(false, false);
        Map properties = new HashMap();
        AntPublisher publisher = new AntPublisher();
        
        publisher.populatePropertesForAntBuilder(successfulBuild, properties);
        assertTrue("true".equals(properties.get("thisbuildsuccessful")));

        publisher.populatePropertesForAntBuilder(failedBuild, properties);
        assertTrue("false".equals(properties.get("thisbuildsuccessful")));
    }
    
}

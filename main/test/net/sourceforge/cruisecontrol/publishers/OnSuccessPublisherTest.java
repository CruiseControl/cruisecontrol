/*
 * Created on Mar 24, 2005
 */
package net.sourceforge.cruisecontrol.publishers;

import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import junit.framework.TestCase;

/**
 * @author Jeffrey Fredrick
 */
public class OnSuccessPublisherTest extends TestCase {
    
    public void testAddPublisher() throws CruiseControlException {
        OnSuccessPublisher publisher = new OnSuccessPublisher();
        MyMockPublisher mock = new MyMockPublisher();
        publisher.add(mock);
        publisher.validate();
        assertTrue(mock.wasValidated());
    }
    
    public void testPublish() throws CruiseControlException {
        OnSuccessPublisher publisher = new OnSuccessPublisher();
        MyMockPublisher mock = new MyMockPublisher();
        publisher.add(mock);
        
        Element successfulBuild = TestUtil.createElement(true, false);
        publisher.publish(successfulBuild);
        assertTrue(mock.wasPublished());
        
        mock.published = false;
        Element failedBuild = TestUtil.createElement(false, true);
        publisher.publish(failedBuild);
        assertFalse(mock.wasPublished());
    }
    
    class MyMockPublisher implements Publisher {
        private boolean validated = false;
        private boolean published = false;
        
        boolean wasValidated() {
            return validated;
        }
        
        boolean wasPublished() {
            return published;
        }
        
        public void validate() throws CruiseControlException {
            validated = true;
        }
        
        public void publish(Element cruisecontrolLog) throws CruiseControlException {
            published = true;
        }
    }

}

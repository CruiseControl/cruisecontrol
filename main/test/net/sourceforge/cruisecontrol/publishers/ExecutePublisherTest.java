package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

public class ExecutePublisherTest extends TestCase {

    public ExecutePublisherTest(String name) {
        super(name);
    }

    public void testValidate() {
        ExecutePublisher publisher = new ExecutePublisher();
        try {
            publisher.validate();
            fail("ExecutePublisher should throw exceptions when required fields are not set.");
        } catch (CruiseControlException cce) {
        }

        publisher.setCommand("command");

        try {
            publisher.validate();
        } catch (CruiseControlException e) {
            fail("ExecutePublisher should not throw exceptions when required fields are set.");
        }
    }
}

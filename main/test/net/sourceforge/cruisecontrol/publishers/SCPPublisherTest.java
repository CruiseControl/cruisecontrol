package net.sourceforge.cruisecontrol.publishers;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import java.io.File;

public class SCPPublisherTest extends TestCase {

    public SCPPublisherTest(String name) {
        super(name);
    }

    public void testValidate() {
        SCPPublisher publisher = new SCPPublisher();
        publisher.setSourceUser("user1");

        try {
            publisher.validate();
            fail("SCPPublisher should throw exceptions when only user is set.");
        } catch (CruiseControlException e) {
        }

        publisher.setSourceUser(null);
        publisher.setSourceHost("host1");

        try {
            publisher.validate();
            fail("SCPPublisher should throw exceptions when only host is set.");
        } catch (CruiseControlException e) {
        }

        publisher.setSourceUser("user1");
        publisher.setSourceHost("host1");

        publisher.setSourceUser(null);
        publisher.setSourceHost("host1");

        try {
            publisher.validate();
            fail("SCPPublisher should throw exceptions when only user is set.");
        } catch (CruiseControlException e) {
        }
    }

    public void testCreateCommandline() {
        SCPPublisher publisher = new SCPPublisher();

        publisher.setSourceUser("user1");
        publisher.setSourceHost("host1");
        publisher.setTargetUser("user2");
        publisher.setTargetHost("host2");
        assertEquals("scp -S ssh user1@host1:." + File.separator + "filename " +
                     "user2@host2:." + File.separator + "filename",
                     publisher.createCommandline("filename").toString());

        publisher.setOptions("-P 1000");
        assertEquals("scp -P 1000 -S ssh user1@host1:." + File.separator + "filename " +
                     "user2@host2:." + File.separator + "filename",
                     publisher.createCommandline("filename").toString());

        publisher.setSSH("plink");
        assertEquals("scp -P 1000 -S plink user1@host1:." + File.separator + "filename " +
                     "user2@host2:." + File.separator + "filename",
                     publisher.createCommandline("filename").toString());

        publisher.setTargetDir(File.separator + "home" + File.separator + "httpd");
        assertEquals("scp -P 1000 -S plink user1@host1:." + File.separator + "filename " +
                     "user2@host2:" + File.separator + "home" + File.separator + "httpd" +
                     File.separator + "filename",
                     publisher.createCommandline("filename").toString());

    }
}

package net.sourceforge.cruisecontrol.publishers.sfee;

import com.vasoftware.sf.soap42.webservices.ClientSoapStubFactory;
import com.vasoftware.sf.soap42.webservices.sfmain.ISourceForgeSoap;
import com.vasoftware.sf.soap42.webservices.sfmain.ProjectSoapList;
import com.vasoftware.sf.soap42.webservices.sfmain.ProjectSoapRow;
import com.vasoftware.sf.soap42.webservices.tracker.ArtifactSoapList;
import com.vasoftware.sf.soap42.webservices.tracker.ArtifactSoapRow;
import com.vasoftware.sf.soap42.webservices.tracker.ITrackerAppSoap;
import com.vasoftware.sf.soap42.webservices.tracker.TrackerSoapList;
import com.vasoftware.sf.soap42.webservices.tracker.TrackerSoapRow;
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import org.jdom.Element;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.RemoteException;

public class SfeeTrackerPublisherTest extends TestCase {

    private static final String SERVER_URL = "http://tapestry.sourceforge.vasoftware.com";
    private static final String USERNAME = "foo";
    private static final String PASSWORD = "bar";
    private static final String PROJECT_NAME = "baz";

    public void setUp() {
        //Instantiate the in-memory stub implementation of SFEE using reflection so that
        //  this class will still compile and run when the REAL implementation of SFEE is used.
        try {
            Class inMemSfeeFactoryClass = Class.forName("com.vasoftware.sf.InMemorySfeeFactory");
            Method resetMethod = inMemSfeeFactoryClass.getMethod("reset", null);
            resetMethod.invoke(null, null);
            Method createMethod = inMemSfeeFactoryClass
                    .getMethod("create", new Class[]{String.class, String.class, String.class});

            Object inMemSfee = createMethod.invoke(null, new Object[]{SERVER_URL, USERNAME, PASSWORD});
            Method addProjectMethod = inMemSfee.getClass().getMethod("addProject", new Class[]{String.class});
            addProjectMethod.invoke(inMemSfee, new Object[]{PROJECT_NAME});

            Method addTracker = inMemSfee.getClass().getMethod("addTracker", new Class[]{String.class, String.class});
            addTracker.invoke(inMemSfee, new Object[]{"UnitTestStatistics", PROJECT_NAME});
        } catch (NoSuchMethodException e) {
            fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (IllegalAccessException e) {
            fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (InvocationTargetException e) {
            fail("Must be using the wrong version of the sfee soap stubs.");
        } catch (ClassNotFoundException e) {
            //Must be running with the real SFEE implementation, which does NOT contain
            // the InMemorySfeeFactory class. So, we can ignore this
            //  exception and go on with the test.
        }
    }

    public void testCanPublishStaticValuesToTracker() throws CruiseControlException, RemoteException {
        SfeeTrackerPublisher publisher = new SfeeTrackerPublisher();
        publisher.setTrackerName("UnitTestStatistics");
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);

        SfeeTrackerPublisher.TrackerChildElement title = publisher.createTitle();
        assertNotNull("createTitle should never return null.", title);
        title.setValue("Testing");

        SfeeTrackerPublisher.TrackerChildElement description = publisher.createDescription();
        assertNotNull("createDescription should never return null.", title);
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);

        SfeeTrackerPublisher.TrackerChildElement status = publisher.createStatus();
        status.setValue("Open");

        SfeeTrackerPublisher.Field field = publisher.createField();
        field.setName("BrokenUnitTests");
        field.setValue("0");

        field = publisher.createField();
        field.setName("SuccessfulUnitTests");
        field.setValue("0");

        field = publisher.createField();
        field.setName("TotalNumberOfUnitTests");
        field.setValue("0");

        publisher.validate();
        publisher.publish(null);

        assertTrackerArtifactCreated(descriptionValue);
    }

    public void testValidate() throws Exception {
        SfeeTrackerPublisher publisher = new SfeeTrackerPublisher();
        assertNotValidatable(publisher);
        publisher.setTrackerName("UnitTestStatistics");
        assertNotValidatable(publisher);
        publisher.setServerURL(SERVER_URL);
        assertNotValidatable(publisher);
        publisher.setUsername(USERNAME);
        assertNotValidatable(publisher);
        publisher.setPassword(PASSWORD);
        assertNotValidatable(publisher);
        publisher.setProjectName(PROJECT_NAME);

        assertNotValidatable(publisher);

        SfeeTrackerPublisher.TrackerChildElement title = publisher.createTitle();
        assertNotNull("createTitle should never return null.", title);
        title.setValue("Testing");
        assertNotValidatable(publisher);

        SfeeTrackerPublisher.TrackerChildElement description = publisher.createDescription();
        assertNotNull("createDescription should never return null.", title);
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);
        assertNotValidatable(publisher);

        SfeeTrackerPublisher.TrackerChildElement status = publisher.createStatus();
        status.setValue("Open");

        publisher.validate();
    }

    public void testValidateFields() throws CruiseControlException {
        SfeeTrackerPublisher.Field field = new SfeeTrackerPublisher.Field();
        try {
            field.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        field.setValue("foo");
        try {
            field.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        field.setName("bar");
        field.validate();
    }

    public void testProjectNameNotFound() throws CruiseControlException {
        SfeeTrackerPublisher publisher = new SfeeTrackerPublisher();
        publisher.setTrackerName("UnitTestStatistics");
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        String projectName = "NON-EXISTENT PROJECT " + System.currentTimeMillis();
        publisher.setProjectName(projectName);

        SfeeTrackerPublisher.TrackerChildElement title = publisher.createTitle();
        title.setValue("Testing");

        SfeeTrackerPublisher.TrackerChildElement description = publisher.createDescription();
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);

        SfeeTrackerPublisher.TrackerChildElement status = publisher.createStatus();
        status.setValue("Open");

        publisher.validate();
        try {
            publisher.publish(null);
            fail("Expected an exception for a non-existent project name");
        } catch (CruiseControlException expected) {
            assertTrue(expected.getMessage().indexOf("projectName [" + projectName + "] not found") >= 0);
        }
    }

    public void testTrackerNameNotFound() throws CruiseControlException {
        SfeeTrackerPublisher publisher = new SfeeTrackerPublisher();
        String trackerName = "Non-Existent Tracker Name" + System.currentTimeMillis();
        publisher.setTrackerName(trackerName);
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);

        SfeeTrackerPublisher.TrackerChildElement title = publisher.createTitle();
        title.setValue("Testing");

        SfeeTrackerPublisher.TrackerChildElement description = publisher.createDescription();
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);

        SfeeTrackerPublisher.TrackerChildElement status = publisher.createStatus();
        status.setValue("Open");

        publisher.validate();
        try {
            publisher.publish(null);
            fail("Expected an exception for a non-existent tracker name");
        } catch (CruiseControlException expected) {
            assertTrue(expected.getMessage().indexOf("trackerName [" + trackerName + "] not found") >= 0);
        }
    }

    public void testFieldXPathExpression() throws CruiseControlException {
        String xmlDocument = "<foo><bar>baz</bar></foo>";
        String bazXPath = "/foo/bar/text()";

        SfeeTrackerPublisher.TrackerChildElement xpathField = new SfeeTrackerPublisher.TrackerChildElement();
        xpathField.setXPathExpression(bazXPath);
        xpathField.setInputStream(new ByteArrayInputStream(xmlDocument.getBytes()));

        assertEquals("baz", xpathField.getValue());
    }

    public void testXPathExpressionAndValueSetOnField() {
        SfeeTrackerPublisher.TrackerChildElement xpathField = new SfeeTrackerPublisher.TrackerChildElement();
        xpathField.setValue("foo");
        xpathField.setXPathExpression("bar");

        try {
            xpathField.validate();
            fail("Expected a validation exception");
        } catch (CruiseControlException expected) {
        }
    }

    public void testXPathExpressionAndValueSetOnDescription() {
        SfeeTrackerPublisher.TrackerChildElement description = new SfeeTrackerPublisher.TrackerChildElement();
        description.setValue("foo");
        description.setXPathExpression("bar");

        try {
            description.validate();
            fail("Expected a validation exception");
        } catch (CruiseControlException expected) {
        }
    }

    public void testXMLFileShouldDefaultToLog() throws CruiseControlException {
        String bazXPath = "/cruisecontrol/info/property[@name='builddate']/@value";

        SfeeTrackerPublisher.TrackerChildElement xpathField = new SfeeTrackerPublisher.TrackerChildElement();
        xpathField.setXPathExpression(bazXPath);
        Element log = TestUtil.createElement(true, true);
        xpathField.setCurrentLog(log);

        assertNotNull(log.getDocument());

        assertEquals("11/30/2005 12:07:27", xpathField.getValue());
    }

    public void testFieldFailsIfCurrentLogNotSet() {
        SfeeTrackerPublisher.TrackerChildElement xpathField = new SfeeTrackerPublisher.TrackerChildElement();
        xpathField.setXPathExpression("foo");

        try {
            xpathField.getValue();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
            assertTrue(expected.getMessage().indexOf("current cruisecontrol log not set") >= 0);
        }
    }

    public void testPublishWithXPath() throws CruiseControlException, RemoteException {
        SfeeTrackerPublisher publisher = new SfeeTrackerPublisher();
        publisher.setTrackerName("UnitTestStatistics");
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);

        SfeeTrackerPublisher.TrackerChildElement title = publisher.createTitle();
        assertNotNull("createTitle should never return null.", title);
        title.setValue("Testing");

        SfeeTrackerPublisher.TrackerChildElement description = publisher.createDescription();
        assertNotNull("createDescription should never return null.", title);
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);

        SfeeTrackerPublisher.TrackerChildElement status = publisher.createStatus();
        status.setValue("Open");

        SfeeTrackerPublisher.Field field = publisher.createField();
        field.setName("BrokenUnitTests");
        field.setValue("0");

        field = publisher.createField();
        field.setName("SuccessfulUnitTests");
        field.setValue("0");

        field = publisher.createField();
        field.setName("TotalNumberOfUnitTests");
        field.setXPathExpression("1+2");

        publisher.validate();
        publisher.publish(TestUtil.createElement(true, true));

        assertTrackerArtifactCreated(descriptionValue);
    }

    private void assertNotValidatable(SfeeTrackerPublisher publisher) {
        try {
            publisher.validate();
            fail("Publisher should not be valid.");
        } catch (CruiseControlException expected) {

        }
    }

    private void assertTrackerArtifactCreated(String uniqueDescription) throws RemoteException {
        ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory
                .getSoapStub(ISourceForgeSoap.class, SERVER_URL);
        String sessionID = soap.login(USERNAME, PASSWORD);
        ProjectSoapList projectList = soap.getProjectList(sessionID);
        ProjectSoapRow[] rows = projectList.getDataRows();
        String projectID = null;
        for (int i = 0; i < rows.length; i++) {
            ProjectSoapRow nextProjectRow = rows[i];
            if (nextProjectRow.getTitle().equals(PROJECT_NAME)) {
                projectID = nextProjectRow.getId();
            }
        }
        assertNotNull(projectID);

        ITrackerAppSoap tracker = (ITrackerAppSoap) ClientSoapStubFactory
                .getSoapStub(ITrackerAppSoap.class, SERVER_URL);
        TrackerSoapList trackerList = tracker.getTrackerList(sessionID, projectID);
        TrackerSoapRow[] trackerListRows = trackerList.getDataRows();
        String trackerID = null;
        for (int i = 0; i < trackerListRows.length; i++) {
            TrackerSoapRow trackerListRow = trackerListRows[i];
            if (trackerListRow.getTitle().equals("UnitTestStatistics")) {
                trackerID = trackerListRow.getId();
            }
        }
        assertNotNull(trackerID);

        ArtifactSoapList artifactList = tracker.getArtifactList(sessionID, trackerID, null);
        ArtifactSoapRow[] artifactDataRows = artifactList.getDataRows();
        boolean found = false;
        for (int i = 0; i < artifactDataRows.length; i++) {
            ArtifactSoapRow nextRow = artifactDataRows[i];
            if (nextRow.getDescription().equals(uniqueDescription)) {
                found = true;
            }
        }
        assertTrue("Didn't find the tracker artifact with description [" + uniqueDescription + "]", found);
    }

}

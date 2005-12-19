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
import net.sourceforge.cruisecontrol.util.XPathAwareChild;
import net.sourceforge.cruisecontrol.util.NamedXPathAwareChild;
import net.sourceforge.cruisecontrol.testutil.TestUtil;

import java.rmi.RemoteException;

public class SfeeTrackerPublisherTest extends TestCase {

    private static final String SERVER_URL = "http://tapestry.sourceforge.vasoftware.com";
    private static final String USERNAME = "foo";
    private static final String PASSWORD = "bar";
    private static final String PROJECT_NAME = "CC Integration";

    public void setUp() {
        SfeeTestUtils util = new SfeeTestUtils();
        util.loadSfeeInMemory(SERVER_URL, USERNAME, PASSWORD);
        util.addProject(PROJECT_NAME);
        util.addTracker("UnitTestStatistics", PROJECT_NAME);
    }

    public void testCanPublishStaticValuesToTracker() throws CruiseControlException, RemoteException {
        SfeeTrackerPublisher publisher = new SfeeTrackerPublisher();
        publisher.setTrackerName("UnitTestStatistics");
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);

        XPathAwareChild title = publisher.createTitle();
        assertNotNull("createTitle should never return null.", title);
        title.setValue("Testing");

        XPathAwareChild description = publisher.createDescription();
        assertNotNull("createDescription should never return null.", title);
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);

        XPathAwareChild status = publisher.createStatus();
        status.setValue("Open");

        NamedXPathAwareChild field = publisher.createField();
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

        XPathAwareChild title = publisher.createTitle();
        assertNotNull("createTitle should never return null.", title);
        title.setValue("Testing");
        assertNotValidatable(publisher);

        XPathAwareChild description = publisher.createDescription();
        assertNotNull("createDescription should never return null.", title);
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);
        assertNotValidatable(publisher);

        XPathAwareChild status = publisher.createStatus();
        status.setValue("Open");

        publisher.validate();
    }

    public void testProjectNameNotFound() throws CruiseControlException {
        SfeeTrackerPublisher publisher = new SfeeTrackerPublisher();
        publisher.setTrackerName("UnitTestStatistics");
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        String projectName = "NON-EXISTENT PROJECT " + System.currentTimeMillis();
        publisher.setProjectName(projectName);

        XPathAwareChild title = publisher.createTitle();
        title.setValue("Testing");

        XPathAwareChild description = publisher.createDescription();
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);

        XPathAwareChild status = publisher.createStatus();
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

        XPathAwareChild title = publisher.createTitle();
        title.setValue("Testing");

        XPathAwareChild description = publisher.createDescription();
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);

        XPathAwareChild status = publisher.createStatus();
        status.setValue("Open");

        publisher.validate();
        try {
            publisher.publish(null);
            fail("Expected an exception for a non-existent tracker name");
        } catch (CruiseControlException expected) {
            assertTrue(expected.getMessage().indexOf("trackerName [" + trackerName + "] not found") >= 0);
        }
    }

    public void testPublishWithXPath() throws CruiseControlException, RemoteException {
        SfeeTrackerPublisher publisher = new SfeeTrackerPublisher();
        publisher.setTrackerName("UnitTestStatistics");
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);

        XPathAwareChild title = publisher.createTitle();
        assertNotNull("createTitle should never return null.", title);
        title.setValue("Testing");

        XPathAwareChild description = publisher.createDescription();
        assertNotNull("createDescription should never return null.", title);
        String descriptionValue = "Testing @ " + System.currentTimeMillis();
        description.setValue(descriptionValue);

        XPathAwareChild status = publisher.createStatus();
        status.setValue("Open");

        NamedXPathAwareChild field = publisher.createField();
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

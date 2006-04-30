package net.sourceforge.cruisecontrol.publishers.sfee;

import com.vasoftware.sf.soap42.webservices.ClientSoapStubFactory;
import com.vasoftware.sf.soap42.webservices.filestorage.IFileStorageAppSoap;
import com.vasoftware.sf.soap42.webservices.frs.FrsFileSoapList;
import com.vasoftware.sf.soap42.webservices.frs.FrsFileSoapRow;
import com.vasoftware.sf.soap42.webservices.frs.IFrsAppSoap;
import com.vasoftware.sf.soap42.webservices.sfmain.ISourceForgeSoap;
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.testutil.UnreadableMockFile;
import org.apache.tools.ant.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import javax.activation.DataHandler;

public class SFEEFrsPublisherTest extends TestCase {
    private static final String SERVER_URL = "http://tapestry.sourceforge.vasoftware.com";
    private static final String USERNAME = "foo";
    private static final String PASSWORD = "bar";
    private static final String RELEASE_ID = "rel1509";

    public void setUp() {
        new SfeeTestUtils().loadSfeeInMemory(SERVER_URL, USERNAME, PASSWORD);
    }

    public void testIsCorrectType() throws Exception {
        assertTrue(Publisher.class.isAssignableFrom(SfeeFrsPublisher.class));
    }

    public void testSimpleUpload() throws IOException, CruiseControlException {
        SfeeFrsPublisher publisher = new SfeeFrsPublisher();

        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setReleaseID(RELEASE_ID);

        final File tempFile = File.createTempFile(SFEEFrsPublisherTest.class.getName(), "temp");
        tempFile.deleteOnExit();
        publisher.setFile(tempFile.getAbsolutePath());

        publisher.validate();
        //The cruise log information isn't used by this task.
        publisher.publish(null);

        assertFileExistsInRelease(tempFile.getName(), SERVER_URL, USERNAME, PASSWORD, RELEASE_ID);
    }

    public void testMissingRequiredFields() throws CruiseControlException {
        SfeeFrsPublisher publisher = new SfeeFrsPublisher();

        try {
            publisher.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        publisher.setServerURL(SERVER_URL);
        try {
            publisher.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        publisher.setPassword(PASSWORD);
        try {
            publisher.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        publisher.setUsername(USERNAME);
        try {
            publisher.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        publisher.setReleaseID(RELEASE_ID);
        try {
            publisher.validate();
            fail("Expected an exception");
        } catch (CruiseControlException expected) {
        }

        publisher.setFile("foo");
        publisher.validate();
    }

    public void testPublishNonExistentFile() throws CruiseControlException {
        SfeeFrsPublisher publisher = new SfeeFrsPublisher();

        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setReleaseID(RELEASE_ID);
        publisher.setFile("THISFILEDOESNTEXIST" + System.currentTimeMillis());

        publisher.validate();

        try {
            publisher.publish(null);
            fail("Expected an exception because the file doesn't exist.");
        } catch (CruiseControlException expected) {
        }
    }

    public void testPublishUnreadableFile() throws CruiseControlException {
        //A file that exists, but isn't readable
        final UnreadableMockFile unreadableFile = new UnreadableMockFile();

        SfeeFrsPublisher publisher = new SfeeFrsPublisher() {
            protected File getFile() {
                return unreadableFile;
            }
        };
        
        publisher.setFile("mocked out");

        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setReleaseID(RELEASE_ID);

        publisher.validate();

        try {
            publisher.publish(null);
            fail("Expected an exception because the file isn't readable.");
        } catch (CruiseControlException expected) {
        }

        assertTrue(unreadableFile.canReadWasCalled());
    }

    public void testPublishDirectoryInsteadOfFile() throws CruiseControlException {
        SfeeFrsPublisher publisher = new SfeeFrsPublisher();

        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setReleaseID(RELEASE_ID);
        publisher.setFile(System.getProperty("java.io.tmpdir"));

        publisher.validate();

        try {
            publisher.publish(null);
            fail("Expected an exception because cannot publish directories.");
        } catch (CruiseControlException expected) {
        }
    }

    public void testUploadFileWithNewName() throws IOException, CruiseControlException {
        SfeeFrsPublisher publisher = new SfeeFrsPublisher();

        String uploadname = "differentFromLocalFilename" + System.currentTimeMillis();
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setReleaseID(RELEASE_ID);
        publisher.setUploadname(uploadname);

        final File tempFile = File.createTempFile(SFEEFrsPublisherTest.class.getName(), "temp");
        tempFile.deleteOnExit();
        publisher.setFile(tempFile.getAbsolutePath());

        publisher.validate();
        publisher.publish(null);

        assertFileExistsInRelease(uploadname, SERVER_URL, USERNAME, PASSWORD, RELEASE_ID);
    }

    private static void assertFileExistsInRelease(String filename, String serverUrl, String username, String password,
            String releaseId) throws RemoteException {
        ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory.getSoapStub(ISourceForgeSoap.class, serverUrl);
        String sessionID = soap.login(username, password);

        IFrsAppSoap frsApp = (IFrsAppSoap) ClientSoapStubFactory.getSoapStub(IFrsAppSoap.class, serverUrl);
        FrsFileSoapList fileList = frsApp.getFrsFileList(sessionID, releaseId);
        FrsFileSoapRow[] files = fileList.getDataRows();
        boolean found = false;
        for (int i = 0; i < files.length && !found; i++) {
            FrsFileSoapRow nextFile = files[ i ];
            if (nextFile.getFilename().equals(filename)) {
                found = true;
            }
        }
        assertTrue("File wasn't found in release.", found);
    }

    public void testUploadShouldOverwrite() throws IOException, CruiseControlException {
        SfeeFrsPublisher publisher = new SfeeFrsPublisher();

        String uploadname = "fileShouldOverWrite.txt";
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setReleaseID(RELEASE_ID);
        publisher.setUploadname(uploadname);

        final File tempFile = File.createTempFile(SFEEFrsPublisherTest.class.getName(), "temp");
        tempFile.deleteOnExit();
        TestUtil.write("run 1", tempFile);
        publisher.setFile(tempFile.getAbsolutePath());

        publisher.validate();
        publisher.publish(null);
        assertOneFileExistsInRelease(uploadname, SERVER_URL, USERNAME, PASSWORD, RELEASE_ID);

        TestUtil.write("run 2", tempFile);
        publisher.publish(null);
        assertOneFileExistsInRelease(uploadname, SERVER_URL, USERNAME, PASSWORD, RELEASE_ID);
        String contents = getReleaseFileContents(uploadname, SERVER_URL, USERNAME, PASSWORD, RELEASE_ID);
        assertTrue("File didn't get overwritten", contents.indexOf("run 2") >= 0);
    }

    private void assertOneFileExistsInRelease(String filename, String serverUrl, String username, String password,
            String releaseId) throws RemoteException {
        ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory.getSoapStub(ISourceForgeSoap.class, serverUrl);
        String sessionID = soap.login(username, password);

        IFrsAppSoap frsApp = (IFrsAppSoap) ClientSoapStubFactory.getSoapStub(IFrsAppSoap.class, serverUrl);
        FrsFileSoapList fileList = frsApp.getFrsFileList(sessionID, releaseId);
        FrsFileSoapRow[] files = fileList.getDataRows();
        int count = 0;
        for (int i = 0; i < files.length; i++) {
            FrsFileSoapRow nextFile = files[ i ];
            if (nextFile.getFilename().equals(filename)) {
                count++;
            }
        }
        assertTrue("File was found [" + count + "] times in release. Expected once.", count == 1);
    }

    private String getReleaseFileContents(String filename, String serverUrl, String username, String password,
            String releaseId) throws IOException {
        ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory.getSoapStub(ISourceForgeSoap.class, serverUrl);
        String sessionID = soap.login(username, password);

        IFrsAppSoap frsApp = (IFrsAppSoap) ClientSoapStubFactory.getSoapStub(IFrsAppSoap.class, serverUrl);
        FrsFileSoapList fileList = frsApp.getFrsFileList(sessionID, releaseId);
        FrsFileSoapRow[] files = fileList.getDataRows();
        boolean found = false;
        for (int i = 0; i < files.length && !found; i++) {
            FrsFileSoapRow nextFile = files[ i ];
            if (nextFile.getFilename().equals(filename)) {
                String fileID = nextFile.getId();
                IFileStorageAppSoap fileStorageApp = (IFileStorageAppSoap) ClientSoapStubFactory
                        .getSoapStub(IFileStorageAppSoap.class, serverUrl);

                DataHandler handler = fileStorageApp.downloadFile(sessionID, frsApp.getFrsFileId(sessionID, fileID));
                return FileUtils.readFully(new InputStreamReader(handler.getInputStream()));
            }
        }

        return null;
    }


}

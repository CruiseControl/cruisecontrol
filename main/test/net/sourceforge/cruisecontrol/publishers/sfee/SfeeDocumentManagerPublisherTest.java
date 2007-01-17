/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2005, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/
package net.sourceforge.cruisecontrol.publishers.sfee;

import com.vasoftware.sf.soap42.webservices.ClientSoapStubFactory;
import com.vasoftware.sf.soap42.webservices.docman.DocumentFolderSoapDO;
import com.vasoftware.sf.soap42.webservices.docman.DocumentSoapDO;
import com.vasoftware.sf.soap42.webservices.docman.DocumentSoapList;
import com.vasoftware.sf.soap42.webservices.docman.DocumentSoapRow;
import com.vasoftware.sf.soap42.webservices.docman.IDocumentAppSoap;
import com.vasoftware.sf.soap42.webservices.filestorage.IFileStorageAppSoap;
import com.vasoftware.sf.soap42.webservices.sfmain.ISourceForgeSoap;
import com.vasoftware.sf.soap42.webservices.sfmain.ProjectSoapList;
import com.vasoftware.sf.soap42.webservices.sfmain.ProjectSoapRow;
import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.rmi.RemoteException;

public class SfeeDocumentManagerPublisherTest extends TestCase {
    private static final String SERVER_URL = "http://tapestry.sourceforge.vasoftware.com";
    private static final String USERNAME = "foo";
    private static final String PASSWORD = "bar";
    private static final String PROJECT_NAME = "CC Integration";
    private String sessionId;
    private String projectId;

    public void setUp() throws Exception {
        SfeeTestUtils testUtil = new SfeeTestUtils();
        testUtil.loadSfeeInMemory(SERVER_URL, USERNAME, PASSWORD);
        testUtil.addProject(PROJECT_NAME);
        testUtil.createFolders(PROJECT_NAME, "/Root Folder/level1/level2");

        ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory
                .getSoapStub(ISourceForgeSoap.class, SERVER_URL);
        sessionId = soap.login(USERNAME, PASSWORD);
        projectId = getProjectId(soap, sessionId);
    }

    public void testSimpleUpload() throws IOException, CruiseControlException {
        String title = getClass().getName() + System.currentTimeMillis() + "Document.txt";
        String description = "This document was created by a unit test at " + System.currentTimeMillis();
        String expectedContent = "testing at " + System.currentTimeMillis();
        String documentPath = "/Root Folder/level1";
        String versionComment = "This is a version created at " + System.currentTimeMillis();
        DocumentStatus status = DocumentStatus.FINAL;

        SfeeDocumentManagerPublisher publisher = new SfeeDocumentManagerPublisher();
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);
        publisher.setFolder("/Root Folder/level1");

        publisher.setData(new StringDataSource(expectedContent, title));
        publisher.createDocumentName().setValue(title);
        publisher.createDescription().setValue(description);
        publisher.createStatus().setValue(status.getName());
        publisher.createVersionComment().setValue(versionComment);
        publisher.setLock(true);

        publisher.validate();
        publisher.publish(null);


        assertDocumentCreated(publisher, documentPath, title, description, expectedContent, status, versionComment);
    }

    public void testPublishingSameDocName() throws CruiseControlException, RemoteException {
        String title = getClass().getName() + System.currentTimeMillis() + "Document.txt";
        String description = "This document was created by a unit test at " + System.currentTimeMillis();
        String expectedContent = "testing at " + System.currentTimeMillis();
        String documentPath = "/Root Folder/level1";
        String versionComment = "This is a version created at " + System.currentTimeMillis();
        DocumentStatus status = DocumentStatus.FINAL;

        SfeeDocumentManagerPublisher publisher = new SfeeDocumentManagerPublisher();
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);
        publisher.setFolder("/Root Folder/level1");

        publisher.setData(new StringDataSource(expectedContent, title));
        publisher.createDocumentName().setValue(title);
        publisher.createDescription().setValue(description);
        publisher.createStatus().setValue(status.getName());
        publisher.createVersionComment().setValue(versionComment);
        publisher.setLock(true);

        publisher.validate();
        publisher.publish(null);

        assertCurrentDocumentVersion(1, title, documentPath, publisher);

        publisher.publish(null);
        assertCurrentDocumentVersion(2, title, documentPath, publisher);

        publisher.publish(null);
        assertCurrentDocumentVersion(3, title, documentPath, publisher);
    }

    private void assertCurrentDocumentVersion(int versionNumber, String docTitle, String docPath,
                                              SfeeDocumentManagerPublisher publisher) throws RemoteException,
            CruiseControlException {

        DocumentSoapDO foundDoc = findCurrentDocument(publisher, docPath, docTitle);
        assertEquals("Wrong version number found.", versionNumber, foundDoc.getVersion());
    }

    public void testValidation() throws CruiseControlException {
        SfeeDocumentManagerPublisher publisher = new SfeeDocumentManagerPublisher();
        assertInvalid(publisher);

        publisher.setServerURL("foo");
        assertInvalid(publisher);

        publisher.setUsername("bar");
        assertInvalid(publisher);

        publisher.setPassword("baz");
        assertInvalid(publisher);

        publisher.setProjectName("foobar");
        assertInvalid(publisher);

        publisher.setFolder("foopath");
        assertInvalid(publisher);

        publisher.setData(new StringDataSource("foobarbaz", "footitle"));
        assertInvalid(publisher);

        publisher.createDocumentName().setValue("biz");
        assertInvalid(publisher);

        publisher.createDescription().setValue("wak");
        assertInvalid(publisher);

        publisher.createStatus().setValue("bizwak");
        assertInvalid(publisher);

        publisher.createStatus().setValue("final");
        publisher.validate();
    }

    private void assertInvalid(SfeeDocumentManagerPublisher publisher) {
        try {
            publisher.validate();
            fail("Publisher should not validate");
        } catch (CruiseControlException expected) {
        }
    }

    public void testGetFolderId() throws CruiseControlException {
        SfeeDocumentManagerPublisher publisher = new SfeeDocumentManagerPublisher();
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);

        DocumentFolderSoapDO rootFolder = publisher.findFolder("/Root Folder");
        assertNotNull(rootFolder);

        DocumentFolderSoapDO level1Folder = publisher.findFolder("/Root Folder/level1");
        assertNotNull(level1Folder);
        assertEquals(rootFolder.getId(), level1Folder.getParentFolderId());

        DocumentFolderSoapDO level2Folder = publisher.findFolder("/Root Folder/level1/level2");
        assertNotNull(level2Folder);
        assertEquals(level1Folder.getId(), level2Folder.getParentFolderId());
    }

    public void testGetFolderWithInvalidPath() throws CruiseControlException {
        SfeeDocumentManagerPublisher publisher = new SfeeDocumentManagerPublisher();
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);

        try {
            publisher.findFolder("/THIS FOLDER DOESN'T EXIST");
            fail("expected an exception");
        } catch (CruiseControlException expected) {
        }

        try {
            publisher.findFolder("THIS FOLDER DOESN'T EXIST");
            fail("expected an exception");
        } catch (CruiseControlException expected) {
        }

        try {
            publisher.findFolder("/Root Folder/level1/level5");
            fail("expected an exception");
        } catch (CruiseControlException expected) {
        }

        try {
            publisher.findFolder("");
            fail("expected an exception");
        } catch (CruiseControlException expected) {
        }

        try {
            publisher.findFolder(null);
            fail("expected an exception");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testDocumentOrDataSourceRequired() throws CruiseControlException {
        SfeeDocumentManagerPublisher publisher = new SfeeDocumentManagerPublisher();
        publisher.setServerURL("foo");
        publisher.setUsername("bar");
        publisher.setPassword("baz");
        publisher.setProjectName("foobar");
        publisher.setFolder("foopath");
        publisher.createDocumentName().setValue("biz");
        publisher.createDescription().setValue("wak");
        publisher.createStatus().setValue("bizwak");
        publisher.createStatus().setValue("final");

        try {
            publisher.validate();
            fail("Expected an exception.");
        } catch (CruiseControlException e) {
            assertTrue(e.getMessage().indexOf("Either a document or a datasource must be specified.") >= 0);
        }

        publisher.setDocument("doesntmatter");
        publisher.validate();
    }

    public void testVersionCommentAndDocumentNameMayBeNull() throws Exception {
        String title = getClass().getName() + System.currentTimeMillis() + "Document.txt";
        String description = "This document was created by a unit test at " + System.currentTimeMillis();
        String expectedContent = "testing at " + System.currentTimeMillis();
        String documentPath = "/Root Folder/level1";
        DocumentStatus status = DocumentStatus.FINAL;

        SfeeDocumentManagerPublisher publisher = new SfeeDocumentManagerPublisher();
        publisher.setServerURL(SERVER_URL);
        publisher.setUsername(USERNAME);
        publisher.setPassword(PASSWORD);
        publisher.setProjectName(PROJECT_NAME);
        publisher.setFolder("/Root Folder/level1");

        publisher.setData(new StringDataSource(expectedContent, title));
        publisher.createDescription().setValue(description);
        publisher.createStatus().setValue(status.getName());
        publisher.setLock(true);

        publisher.validate();
        publisher.publish(null);


        assertDocumentCreated(publisher, documentPath, title, description, expectedContent, status, null);
    }

    private void assertDocumentCreated(SfeeDocumentManagerPublisher publisher, String documentPath, String title,
                                       String description, String expectedContent, DocumentStatus status,
                                       String versionComment)
            throws IOException, CruiseControlException {
        assertNotNull(projectId);
        assertNotNull(sessionId);
        IDocumentAppSoap docApp = (IDocumentAppSoap) ClientSoapStubFactory
                .getSoapStub(IDocumentAppSoap.class, SERVER_URL);

        DocumentSoapDO foundDoc = findCurrentDocument(publisher, documentPath, title);

        assertEquals(USERNAME, foundDoc.getLockedBy());
        assertEquals(title, foundDoc.getTitle());
        assertEquals(description, foundDoc.getDescription());
        assertEquals(status.getName(), foundDoc.getStatus());
        assertEquals(versionComment, foundDoc.getVersionComment());

        String documentFileId = docApp.getDocumentFileId(sessionId, foundDoc.getId(), foundDoc.getCurrentVersion());
        IFileStorageAppSoap fileStorageApp = (IFileStorageAppSoap) ClientSoapStubFactory
                .getSoapStub(IFileStorageAppSoap.class, SERVER_URL);
        DataHandler handler = fileStorageApp.downloadFile(sessionId, documentFileId);
        BufferedReader in = new BufferedReader(new InputStreamReader(handler.getInputStream()));
        String foundContent = in.readLine();
        assertEquals(expectedContent, foundContent);
    }

    private DocumentSoapDO findCurrentDocument(SfeeDocumentManagerPublisher publisher, String documentPath,
                                               String title) throws CruiseControlException,
            RemoteException {

        IDocumentAppSoap docApp = (IDocumentAppSoap) ClientSoapStubFactory
                .getSoapStub(IDocumentAppSoap.class, SERVER_URL);

        DocumentFolderSoapDO documentFolder = publisher.findFolder(documentPath);
        DocumentSoapList documentList = docApp.getDocumentList(sessionId, documentFolder.getId(), null);
        DocumentSoapRow[] documents = documentList.getDataRows();
        DocumentSoapDO foundDoc = null;
        for (int i = 0; i < documents.length; i++) {
            DocumentSoapRow document = documents[i];
            if (document.getTitle().equals(title)) {
                foundDoc = docApp.getDocumentData(sessionId, document.getId(), document.getCurrentVersion());
            }
        }

        assertNotNull(foundDoc);
        return foundDoc;
    }

    private static String getProjectId(ISourceForgeSoap soap, String sessionID) throws RemoteException {
        ProjectSoapList projectList = soap.getProjectList(sessionID);
        ProjectSoapRow[] rows = projectList.getDataRows();
        String projectID = null;
        for (int i = 0; i < rows.length; i++) {
            ProjectSoapRow nextProjectRow = rows[i];
            if (nextProjectRow.getTitle().equals(PROJECT_NAME)) {
                projectID = nextProjectRow.getId();
            }
        }
        return projectID;
    }

    public static class StringDataSource implements DataSource {
        private final String data;
        private final String name;

        public StringDataSource(String data, String name) {

            this.data = data;
            this.name = name;
        }

        public InputStream getInputStream() throws IOException {
            return new ByteArrayInputStream(data.getBytes());
        }

        public OutputStream getOutputStream() throws IOException {
            return new ByteArrayOutputStream();
        }

        public String getContentType() {
            return "text/plain";
        }

        public String getName() {
            return name;
        }
    }
}

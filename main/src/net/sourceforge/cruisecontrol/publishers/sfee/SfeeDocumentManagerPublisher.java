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
import com.vasoftware.sf.soap42.webservices.docman.DocumentFolderSoapList;
import com.vasoftware.sf.soap42.webservices.docman.DocumentFolderSoapRow;
import com.vasoftware.sf.soap42.webservices.docman.DocumentSoapDO;
import com.vasoftware.sf.soap42.webservices.docman.DocumentSoapList;
import com.vasoftware.sf.soap42.webservices.docman.DocumentSoapRow;
import com.vasoftware.sf.soap42.webservices.docman.IDocumentAppSoap;
import com.vasoftware.sf.soap42.webservices.filestorage.IFileStorageAppSoap;
import com.vasoftware.sf.soap42.webservices.sfmain.ISourceForgeSoap;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.XPathAwareChild;
import org.jdom.Element;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

/**
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author <a href="mailto:krs@thoughtworks.com">Kent Spillner</a>
 */
public class SfeeDocumentManagerPublisher extends SfeePublisher {

    private String projectName;
    private XPathAwareChild documentName;
    private XPathAwareChild description;
    private DataSource dataSrc;
    private String path;
    private Status status;
    private XPathAwareChild versionComment;
    private boolean lock;
    private String documentPath;

    public Status createStatus() {
        status = new Status();
        return status;
    }

    public XPathAwareChild createVersionComment() {
        versionComment = new XPathAwareChild();
        return versionComment;
    }

    public void setLock(boolean lock) {
        this.lock = lock;
    }

    public void setData(DataSource dataSrc) {
        this.dataSrc = dataSrc;
    }

    public XPathAwareChild createDocumentName() {
        documentName = new XPathAwareChild();
        return documentName;
    }

    public XPathAwareChild createDescription() {
        description = new XPathAwareChild();
        return description;
    }

    public void setProjectName(String name) {
        projectName = name;
    }

    public void setFolder(String path) {
        this.path = path;
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {

        ISourceForgeSoap sfee = (ISourceForgeSoap) ClientSoapStubFactory
                .getSoapStub(ISourceForgeSoap.class, getServerURL());
        String sessionID;
        try {
            sessionID = sfee.login(getUsername(), getPassword());
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        }

        IFileStorageAppSoap fileStorage = (IFileStorageAppSoap) ClientSoapStubFactory
                .getSoapStub(IFileStorageAppSoap.class, getServerURL());
        String fileID;
        String mimeType;
        try {
            if (documentPath != null) {
                dataSrc = new FileDataSource(documentPath);
            }
            DataHandler dataHandler = new DataHandler(dataSrc);
            fileID = fileStorage.uploadFile(sessionID, dataHandler);
            mimeType = dataHandler.getContentType();
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        }

        DocumentFolderSoapDO folder = findFolder(path);
        String intendedDocumentName =
                (documentName != null ? documentName.lookupValue(cruisecontrolLog) : dataSrc.getName());
        String intendedVersionComment =
                (versionComment != null ? versionComment.lookupValue(cruisecontrolLog) : null);

        DocumentSoapDO existingDocument;
        try {
            existingDocument = findExistingDocument(sessionID, folder, intendedDocumentName);
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        }

        if (existingDocument != null) {
            updateExistingDocument(sessionID, existingDocument, intendedVersionComment, mimeType, fileID,
                    cruisecontrolLog);
        } else {
            createNewDocument(cruisecontrolLog, sessionID, intendedDocumentName, intendedVersionComment, folder,
                    mimeType, fileID);
        }
    }

    private void updateExistingDocument(String sessionID, DocumentSoapDO existingDocument, String versionComment,
                                        String mimeType,
                                        String fileID, Element cruiseControlLog) throws CruiseControlException {
        IDocumentAppSoap docApp = (IDocumentAppSoap) ClientSoapStubFactory
                .getSoapStub(IDocumentAppSoap.class, getServerURL());
        existingDocument.setDescription(description.lookupValue(cruiseControlLog));
        existingDocument.setStatus(status.lookupValue(cruiseControlLog));
        if (lock) {
            existingDocument.setLockedBy(getUsername());
        } else {
            existingDocument.setLockedBy(null);
        }
        existingDocument.setMimeType(mimeType);
        existingDocument.setVersionComment(versionComment);

        try {
            docApp.setDocumentData(sessionID, existingDocument, fileID);
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        }
    }

    private DocumentSoapDO findExistingDocument(String sessionID, DocumentFolderSoapDO folder,
                                                String intendedDocumentName)
            throws RemoteException {
        IDocumentAppSoap docApp = (IDocumentAppSoap) ClientSoapStubFactory
                .getSoapStub(IDocumentAppSoap.class, getServerURL());
        DocumentSoapList documentList = docApp.getDocumentList(sessionID, folder.getId(), null);
        DocumentSoapRow[] existingDocuments = documentList.getDataRows();
        for (int i = 0; i < existingDocuments.length; i++) {
            DocumentSoapRow existingDocument = existingDocuments[i];
            if (existingDocument.getTitle().equals(intendedDocumentName)) {
                return docApp
                        .getDocumentData(sessionID, existingDocument.getId(), existingDocument.getCurrentVersion());
            }
        }
        return null;
    }

    private void createNewDocument(Element cruisecontrolLog, String sessionID, String intendedDocumentName,
                                   String intendedVersionComment, DocumentFolderSoapDO folder, String mimeType,
                                   String fileID) throws
            CruiseControlException {
        IDocumentAppSoap docApp = (IDocumentAppSoap) ClientSoapStubFactory
                .getSoapStub(IDocumentAppSoap.class, getServerURL());
        try {
            docApp.createDocument(sessionID, folder.getId(), intendedDocumentName,
                    description.lookupValue(cruisecontrolLog), intendedVersionComment,
                    status.lookupValue(cruisecontrolLog), lock, intendedDocumentName, mimeType,
                    fileID);
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        }
    }

    public void subValidate() throws CruiseControlException {
        ValidationHelper.assertIsSet(path, "folder", SfeeDocumentManagerPublisher.class);
        ValidationHelper.assertIsSet(projectName, "projectName", SfeeDocumentManagerPublisher.class);
        if (documentPath == null && dataSrc == null) {
            throw new CruiseControlException("Either a document or a datasource must be specified.");
        }
        ValidationHelper.assertHasChild(description, "description", SfeeDocumentManagerPublisher.class);
        description.validate();
        ValidationHelper.assertHasChild(status, "status", SfeeDocumentManagerPublisher.class);
        status.validate();
        if (documentName != null) {
            documentName.validate();
        }
        if (versionComment != null) {
            versionComment.validate();
        }
    }

    DocumentFolderSoapDO findFolder(String path) throws CruiseControlException {
        if (path == null) {
            throw new IllegalArgumentException("path can not be null.");
        }

        StringTokenizer tokens = new StringTokenizer(path, "/");

        DocumentFolderSoapDO folder;
        try {
            ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory
                    .getSoapStub(ISourceForgeSoap.class, getServerURL());
            String sessionID = soap.login(getUsername(), getPassword());
            String projectID = SfeeUtils.findProjectID(soap, sessionID, projectName);

            IDocumentAppSoap docApp = (IDocumentAppSoap) ClientSoapStubFactory
                    .getSoapStub(IDocumentAppSoap.class, getServerURL());

            folder = findFolderInternal(sessionID, projectID, tokens, docApp);

        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        }

        if (folder == null) {
            throw new CruiseControlException("Unable to find a folder for the path specified [" + path + "].");
        }

        return folder;
    }

    /**
     * Recursively traverses the folder tree until the desired one is found.
     *
     * @return null if folder not found.
     */
    private static DocumentFolderSoapDO findFolderInternal(String sessionID, String parentID,
                                                           StringTokenizer pathTokens, IDocumentAppSoap docApp)
            throws RemoteException {

        if (!pathTokens.hasMoreTokens()) {
            return null;
        }

        DocumentFolderSoapList folderList = docApp.getDocumentFolderList(sessionID, parentID, false);
        DocumentFolderSoapRow[] folders = folderList.getDataRows();
        String nextPathElement = pathTokens.nextToken();
        boolean isFinalPathElement = !pathTokens.hasMoreTokens();

        for (int i = 0; i < folders.length; i++) {
            DocumentFolderSoapRow nextFolder = folders[i];
            String nextFolderTitle = nextFolder.getTitle();
            boolean folderNamesMatch = nextFolderTitle.equals(nextPathElement);
            if (folderNamesMatch && isFinalPathElement) {
                return docApp.getDocumentFolderData(sessionID, nextFolder.getId());
            } else if (folderNamesMatch) {
                return findFolderInternal(sessionID, nextFolder.getId(), pathTokens, docApp);
            }
        }

        return null;
    }

    public void setDocument(String documentPath) {
        this.documentPath = documentPath;
    }


    public static class Status extends XPathAwareChild {
        public void validate() throws CruiseControlException {

            if (getXpathExpression() == null && getFixedValue() != null
                    && !(getFixedValue().equalsIgnoreCase("final") || getFixedValue().equalsIgnoreCase("draft"))) {
                throw new CruiseControlException(
                        "Status value specified [] is not valid. Valid values are 'final' and 'draft'.");
            }

            super.validate();
        }
    }
}

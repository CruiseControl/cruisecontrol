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
import com.vasoftware.sf.soap42.webservices.filestorage.IFileStorageAppSoap;
import com.vasoftware.sf.soap42.webservices.frs.FrsFileSoapList;
import com.vasoftware.sf.soap42.webservices.frs.FrsFileSoapRow;
import com.vasoftware.sf.soap42.webservices.frs.IFrsAppSoap;
import com.vasoftware.sf.soap42.webservices.sfmain.ISourceForgeSoap;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.jdom.Element;

import javax.activation.DataHandler;
import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * <p>Publishes to a SourceForge Enterprise Edition File Release System.</p>
 *
 * @author <a href="mailto:kspillne@thoughtworks.com">Kent Spillner</a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 */
public class SfeeFrsPublisher extends SfeePublisher {
    private File file;
    private String releaseID;
    private String uploadName;

    public void setFile(String filename) {
        this.file = new File(filename);
    }

    public void setReleaseID(String releaseID) {
        this.releaseID = releaseID;
    }

    public void setUploadname(String uploadname) {
        uploadName = uploadname;
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        ValidationHelper.assertExists(getFile(), "file", this.getClass());
        ValidationHelper.assertIsNotDirectory(getFile(), "file", this.getClass());
        ValidationHelper.assertIsReadable(getFile(), "file", this.getClass());

        if (uploadName == null) {
            uploadName = getFile().getName();
        }

        ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory
                .getSoapStub(ISourceForgeSoap.class, getServerURL());
        try {
            String sessionID = soap.login(getUsername(), getPassword());

            IFrsAppSoap frsApp = (IFrsAppSoap) ClientSoapStubFactory.getSoapStub(IFrsAppSoap.class, getServerURL());

            FrsFileSoapList fileList = frsApp.getFrsFileList(sessionID, releaseID);
            Collection existingFiles = findExistingFiles(fileList, uploadName);
            for (Iterator i = existingFiles.iterator(); i.hasNext();) {
                String id = (String) i.next();
                frsApp.deleteFrsFile(sessionID, id);
            }

            DataHandler dataHandler = new DataHandler(getFile().toURI().toURL());
            IFileStorageAppSoap fileStorageApp =
                    (IFileStorageAppSoap) ClientSoapStubFactory.getSoapStub(IFileStorageAppSoap.class, getServerURL());
            String storedFileId = fileStorageApp.uploadFile(sessionID, dataHandler);
            frsApp.createFrsFile(sessionID, releaseID, uploadName, dataHandler.getContentType(), storedFileId);
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        } catch (MalformedURLException e) {
            throw new CruiseControlException(e);
        }


    }

    protected File getFile() {
        return file;
    }

    private static Collection findExistingFiles(FrsFileSoapList fileList, String filename) {
        FrsFileSoapRow[] files = fileList.getDataRows();
        ArrayList duplicates = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            FrsFileSoapRow nextFile = files[i];
            if (nextFile.getFilename().equals(filename)) {
                duplicates.add(nextFile.getId());
            }
        }
        return duplicates;
    }

    public void subValidate() throws CruiseControlException {
        ValidationHelper.assertIsSet(releaseID, "releaseid", this.getClass());
        ValidationHelper.assertIsSet(getFile(), "file", this.getClass());
    }
}

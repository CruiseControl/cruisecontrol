package net.sourceforge.cruisecontrol.publishers.sfee;

import com.vasoftware.sf.soap42.webservices.ClientSoapStubFactory;
import com.vasoftware.sf.soap42.webservices.filestorage.IFileStorageAppSoap;
import com.vasoftware.sf.soap42.webservices.frs.FrsFileSoapList;
import com.vasoftware.sf.soap42.webservices.frs.FrsFileSoapRow;
import com.vasoftware.sf.soap42.webservices.frs.IFrsAppSoap;
import com.vasoftware.sf.soap42.webservices.sfmain.ISourceForgeSoap;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Publisher;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.jdom.Element;
import java.io.File;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import javax.activation.DataHandler;

public class SfeeFrsPublisher implements Publisher {
    private String url;
    private String username;
    private String password;
    private File file;
    private String releaseID;
    private String uploadName;

    public void setServerURL(String url) {
        this.url = url;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public void setFile(String filename) {
        setFile(new File(filename));
    }

    public void setReleaseID(String releaseID) {
        this.releaseID = releaseID;
    }

    public void setUploadname(String uploadname) {
        uploadName = uploadname;
    }

    public void publish(Element cruisecontrolLog) throws CruiseControlException {
        ValidationHelper.assertExists(file, "file", this.getClass());
        ValidationHelper.assertIsNotDirectory(file, "file", this.getClass());
        ValidationHelper.assertIsReadable(file, "file", this.getClass());

        if (uploadName == null) {
            uploadName = file.getName();
        }

        ISourceForgeSoap soap = (ISourceForgeSoap) ClientSoapStubFactory.getSoapStub(ISourceForgeSoap.class, url);
        try {
            String sessionID = soap.login(username, password);

            IFrsAppSoap frsApp = (IFrsAppSoap) ClientSoapStubFactory.getSoapStub(IFrsAppSoap.class, url);

            FrsFileSoapList fileList = frsApp.getFrsFileList(sessionID, releaseID);
            Collection existingFiles = findExistingFiles(fileList, uploadName);
            for (Iterator i = existingFiles.iterator(); i.hasNext();) {
                String id = (String) i.next();
                frsApp.deleteFrsFile(sessionID, id);
            }

            DataHandler dataHandler = new DataHandler(file.toURL());
            IFileStorageAppSoap fileStorageApp =
                    (IFileStorageAppSoap) ClientSoapStubFactory.getSoapStub(IFileStorageAppSoap.class, url);
            String storedFileId = fileStorageApp.uploadFile(sessionID, dataHandler);
            frsApp.createFrsFile(sessionID, releaseID, uploadName, dataHandler.getContentType(), storedFileId);
        } catch (RemoteException e) {
            throw new CruiseControlException(e);
        } catch (MalformedURLException e) {
            throw new CruiseControlException(e);
        }


    }

    private static Collection findExistingFiles(FrsFileSoapList fileList, String filename) {
        FrsFileSoapRow[] files = fileList.getDataRows();
        ArrayList duplicates = new ArrayList();
        for (int i = 0; i < files.length; i++) {
            FrsFileSoapRow nextFile = files[ i ];
            if (nextFile.getFilename().equals(filename)) {
                duplicates.add(nextFile.getId());
            }
        }
        return duplicates;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(url, "serverurl", this.getClass());
        ValidationHelper.assertIsSet(username, "username", this.getClass());
        ValidationHelper.assertIsSet(password, "password", this.getClass());
        ValidationHelper.assertIsSet(releaseID, "releaseid", this.getClass());
        ValidationHelper.assertIsSet(file, "file", this.getClass());
    }
}

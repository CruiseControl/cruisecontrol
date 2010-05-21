package net.sourceforge.cruisecontrol.distributed.core;

import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Forwards reader calls to remote agent.
 * @author Dan Rollo
 *         Date: May 19, 2010
 *         Time: 10:03:34 PM
 */
public class BuildOutputLoggerRemote extends BuildOutputLogger implements Serializable {

    private static final Logger LOG = Logger.getLogger(BuildOutputLoggerRemote.class);

    private static final long serialVersionUID = 6056484458144143629L;


    private static final String DUMMY_FILE_PREFIX = "dummyRemoteFile-";
    
    private static final String MSG_PART = " - Error in LiveOutputReaderRemote.";

    private final String project;
    private final LiveOutputReaderRemote agent;

    public BuildOutputLoggerRemote(final String projectName, final LiveOutputReaderRemote remoteAgent) {
        super(new File(DUMMY_FILE_PREFIX + projectName));
        project = projectName;
        agent = remoteAgent;
    }

    @Override
    public String[] retrieveLines(int firstLine) {
        try {
            return agent.retrieveLinesRemote(firstLine);
        } catch (RemoteException e) {
            final String msg = project + MSG_PART + "retrieveLinesRemote(): ";
            LOG.warn(msg, e);
            return new String[] {msg + e.getMessage()};
        }
    }

    @Override
    public String getID() {
        try {
            return agent.getIDRemote();
        } catch (RemoteException e) {
            final String msg = project + MSG_PART + "getIDRemote(): ";
            LOG.warn(msg, e);
            return msg + e.getMessage();
        }
    }

    @Override
    public void clear() {
        throw new IllegalStateException();
    }

    @Override
    public void consumeLine(String line) {
        throw new IllegalStateException();
    }

    @Override
    public boolean isDataFileSet() {
        throw new IllegalStateException();
    }

    @Override
    public boolean isDataFileEquals(File otherDataFile) {
        throw new IllegalStateException();
    }
}

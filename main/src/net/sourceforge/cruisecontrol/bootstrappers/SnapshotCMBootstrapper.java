package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import org.apache.log4j.Logger;

import java.io.PrintWriter;

/**
 * Since we rely on our build.xml to handle updating our source code, there has
 * always been a problem with what happens when the build.xml file itself
 * changes.  Previous workarounds have included writing a wrapper build.xml that
 * will check out the "real" build.xml.  This class is a substitute for that
 * practice.
 *
 * The SnapshotCMBootstrapper will handle updating a single file from
 * SnapshotCM before the build begins.
 *
 * Usage:
 *
 *     &lt;snapshotcmbootstrapper file="" /&gt;
 *
 *  @author patrick.conant@hp.com
 */
public class SnapshotCMBootstrapper implements Bootstrapper {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(SnapshotCMBootstrapper.class);

    /**
     *  Reference to the file to bootstrap.
     */
    private String filename;


    public void setFile(String name) {
        filename = name;
    }

    /**
     *  Update the specified file.
     */
    public void bootstrap() {
        Commandline commandLine = buildUpdateCommand();
        Process p = null;

        if (log.isDebugEnabled()) {
            log.debug("Executing: " + commandLine.toString());
        }
        try {
            p = Runtime.getRuntime().exec(commandLine.getCommandline());
            StreamPumper errorPumper =
                new StreamPumper(p.getErrorStream(), new PrintWriter(System.err, true));
            new Thread(errorPumper).start();
            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (Exception e) {
            log.error("Error executing SnapshotCM update command", e);
        }
    }

    public void validate() throws CruiseControlException {
        if (filename == null) {
            throw new CruiseControlException("'file' is required for SnapshotCMBootstrapper");
        }
    }

    protected Commandline buildUpdateCommand() {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("wco");

        commandLine.createArgument().setValue("-fR");
        commandLine.createArgument().setValue(getFullPathFileName());

        return commandLine;
    }

    private String getFullPathFileName() {
        return filename;
    }

}

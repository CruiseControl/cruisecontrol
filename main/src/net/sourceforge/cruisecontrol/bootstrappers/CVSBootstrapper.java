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
 * The CVSBootstrapper will handle updating a single file from CVS before the
 * build begins.
 *
 * Usage:
 *
 *     &lt;cvsbootstrapper cvsroot="" file=""/&gt;
 *
 */
public class CVSBootstrapper implements Bootstrapper {

    /** enable logging for this class */
    private static Logger log = Logger.getLogger(CVSBootstrapper.class);

    private String _filename;
    private String _cvsroot;

    public void setCvsroot(String cvsroot) {
        _cvsroot = cvsroot;
    }

    public void setFile(String filename) {
        _filename = filename;
    }

    /**
     *  Update the specified file.
     */
    public void bootstrap() {
        Commandline commandLine = buildUpdateCommand();
        Process p = null;

        log.debug("Executing: " + commandLine.toString());
        try {
            p = Runtime.getRuntime().exec(commandLine.getCommandline());
            StreamPumper errorPumper = new StreamPumper(p.getErrorStream(),
                    new PrintWriter(System.err, true));
            new Thread(errorPumper).start();
            p.waitFor();
            p.getInputStream().close();
            p.getOutputStream().close();
            p.getErrorStream().close();
        } catch (Exception e) {
            log.error("Error executing CVS update command", e);
        }
    }

    public void validate() throws CruiseControlException {
        if(_filename == null) {
            throw new CruiseControlException("'file' is required for CVSBootstrapper");
        }
    }

    protected Commandline buildUpdateCommand() {
        Commandline commandLine = new Commandline();
        commandLine.setExecutable("cvs");
        if (_cvsroot != null) {
            commandLine.createArgument().setValue("-d");
            commandLine.createArgument().setValue(_cvsroot);
        }

        commandLine.createArgument().setValue("update");
        commandLine.createArgument().setValue(_filename);

        return commandLine;
    }

    /** for testing */
    public static void main(String[] args) {
        CVSBootstrapper bootstrapper = new CVSBootstrapper();
        bootstrapper.setCvsroot(":pserver:anonymous@cvs.cruisecontrol.sourceforge.net:/cvsroot/cruisecontrol");
        bootstrapper.setFile("build.xml");
        bootstrapper.bootstrap();
    }

}

package net.sourceforge.cruisecontrol.bootstrappers;

import net.sourceforge.cruisecontrol.Bootstrapper;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.StreamPumper;

import java.io.IOException;

import org.apache.log4j.Logger;

/**
 * Bootstrapper for Perforce. Accepts one path that we sync.
 * @author <a href="mailto:mroberts@thoughtworks.com">Mike Roberts</a>
 * @author <a href="mailto:cstevenson@thoughtworks.com">Chris Stevenson</a>
 */
public class P4Bootstrapper implements Bootstrapper {
    private static Logger log = Logger.getLogger(P4Bootstrapper.class);
    private String path;
    private String p4Port;
    private String p4Client;
    private String p4User;

    public void setPath(String path) {
        this.path = path;
    }

    public void setP4Port(String p4Port) {
        this.p4Port = p4Port;
    }

    public void setP4Client(String p4Client) {
        this.p4Client = p4Client;
    }

    public void setP4User(String p4User) {
        this.p4User = p4User;
    }

    public void validate() throws CruiseControlException {
        if (path == null) {
            throw new CruiseControlException("Path is not set.");
        }
        failIfNotNullButEmpty(path, "path");
        failIfNotNullButEmpty(p4Port, "P4Port");
        failIfNotNullButEmpty(p4Client, "P4Client");
        failIfNotNullButEmpty(p4User, "P4User");
    }

    private void failIfNotNullButEmpty(String stringToTest, String nameOfStringToTest) throws CruiseControlException {
        if (stringToTest != null && stringToTest.equals(""))
            throw new CruiseControlException(nameOfStringToTest + " cannot to be set empty");
    }

    public void bootstrap() throws CruiseControlException {
        String commandline = createCommandline();
        if (log.isDebugEnabled()) {
            log.debug("Executing commandline [" + commandline + "]");
        }
        executeCommandLine(commandline);
    }

    public String createCommandline() throws CruiseControlException{
        validate();
        StringBuffer commandline = new StringBuffer("p4 -s ");
        if (p4Port != null) {
            commandline.append("-p ");
            commandline.append(p4Port);
            commandline.append(' ');
        }
        if (p4Client != null) {
            commandline.append("-c ");
            commandline.append(p4Client);
            commandline.append(' ');
        }
        if (p4User != null) {
            commandline.append("-u ");
            commandline.append(p4User);
            commandline.append(' ');
        }
        commandline.append("sync ");
        commandline.append(path);
        return commandline.toString();
    }

    // TODO: Refactor this into a class. Then we can mock it and unit test bootstrap()
    private void executeCommandLine(String commandline) throws CruiseControlException {
        try {
            Process p = Runtime.getRuntime().exec(commandline);
            new Thread(new StreamPumper(p.getInputStream())).start();
            new Thread(new StreamPumper(p.getErrorStream())).start();
            p.waitFor();
        } catch (IOException e) {
            throw new CruiseControlException("Problem trying to execute command line process", e);
        } catch (InterruptedException e) {
            throw new CruiseControlException("Problem trying to execute command line process", e);
        }
    }

    // For 'with environment' testing. Change values if you want to try yourself
    public static void main(String[] args) throws Exception {
        P4Bootstrapper bootstrapper = new P4Bootstrapper();
        bootstrapper.setPath("//depot/...");
        bootstrapper.setP4Port("localhost:1666");
        bootstrapper.setP4User("mroberts");
        bootstrapper.setP4Client("robertsm");
        bootstrapper.bootstrap();
    }
}
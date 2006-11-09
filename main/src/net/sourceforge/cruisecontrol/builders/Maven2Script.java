package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamConsumer;

/**
 * Maven2 script class based on the Maven builder class from
 * <a href="mailto:epugh@opensourceconnections.com">Eric Pugh</a>.
 * <br />
 * Contains all the details related to running a Maven based build.
 * @author Steria Benelux Sa/Nv - Provided without any warranty
 */
public class Maven2Script implements Script, StreamConsumer {

    private static final String ERROR   = "error";
    private static final String SUCCESS = "success";
    private static final Logger LOG = Logger.getLogger(Maven2Script.class);

    private String goalset;
    private String mvn;
    private String pomFile;
    private String settingsFile;
    private String flags;
    private final Element buildLogElement; //Log to store result of the execution for CC
    private Map buildProperties;
    private String activateProfiles;

    private int exitCode;
    private Element currentElement;

    /**
     *
     * @param mvn path to the mvn script
     * @param pomFile path to the pom file
     * @param goals the goalset to execute
     * @param settingsFile path to the settings file (not required)
     * @param activateProfiles comma-delimited list of profiles to activate. (not required)
     * @param flags extra parameter to pass to mvn e.g.: -U (not required)
     */
    public Maven2Script(Element buildLogElement, String mvn, String pomFile, String goals,
                        String settingsFile, String activateProfiles, String flags) {

        this.buildLogElement = buildLogElement;
        this.mvn = mvn;
        this.pomFile = pomFile;
        this.goalset = goals;
        this.settingsFile = settingsFile;
        this.flags = flags;
        this.activateProfiles = activateProfiles;
    }

    /**
     * Construct the command that we're going to execute.
     * @return Commandline holding command to be executed
     * @throws CruiseControlException
     */
    public Commandline buildCommandline() throws CruiseControlException {

        //usage: maven [options] [<goal(s)>] [<phase(s)>]
        Commandline cmdLine = new Commandline();
        cmdLine.setExecutable(mvn);

        //Run in non-interactive mode.
        cmdLine.createArgument("-B");

        //If log is enabled for CC, enable it for mvn
        if (LOG.isDebugEnabled()) {
            cmdLine.createArgument("-X");
        }

        //Alternate path for the user settings file
        if (settingsFile != null) {
            cmdLine.createArguments("-s", settingsFile);
        }

        if (pomFile != null) {
            cmdLine.createArguments("-f", new File(pomFile).getName());
        }

        //activate specified profiles
        if (activateProfiles != null) {
            cmdLine.createArguments("-P", activateProfiles);
        }

        if (flags != null) {
            cmdLine.createArgument(flags);
        }


        if (goalset != null) {
            StringTokenizer stok = new StringTokenizer(goalset, " \t\r\n");
            while (stok.hasMoreTokens()) {
                cmdLine.createArgument(stok.nextToken());
            }
        }

        Iterator propertiesIterator = buildProperties.keySet().iterator();
        while (propertiesIterator.hasNext()) {
            String key = (String) propertiesIterator.next();
            String value = (String) buildProperties.get(key);
            // TODO doesn't work when properties contains spaces.
            if (value.indexOf(' ') == -1) {
                cmdLine.createArgument("-D" + key + "=" + value);
            } else {
                LOG.error("Maven2Script ignoring property with space. Key:" + key + "; Value:" + value);
            }
        }

        //If log is enabled, log the command line
        if (LOG.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Executing Command: ");
            String[] args = cmdLine.getCommandline();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                sb.append(arg);
                sb.append(" ");
            }
            LOG.debug(sb.toString());
        }

        return cmdLine;
    }

    /**
     * Analyze the output of the mvn command. This is used to detect errors
     * or successful build.
     */
    public void consumeLine(String line) {
        final String level;
        final String infoLine;
        if (line == null || line.length() == 0 || buildLogElement == null) {
            return;
        }

        synchronized (buildLogElement) {
            //To detect errors, we will parse the output of the mvn command.
            //Don't forget that this can stop working if changes are made in mvn.
            if (line.startsWith("[ERROR]")) {
                level = "error";
                infoLine = line.substring(line.indexOf(']') + 1).trim();
            } else if (line.startsWith("[INFO]") || line.startsWith("[DEBUG]")) {
                level = "info";
                infoLine = line.substring(line.indexOf(']') + 1).trim();
            } else {
                level = "info";
                infoLine = line;
            }
            if (infoLine.startsWith("BUILD SUCCESSFUL")) {
                buildLogElement.setAttribute(SUCCESS, "BUILD SUCCESSFUL detected");
            } else if (infoLine.startsWith("BUILD FAILURE")) {
                buildLogElement.setAttribute(ERROR, "BUILD FAILURE detected");
            } else if (infoLine.startsWith("BUILD ERROR")) {
                buildLogElement.setAttribute(ERROR, "BUILD ERROR detected");
            } else if (infoLine.startsWith("FATAL ERROR")) {
                buildLogElement.setAttribute(ERROR, "FATAL ERROR detected");
            /*} else if (line.startsWith("org.apache.maven.MavenException")) {
             buildLogElement.setAttribute("error", "You have encountered an unknown error running Maven: " + line);
             } else if (line.startsWith("The build cannot continue")) {
             buildLogElement.setAttribute("error", "The build cannot continue: Unsatisfied Dependency");*/
            } else if (infoLine.startsWith("[")
                    && infoLine.endsWith("]")
                    && infoLine.indexOf(":") > -1) { // heuristically this is a goal marker,
                makeNewCurrentElement(infoLine.substring(1, infoLine.length() - 1));
                return; // Do not log the goal itself
            }

            Element msg = new Element("message");
            msg.addContent(new CDATA(line));
            // Initially call it "info" level.
            // If "the going gets tough" we'll switch this to "error"
            msg.setAttribute("priority", level);
            if (currentElement == null) {
                buildLogElement.addContent(msg);
            } else {
                currentElement.addContent(msg);
            }
        }
    }

    private Element makeNewCurrentElement(String cTask) {
        if (buildLogElement == null) {
            return null;
        }
        synchronized (buildLogElement) {
            flushCurrentElement();
            currentElement = new Element("mavengoal");
            currentElement.setAttribute("name", cTask);
            return currentElement;
        }
    }

    protected void flushCurrentElement() {

        if (buildLogElement == null) {
            return;
        }
        synchronized (buildLogElement) {
            if (currentElement != null) {

                if (buildLogElement.getAttribute(SUCCESS) != null && buildLogElement.getAttribute(ERROR) == null) {
                    LOG.debug("Ok : BUILD SUCCESSFUL"); // Ok build successfull
                } else if (buildLogElement.getAttribute(ERROR) != null) {
                    // All the messages of the last (failed) goal should be
                    // switched to priority error
                    List lst = currentElement.getChildren("message");
                    if (lst != null) {
                        Iterator it = lst.iterator();
                        while (it.hasNext()) {
                            Element msg = (Element) it.next();
                            msg.setAttribute("priority", ERROR);
                        }
                    }
                }
                buildLogElement.addContent(currentElement);
                currentElement = null;
            }
        }
    }


    /**
     * @param buildProperties The buildProperties to set.
     */
    public void setBuildProperties(Map buildProperties) {
        this.buildProperties = buildProperties;
    }
    /**
     * @param goalset The goalset to set.
     */
    public void setGoalset(String goalset) {
        this.goalset = goalset;
    }
    /**
     * @param mvnScript The mavenScript to set.
     */
    public void setMvnScript(String mvnScript) {
        this.mvn = mvnScript;
    }
    /**
     * @param pomFile The projectFile to set.
     */
    public void setPomFile(String pomFile) {
        this.pomFile = pomFile;
    }
    
    /**
     * @return the exitCode.
     */
    public int getExitCode() {

        return exitCode;
    }

    /**
     * @param exitCode The exitCode to set.
     */
    public void setExitCode(int exitCode) {

        this.exitCode = exitCode;
    }

}

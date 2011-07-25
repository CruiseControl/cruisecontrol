package net.sourceforge.cruisecontrol.builders;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.util.OSEnvironment;
import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Progress;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamConsumer;

/**
 * Maven2 script class based on the Maven builder class from
 * <a href="mailto:epugh@opensourceconnections.com">Eric Pugh</a>.
 * <br />
 * Contains all the details related to running a Maven based build.
 * @author Steria Benelux Sa/Nv - Provided without any warranty
 */
public final class Maven2Script implements Script, StreamConsumer {

    private static final String ERROR   = "error";
    private static final String SUCCESS = "success";
    private static final Logger LOG = Logger.getLogger(Maven2Script.class);

    private String goalset;
    private String mvn;
    private String pomFile;
    private final String settingsFile;
    private final String flags;
    private final Element buildLogElement; //Log to store result of the execution for CC
    private Map<String, String> buildProperties;
    private final String activateProfiles;
    private List<Property> properties;
    private final Progress progress;
    private OSEnvironment env;

    private int exitCode;
    private Element currentElement;

    /**
     * @param maven2Builder the maven2builder executing this script
     * @param buildLogElement Log to store result of the execution for CC
     * @param goals the goalset to execute
     * @param progress used to update progress
     */
    public Maven2Script(final Maven2Builder maven2Builder, final Element buildLogElement, final String goals,
                        final Progress progress) {

        this.buildLogElement = buildLogElement;
        this.mvn = maven2Builder.getMvnScript();
        this.pomFile = maven2Builder.getPomFile();
        this.goalset = goals;
        this.settingsFile = maven2Builder.getSettingsFile();
        this.flags = maven2Builder.getFlags();
        this.activateProfiles = maven2Builder.getActivateProfiles();
        this.progress = progress;
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
            StringTokenizer stok = new StringTokenizer(flags, " \t\r\n");
            while (stok.hasMoreTokens()) {
                cmdLine.createArgument(stok.nextToken());
            }
        }


        if (goalset != null) {
            StringTokenizer stok = new StringTokenizer(goalset, " \t\r\n");
            while (stok.hasMoreTokens()) {
                cmdLine.createArgument(stok.nextToken());
            }
        }

        for (final String key : buildProperties.keySet()) {
            final String value = buildProperties.get(key);
            if (value.indexOf(' ') == -1) {
                cmdLine.createArgument("-D" + key + "=" + value);
            } else {
                // @todo Find better way to handle when property values contains spaces.
                // Just using Commandline results in entire prop being quoted:
                //  "-Dcvstimestamp=2006-11-29 03:41:04 GMT"
                // and using Commandline.quoteArgument() results in this when used on value only:
                //  '-Dcvstimestamp="2006-11-29 03:41:04 GMT"'
                // all these (includeing no manipulation) cause errors in maven's parsing of the command line.
                // For now, we just replace spaces with underscores so at least some form of the prop is available
                final String spacelessValue = value.replace(' ', '_');
                LOG.warn("Maven2Script altering build property with space. Key:" + key + "; Orig Value:" + value
                        + "; New Value: " + spacelessValue);
                cmdLine.createArgument("-D" + key + "=" + spacelessValue);
            }
        }

        for (final Property property : properties) {
            cmdLine.createArgument("-D" + property.getName() + "=" + property.getValue());
        }

        cmdLine.setEnv(env);

        //If log is enabled, log the command line
        if (LOG.isDebugEnabled()) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Executing Command: ");
            final String[] args = cmdLine.getCommandline();
            for (final String arg : args) {
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
    public void consumeLine(final String line) {
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

            final Element msg = new Element("message");
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

    private void makeNewCurrentElement(String cTask) {
        if (buildLogElement == null) {
            return;
        }
        synchronized (buildLogElement) {
            flushCurrentElement();
            currentElement = new Element("mavengoal");
            currentElement.setAttribute("name", cTask);

            if (progress != null) {
                progress.setValue(cTask);
            }
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
                    final List lst = currentElement.getChildren("message");
                    if (lst != null) {
                        for (final Object aLst : lst) {
                            final Element msg = (Element) aLst;
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
    public void setBuildProperties(final Map<String, String> buildProperties) {
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
     * @param properties The properties to set.
     */
    public void setProperties(final List<Property> properties) {
        this.properties = properties;
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

    /**
     * @param env
     *            The environment variables of the script, or <code>null</code> if to
     *            inherit the environment of the current process.
     */
    public void setEnv(final OSEnvironment env) {
        this.env = env;
    } // setEnv
}

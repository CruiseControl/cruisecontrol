/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
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
package net.sourceforge.cruisecontrol.builders;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.util.StreamConsumer;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import org.apache.log4j.Logger;
import org.jdom.CDATA;
import org.jdom.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Maven builder class.
 *
 * Attempts to mimic the behavior of Ant builds, at least as far as CC is
 * concerned. Basically it's a (heavily) edited version of AntBuilder.
 * No style at all, but serves its purpose. :)
 *
 * @author <a href="mailto:fvancea@maxiq.com">Florin Vancea</a>
 */
public class MavenBuilder extends Builder implements StreamConsumer {

    private static final Logger LOG = Logger.getLogger(MavenBuilder.class);

    private String projectFile;
    private String goal;
    private String mavenScript;

    // We must produce an Ant-like log, but it's a little difficult.
    // Therefore we'll produce <mavengoal> tags containing <message> tags
    // and adapt accordingly the reporting side.
    private Element buildLogElement = null; // Global log to produce
    private Element currentElement = null;

    public void validate() throws CruiseControlException {
        super.validate();

        File ckFile = null;
        if (mavenScript == null) {
            throw new CruiseControlException("'mavenscript' is a required attribute on MavenBuilder");
        }
        ckFile = new File(mavenScript);
        if (!ckFile.exists()) {
            throw new CruiseControlException(
                "Script " + ckFile.getAbsolutePath() + " does not exist");
        }
        if (projectFile == null) {
            throw new CruiseControlException("'projectfile' is a required attribute on MavenBuilder");
        }
        ckFile = new File(projectFile);
        if (!ckFile.exists()) {
            throw new CruiseControlException(
                "Project descriptor " + ckFile.getAbsolutePath() + " does not exist");
        }
    }

    /**
     * build and return the results via xml.  debug status can be determined
     * from log4j category once we get all the logging in place.
     */
    public Element build(Map buildProperties) throws CruiseControlException {

        File projDir = (new File(projectFile)).getParentFile();
        Process p = null;

        buildLogElement = new Element("build");

        List runs = getGoalSets();
        for (int runidx = 0; runidx < runs.size(); runidx++) {
            String goalset = (String) runs.get(runidx);
            final String[] commandLineArgs =
                getCommandLineArgs(buildProperties, isWindows(), goalset);
            try {
                p = Runtime.getRuntime().exec(commandLineArgs, null, projDir);
            } catch (IOException e) {
                throw new CruiseControlException(
                    "Encountered an IO exception while attempting to execute Maven."
                        + " CruiseControl cannot continue.",
                    e);
            }

            StreamPumper errorPumper = new StreamPumper(p.getErrorStream(), this);
            StreamPumper outPumper = new StreamPumper(p.getInputStream(), this);
            Thread errorPumperThread = new Thread(errorPumper);
            Thread outPumperThread = new Thread(outPumper);
            errorPumperThread.start();
            outPumperThread.start();
            int exitCode = 1;

            try {
                exitCode = p.waitFor();
                errorPumperThread.join();
                outPumperThread.join();
                p.getInputStream().close();
                p.getOutputStream().close();
                p.getErrorStream().close();
            } catch (InterruptedException e) {
                LOG.info(
                    "Was interrupted while waiting for Maven to finish."
                        + " CruiseControl will continue, assuming that it completed");
            } catch (IOException ie) {
                LOG.info("Exception trying to close Process streams.", ie);
            }

            outPumper.flush();
            errorPumper.flush();
            flushCurrentElement();

            // The maven.bat actually never returns error,
            // due to internal cleanup called after the execution itself...
            if (exitCode != 0) {
                synchronized (buildLogElement) {
                    buildLogElement.setAttribute("error", "Return code is " + exitCode);
                }
            }
            if (buildLogElement.getAttribute("error") != null) {
                break;
            }

        }
        return buildLogElement;
    }

    //***************************** Param setters ****************************

    /**
     * The path to the Maven script
     * (i.e. the maven.bat file, because I tested this only on w2k)
     */
    public void setMavenScript(String mavenScript) {
        this.mavenScript = mavenScript;
    }

    /**
     * Maven goal to run. Supports sets of goals.
     * Examples:
     * "clean java:compile" will run 'clean' then 'java:compile' in one single invocation of Maven
     * "clean jar:jar|site:generate" will run 'clean' and 'jar:jar' in one invocation,
     *    then 'site:generate' in _another_ invocation. Useful for updating from SCM
     *    with Maven goals, then doing the actual build with freshly loaded files.
     *    Notice the '|' as separator of sets.
     */
    public void setGoal(String goal) {
        this.goal = goal;
    }

    /**
     * project.xml to use
     */
    public void setProjectFile(String projectFile) {
        this.projectFile = projectFile;
    }

    //******************** Command line generation **********************

    /**
     *  construct the command that we're going to execute.
     *  @param buildProperties Map holding key/value pairs of arguments to the build process
     *  @param goalset A set of goals to run (list, separated by emptyspace)
     *  @return String[] holding command to be executed
     */
    protected String[] getCommandLineArgs(Map buildProperties, boolean isWindows, String goalset) {
        List al = new ArrayList();

        if (mavenScript != null) {
            if (isWindows) {
                al.add("cmd.exe");
                al.add("/C");
                al.add(mavenScript);
            } else {
                al.add(mavenScript);
            }
        } else {
            throw new RuntimeException(
                "Non-script running is not implemented yet.\n"
                    + "As of 1.0-beta-10 Maven startup mechanism is still changing...");
        }

        Iterator propertiesIterator = buildProperties.keySet().iterator();
        while (propertiesIterator.hasNext()) {
            String key = (String) propertiesIterator.next();
            al.add("-D" + key + "=" + buildProperties.get(key));
        }

        if (LOG.isDebugEnabled()) {
            al.add("-X");
        }

        al.add("-b"); // no banner
        if (projectFile != null) {
            // we need only the name of the file
            File pFile = new File(projectFile);
            al.add("-p");
            al.add(pFile.getName());
        }
        if (goalset != null) {
            StringTokenizer stok = new StringTokenizer(goalset, " \t\r\n");
            while (stok.hasMoreTokens()) {
                al.add(stok.nextToken());
            }
        }

        if (LOG.isDebugEnabled()) {
            StringBuffer sb = new StringBuffer();
            sb.append("Executing Command: ");
            Iterator argIterator = al.iterator();
            while (argIterator.hasNext()) {
                String arg = (String) argIterator.next();
                sb.append(arg);
                sb.append(" ");
            }
            LOG.debug(sb.toString());
        }

        return (String[]) al.toArray(new String[al.size()]);
    }

    protected boolean isWindows() {
        String osn = System.getProperty("os.name");
        return osn.indexOf("Windows") >= 0;
    }

    /**
     * Produces sets of goals, ready to be run each in a distinct call to Maven.
     * Separation of sets in "goal" attribute is made with '|'.
     *
     * @return a List containing String elements
     */
    protected List getGoalSets() {
        List al = new ArrayList();
        if (goal != null) {
            StringTokenizer stok = new StringTokenizer(goal, "|");
            while (stok.hasMoreTokens()) {
                String subSet = stok.nextToken().trim();
                if (subSet == null || subSet.length() == 0) {
                    continue;
                }
                al.add(subSet);
            }
        }
        return al;
    }

    //********************* Log interception and production ******************

    /**
     * Ugly parsing of Maven output into some Elements.
     * Gets called from StreamPumper.
     */
    public synchronized void consumeLine(String line) {
        if (line == null || line.length() == 0 || buildLogElement == null) {
            return;
        }

        synchronized (buildLogElement) {
            // The BAT never returns errors, so I'll catch it like this. Brrr.
            if (line.startsWith("BUILD FAILED")) {
                buildLogElement.setAttribute("error", "BUILD FAILED detected");
            } else if (
                line.endsWith(":") // heuristically this is a goal marker,
                    && !line.startsWith(" ") // but debug lines might look like that
                    && !line.startsWith("\t")) {
                makeNewCurrentElement(line.substring(0, line.lastIndexOf(':')));
                return; // Do not log the goal itself
            }
            Element msg = new Element("message");
            msg.addContent(new CDATA(line));
            // Initially call it "info" level.
            // If "the going gets tough" we'll switch this to "error"
            msg.setAttribute("priority", "info");
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
            currentElement.setAttribute("time", "? seconds");
            return currentElement;
        }
    }

    private void flushCurrentElement() {
        if (buildLogElement == null) {
            return;
        }
        synchronized (buildLogElement) {
            if (currentElement != null) {
                if (buildLogElement.getAttribute("error") != null) {
                    // All the messages of the last (failed) goal should be
                    // switched to priority error
                    List lst = currentElement.getChildren("message");
                    if (lst != null) {
                        Iterator it = lst.iterator();
                        while (it.hasNext()) {
                            Element msg = (Element) it.next();
                            msg.setAttribute("priority", "error");
                        }
                    }
                }
                buildLogElement.addContent(currentElement);
            }
            currentElement = null;
        }
    }
}

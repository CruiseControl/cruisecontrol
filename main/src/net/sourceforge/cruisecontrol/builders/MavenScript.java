/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 600
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
 * Maven script class.
 *
 * Contains all the details related to running a Maven based build.
 * @author <a href="mailto:epugh@opensourceconnections.com">Eric Pugh</a>
 */
public class MavenScript implements Script, StreamConsumer {
    private static final Logger LOG = Logger.getLogger(MavenScript.class);
    
    private Map buildProperties;    
    private String goalset;    
    private String mavenScript;
    private String projectFile;
    private int exitCode;
    /** Global log to produce, passed in from MavenBuilder */
    private Element buildLogElement;
    private Element currentElement = null;

    /**
     *  construct the command that we're going to execute.
     *  @return Commandline holding command to be executed
     * @throws CruiseControlException
     */
    public Commandline buildCommandline() throws CruiseControlException {
        Commandline cmdLine = new Commandline();

        if (mavenScript != null) {
            cmdLine.setExecutable(mavenScript);
        } else {
            throw new CruiseControlException(
                "Non-script running is not implemented yet.\n"
                    + "As of 1.0-beta-10 Maven startup mechanism is still changing...");
        }

        Iterator propertiesIterator = buildProperties.keySet().iterator();
        while (propertiesIterator.hasNext()) {
            String key = (String) propertiesIterator.next();
            cmdLine.createArgument("-D" + key + "=" + buildProperties.get(key));
        }

        if (LOG.isDebugEnabled()) {
            cmdLine.createArgument("-X");
        }

        cmdLine.createArgument("-b"); // no banner
        if (projectFile != null) {
            // we need only the name of the file
            File pFile = new File(projectFile);
            cmdLine.createArguments("-p", pFile.getName());
        }
        if (goalset != null) {
            StringTokenizer stok = new StringTokenizer(goalset, " \t\r\n");
            while (stok.hasMoreTokens()) {
                cmdLine.createArgument(stok.nextToken());
            }
        }

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
            } else if (line.startsWith("org.apache.maven.MavenException")) {
                buildLogElement.setAttribute("error", "You have encountered an unknown error running Maven: " + line);
            } else if (line.startsWith("The build cannot continue")) {
                buildLogElement.setAttribute("error", "The build cannot continue: Unsatisfied Dependency");
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

    protected void flushCurrentElement() {
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
     * @param mavenScript The mavenScript to set.
     */
    public void setMavenScript(String mavenScript) {
        this.mavenScript = mavenScript;
    }
    /**
     * @param projectFile The projectFile to set.
     */
    public void setProjectFile(String projectFile) {
        this.projectFile = projectFile;
    }
    /**
     * @return Returns the exitCode.
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
     * @param buildLogElement The buildLogElement to set.
     */
    public void setBuildLogElement(Element buildLogElement) {
        this.buildLogElement = buildLogElement;
    }
}

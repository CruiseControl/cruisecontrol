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
package net.sourceforge.cruisecontrol.buildloggers;

import net.sourceforge.cruisecontrol.BuildLogger;
import net.sourceforge.cruisecontrol.CruiseControlException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.FileFilter;

/**
 * This BuildLogger implementation merges other XML logs into the CruiseControl
 * main log. It can work with either a single file, or a directory. If a
 * directory is specified, then all the XML files in that directory will be
 * merged into the CruiseControl log.
 *
 */
public class MergeLogger implements BuildLogger {

    private static final org.apache.log4j.Logger LOG4J =
            org.apache.log4j.Logger.getLogger(MergeLogger.class);

    private String file;
    private String dir;
    private boolean removeProperties = true;

    public void setFile(String file) {
        this.file = file;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void validate() throws CruiseControlException {
        if (file == null && dir == null) {
            throw new CruiseControlException(
                    "one of file or dir are required attributes");
        } else if (file != null && dir != null) {
            throw new CruiseControlException(
                    "only one of file or dir may be specified");
        }
    }

    public void log(Element buildLog) throws CruiseControlException {
        String nextLogFilename = ((file != null) ? file : dir);

        mergeFile(new File(nextLogFilename), buildLog);
    }

    /**
     * Recursive method that merges the specified file into the buildLog. If
     * the file is a directory, then all it's children that are XML files
     * are merged.
     */
    private void mergeFile(File nextLogFile, Element buildLog) {

        if (nextLogFile.isDirectory()) {
            File[] children = nextLogFile.listFiles(new FileFilter() {
                public boolean accept(File file) {
                    return file.isDirectory()
                            || file.getName().endsWith(".xml");
                }
            });
            for (int j = 0; j < children.length; j++) {
                mergeFile(children[j], buildLog);
            }
        } else {
            Element auxLogElement = getElement(nextLogFile);
            if (auxLogElement != null) {
                buildLog.addContent(auxLogElement.detach());
            }
        }
    }

    /**
     *  Get a JDOM <code>Element</code> from an XML file.
     *
     *  @param xmlFile The file name to read.
     *  @return JDOM <code>Element</code> representing that xml file.
     */
    Element getElement(File xmlFile) {
        try {
            SAXBuilder builder =
                    new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Element element = builder.build(xmlFile).getRootElement();
            if (element.getName().equals("testsuite") && removeProperties) {
                if (element.getChild("properties") != null) {
                    element.getChild("properties").detach();
                }
            }
            return element;
        } catch (JDOMException e) {
            LOG4J.warn("Could not read log: " + xmlFile + ".  Skipping...", e);
        }

        return null;
    }

    public void setRemoveProperties(boolean b) {
        removeProperties = b;
    }
}

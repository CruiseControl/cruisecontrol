/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 200 E. Randolph, 25th Floor
 * Chicago, IL 60601 USA
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommand;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevCommandline;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.AccurevInputParser;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.DateTimespec;
import net.sourceforge.cruisecontrol.sourcecontrols.accurev.Runner;

import org.apache.log4j.Logger;

/**
 * This class handles all Accurev aspects of determining the modifications since the last good
 * build.
 *
 * @author <a href="mailto:jason_chown@scee.net">Jason Chown </a>
 * @author <a href="mailto:Nicola_Orru@scee.net">Nicola Orru'</a>
 */
public class Accurev implements SourceControl, AccurevInputParser {
    private static final Logger LOG = Logger.getLogger(Accurev.class);
    private String stream;
    private boolean verbose;
    private ArrayList modifications;
    private Runner runner;
    private SourceControlProperties properties = new SourceControlProperties();

    /**
     * Sets the Accurev stream to search for changes
     *
     * @param stream the name of the stream
     */
    public void setStream(String stream) {
        this.stream = stream;
    }

    /**
     * Enables/disables verbose logging
     *
     * @param verbose set to true to enable verbose logging
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Choose a property to be set if the project has modifications
     *
     * @param propertyName the name of the property
     */
    public void setProperty(String propertyName) {
        properties.assignPropertyName(propertyName);
    }

    public void validate() throws CruiseControlException {
        if (stream == null) {
            throw new CruiseControlException("'stream' is a required attribute for Accurev");
        }
    }

    /**
     * Calls "accurev hist -s [stream] -t "[now] - [lastBuild]" or something like that ; )
     *
     * @param lastBuild the date and time of the last successful build
     * @param now       the current date and time
     * @return the List of all detected modifications
     */
    public List getModifications(Date lastBuild, Date now) {
        LOG.info("Accurev: getting modifications for " + stream);
        AccurevCommandline hist = AccurevCommand.HIST.create();
        if (runner != null) {
            hist.setRunner(runner);
        }
        hist.setVerbose(verbose);
        hist.setInputParser(this);
        hist.setStream(stream);
        hist.setTransactionRange(new DateTimespec(lastBuild), new DateTimespec(now));
        hist.run();
        return modifications;
    }

    /**
     * Parse the output from Accurev. These are lines of the form: <code>
     * transaction &lt;id>; &lt;verb&gt;; YYYY/MM/DD hh:mm:ss ; user: &lt;user&gt;
     * # &lt;comment&gt;
     * \.\PathTo\FileChanged.cpp &lt;version&gt;
     * </code>
     * <p/>
     * Where <verb>can be promote, chstream or purge. There can be multiple lines of comments and
     * files.
     *
     * @param input the output of the "accurev hist" command run
     * @return true at the end
     * @throws IOException
     */
    public boolean parseStream(InputStream input) throws IOException, CruiseControlException {
        modifications = new ArrayList();
        Modification modification = null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            LOG.debug(line);
            if (line.startsWith("transaction")) {
                // transaction <id>; <verb>; YYYY/MM/DD hh:mm:ss ; user: <user>
                modification = new Modification();
                String[] parts = getParts(line);
                modification.comment = "";
                modification.revision = parts[0].substring(parts[0].indexOf(' ') + 1);
                modification.type = parts[1].trim();
                modification.modifiedTime = DateTimespec.parse(parts[2].trim());
                modification.userName = parts[3].substring(6).trim();
                modifications.add(modification);
                properties.modificationFound();
            } else if (line.startsWith("  #")) {
                // # Comment
                if (modification != null) {
                    modification.comment += line.substring(3) + "\n";
                } else {
                    LOG.warn("Comment outside modification - skipping");
                }
                // Accurev is returning always \\ instead of File.separatorChar
            } else if (line.startsWith("  \\.\\") || line.startsWith("  /./")) {
                // ...but just for the sake of paranoia...
                final char separator = line.charAt(2);
                final int lastSlash = line.lastIndexOf(separator);
                int lastSpace = line.lastIndexOf(' ');
                lastSpace = line.lastIndexOf(' ', lastSpace - 1);
                if (lastSpace > lastSlash) {
                    String fileName = line.substring(lastSlash + 1, lastSpace);
                    String folderName = ((lastSlash > 5) ? line.substring(5, lastSlash) : line.substring(5)).replace(
                            separator, '/');
                    Modification.ModifiedFile modfile = modification.createModifiedFile(fileName, folderName);
                    modfile.action = "change";
                }
            }
        }
        return true;
    }

    private String[] getParts(String line) {
        List partsList = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(line, ";");
        while (tokenizer.hasMoreTokens()) {
            partsList.add(tokenizer.nextToken());
        }
        String[] parts = new String[partsList.size()];
        partsList.toArray(parts);
        return parts;
    }

    public void setRunner(Runner runner) {
        this.runner = runner;
    }

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }
}
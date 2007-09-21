/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * This class implements the SourceControl methods for a Store repository, which
 * is the version control system used by Cincom Smalltalk Visualworks.
 *
 * @see    <a href="http://smalltalk.cincom.com/">smalltalk.cincom.com</a>
 * @author <a href="rcoulman@gmail.com">Randy Coulman</a>
 */
public class Store implements SourceControl {

    private static final Logger LOG = Logger.getLogger(Store.class);

    /** Date format expected by Store */
    private static final String STORE_DATE_FORMAT = "MM/dd/yyyy HH:mm:ss.SSS";

    private final SourceControlProperties properties = new SourceControlProperties();

    /** Configuration parameters */
    private String workingDirectory;
    private String script;
    private String profile;
    private List packages;
    private String versionRegex;
    private String minimumBlessingLevel;
    private String parcelBuilderFile;

    public Map getProperties() {
        return properties.getPropertiesAndReset();
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    /**
     * Sets the working directory to use when interacting with Store.
     * 
     * @param directory  String indicating the directory to use as the 
     *                   working directory
     */
    public void setWorkingDirectory(String directory) {
        this.workingDirectory = directory;
    }

    /**
     * Sets the script to use to make calls to Store.
     * 
     * This script should start a VisualWorks image with the CruiseControl
     * package loaded and pass on the rest of the command-line arguments 
     * supplied by this plugin.
     * 
     * @param script  String indicating the executable script to
     *                use when making calls to Store.
     */
    public void setScript(String script) {
        this.script = script;
    }

    /**
     * Sets the name of the Store profile to check for modifications.
     *
     * @param profile  String indicating the name of the Store profile to
     *                 connect to when checking for modifications
     */
    public void setProfile(String profile) {
        this.profile = profile;
    }

    /**
     * Sets the list of Store packages to be checked.
     *
     * @param packageNames  a comma-separated list of package names
     */
    public void setPackages(String packageNames) {
        if (packageNames != null) {
            StringTokenizer st = new StringTokenizer(packageNames, ",");
            this.packages = new ArrayList();
            while (st.hasMoreTokens()) {
                this.packages.add(st.nextToken());
            }
        }
    }

    /**
     * Sets a regex to use to select versions of interest.
     *
     * @param regex  String containing a regular expression that 
     *               matches versions of interest
     */
    public void setVersionRegex(String regex) {
        this.versionRegex = regex;
    }

    /**
     * Sets a minimum blessing level to select versions of interest.
     *
     * @param blessing  String containing the minimum blessing level
     *                  that package versions must have to be included
     */
    public void setMinimumBlessingLevel(String blessing) {
        this.minimumBlessingLevel = blessing;
    }

    /**
     * Sets the name of a file to store the list of head package versions.
     *
     * @param filename  String containing the filename used to store input
     *                  for ParcelBuilder to use to deploy parcels
     */
    public void setParcelBuilderFile(String filename) {
        this.parcelBuilderFile = filename;
    }

    /**
     * This method validates that at least the repository location or the local
     * working copy location has been specified.
     *
     * @throws CruiseControlException  Thrown when the repository location and
     *                                 the local working copy location are both
     *                                 null
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(workingDirectory != null,
                                    "'workingDirectory is a required attribute on the Store task");
        if (workingDirectory != null) {
            File directory = new File(workingDirectory);
            ValidationHelper.assertTrue(directory.exists() && directory.isDirectory(),
                                        "'workingDirectory' must be an existing directory. Was "
                                        + directory.getAbsolutePath());
        }
        
        ValidationHelper.assertTrue(script != null, "'script' is a required attribute on the Store task");
        if (script != null) {
            File scriptFile = new File(script);
            ValidationHelper.assertTrue(scriptFile.exists(), "'script' must be an existing file. Was "
                                        + scriptFile.getAbsolutePath());
        }
        
        ValidationHelper.assertTrue(profile != null, "'profile' is a required attribute on the Store task");
        ValidationHelper.assertTrue(packages != null, "'packages' is a required attribute on the Store task");
        ValidationHelper.assertTrue(packages.size() > 0, "'packages' must specify at least one package");
    }

    /**
     * Returns a list of modifications detailing all the changes between
     * the last build and the latest revision in the repository.
     * @return the list of modifications, or an empty list if we failed
     * to retrieve the changes.
     */
    public List getModifications(Date lastBuild, Date now) {
        List modifications = new ArrayList();
        Commandline command;
        try {
            command = buildCommand(lastBuild, now);
        } catch (CruiseControlException e) {
            LOG.error("Error building history command", e);
            return modifications;
        }
        try {
            modifications = execCommand(command);
        } catch (Exception e) {
            LOG.error("Error executing svn log command " + command, e);
        }
        fillPropertiesIfNeeded(modifications);
        return modifications;
    }


    /**
     * Generates the command line for the store log command.
     *
     * For example:
     *
     * 'storeScript -profile local -packages PackageA "Package B" -lastBuild {lastbuildtime} -now {currentTime} -check'
     */
    Commandline buildCommand(Date lastBuild, Date checkTime) throws CruiseControlException {
        Commandline command = new Commandline();
        command.setWorkingDirectory(workingDirectory);
        command.setExecutable(script);

        command.createArguments("-profile", profile);
        command.createArgument("-packages");
        for (Iterator iterator = packages.iterator(); iterator.hasNext(); ) {
            command.createArgument((String) iterator.next());
        }
        if (versionRegex != null) {
            command.createArguments("-versionRegex", versionRegex);
        }
        if (minimumBlessingLevel != null) {
            command.createArguments("-blessedAtLeast", minimumBlessingLevel);
        }
        command.createArguments("-lastBuild", formatDate(lastBuild));
        command.createArguments("-now", formatDate(checkTime));
        if (parcelBuilderFile != null) {
            command.createArguments("-parcelBuilderFile", parcelBuilderFile);
        }
        command.createArgument("-check");

        LOG.debug("Executing command: " + command);

        return command;
    }

    static String formatDate(Date date) {
        return getDateFormatter().format(date);
    }

    private List execCommand(Commandline command)
        throws InterruptedException, IOException, ParseException, JDOMException {

        Process p = command.execute();

        Thread stderr = logErrorStream(p);
        InputStream storeStream = p.getInputStream();
        List modifications = parseStream(storeStream);

        p.waitFor();
        stderr.join();
        IO.close(p);

        return modifications;
    }

    private static Thread logErrorStream(Process p) {
        Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, p.getErrorStream()));
        stderr.start();
        return stderr;
    }

    private List parseStream(InputStream storeStream)
        throws JDOMException, IOException, ParseException {

        final InputStreamReader reader = new InputStreamReader(storeStream, "UTF-8");
        try {
            return StoreLogXMLParser.parse(reader);
        } finally {
            reader.close();
        }
    }

    void fillPropertiesIfNeeded(List modifications) {
        if (!modifications.isEmpty()) {
            properties.modificationFound();
        }
    }

    public static DateFormat getDateFormatter() {
        DateFormat f = new SimpleDateFormat(STORE_DATE_FORMAT);
        f.setTimeZone(TimeZone.getTimeZone("GMT"));
        return f;
    }

    static final class StoreLogXMLParser {

        private StoreLogXMLParser() {
        }

        static List parse(Reader reader)
            throws ParseException, JDOMException, IOException {

            SAXBuilder builder = new SAXBuilder(false);
            Document document = builder.build(reader);
            return parseDOMTree(document);
        }

        static List parseDOMTree(Document document) throws ParseException {
            List modifications = new ArrayList();

            Element rootElement = document.getRootElement();
            List packageEntries = rootElement.getChildren("package");
            for (Iterator iterator = packageEntries.iterator(); iterator.hasNext(); ) {
                Element packageEntry = (Element) iterator.next();

                List modificationsOfRevision = parsePackageEntry(packageEntry);
                modifications.addAll(modificationsOfRevision);
            }

            return modifications;
        }

        static List parsePackageEntry(Element packageEntry) throws ParseException {
            List modifications = new ArrayList();

            List blessings = packageEntry.getChildren("blessing");
            for (Iterator iterator = blessings.iterator(); iterator.hasNext();) {
                Element blessing = (Element) iterator.next();

                Modification modification = new Modification("store");

                modification.modifiedTime = convertDate(blessing.getAttributeValue("timestamp"));
                modification.userName = blessing.getAttributeValue("user");
                modification.comment = blessing.getText();
                modification.revision = packageEntry.getAttributeValue("version");

                Modification.ModifiedFile modfile =
                    modification.createModifiedFile(packageEntry.getAttributeValue("name"), null);
                modfile.action = packageEntry.getAttributeValue("action");
                modfile.revision = modification.revision;

                modifications.add(modification);
            }

            return modifications;
        }

        /**
         * Converts the specified Store date string into a Date.
         * @param date with format "MM/dd/yyyy HH:mm:ss.SSS"
         * @return converted date
         * @throws ParseException if specified date doesn't match the expected format 
         */
        static Date convertDate(String date) throws ParseException {
            return getDateFormatter().parse(date);
        }
    }
}

/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

/**
 * This class implements the SourceControl methods for a Subversion repository.
 * The call to Subversion is assumed to work without any setup. This implies
 * that either authentication data must be available or the login parameters are
 * specified in the cc configuration file.
 *
 * Note: You can also observe for changes a Subversion repository that you have
 *       not checked out locally.
 *
 * @see    <a href="http://subversion.tigris.org/">subversion.tigris.org</a>
 * @author <a href="etienne.studer@canoo.com">Etienne Studer</a>
 */
public class SVN implements SourceControl {

    private static final Logger LOG = Logger.getLogger(SVN.class);

    /** Date format expected by Subversion */
    static final SimpleDateFormat SVN_DATE_FORMAT_IN =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    /** Date format returned by Subversion in XML output */
    static final SimpleDateFormat SVN_DATE_FORMAT_OUT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /** Set the time zone of the formatters to GMT. */
    static {
        SVN_DATE_FORMAT_IN.setTimeZone(TimeZone.getTimeZone("GMT"));
        SVN_DATE_FORMAT_OUT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private Hashtable properties = new Hashtable();

    /** Configuration parameters */
    private String repositoryLocation;
    private String localWorkingCopy;
    private String userName;
    private String password;

    public Hashtable getProperties() {
        return properties;
    }

    public void setProperty(String unused) {
        throw new UnsupportedOperationException("attribute 'property' is not supported");
    }

    public void setPropertyOnDelete(String unused) {
        throw new UnsupportedOperationException("attribute 'propertyOnDelete' is not supported");
    }

    /**
     * Sets the repository location to use when making calls to Subversion.
     *
     * @param repositoryLocation  String indicating the url to the Subversion
     *                            repository on which to find the log history.
     */
    public void setRepositoryLocation(String repositoryLocation) {
        this.repositoryLocation = repositoryLocation;
    }

    /**
     * Sets the local working copy to use when making calls to Subversion.
     *
     * @param localWorkingCopy  String indicating the relative or absolute path
     *                          to the local working copy of the Subversion
     *                          repository of which to find the log history.
     */
    public void setLocalWorkingCopy(String localWorkingCopy) {
        this.localWorkingCopy = localWorkingCopy;
    }

    /**
     * Sets the username for authentication.
     */
    public void setUsername(String userName) {
        this.userName = userName;
    }

    /**
     * Sets the password for authentication.
     */
    public void setPassword(String password) {
        this.password = password;
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
        if (repositoryLocation == null && localWorkingCopy == null) {
            throw new CruiseControlException(
                "At least 'repositoryLocation'or 'localWorkingCopy' "
                    + "is a required attribute on the Subversion task ");
        }

        if (localWorkingCopy != null) {
            File workingDir = new File(localWorkingCopy);
            if (!workingDir.exists() || !workingDir.isDirectory()) {
                throw new CruiseControlException(
                    "'localWorkingCopy' must be an existing " + "directory.");
            }
        }
    }

    /**
     * Returns a list of modifications detailing all the changes between
     * the last build and the latest revision in the repository.
     */
    public List getModifications(Date lastBuild, Date now) {
        List modifications = new ArrayList();
        try {
            String headRevision = getHeadRevision();
            properties.put("svn.revision", headRevision);

            Commandline command = buildHistoryCommand(lastBuild, headRevision);
            modifications = execHistoryCommand(command, lastBuild);
        } catch (Exception e) {
            LOG.error("Error executing svn log command", e);
        }
        return modifications;
    }

    private String getHeadRevision()
            throws CruiseControlException, InterruptedException, IOException, ParseException, JDOMException {
        String headRevision = "HEAD";
        Commandline command = buildHistoryCommand(null, headRevision);
        List revisionCheck = execHistoryCommand(command, null);

        if (revisionCheck.size() == 0) {
            LOG.error("Unable to determine HEAD revision (svn.revision will be set to 'HEAD')");
        } else {
            headRevision = ((Modification) revisionCheck.get(0)).revision;
        }
        return headRevision;
    }

    /**
     * Generates the command line for the svn log command.
     *
     * For example:
     *
     * 'svn log --non-interactive --xml -v -r {lastbuildTime}:headRevision repositoryLocation'
     */
    Commandline buildHistoryCommand(Date lastBuild, String headRevision) throws CruiseControlException {
        Commandline command = new Commandline();
        command.setExecutable("svn");

        if (localWorkingCopy != null) {
            command.setWorkingDirectory(localWorkingCopy);
        }

        command.createArgument().setValue("log");
        command.createArgument().setValue("--non-interactive");
        command.createArgument().setValue("--xml");
        command.createArgument().setValue("-v");
        command.createArgument().setValue("-r");
        if (lastBuild == null) {
            command.createArgument().setValue("HEAD:COMMITTED");
        } else {
            command.createArgument().setValue(
                "{" + SVN_DATE_FORMAT_IN.format(lastBuild) + "}" + ":" + headRevision);
        }
        if (userName != null) {
            command.createArgument().setValue("--username");
            command.createArgument().setValue(userName);
        }
        if (password != null) {
            command.createArgument().setValue("--password");
            command.createArgument().setValue(password);
        }
        if (repositoryLocation != null) {
            command.createArgument().setValue(repositoryLocation);
        }

        LOG.debug("Executing command: " + command);

        return command;
    }

    private List execHistoryCommand(Commandline command, Date lastBuild)
        throws InterruptedException, IOException, ParseException, JDOMException {

        Process p = command.execute();

        logErrorStream(p);
        InputStream svnStream = p.getInputStream();
        List modifications = parseStream(svnStream, lastBuild);

        p.waitFor();
        p.getInputStream().close();
        p.getOutputStream().close();
        p.getErrorStream().close();

        return modifications;
    }

    private void logErrorStream(Process p) {
        StreamPumper errorPumper =
            new StreamPumper(p.getErrorStream(), new PrintWriter(System.err, true));
        new Thread(errorPumper).start();
    }

    private List parseStream(InputStream svnStream, Date lastBuild)
        throws JDOMException, IOException, ParseException, UnsupportedEncodingException {

        return SVNLogXMLParser.parseAndFilter(svnStream, lastBuild);
    }

    static final class SVNLogXMLParser {
        
        private SVNLogXMLParser() {
            
        }
        
        static List parseAndFilter(InputStream inputStream, Date lastBuild)
            throws JDOMException, IOException, ParseException, UnsupportedEncodingException {

            Modification[] modifications = parse(inputStream);
            return filterModifications(modifications, lastBuild);
        }

        static List parseAndFilter(String xmlContent, Date lastBuild)
            throws JDOMException, ParseException, IOException {
            // TODO: why isn't this delegating to the InputStream version?
            Modification[] modifications = parse(xmlContent);
            return filterModifications(modifications, lastBuild);
        }

        static Modification[] parse(InputStream inputStream)
            throws JDOMException, IOException, ParseException, UnsupportedEncodingException {
                
            SAXBuilder builder = new SAXBuilder(false);
            Document document = builder.build(new InputStreamReader(inputStream, "UTF-8"));
            return parseDOMTree(document);
        }

        static Modification[] parse(String xmlContent) throws JDOMException, ParseException, IOException {
            // TODO: why isn't this delegating to the inputStream version?
            SAXBuilder builder = new SAXBuilder(false);
            // getBytes()?!? TODO: write the test to show how this is broken for i18n
            Document document = builder.build(new ByteArrayInputStream(xmlContent.getBytes()));
            return parseDOMTree(document);
        }

        static Modification[] parseDOMTree(Document document) throws ParseException {
            List modifications = new ArrayList();

            Element rootElement = document.getRootElement();
            List logEntries = rootElement.getChildren("logentry");
            for (Iterator iterator = logEntries.iterator(); iterator.hasNext();) {
                Element logEntry = (Element) iterator.next();

                Modification[] modificationsOfRevision = parseLogEntry(logEntry);
                modifications.addAll(Arrays.asList(modificationsOfRevision));
            }

            return (Modification[]) modifications.toArray(new Modification[modifications.size()]);
        }

        static Modification[] parseLogEntry(Element logEntry) throws ParseException {
            List modifications = new ArrayList();

            Element logEntryPaths = logEntry.getChild("paths");
            List paths = logEntryPaths.getChildren("path");
            for (Iterator iterator = paths.iterator(); iterator.hasNext();) {
                Element path = (Element) iterator.next();

                Modification modification = new Modification();
                modification.modifiedTime = convertDate(logEntry.getChildText("date"));
                modification.userName = logEntry.getChildText("author");
                modification.comment = logEntry.getChildText("msg");
                modification.revision = logEntry.getAttributeValue("revision");
                modification.folderName = "";
                modification.fileName = path.getText();
                modification.type = convertAction(path.getAttributeValue("action"));
                modifications.add(modification);
            }

            return (Modification[]) modifications.toArray(new Modification[modifications.size()]);
        }

        static Date convertDate(String date) throws ParseException {
            String withoutMicroSeconds = date.substring(0, date.indexOf('Z') - 3);
            return SVN_DATE_FORMAT_OUT.parse(withoutMicroSeconds);
        }

        static String convertAction(String action) {
            if (action.equals("A")) {
                return "added";
            }
            if (action.equals("M")) {
                return "modified";
            }
            if (action.equals("D")) {
                return "deleted";
            }
            return "unknown";
        }

        /**
         * Unlike CVS, Subversion maps dates to revisions which leads to a
         * different behavior when using the svn log command in conjunction with
         * dates, e.g., a date maps to a revision but the revision may have been
         * created earlier than the specified date. Therefore, if we are only
         * interested in changes that took place after the last build date, we
         * have to filter the modifications returned from the log command and
         * omit modifications that are older than the last build date.
         *
         * @see <a href="http://subversion.tigris.org/">subversion.tigris.org</a>
         */
        static List filterModifications(Modification[] modifications, Date lastBuild) {
            List filtered = new ArrayList();
            for (int i = 0; i < modifications.length; i++) {
                Modification modification = modifications[i];
                if (lastBuild == null || modification.modifiedTime.getTime() > lastBuild.getTime()) {
                    filtered.add(modification);
                }
            }
            return filtered;
        }
    }
}
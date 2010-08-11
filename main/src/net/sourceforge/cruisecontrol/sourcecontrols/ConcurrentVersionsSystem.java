/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.Modification.ModifiedFile;
import net.sourceforge.cruisecontrol.util.CVSDateUtil;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.DiscardConsumer;
import net.sourceforge.cruisecontrol.util.OSEnvironment;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.StreamPumper;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.IO;

import org.apache.log4j.Logger;

/**
 * This class implements the SourceControlElement methods for a CVS repository. The call to CVS is assumed to work
 * without any setup. This implies that if the authentication type is pserver the call to cvs login should be done prior
 * to calling this class. <p/> There are also differing CVS client/server implementations (e.g. the <i>official</i> CVS
 * and the CVSNT fork). <p/> Note that the LOG formats of the official CVS have changed starting from version 1.12.9.
 * This class currently knows of 2 different outputs referred to as the 'old' and the 'new' output formats.
 * 
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius</a>
 * @author Robert Watkins
 * @author Frederic Lavigne
 * @author <a href="mailto:jcyip@thoughtworks.com">Jason Yip</a>
 * @author Marc Paquette
 * @author <a href="mailto:johnny.cass@epiuse.com">Johnny Cass</a>
 * @author <a href="mailto:m@loonsoft.com">McClain Looney</a>
 * @author Detlef Keil
 */
public class ConcurrentVersionsSystem implements SourceControl, Cloneable {
    private static final long serialVersionUID = -3714548093682602092L;
    /**
     * name of the official cvs as returned as part of the 'cvs version' command output
     */
    static final String OFFICIAL_CVS_NAME = "CVS";
    static final Version DEFAULT_CVS_SERVER_VERSION = new Version(OFFICIAL_CVS_NAME, "1.11");
    public static final String LOG_DATE_FORMAT = "yyyy/MM/dd HH:mm:ss z";
    private boolean reallyQuiet;
    private String compression;

    /**
     * This paramter has only effect if local is set and is not itself
     * a under control of CVS (that is, does not have a CVS subdirectory).
     * If set to true all subdirectories are searched recursively.
     * All subdirectories which are under control of CVS are searched for
     * modifications in the usual manner.
     */
    private boolean recurseLocalWorkingCopy;

    /**
     * Represents the version of a CVS client or server
     */
    static class Version implements Serializable {

        private static final long serialVersionUID = -2433230091640056090L;

        private final String cvsName;
        private final String cvsVersion;

        public Version(String name, String version) {
            if (name == null) {
                throw new IllegalArgumentException("name can't be null");
            }
            if (version == null) {
                throw new IllegalArgumentException("version can't be null");
            }
            this.cvsName = name;
            this.cvsVersion = version;
        }

        public String getCvsName() {
            return cvsName;
        }

        public String getCvsVersion() {
            return cvsVersion;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof Version)) {
                return false;
            }

            final Version version = (Version) o;

            if (!cvsName.equals(version.cvsName)) {
                return false;
            } else if (!cvsVersion.equals(version.cvsVersion)) {
                return false;
            }

            return true;
        }

        public int hashCode() {
            int result;
            result = cvsName.hashCode();
            result = 29 * result + cvsVersion.hashCode();
            return result;
        }

        public String toString() {
            return cvsName + " " + cvsVersion;
        }
    }

    private SourceControlProperties properties = new SourceControlProperties();

    /**
     * CVS allows for mapping user names to email addresses. If CVSROOT/users exists, it's contents will be parsed and
     * stored in this hashtable.
     */
    private Hashtable<String, String> mailAliases;

    /**
     * The caller can provide the CVSROOT to use when calling CVS, or the CVSROOT environment variable will be used.
     */
    private String cvsroot;

    /**
     * The caller must indicate where the local copy of the repository exists.
     */
    private String local;

    /**
     * The CVS tag we are dealing with.
     */
    private String tag;

    /**
     * The CVS module we are dealing with.
     */
    private String module;

    /**
     * The version of the cvs server
     */
    private Version cvsServerVersion;
    
   /**
     * If set to true, the mailAliases from CVSROOT/users are not fetched.  
     */
    private boolean skipEmailsFetching = false;

    /**
     * enable logging for this class
     */
    private static final Logger LOG = Logger.getLogger(ConcurrentVersionsSystem.class);

    /**
     * This line delimits separate files in the CVS LOG information.
     */
    private static final String CVS_FILE_DELIM = "==================================================================="
            + "==========";

    /**
     * This is the keyword that precedes the name of the RCS filename in the CVS LOG information.
     */
    private static final String CVS_RCSFILE_LINE = "RCS file: ";

    /**
     * This is the keyword that precedes the name of the working filename in the CVS LOG information.
     */
    private static final String CVS_WORKINGFILE_LINE = "Working file: ";

    /**
     * This line delimits the different revisions of a file in the CVS LOG information.
     */
    private static final String CVS_REVISION_DELIM = "----------------------------";

    /**
     * This is the keyword that precedes the timestamp of a file revision in the CVS LOG information.
     */
    private static final String CVS_REVISION_DATE = "date:";

    /**
     * This is the name of the tip of the main branch, which needs special handling with the LOG entry parser
     */
    private static final String CVS_HEAD_TAG = "HEAD";

    /**
     * This is the keyword that tells us when we have reached the end of the header as found in the CVS LOG information.
     */
    private static final String CVS_DESCRIPTION = "description:";

    /**
     * This is a state keyword which indicates that a revision to a file was not relevant to the current branch, or the
     * revision consisted of a deletion of the file (removal from branch..).
     */
    private static final String CVS_REVISION_DEAD = "dead";

    /**
     * System dependent new line separator.
     */
    private static final String NEW_LINE = System.getProperty("line.separator");

    /**
     * This is the date format returned in the LOG information from CVS.
     */
    private final SimpleDateFormat logDateFormatter = new SimpleDateFormat(LOG_DATE_FORMAT);

    /**
     * Sets the CVSROOT for all calls to CVS.
     * 
     * @param cvsroot
     *            CVSROOT to use.
     */
    public void setCvsRoot(String cvsroot) {
        this.cvsroot = cvsroot;
    }

    /**
     * Sets the local working copy to use when making calls to CVS.
     * 
     * @param local
     *            String indicating the relative or absolute path to the local working copy of the module of which to
     *            find the LOG history.
     */
    public void setLocalWorkingCopy(String local) {
        this.local = local;
    }

    /**
     * Sets the behavior when a local working coppy is set which is
     * not under control of CVS itself (that is, does not have a CVS
     * subdirectory).
     * @param recurseLocalWorkingCopy If set to true all subdirectories are searched recursively.
     * All subdirectories which are under control of CVS are searched for
     * modifications in the usual manner.
     */
    public void setRecurseLocalWorkingCopy(final boolean recurseLocalWorkingCopy) {
        this.recurseLocalWorkingCopy = recurseLocalWorkingCopy;
    }

    /**
     * Set the cvs tag. Note this should work with names, numbers, and anything else you can put on LOG -rTAG
     * 
     * @param tag
     *            the cvs tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Set the cvs module name. Note that this is only used when localworkingcopy is not set.
     * 
     * @param module
     *            the cvs module
     */
    public void setModule(String module) {
        this.module = module;
    }

    public void setProperty(String property) {
        properties.assignPropertyName(property);
    }

    public void setPropertyOnDelete(String propertyOnDelete) {
        properties.assignPropertyOnDeleteName(propertyOnDelete);
    }

    /**
     * @param reallyQuiet When true, this class should use the -Q cvs option instead of -q for the LOG command.
     */
    public void setReallyQuiet(boolean reallyQuiet) {
        this.reallyQuiet = reallyQuiet;
    }

    /**
     * Sets the compression level used for the call to cvs, corresponding to the "-z" command line parameter. When not
     * set, the command line parameter is NOT included.
     *
     * @param level Valid levels are 1 (high speed, low compression) to 9 (low speed, high compression), or 0
     * to disable compression.
     */
    public void setCompression(String level) {
        compression = level;
    }
    
    /**
     * @param skipEmailsFetching If set to true, the CVSROOT/users won't be fetched.
     */
    public void setSkipEmailsFetching(final boolean skipEmailsFetching) {
        this.skipEmailsFetching = skipEmailsFetching;
    }

     protected Version getCvsServerVersion() {
        if (cvsServerVersion == null) {

            Commandline commandLine = getCommandline();
            commandLine.setExecutable("cvs");

            if (cvsroot != null) {
                commandLine.createArguments("-d", cvsroot);
            }

            commandLine.createArgument().setLine("version");

            Process p = null;
            try {
                if (local != null) {
                    commandLine.setWorkingDirectory(local);
                }

                p = commandLine.execute();
                Thread stderr = logErrorStream(p);
                InputStream is = p.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));

                cvsServerVersion = extractCVSServerVersionFromCVSVersionCommandOutput(in);

                LOG.debug("cvs server version: " + cvsServerVersion);

                p.waitFor();
                stderr.join();
                IO.close(p);
            } catch (IOException e) {
                LOG.error("Failed reading cvs server version", e);
            } catch (CruiseControlException e) {
                LOG.error("Failed reading cvs server version", e);
            } catch (InterruptedException e) {
                LOG.error("Failed reading cvs server version", e);
            }

            if (p == null || p.exitValue() != 0 || cvsServerVersion == null) {
                if (p == null) {
                    LOG.debug("Process p was null in CVS.getCvsServerVersion()");
                } else {
                    LOG.debug("Process exit value = " + p.exitValue());
                }
                cvsServerVersion = DEFAULT_CVS_SERVER_VERSION;
                LOG.warn("problem getting cvs server version; using " + cvsServerVersion);
            }
        }
        return cvsServerVersion;
    }

    /**
     * This method retrieves the cvs server version from the specified output. The line it parses will have the
     * following format:
     * 
     * <pre>
     *    Server: Concurrent Versions System (CVS) 1.11.16 (client/server)
     * </pre>
     * 
     * @param in reader
     * @return the version of null if the version couldn't be extracted
     * @throws IOException if something breaks
     */
    private Version extractCVSServerVersionFromCVSVersionCommandOutput(final BufferedReader in) throws IOException {
        String line = in.readLine();
        if (line == null) {
            return null;
        }
        if (line.startsWith("Client:")) {
            line = in.readLine();
            if (line == null) {
                return null;
            }
            if (!line.startsWith("Server:")) {
                LOG.warn("Warning expected a line starting with \"Server:\" but got " + line);
                // we try anyway
            }
        }
        LOG.debug("server version line: " + line);
        final int nameBegin = line.indexOf(" (");
        final int nameEnd = line.indexOf(") ", nameBegin);
        final String name;
        final String version;
        if (nameBegin == -1 || nameEnd < nameBegin || nameBegin + 2 >= line.length()) {
            LOG.warn("cvs server version name couldn't be parsed from " + line);
            return null;
        }
        name = line.substring(nameBegin + 2, nameEnd);
        int verEnd = line.indexOf(" ", nameEnd + 2);
        if (verEnd < nameEnd + 2) {
            LOG.warn("cvs server version number couldn't be parsed from " + line);
            return null;
        }
        version = line.substring(nameEnd + 2, verEnd);

        return new Version(name, version);
    }

    public boolean isCvsNewOutputFormat() {
        final Version version = getCvsServerVersion();
        if (OFFICIAL_CVS_NAME.equals(version.getCvsName())) {
            final String csv = version.getCvsVersion();
            final StringTokenizer st = new StringTokenizer(csv, ".");
            try {
                st.nextToken();
                final int subversion = Integer.parseInt(st.nextToken());
                if (subversion > 11) {
                    if (subversion == 12) {
                        if (Integer.parseInt(st.nextToken()) < 9) {
                            return false;
                        }
                    }
                    return true;
                }
            } catch (Throwable e) {
                LOG.warn("problem identifying cvs server. Assuming output is of 'old' type");
            }
        }
        return false;
    }

    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }

    /*
     * for mocking
     */
    protected OSEnvironment getOSEnvironment() {
        return new OSEnvironment();
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(local == null && (cvsroot == null || module == null),
                "must specify either 'localWorkingCopy' or 'cvsroot' and 'module' on CVS");
        ValidationHelper.assertFalse(local != null && (cvsroot != null || module != null),
                "if 'localWorkingCopy' is specified then cvsroot and module are not allowed on CVS");

        ValidationHelper.assertFalse(local == null && recurseLocalWorkingCopy,
                "'recurseLocalWorkingCopy' can only be set to true when 'localWorkingCopy' is specified.");

        ValidationHelper.assertFalse(local != null && !new File(local).exists(), "Local working copy \"" + local
                + "\" does not exist!");

        if (compression != null) {
            ValidationHelper.assertIntegerInRange(compression, 0, 9,
                    "'compression' must be an integer between 0 and 9, inclusive.");
        }
    }

    /**
     * Returns a List of Modifications detailing all the changes between the last build and the latest revision at the
     * repository
     *
     * @param lastBuild
     *            last build time
     * @return maybe empty, never null.
     */
    public List<Modification> getModifications(final Date lastBuild, final Date now) {

        mailAliases = getMailAliases();

        if (recurseLocalWorkingCopy) {
            if (localDirectoryNotUnderCVS()) {
                return getModificationsFromSubdirectories(lastBuild, now);
            }
        }

        List<Modification> mods = null;
        try {
            mods = execHistoryCommand(buildHistoryCommand(lastBuild, now));
        } catch (Exception e) {
            LOG.error("Log command failed to execute successfully", e);
        }

        if (mods == null) {
            return new ArrayList<Modification>();
        }
        return mods;
    }

    private List<Modification> getModificationsFromSubdirectories(Date lastBuild, Date now) {
        List<Modification> modifications = new ArrayList<Modification>();

        File[] subDirectories = new File(local).listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });
        
        if (subDirectories == null) {
            return modifications;
        }

        for (File dir : subDirectories) {
            try {
                ConcurrentVersionsSystem delegate = (ConcurrentVersionsSystem) this.clone();
                delegate.setLocalWorkingCopy(dir.getPath());
                delegate.setSkipEmailsFetching(true);
                List<Modification> modsFromSubdirectory = delegate.getModifications(lastBuild, now);
                for (Modification mod : modsFromSubdirectory) {
                    for (Object o : mod.files) {
                        ModifiedFile modfile = (ModifiedFile) o;
                        modfile.folderName = dir.getName() + '/' + modfile.folderName;
                    }
                }
                modifications.addAll(modsFromSubdirectory);
            } catch (CloneNotSupportedException e) {
                LOG.error("this should never happen", e);
                throw new RuntimeException(e);
            }
        }
        
        return modifications;
    }

    private boolean localDirectoryNotUnderCVS() {
        File cvsDir = new File(local, "CVS");
        return !cvsDir.exists() || !cvsDir.isDirectory();
    }

    /**
     * Get CVS's idea of user/address mapping. Only runs once per class instance. Won't run if the mailAlias was already
     * set.
     * 
     * @return a Hashtable containing the mapping defined in CVSROOT/users. If CVSROOT/users doesn't exist, an empty
     *         Hashtable is returned.
     */
    private Hashtable<String, String> getMailAliases() {
        if (mailAliases == null) {
            if (skipEmailsFetching) {
                mailAliases = new Hashtable<String, String>();
                return mailAliases;
            }

            mailAliases = new Hashtable<String, String>();
            Commandline commandLine = getCommandline();
            commandLine.setExecutable("cvs");

            if (cvsroot != null) {
                commandLine.createArguments("-d", cvsroot);
            }

            commandLine.createArgument().setLine("-q co -p CVSROOT/users");

            Process p = null;
            try {
                if (local != null) {
                    commandLine.setWorkingDirectory(local);
                }

                p = commandLine.execute();
                Thread stderr = logErrorStream(p);
                InputStream is = p.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(is));

                String line;
                while ((line = in.readLine()) != null) {
                    addAliasToMap(line);
                }

                p.waitFor();
                stderr.join();
                IO.close(p);
            } catch (Exception e) {
                LOG.error("Failed reading mail aliases", e);
            }

            if (p == null || p.exitValue() != 0) {
                if (p == null) {
                    LOG.debug("Process p was null in CVS.getMailAliases()");
                } else {
                    LOG.debug("Process exit value = " + p.exitValue());
                }
                LOG.warn("problem getting CVSROOT/users; using empty email map");
                mailAliases = new Hashtable<String, String>();
            }
        }

        return mailAliases;
    }

    void addAliasToMap(String line) {
        LOG.debug("Mapping " + line);
        int colon = line.indexOf(':');

        if (colon >= 0) {
            String user = line.substring(0, colon);
            String address = line.substring(colon + 1);
            mailAliases.put(user, address);

        }
    }

    /**
     * @param lastBuildTime last build date
     * @param checkTime current time
     * @return CommandLine for "cvs -d CVSROOT -q LOG -N -dlastbuildtime<checktime "
     * @throws CruiseControlException if something breaks
     */
    public Commandline buildHistoryCommand(final Date lastBuildTime, final Date checkTime)
            throws CruiseControlException {

        final Commandline commandLine = getCommandline();
        commandLine.setExecutable("cvs");

        if (compression != null) {
            commandLine.createArgument("-z" + compression);
        }
        if (cvsroot != null) {
            commandLine.createArguments("-d", cvsroot);
        }
        commandLine.createArgument(reallyQuiet ? "-Q" : "-q");

        if (local != null) {
            commandLine.setWorkingDirectory(local);
            commandLine.createArgument("log");
        } else {
            commandLine.createArgument("rlog");
        }

        commandLine.createArgument("-N");
        commandLine.createArgument("-S");
        final String dateRange = formatCVSDate(lastBuildTime) + "<" + formatCVSDate(checkTime);
        commandLine.createArgument("-d" + dateRange);

        if (!useHead()) {
            // add -b and -rTAG to list changes relative to the current branch,
            // not relative to the default branch, which is HEAD

            // note: -r cannot have a space between itself and the tag spec.
            commandLine.createArgument("-r" + tag);
        } else {
            // This is used to include the head only if a Tag is not specified.
            commandLine.createArgument("-b");
        }

        if (local == null) {
            commandLine.createArgument(module);
        }

        return commandLine;
    }

    // factory method for mock...
    protected Commandline getCommandline() {
        return new Commandline();
    }

    static String formatCVSDate(Date date) {
        return CVSDateUtil.formatCVSDate(date);
    }

    /**
     * Parses the input stream, which should be from the cvs LOG command. This method will format the data found in the
     * input stream into a List of Modification instances.
     * 
     * @param input
     *            InputStream to get LOG data from.
     * @return List of Modification elements, maybe empty never null.
     * @throws IOException if something breaks
     */
    protected List<Modification> parseStream(final InputStream input) throws IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(input));

        // Read to the first RCS file name. The first entry in the LOG
        // information will begin with this line. A CVS_FILE_DELIMITER is NOT
        // present. If no RCS file lines are found then there is nothing to do.

        String line = readToNotPast(reader, CVS_RCSFILE_LINE, null);
        final ArrayList<Modification> mods = new ArrayList<Modification>();

        while (line != null) {
            // Parse the single file entry, which may include several
            // modifications.
            final List<Modification> returnList = parseEntry(reader, line);

            // Add all the modifications to the local list.
            mods.addAll(returnList);

            // Read to the next RCS file line. The CVS_FILE_DELIMITER may have
            // been consumed by the parseEntry method, so we cannot read to it.
            line = readToNotPast(reader, CVS_RCSFILE_LINE, null);
        }

        return mods;
    }

    private void getRidOfLeftoverData(InputStream stream) {
        new StreamPumper(stream, new DiscardConsumer()).run();
    }

    List<Modification> execHistoryCommand(final Commandline command) throws Exception {
        final Process p = command.execute();

        final Thread stderr = logErrorStream(p);
        final InputStream cvsLogStream = p.getInputStream();
        final List<Modification> mods = parseStream(cvsLogStream);

        getRidOfLeftoverData(cvsLogStream);
        p.waitFor();
        stderr.join();
        IO.close(p);

        return mods;
    }

    protected void setMailAliases(final Hashtable<String, String> mailAliases) {
        this.mailAliases = mailAliases;
    }

    private static Thread logErrorStream(Process p) {
        return logErrorStream(p.getErrorStream());
    }

    static Thread logErrorStream(InputStream error) {
        Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, error));
        stderr.start();
        return stderr;
    }

    // (PENDING) Extract CVSEntryParser class

    /**
     * Parses a single file entry from the reader. This entry may contain zero or more revisions. This method may
     * consume the next CVS_FILE_DELIMITER line from the reader, but no further. <p/> When the LOG is related to a non
     * branch tag, only the last modification for each file will be listed.
     * 
     * @param reader
     *            Reader to parse data from.
     * @param rcsLine line to parse
     * @return modifications found in this entry; maybe empty, never null.
     * @throws IOException if something breaks
     */
    private List<Modification> parseEntry(final BufferedReader reader, final String rcsLine) throws IOException {
        final ArrayList<Modification> mods = new ArrayList<Modification>();

        String nextLine = "";

        // Read to the working file name line to get the filename.
        // If working file name line isn't found we'll extract is from the RCS file line
        final String workingFileName;
        if (module != null && cvsroot != null) {
            final String repositoryRoot = cvsroot.substring(cvsroot.lastIndexOf(":") + 1);
            final int startAt = "RCS file: ".length() + repositoryRoot.length();
            workingFileName = rcsLine.substring(startAt, rcsLine.length() - 2);
        } else {
            final String workingFileLine = readToNotPast(reader, CVS_WORKINGFILE_LINE, null);
            workingFileName = workingFileLine.substring(CVS_WORKINGFILE_LINE.length());
        }

        final boolean newCVSVersion = isCvsNewOutputFormat();
        while (nextLine != null && !nextLine.startsWith(CVS_FILE_DELIM)) {
            nextLine = readToNotPast(reader, "revision", CVS_FILE_DELIM);
            if (nextLine == null) {
                // No more revisions for this file.
                break;
            }

            StringTokenizer tokens = new StringTokenizer(nextLine, " ");
            tokens.nextToken();
            final String revision = tokens.nextToken();

            // Read to the revision date. It is ASSUMED that each revision
            // section will include this date information line.
            nextLine = readToNotPast(reader, CVS_REVISION_DATE, CVS_FILE_DELIM);
            if (nextLine == null) {
                break;
            }

            tokens = new StringTokenizer(nextLine, " \t\n\r\f;");
            // First token is the keyword for date, then the next two should be
            // the date and time stamps.
            tokens.nextToken();
            final String dateStamp = tokens.nextToken();
            final String timeStamp = tokens.nextToken();

            // New format sometimes has a +0000 in it. This skips it if we don't see
            // the start of the author: section
            final String isThisTimeOffset = tokens.nextToken();
            if (!isThisTimeOffset.equals("author:")) {            
                tokens.nextToken();
            }
            // The next token should be the author keyword, then the author name.
            final String authorName = tokens.nextToken();

            // The next token should be the state keyword, then the state name.
            tokens.nextToken();
            final String stateKeyword = tokens.nextToken();

            // if no lines keyword then file is added
            final boolean isAdded = !tokens.hasMoreTokens();

            // All the text from now to the next revision delimiter or working
            // file delimiter constitutes the message.
            String message = "";
            nextLine = reader.readLine();
            boolean multiLine = false;

            while (nextLine != null && !nextLine.startsWith(CVS_FILE_DELIM)
                    && !nextLine.startsWith(CVS_REVISION_DELIM)) {

                if (multiLine) {
                    message += NEW_LINE;
                } else {
                    multiLine = true;
                }
                message += nextLine;

                // Go to the next line.
                nextLine = reader.readLine();
            }

            final Modification nextModification = new Modification("cvs");
            nextModification.revision = revision;

            final int lastSlashIndex = workingFileName.lastIndexOf("/");

            final String fileName;
            String folderName = null;
            fileName = workingFileName.substring(lastSlashIndex + 1);
            if (lastSlashIndex != -1) {
                folderName = workingFileName.substring(0, lastSlashIndex);
            }
            final Modification.ModifiedFile modfile = nextModification.createModifiedFile(fileName, folderName);
            modfile.revision = nextModification.revision;

            try {
                if (newCVSVersion) {
                    nextModification.modifiedTime = CVSDateUtil.parseCVSDate(dateStamp + " " + timeStamp + " GMT");
                } else {
                    nextModification.modifiedTime = logDateFormatter.parse(dateStamp + " " + timeStamp + " GMT");
                }
            } catch (ParseException pe) {
                LOG.error("Error parsing cvs LOG for date and time", pe);
                return null;
            }

            nextModification.userName = authorName;

            final String address = mailAliases.get(authorName);
            if (address != null) {
                nextModification.emailAddress = address;
            }

            nextModification.comment = message;

            if (stateKeyword.equalsIgnoreCase(CVS_REVISION_DEAD)
                    && message.indexOf("was initially added on branch") != -1) {
                LOG.debug("skipping branch addition activity for " + nextModification);
                // this prevents additions to a branch from showing up as action "deleted" from head
                continue;
            }

            if (stateKeyword.equalsIgnoreCase(CVS_REVISION_DEAD)) {
                modfile.action = "deleted";
                properties.deletionFound();
            } else if (isAdded) {
                modfile.action = "added";
            } else {
                modfile.action = "modified";
            }
            properties.modificationFound();
            mods.add(nextModification);
        }
        return mods;
    }

    /**
     * Find the CVS branch revision name, when the tag is not HEAD The reader will consume all lines up to the next
     * description.
     *
     * @param reader input stream to read
     * @return the branch revision name, or <code>null</code> if not applicable or none was found.
     * @throws IOException if something breaks
     */
    private String parseBranchRevisionName(final BufferedReader reader) throws IOException {
        String branchRevisionName = null;

        if (!useHead()) {
            // Look for the revision of the form "tag: *.(0.)y ". this doesn't work for HEAD
            // get line with branch revision on it.

            String branchRevisionLine = readToNotPast(reader, "\t" + tag + ": ", CVS_DESCRIPTION);

            if (branchRevisionLine != null) {
                // Look for the revision of the form "tag: *.(0.)y ", return "*.y"
                branchRevisionName = branchRevisionLine.substring(tag.length() + 3);
                if (branchRevisionName.charAt(branchRevisionName.lastIndexOf(".") - 1) == '0') {
                    branchRevisionName = branchRevisionName.substring(0, branchRevisionName.lastIndexOf(".") - 2)
                            + branchRevisionName.substring(branchRevisionName.lastIndexOf("."));
                }
            }
        }
        return branchRevisionName;
    }

    /**
     * This method will consume lines from the reader up to the line that begins with the String specified but not past
     * a line that begins with the notPast String. If the line that begins with the beginsWith String is found then it
     * will be returned. Otherwise null is returned.
     * 
     * @param reader
     *            Reader to read lines from.
     * @param beginsWith
     *            String to match to the beginning of a line.
     * @param notPast
     *            String which indicates that lines should stop being consumed, even if the begins with match has not
     *            been found. Pass null to this method to ignore this string.
     * @return String that begin as indicated, or null if none matched to the end of the reader or the notPast line was
     *         found.
     * @throws IOException if something breaks
     */
    private static String readToNotPast(final BufferedReader reader, final String beginsWith, final String notPast)
            throws IOException {

        final boolean checkingNotPast = notPast != null;

        String nextLine = reader.readLine();
        while (nextLine != null && !nextLine.startsWith(beginsWith)) {
            if (checkingNotPast && nextLine.startsWith(notPast)) {
                return null;
            }
            nextLine = reader.readLine();
        }

        return nextLine;
    }

    boolean useHead() {
        return tag == null || tag.equals(CVS_HEAD_TAG) || tag.equals("");
    }

}

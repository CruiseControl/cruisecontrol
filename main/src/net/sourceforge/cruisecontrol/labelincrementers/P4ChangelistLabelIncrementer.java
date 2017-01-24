/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2006, ThoughtWorks, Inc.
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

package net.sourceforge.cruisecontrol.labelincrementers;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.LabelIncrementer;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamLogger;

import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Delete;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet.NameEntry;
import org.jdom.Element;

/**
 * This class uses the most current changelist of the user in Perforce as the
 * label for the builds.  It can also sync the Perforce managed files to that
 * changelist number, as well as clean out the existing managed files.
 *
 * @author <a href="mailto:groboclown@users.sourceforge.net">Matt Albrecht</a>
 */
public class P4ChangelistLabelIncrementer implements LabelIncrementer {

    private static final Logger LOG =
        Logger.getLogger(P4ChangelistLabelIncrementer.class);
    private static final String CHANGELIST_PREFIX = "@";
    private static final String REVISION_PREFIX = "#";
    private static final String RECURSE_U = "/...";
    private static final String RECURSE_W = "\\...";

    private String p4Port;
    private String p4Client;
    private String p4User;
    private String p4View;
    private String p4Passwd;

    private boolean clean = false;
    private boolean delete = false;
    private boolean sync = true;

    private int baseChangelist = -1;

    /**
     * Retrieves the current changelist, or, if given, the specified changelist,
     * and also performs any necessary actions the user requested.
     *
     * @param oldLabel Label from previous successful build.
     * @return Label to use for most recent successful build.
     */
    public String incrementLabel(String oldLabel, Element buildLog) {
        String label = null;
        try {
            validate();

            // Perform conditional actions.
            // Since the settings might change or be executed in any order,
            // we perform the checks on which actions to run here.
            boolean delTree = delete;
            boolean cleanP4 = delTree || clean;
            boolean syncP4 = cleanP4 || sync;

            if (cleanP4) {
                LOG.info("Cleaning Perforce clientspec " + p4Client);
                syncTo(REVISION_PREFIX + 0);
            }
            if (delTree) {
                deleteView();
            }

            label = getDefaultLabel();

            if (syncP4) {
                syncTo(CHANGELIST_PREFIX + label);
            }
        } catch (CruiseControlException cce) {
            LOG.warn("Couldn't run expected tasks", cce);
        }

        return label;
    }

    public boolean isPreBuildIncrementer() {
        // This only has use when used as a pre-build incrementer
        return true;
    }

    /**
     * Verify that the label specified -- the previous label -- is a valid label.
     * In this case any label is valid because the next label will not be based on
     * previous label but on information from Perforce.
     */
    public boolean isValidLabel(String label) {
        return true;
    }

    /**
     * The instance must be fully initialized before calling this method.
     * @throws IllegalStateException if the instance is not properly initialized
     */
    public String getDefaultLabel() {
        if (baseChangelist > 0) {
            return Integer.toString(baseChangelist);
        }
        // else

        try {
            validate();

            return getCurrentChangelist();
        } catch (CruiseControlException cce) {
            cce.printStackTrace();
            LOG.fatal("Problem accessing Perforce changelist", cce);
            throw new IllegalStateException(
                "Problem accessing Perforce changelist");
        }
    }

    // User settings

    /**
     * Set the changelist number that you want to build at.  If this isn't
     * set, then the class will get the most current submitted changelist
     * number.  Note that setting this will cause the build to ALWAYS build
     * at this changelist number.
     *
     * @param syncChange the changelist number to perform the sync to.
     */
    public void setChangelist(int syncChange) {
        baseChangelist = syncChange;
    }



    public void setPort(String p4Port) {
        this.p4Port = p4Port;
    }

    public void setClient(String p4Client) {
        this.p4Client = p4Client;
    }

    public void setUser(String p4User) {
        this.p4User = p4User;
    }

    public void setView(String p4View) {
        this.p4View = p4View;
    }

    public void setPasswd(String p4Passwd) {
        this.p4Passwd = p4Passwd;
    }

    /**
     * Disables the label incrementer from synchronizing Perforce to the
     * view.
     *
     * @param b if true, Disables the label incrementer from synchronizing Perforce to the
     * view.
     */
    public void setNoSync(boolean b) {
        this.sync = !b;
    }

    /**
     * Perform a "p4 sync -f [view]#0" before syncing anew.  This will force
     * the sync to happen.
     *
     * @param b if true, perform a "p4 sync -f [view]#0" before syncing anew
     */
    public void setClean(boolean b) {
        this.clean = b;
    }


    /**
     * Perform a recursive delete of the clientspec view.  This
     * will force a clean {@literal &} sync.  Note that this can potentially
     * be very destructive, so use with the utmost caution.
     *
     * @param b if true, force clean {@literal &} sync
     */
    public void setDelete(boolean b) {
        this.delete = b;
    }


    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(p4View, "view", this.getClass());
        ValidationHelper.assertNotEmpty(p4View, "view", this.getClass());
        ValidationHelper.assertNotEmpty(p4Client, "client", this.getClass());
        ValidationHelper.assertNotEmpty(p4Port, "port", this.getClass());
        ValidationHelper.assertNotEmpty(p4User, "user", this.getClass());
        ValidationHelper.assertNotEmpty(p4Passwd, "passwd", this.getClass());
    }



    protected String getCurrentChangelist()
            throws CruiseControlException {
        Commandline cmd = buildBaseP4Command();
        cmd.createArgument("changes");
        cmd.createArgument("-m1");
        cmd.createArgument("-ssubmitted");

        ParseChangelistNumbers pcn = new ParseChangelistNumbers();
        runP4Cmd(cmd, pcn);

        String[] changes = pcn.getChangelistNumbers();
        if (changes != null && changes.length == 1) {
            return changes[0];
        } else {
            throw new CruiseControlException(
                "Could not discover the changelist");
        }
    }


    protected void syncTo(String viewArg) throws CruiseControlException {
        Commandline cmd = buildBaseP4Command();
        cmd.createArguments("sync", p4View + viewArg);

        runP4Cmd(cmd, new P4CmdParserAdapter());
    }


    protected void deleteView() throws CruiseControlException {
        // despite what people tell you, deleting correctly in Java is
        // hard.  So, let Ant do our dirty work for us.
        try {
            Project p = createProject();
            FileSet fs = getWhereView(p);
            Delete d = createDelete(p);
            d.setProject(p);
            d.setVerbose(true);
            d.addFileset(fs);
            d.execute();
        } catch (BuildException be) {
            throw new CruiseControlException(be.getMessage(), be);
        }
    }


    /**
     * If the view mapping contains a reference to a single file,
     *
     * @param p project
     * @return the collection of recursive directories inside the Perforce
     *      view.
     * @throws CruiseControlException if something breaks
     */
    protected FileSet getWhereView(final Project p) throws CruiseControlException {
        String view = p4View;
        if (view == null) {
            view = "//...";
        }
        if (!view.endsWith(RECURSE_U) && !view.endsWith(RECURSE_W)) {
            // we'll only care about the recursive view.  Anything else
            // should be handled by the sync view#0
            LOG.debug("view [" + view + "] isn't recursive.");
            return null;
        }
        final Commandline cmd = buildBaseP4Command();
        cmd.createArguments("where", view);

        final ParseOutputParam pop = new ParseOutputParam("");
        runP4Cmd(cmd, pop);
        final String[] values = pop.getValues();
        if (values == null || values.length <= 0) {
            LOG.debug("Didn't find any files for view");
            return null;
        }
        final FileSet fs = createFileSet(p);

        // on windows, this is considered higher than the drive letter.
        fs.setDir(new File("/"));
        int count = 0;

        for (final String s : values) {
            // first token: the depot name
            // second token: the client name
            // third token+: the local file system name

            // like above, we only care about the recursive view.  If the
            // line doesn't end in /... or \... (even if it's a %%1), we ignore
            // it.  This makes our life so much simpler when dealing with
            // spaces.
            //LOG.debug("Parsing view line " + i + " [" + s + "]");
            if (!s.endsWith(RECURSE_U) && !s.endsWith(RECURSE_W)) {
                continue;
            }

            final String[] tokens = new String[3];
            int pos = 0;
            for (int j = 0; j < 3; ++j) {
                final StringBuffer sb = new StringBuffer();
                boolean neot = true;
                while (neot) {
                    if (pos >= s.length()) {
                        break;
                    }
                    final int q1 = s.indexOf('\'', pos);
                    final int q2 = s.indexOf('"', pos);
                    final int sp = s.indexOf(' ', pos);
                    if (q1 >= 0 && (q1 < q2 || q2 < 0) && (q1 < sp || sp < 0)) {
                        sb.append(s.substring(pos, q1));
                        pos = q1 + 1;
                    } else if (q2 >= 0 && (q2 < q1 || q1 < 0) && (q2 < sp || sp < 0)) {
                        sb.append(s.substring(pos, q2));
                        pos = q2 + 1;
                    } else if (sp >= 0) {
                        // check if we're at the end of the token
                        final String sub = s.substring(pos, sp);
                        pos = sp + 1;
                        sb.append(sub);
                        if (sub.endsWith(RECURSE_U) || sub.endsWith(RECURSE_W)) {
                            neot = false;
                        } else {
                            // keep the space - it's inside the token
                            sb.append(' ');
                        }
                    } else {
                        sb.append(s.substring(pos));
                        neot = false;
                    }
                }
                tokens[j] = new String(sb).trim();
            }
            if (tokens[0] != null && tokens[1] != null && tokens[2] != null
                    && (tokens[2].endsWith(RECURSE_U)
                    || tokens[2].endsWith(RECURSE_W))) {
                // convert the P4 recurse expression with the Ant
                // recurse expression
                final String f = tokens[2].substring(0,
                        tokens[2].length() - RECURSE_W.length())
                        + File.separator + "**";
                // a - in front of the depot name means to exclude this path
                if (tokens[0].startsWith("-//")) {
                    final NameEntry ne = fs.createExclude();
                    ne.setName(f);
                } else {
                    final NameEntry ne = fs.createInclude();
                    ne.setName(f);
                }
                ++count;
            }
        }
        if (count > 0) {
            return fs;
        } else {
            LOG.debug("no files in view to delete");
            return null;
        }
    }


    protected Project createProject() {
        final Project p = new Project();
        p.init();
        return p;
    }


    protected Delete createDelete(final Project p) throws CruiseControlException {
        Object o = p.createTask("delete");
        if (o == null || !(o instanceof Delete)) {
            // Backup code just in case we didn't work right.
            // If we can guarantee the above operation works all the time,
            // then this log note should be replaced with an exception.
            LOG.info("Could not find <delete> task in Ant.  Defaulting to basic constructor.");
            final Delete d = new Delete();
            d.setProject(p);
            o = d;
        }
        return (Delete) o;
    }


    protected FileSet createFileSet(final Project p) throws CruiseControlException {
        Object o = p.createDataType("fileset");
        if (o == null || !(o instanceof FileSet)) {
            // Backup code just in case we didn't work right.
            // If we can guarantee the above operation works all the time,
            // then this log note should be replaced with an exception.
            LOG.info("Could not find <fileset> type in Ant.  Defaulting to basic constructor.");
            final FileSet fs = new FileSet();
            fs.setProject(p);
            o = fs;
        }
        return (FileSet) o;
    }


    protected Commandline buildBaseP4Command() {
        final Commandline commandLine = new Commandline();
        commandLine.setExecutable("p4");
        commandLine.createArgument("-s");

        if (p4Client != null) {
            commandLine.createArguments("-c", p4Client);
        }

        if (p4Port != null) {
            commandLine.createArguments("-p", p4Port);
        }

        if (p4User != null) {
            commandLine.createArguments("-u", p4User);
        }

        if (p4Passwd != null) {
            commandLine.createArguments("-P", p4Passwd);
        }
        return commandLine;
    }


    protected void runP4Cmd(final Commandline cmd, final P4CmdParser parser)
            throws CruiseControlException {
        try {
            final Process p = cmd.execute();

            try {
                Thread stderr = new Thread(StreamLogger.getWarnPumper(LOG, p));
                stderr.start();

                InputStream p4Stream = p.getInputStream();
                parseStream(p4Stream, parser);
                stderr.join();
            } finally {
                p.waitFor();
                IO.close(p);
            }
        } catch (IOException e) {
            throw new CruiseControlException("Problem trying to execute command line process", e);
        } catch (InterruptedException e) {
            throw new CruiseControlException("Problem trying to execute command line process", e);
        }
    }

    protected void parseStream(final InputStream stream, final P4CmdParser parser)
            throws IOException {
        String line;
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("error:")) {
                throw new IOException("Error reading P4 stream: P4 says: " + line);
            } else if (line.startsWith("exit: 0")) {
                LOG.debug("p4cmd: Found exit 0");
                break;
            } else if (line.startsWith("exit:")) {
                // not an exit code of 0
                LOG.error("p4cmd: Found exit " + line);
                throw new IOException("Error reading P4 stream: P4 says: " + line);
            } else if (line.startsWith("warning:")) {
                parser.warning(line.substring(8));
            } else if (line.startsWith("info:") || line.startsWith("info1:")) {
                parser.info(line.substring(5));
            } else if (line.startsWith("text:")) {
                parser.text(line.substring(5));
            }
        }
        if (line == null) {
            throw new IOException("Error reading P4 stream: Unexpected EOF reached");
        }
    }

    protected static interface P4CmdParser {
        public void warning(String msg);
        public void info(String msg);
        public void text(String msg);
    }

    protected static class P4CmdParserAdapter implements P4CmdParser {
        public void warning(final String msg) {
            // empty
        }
        public void info(final String msg) {
            // empty
        }
        public void text(final String msg) {
            // empty
        }
    }

    protected static class ParseChangelistNumbers extends P4CmdParserAdapter {
        private final ArrayList<String> changelists = new ArrayList<String>();
        public void info(final String msg) {
            final StringTokenizer st = new StringTokenizer(msg);
            st.nextToken(); // skip 'Change' text
            changelists.add(st.nextToken());
        }

        public String[] getChangelistNumbers() {
            final String[] changelistNumbers = new String[ 0 ];
            return changelists.toArray(changelistNumbers);
        }
    }

    protected static class ParseOutputParam extends P4CmdParserAdapter {
        public ParseOutputParam(final String paramName) {
            this.paramName = paramName;
        }
        private final String paramName;
        private final List<String> values = new ArrayList<String>();
        public void info(final String msg) {
            final String m = msg.trim();
            if (m.startsWith(paramName)) {
                final String m2 = m.substring(paramName.length()).trim();
                values.add(m2);
            }
        }

        public String[] getValues() {
            final String[] v = new String[ 0 ];
            return values.toArray(v);
        }
    }
}

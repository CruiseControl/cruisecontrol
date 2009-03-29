/*****************************************************************************
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
 *****************************************************************************/
package net.sourceforge.cruisecontrol.sourcecontrols;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.Commandline;
import net.sourceforge.cruisecontrol.util.IO;
import net.sourceforge.cruisecontrol.util.StreamLogger;
import net.sourceforge.cruisecontrol.util.Util;
import net.sourceforge.cruisecontrol.util.ValidationHelper;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * This class implements the SourceControl methods for a git repository.  The
 * call to git is assumed to work without any setup.  This implies that
 * authentication data must be available.
 *
 * @see <a href="http://git.or.cz/">git.or.cz</a>
 * @author <a href="rschiele@gmail.com">Robert Schiele</a>
 */
public class Git implements SourceControl {
    private static final Logger LOG = Logger.getLogger(Git.class);
    private static final Pattern COMMITPATTERN =
    Pattern.compile("commit ([0-9a-f]{40})");
    private static final Pattern AUTHORPATTERN =
    Pattern.compile("author (.*) <(.*)> ([0-9]*) [+-][0-9]{4}");
    private static final Pattern DIFFPATTERN =
    Pattern.compile("diff --git (a/.* b/.*)");
    private static final Pattern NEWFILEPATTERN =
    Pattern.compile("new file mode [0-7]{6}");
    private static final Pattern DELETEDFILEPATTERN =
    Pattern.compile("deleted file mode [0-7]{6}");
    private static final String NEWLINE = System.getProperty("line.separator");
    
    private final SourceControlProperties props =
    new SourceControlProperties();
    private String lwc;

    public Map<String, String> getProperties() {
        return props.getPropertiesAndReset();
    }

    public void setProperty(String p) {
        props.assignPropertyName(p);
    }
    
    public void setPropertyOnDelete(String p) {
        props.assignPropertyOnDeleteName(p);
    }
    
    /**
     * Sets the local working copy to use when making calls to git.
     *
     * @param d String indicating the relative or absolute path to the local
     * working copy of the git repository of which to find the log history.
     */
    public void setLocalWorkingCopy(String d) {
        lwc = d;
    }

    /**
     * This method validates that the local working copy location has been
     * specified.
     *
     * @throws CruiseControlException Thrown when the local working copy
     * location is null
     */
    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(lwc != null,
                                    "'localWorkingCopy' is a required "
                                    + "attribute on the Git task");

        final File wd = new File(lwc);
        ValidationHelper.assertTrue(wd.exists() && wd.isDirectory(),
                                    "'localWorkingCopy' must be an existing "
                                    + "directory. Was "
                                    + wd.getAbsolutePath());
    }

    /**
     * Returns a list of modifications detailing all the changes between the
     * last build and the latest revision in the repository.
     * @return the list of modifications, or an empty list if we failed to
     * retrieve the changes.
     */
    public List<Modification> getModifications(final Date from, final Date to) {
        final List<Modification> mods = new ArrayList<Modification>();
        final Commandline cmd = new Commandline();
        cmd.setExecutable("git");
        try {
            cmd.setWorkingDirectory(lwc);
        } catch (CruiseControlException e) {
            LOG.error("Error building history command", e);
            return mods;
        }
        cmd.createArgument("log");
        cmd.createArgument("-p");
        cmd.createArgument("--pretty=raw");
        cmd.createArgument(gitRevision(from) + ".." + gitRevision(to));
        LOG.debug("Executing command: " + cmd);
        try {
            final Process p = cmd.execute();
            final Thread stderr = new
                Thread(StreamLogger.getWarnPumper(LOG, p.getErrorStream()));
            stderr.start();
            parseLog(new InputStreamReader(p.getInputStream(), "UTF-8"),
                     mods, props);
            p.waitFor();
            stderr.join();
            IO.close(p);
        } catch (Exception e) {
            LOG.error("Error executing git log command " + cmd, e);
        }
        return mods;
    }

    static void parseLog(final Reader grd, final List<Modification> mods, final SourceControlProperties props)
        throws IOException {
        final BufferedReader rd = new BufferedReader(grd);
        boolean diffmode = false;
        Modification mod = null;
        while (true) {
            String l = rd.readLine();
            if (l == null) {
                break;
            }
            if (l.equals("")) {
                /* If in diff mode this ends the diff mode.  Otherwise it
                   starts the comment block. */
                if (diffmode) {
                    diffmode = false;
                } else {
                    mod.comment = "";
                    while (true) {
                        l = rd.readLine();
                        if (l == null || l.equals("")) {
                            break;
                        }
                        mod.comment += l.substring(4) + NEWLINE;
                    }
                }
                continue;
            }
            Matcher matcher = COMMITPATTERN.matcher(l);
            if (matcher.matches()) {
                /* If this is the latest modification store commit id as
                   property. */
                if (mod == null) {
                    props.put("gitcommitid", matcher.group(1));
                }
                mod = new Modification("git");
                mods.add(mod);
                props.modificationFound();
                continue;
            }
            matcher = AUTHORPATTERN.matcher(l);
            if (matcher.matches()) {
                mod.userName = matcher.group(1);
                mod.emailAddress = matcher.group(2);
                final long dt = new Long(matcher.group(3));
                /* Set revision to commit date. */
                mod.revision = "" + dt;
                mod.modifiedTime = new Date(dt * 1000);
                continue;
            }
            matcher = DIFFPATTERN.matcher(l);
            if (matcher.matches()) {
                final String m1 = matcher.group(1);
                final Modification.ModifiedFile modfile =
                    mod.createModifiedFile(m1.substring(m1.length() / 2 + 3),
                                           null);
                l = rd.readLine();
                if (DELETEDFILEPATTERN.matcher(l).matches()) {
                    modfile.action = "deleted";
                    props.deletionFound();
                } else {
                    modfile.action = NEWFILEPATTERN.matcher(l).matches()
                        ? "added" : "modified";
                }
                modfile.revision = mod.revision;
                /* Remember we are in diffmode.  Parser needs this information
                   to handle empty lines correctly. */
                diffmode = true;
                //continue;  // 'contiue' is unnecessary as last statement in loop
            }
        }
    }

    static String gitRevision(Date dt) {
        final String dts = "@{ " + (dt.getTime() / 1000) + "}";
        /* The SVN plugin claims we have to quote this for Windows. */
        return Util.isWindows() ? ("\"" + dts + "\"") : dts;
    }
}

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.SourceControl;
import net.sourceforge.cruisecontrol.util.ManagedCommandline;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * This class implements the SourceControl methods for an AlienBrain
 * repository.  It does this by taking advantage of the AlienBrain command-
 * line utility.  Obviously, the command line utility  must be installed
 * and working in order for this class to work.
 *
 * This class is based very heavily on P4.java.
 *
 * @author <a href="mailto:scottj+cc@escherichia.net">Scott Jacobs</a>
 */
public class AlienBrain extends AlienBrainCore implements SourceControl {

    private static final Logger LOG = Logger.getLogger(AlienBrain.class);
    /*
     * The difference between January 1, 1601 0:00:00 UTC and January 1,
     * 1970 0:00:00 UTC in milliseconds.
     * ((369 years * 365 days) + 89 leap days) * 24h * 60m * 60s * 1000ms
     */
    private static final long FILETIME_EPOCH_DIFF = 11644473600000L;
    /* 100-ns intervals per ms */
    private static final long HUNDRED_NANO_PER_MILLI_RATIO = 10000L;
    private static final String AB_NO_MODIFICATIONS = "No files or folders found!";
    private static final String AB_MODIFICATION_SUMMARY_PREFIX = "Total of ";
    
    private final SourceControlProperties properties = new SourceControlProperties();

    /**
     * Any properties that have been set in this sourcecontrol.
     */
    public Map<String, String> getProperties() {
        return properties.getPropertiesAndReset();
    }
    
    public void setProperty(String propertyName) {
        properties.assignPropertyName(propertyName);
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertIsSet(getPath(), "path", this.getClass());
    }

    /**
     *  Get a List of Modifications detailing all the changes between now and
     *  the last build
     *
     *@param  lastBuild
     *@param  now
     *@return List of Modification objects
     */
    public List<Modification> getModifications(Date lastBuild, Date now) {
        List<Modification> mods = new ArrayList<Modification>();
        try {
            validate();
            mods = getModificationsFromAlienBrain(lastBuild, now);
        } catch (Exception e) {
            LOG.error("Log command failed to execute succesfully", e);
        }

        if (!mods.isEmpty()) {
            properties.modificationFound();
        }
        
        return mods;
    }

    /**
     * Convert a Java Date into an AlienBrain SCIT timestamp.
     * AlienBrain provides a 64-bit modification timestamp that is in windows
     * FILETIME format, which is a 65-bit value representing the number of
     * 100-nanosecond intervals since January 1, 1601 (UTC).
     */
    public static long dateToFiletime(Date date) {
        long milliSecsSinceUnixEpoch = date.getTime();
        long milliSecsSinceFiletimeEpoch = milliSecsSinceUnixEpoch + FILETIME_EPOCH_DIFF;
        return milliSecsSinceFiletimeEpoch * HUNDRED_NANO_PER_MILLI_RATIO;
    }

    /**
     * Convert an AlienBrain SCIT timestamp into a Java Date.
     * AlienBrain provides a 64-bit modification timestamp that is in windows
     * FILETIME format, which is a 64-bit value representing the number of
     * 100-nanosecond intervals since January 1, 1601 (UTC).
     */
    public static Date filetimeToDate(long filetime) {
        long milliSecsSinceFiletimeEpoch = filetime / HUNDRED_NANO_PER_MILLI_RATIO;
        long milliSecsSinceUnixEpoch = milliSecsSinceFiletimeEpoch - FILETIME_EPOCH_DIFF;
        return new Date(milliSecsSinceUnixEpoch);
    }

    /**
     * Construct a ManagedCommandline which will run the AlienBrain command-line
     * client in such a way that it will return a list of modifications.
     *
     *@param  lastBuild
     *@param  now
     */
    protected ManagedCommandline buildGetModificationsCommand(Date lastBuild, Date now) {
        ManagedCommandline cmdLine = buildCommonCommand();
        cmdLine.createArguments("find", getPath());
        cmdLine.createArguments("-regex", "SCIT > " + dateToFiletime(lastBuild));
        cmdLine.createArguments("-format", "#SCIT#|#DbPath#|#Changed By#|#CheckInComment#");

        return cmdLine;
    }

    /**
     * Run the AlienBrain command-line client and return a list of
     * Modifications since lastBuild, if any.
     *@param  lastBuild
     *@param  now
     */
    protected List<Modification> getModificationsFromAlienBrain(Date lastBuild, Date now)
        throws IOException, CruiseControlException {

        if (getBranch() != null) {
            setActiveBranch(getBranch());
        }

        ManagedCommandline cmdLine = buildGetModificationsCommand(lastBuild, now);
        LOG.debug("Executing: " + cmdLine.toString());
        cmdLine.execute();

        return parseModifications(cmdLine.getStdoutAsList());
    }

    /**
     * Turn a stream containing the results of running the AlienBrain
     * command-line client into a list of Modifications.
     */
    protected List<Modification> parseModifications(List modifications) {
        final List<Modification> mods = new ArrayList<Modification>();

        for (Iterator it = modifications.iterator(); it.hasNext(); ) {
            String line = (String) it.next();
            line = line.trim();
            if (line.equals(AB_NO_SESSION)) {
                LOG.error(AB_NO_SESSION);
                continue;
            } else if (line.equals(AB_NO_MODIFICATIONS)) {
                continue;
            } else if (line.startsWith(AB_MODIFICATION_SUMMARY_PREFIX)) {
                continue;
            } else if (line.startsWith("|")) {
                //Folders don't seem to always have a checked-in time, so
                //fake one.
                line = "0" + line;
            }

            Modification m = parseModificationDescription(line);
            mods.add(m);
        }
        return mods;
    }

    /**
     * Turns a string, most likely provided from the AlienBrain command-line
     * client, into a Modification.
     */
    protected static Modification parseModificationDescription(String description) {
        Modification m = new Modification("AlienBrain");

        StringTokenizer st = new StringTokenizer(description, "|");

        m.modifiedTime = AlienBrain.filetimeToDate(Long.parseLong(st.nextToken()));
        m.createModifiedFile(st.nextToken(), null);
        m.userName = st.nextToken();
        while (st.hasMoreTokens()) {
            m.comment += st.nextToken();
        }

        return m;
    }
}

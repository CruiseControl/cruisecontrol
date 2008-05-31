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
package net.sourceforge.cruisecontrol.logmanipulators;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Manipulator;
import net.sourceforge.cruisecontrol.Log;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

public abstract class BaseManipulator implements Manipulator {

    private static final Map UNITS;

    private transient Integer unit = null;
    private transient int every = -1;

    static {
        Map units = new HashMap(4, 1.0f);
        units.put("DAY", new Integer(Calendar.DAY_OF_MONTH));
        units.put("WEEK", new Integer(Calendar.WEEK_OF_YEAR));
        units.put("MONTH", new Integer(Calendar.MONTH));
        units.put("YEAR", new Integer(Calendar.YEAR));
        UNITS = Collections.unmodifiableMap(units);
    }

    public BaseManipulator() {
        super();
    }

    /**
     * Identifies the relevant Logfiles from the given Logdir
     * @param logDir the logDir as String
     * @return File-Array of the the relevant files.
     */
    protected File[] getRelevantFiles(String logDir, boolean ignoreSuffix) {
        File[] backupFiles = null;
        if (this.every != -1 && this.unit != null) {
            File dir = new File(logDir);
            Calendar cal = Calendar.getInstance();

            cal.add(unit.intValue(), -every);

            backupFiles = dir.listFiles(getFilenameFilter(cal.getTime(), ignoreSuffix));
        }
        return backupFiles;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(every == -1 || unit == null,
                "BackupEvery and backupUnit must be set");
    }

    /**
     * sets the backup keep amount
     *
     * @param every
     * @throws CruiseControlException
     */
    public void setEvery(int every) throws CruiseControlException {
        this.every = every;
    }

    /**
     * sets the unit on which the backup should run. valid are YEAR, MONTH, WEEK, DAY
     *
     * @param unit String that is used as Key for the Calendar-Constants
     * @throws CruiseControlException
     */
    public void setUnit(String unit) throws CruiseControlException {
        this.unit = (Integer) UNITS.get(unit.toUpperCase());
    }

    Integer getUnit() {
        return this.unit;
    }

    /**
     * Can be overriden to provide different FilenameFilter implemenations.
     * @param logdate the date of 'old' build file(s) on which some action should be taken.
     * @param ignoreSuffix true to ignore ".xml" suffix during matching
     * @return a FilenameFilter to be used to select files older that a certain date for manipulation.
     */
    protected FilenameFilter getFilenameFilter(final Date logdate, final boolean ignoreSuffix) {
        return new LogfileNameFilter(logdate, ignoreSuffix);
    }

    private class LogfileNameFilter implements FilenameFilter {

        private Date logdate = null;

        private boolean ignoreSuffix = false;

        public LogfileNameFilter(Date logdate, boolean ignoreSuffix) {
            this.logdate = logdate;
            this.ignoreSuffix = ignoreSuffix;
        }

        public boolean accept(File dir, String name) {
            boolean result = name.startsWith("log");
            if (!ignoreSuffix) {
                result &= name.endsWith(".xml");
            }
            if (result) {
                try {
                    Date logfileDate = Log.parseDateFromLogFileName(name);
                    result = logfileDate.before(logdate);
                } catch (Exception e) {
                    result = false;
                }
            }
            return result;
        }

    }

}

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.apache.log4j.Logger;

/**
 * Provide a "time" using hhmm format that specifies when a build should be
 * triggered. Once one successful build occurs, no more occur. If a build occurs
 * successfully via other means as the time threshold is crossed then this build
 * won't occur.
 *
 * The is useful when you need a project to be built on a time basis despite no
 * changes to source control.
 *
 * @author <a href="mailto:epugh@opensourceconnections.com">Eric Pugh </a>
 * @version $Id$
 */
public class TimeBuild extends FakeUserSourceControl {
    private static final Logger LOG = Logger.getLogger(TimeBuild.class);

    private int time = Builder.NOT_SET;

    /**
     * The threshold time to cross that starts triggering a build
     *
     * @param timeString
     *            The time in hhmm format
     */
    public void setTime(final String timeString) {
        time = Integer.parseInt(timeString);
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertFalse(time == Builder.NOT_SET, "the 'time' attribute is mandatory");
    }

    /**
     * Check if TimeBuild "time" threshold has passed with out a successful
     * build. If so, trigger the build.
     *
     * @param lastBuild
     *            date of last build
     * @param now
     *            current time
     */
    public List<Modification> getModifications(final Date lastBuild, final Date now) {
        LOG.debug("LastBuild:" + lastBuild + ", now:" + now);
        final List<Modification> modifications = new ArrayList<Modification>();

        /*
         *
         * if now and lastbuild occur on the same day, only trigger a build when
         * lastbuildtime is before 'time' and 'time' is before nowtime
         *
         * if now and lastbuild do not occur on the same day, only trigger a
         * build when nowtime is after 'time'
         *
         *
         */
        // TODO trigger at time, not just after it
        final int lastBuildTime = DateUtil.getTimeFromDate(lastBuild);
        final int nowTime = DateUtil.getTimeFromDate(now);
        if (onSameDay(lastBuild, now)) {
            if (lastBuildTime < time && time < nowTime) {
                modifications.add(getMod(now));
            }
        } else {
            if (nowTime > time || lastBuildTime < time) {
                modifications.add(getMod(now));
            }
        }

        if (!modifications.isEmpty()) {
            getSourceControlProperties().modificationFound();
        }
        
        return modifications;
    }

    private boolean onSameDay(final Date date1, final Date date2) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date1);
        final int day1 = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.setTime(date2);
        final int day2 = calendar.get(Calendar.DAY_OF_MONTH);
        return day1 == day2;
    }

    private Modification getMod(final Date now) {
        final Modification mod = new Modification("always");
        final Modification.ModifiedFile modfile = mod.createModifiedFile("time build", "time build");
        modfile.action = "change";
        mod.userName = getUserName();
        final Calendar nowTimeBuild = Calendar.getInstance();
        nowTimeBuild.setTime(now);
        final int modifHour = this.time / 100;
        final int modifMinute = this.time - modifHour * 100;
        nowTimeBuild.set(Calendar.HOUR_OF_DAY, modifHour);
        nowTimeBuild.set(Calendar.MINUTE, modifMinute);
        nowTimeBuild.set(Calendar.MILLISECOND, 0);
        mod.modifiedTime = nowTimeBuild.getTime();
        mod.comment = "";
        return mod;
    }

    public String toString() {
        return getUserName() + ", " + time;
    }

}

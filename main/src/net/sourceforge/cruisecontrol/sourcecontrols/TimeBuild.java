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

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

import net.sourceforge.cruisecontrol.Builder;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.util.Util;

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
 */
public class TimeBuild extends FakeUserSourceControl {
    private static final Logger LOG = Logger.getLogger(TimeBuild.class);

    private int time = Builder.NOT_SET;

    private Hashtable properties = new Hashtable();

    /**
     * The threshold time to cross that starts triggering a build
     * 
     * @param timeString
     *            The time in hhmm format
     */
    public void setTime(String timeString) {
        time = Integer.parseInt(timeString);
    }

    /**
     * Unsupported by TimeBuild.
     */
    public void setProperty(String property) {

    }

    /**
     * Unsupported by TimeBuild.
     */
    public void setPropertyOnDelete(String propertyOnDelete) {
    }

    public Hashtable getProperties() {
        return properties;
    }

    public void validate() throws CruiseControlException {
        if (time == Builder.NOT_SET) {
            throw new CruiseControlException(
                    "the 'time' attribute is manditory");
        }
    }

    /**
     * Check if TimeBuild "time" threshold has passed with out a successful
     * build. If so, trigger the build.
     * 
     * @param lastBuild
     *            date of last build
     * @param now
     *            IGNORED
     */
    public List getModifications(Date lastBuild, Date now) {
        LOG.debug("LastBuild:" + lastBuild + ", now:" + now);
        List modifications = new ArrayList();

        int nowTime = Util.getTimeFromDate(now);
        if (nowTime > time) { // possible opportunity to run
            int lastBuildTime = Util.getTimeFromDate(lastBuild);
            if (lastBuildTime < time) {
                Modification mod = new Modification("always");
                Modification.ModifiedFile modfile = mod.createModifiedFile(
                        "time build", "time build");
                modfile.action = "change";
                mod.userName = getUserName();
                mod.modifiedTime = new Date((new Date()).getTime() - 100000);
                mod.comment = "";
                modifications.add(mod);
            }
        }

        return modifications;
    }

}
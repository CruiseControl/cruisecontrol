/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
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
package net.sourceforge.cruisecontrol;

import org.jdom.Element;

import java.util.*;

/**
 *  Handles scheduling different builds.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2001-2
 */
public class Schedule {

    private List _builders = new ArrayList();
    private List _pauseBuilders = new ArrayList();

    public void addBuilder(Builder builder) {
        _builders.add(builder);
    }

    public void addPauseBuilder(PauseBuilder pauseBuilder) {
        _pauseBuilders.add(pauseBuilder);
    }

    /**
     *  Determine if CruiseControl should run a build, given the current time.
     *
     *  @param now The current date
     *  @return true if CruiseControl is currently paused (no build should run).
     */
    public boolean isPaused(Date now) {
        Iterator pauseBuilderIterator = _pauseBuilders.iterator();
        while (pauseBuilderIterator.hasNext()) {
            PauseBuilder builder = (PauseBuilder) pauseBuilderIterator.next();
            if (builder.getStartTime() <= getTimeFromDate(now) &&
                    getTimeFromDate(now) <= builder.getEndTime() &&
                    builder.isValidDay(now))
                return true;
        }
        return false;
    }

    /**
     *  Select the correct <code>Builder</code> and start a build.
     *
     *  @param buildNumber The sequential build number.
     *  @param lastBuild The date of the last build.
     *  @param now The current time.
     *  @param properties Properties that would need to be passed in to the actual build tool.
     *
     *  @return JDOM Element representation of build log.
     */
    public Element build(int buildNumber, Date lastBuild, Date now, Map properties) throws CruiseControlException {
        Builder builder = selectBuilder(buildNumber, lastBuild, now);
        return builder.build(properties);
    }

    /**
     *  Select the correct build based on the current buildNumber and time.
     *
     *  @param lastBuild The date of the last build.
     *  @param now The current time.
     *  @param properties Properties that would need to be passed in to the actual build tool.
     *
     *  @return The <code>Builder</code> that should be run.
     */
    protected Builder selectBuilder(int buildNumber, Date lastBuild, Date now) throws CruiseControlException {
        Iterator builderIterator = _builders.iterator();
        while (builderIterator.hasNext()) {
            Builder builder = (Builder) builderIterator.next();
            if (builder.getTime() > 0) {
                if (getTimeFromDate(lastBuild) < builder.getTime() &&
                        builder.getTime() < getTimeFromDate(now) && builder.isValidDay(now)) {
                    return builder;
                }
            } else if (builder.getMultiple() > 0) {
                if ((buildNumber % builder.getMultiple()) == 0) {
                    return builder;
                }
            } else {
                throw new CruiseControlException(""); //builder not appropriately configured
            }
        }
        throw new CruiseControlException(""); //no builder matches
    }

    /**
     *  Create an integer time from a <code>Date</code> object.
     *
     *  @param date The date to get the timestamp from.
     *  @return The time as an integer formatted as "HHmm".
     */
    protected int getTimeFromDate(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int hour = calendar.get(Calendar.HOUR_OF_DAY) * 100;
        int minute = calendar.get(Calendar.MINUTE);
        return hour + minute;
    }
}

/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Iterator;
import java.util.Calendar;

import net.sourceforge.cruisecontrol.util.Util;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 *  Handles scheduling different builds.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2001-2
 */
public class Schedule {

    private static final Logger LOG = Logger.getLogger(Schedule.class);

    static final long ONE_SECOND = 1000;
    static final long ONE_MINUTE = 60 * ONE_SECOND;
    static final long ONE_DAY = 24 * 60 * ONE_MINUTE;
    static final long ONE_YEAR = ONE_DAY * 365;

    static final long MAX_INTERVAL_SECONDS = 60 * 60 * 24 * 365;
    static final long MAX_INTERVAL_MILLISECONDS = MAX_INTERVAL_SECONDS * 1000;

    private List builders = new ArrayList();
    private List pauseBuilders = new ArrayList();
    private long interval = 300 * ONE_SECOND;

    /** date formatting for time statements */
    private static final DateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");

    public void addBuilder(Builder builder) {
        builders.add(builder);
    }

    public void addPauseBuilder(PauseBuilder pauseBuilder) {
        pauseBuilders.add(pauseBuilder);
    }

    /**
     *  Determine if CruiseControl should run a build, given the current time.
     *
     *  @param now The current date
     *  @return true if CruiseControl is currently paused (no build should run).
     */
    public boolean isPaused(Date now) {
        PauseBuilder pause = findPause(now);
        if (pause != null) {
            LOG.info("CruiseControl is paused until: " + getEndTimeString(pause));
            return true;
        }
        return false;
    }

    /**
     * Returns a String representing the time following the end time of
     * the given {@link PauseBuilder}.
     *
     * @param builder the <code>PauseBuilder</code> to be considered.
     * @return a String representing the time following the end time of
     *         the <code>PauseBuilder</code>.
     */
    private String getEndTimeString(PauseBuilder builder) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, builder.getEndTime() / 100);
        cal.set(Calendar.MINUTE, builder.getEndTime() % 100);
        cal.add(Calendar.MINUTE, 1);
        return TIME_FORMAT.format(cal.getTime());
    }

    PauseBuilder findPause(Date date) {
        Iterator pauseBuilderIterator = pauseBuilders.iterator();
        while (pauseBuilderIterator.hasNext()) {
            PauseBuilder builder = (PauseBuilder) pauseBuilderIterator.next();
            if (builder.isPaused(date)) {
                return builder;
            }
        }
        return null;
    }

    /**
     *  Select the correct <code>Builder</code> and start a build.
     *
     * @param buildNumber The sequential build number.
     * @param lastBuild The date of the last build.
     * @param now The current time.
     * @param properties Properties that would need to be passed in to the
     * actual build tool.
     *
     *  @return JDOM Element representation of build log.
     */
    public Element build(int buildNumber, Date lastBuild, Date now, Map properties)
        throws CruiseControlException {
        Builder builder = selectBuilder(buildNumber, lastBuild, now);
        return builder.build(properties);
    }

    /**
     *  Select the correct build based on the current buildNumber and time.
     *
     * @param buildNumber The sequential build number
     * @param lastBuild The date of the last build.
     * @param now The current time.
     *
     *  @return The <code>Builder</code> that should be run.
     */
    protected Builder selectBuilder(int buildNumber, Date lastBuild, Date now)
        throws CruiseControlException {
        Builder builder = findBuilder(buildNumber, lastBuild, now);
        
        if (builder == null) {
            long timeToNextBuild = getTimeToNextBuild(now, ONE_MINUTE);
            Date futureDate = getFutureDate(now, timeToNextBuild);
            builder = findBuilder(buildNumber, now, futureDate);
        }
        
        if (builder == null) {
            validate();
            throw new CruiseControlException("configuration error not caught by validate? no builder selected");
        }

        return builder;
    }

    private Builder findBuilder(int buildNumber, Date lastBuild, Date now) throws CruiseControlException {
        Iterator builderIterator = builders.iterator();
        while (builderIterator.hasNext()) {
            Builder builder = (Builder) builderIterator.next();
            int buildTime = builder.getTime();
            boolean isTimeBuilder = buildTime >= 0;
            if (isTimeBuilder) {
                boolean didntBuildToday = builderDidntBuildToday(lastBuild, now, buildTime);
                boolean isAfterBuildTime = buildTime <= Util.getTimeFromDate(now);
                boolean isValidDay = builder.isValidDay(now);
                if (didntBuildToday && isAfterBuildTime && isValidDay) {
                    return builder;
                }
            } else if (builder.getMultiple() > 0) {
                if ((buildNumber % builder.getMultiple()) == 0) {
                    return builder;
                }
            } else {
                throw new CruiseControlException("The selected Builder is not properly configured");
            }
        }
               
        return null;
    }

    boolean builderDidntBuildToday(Date lastBuild, Date now, int buildTime) {
        int time = Util.getTimeFromDate(now);
        long timeMillis = Util.convertToMillis(time);
        long startOfToday = now.getTime() - timeMillis;
        boolean lastBuildYesterday = lastBuild.getTime() < startOfToday;
        boolean lastBuildTimeBeforeBuildTime = Util.getTimeFromDate(lastBuild) < buildTime;
        boolean didntBuildToday = lastBuildYesterday || lastBuildTimeBeforeBuildTime;
        return didntBuildToday;
    }

    long getTimeToNextBuild(Date now, long sleepInterval) {
        long timeToNextBuild = sleepInterval;
        LOG.debug("getTimeToNextBuild: initial timeToNextBuild = " + timeToNextBuild);
        timeToNextBuild = checkTimeBuilders(now, timeToNextBuild);
        LOG.debug("getTimeToNextBuild: after checkTimeBuilders = " + timeToNextBuild);
        timeToNextBuild = checkPauseBuilders(now, timeToNextBuild);
        LOG.debug("getTimeToNextBuild: after checkPauseBuilders = " + timeToNextBuild);
        return timeToNextBuild;
    }

    private boolean hasOnlyTimeBuilders() {
        boolean onlyTimeBuilders = true;
        Iterator iterator = builders.iterator();
        while (iterator.hasNext()) {
            Builder builder = (Builder) iterator.next();
            boolean isTimeBuilder = builder.getTime() != Builder.NOT_SET;
            if (!isTimeBuilder) {
                onlyTimeBuilders = false;
                break;
            }
        }
        return onlyTimeBuilders;
    }

    long checkTimeBuilders(Date now, long proposedTime) {
        long timeToNextBuild = proposedTime;
        if (hasOnlyTimeBuilders()) {
            timeToNextBuild = Long.MAX_VALUE;
        }
        int nowTime = Util.getTimeFromDate(now);
        Iterator builderIterator = builders.iterator();
        while (builderIterator.hasNext()) {
            Builder builder = (Builder) builderIterator.next();
            int thisBuildTime = builder.getTime();
            boolean isTimeBuilder = thisBuildTime != Builder.NOT_SET;
            if (isTimeBuilder) {
                long timeToThisBuild = Long.MAX_VALUE;
                boolean isBeforeBuild = nowTime <= thisBuildTime;
                boolean isValidDay = builder.isValidDay(now);
                if (isBeforeBuild && isValidDay) {
                    timeToThisBuild = Util.milliTimeDiffernce(nowTime, thisBuildTime);
                } else {
                    Date tomorrow = new Date(now.getTime() + ONE_DAY);
                    boolean tomorrowIsValid = builder.isValidDay(tomorrow);
                    if (tomorrowIsValid) {
                        long remainingTimeToday = ONE_DAY - Util.convertToMillis(nowTime);
                        long timeTomorrow = Util.convertToMillis(thisBuildTime);
                        timeToThisBuild = remainingTimeToday + timeTomorrow;
                    }
                }
                if (timeToThisBuild < timeToNextBuild) {
                    timeToNextBuild = timeToThisBuild;
                }
            }
        }

        if (timeToNextBuild > MAX_INTERVAL_MILLISECONDS) {
            LOG.error(
                "checkTimeBuilders exceeding maximum interval. using proposed value ["
                    + proposedTime
                    + "] instead");
            timeToNextBuild = proposedTime;
        }
        return timeToNextBuild;
    }

    long checkPauseBuilders(Date now, long proposedTime) {
        long oldTime = proposedTime;
        long newTime = checkForPauseAtProposedTime(now, oldTime);
        while (oldTime != newTime) {
            oldTime = newTime;
            newTime = checkForPauseAtProposedTime(now, oldTime);
        }

        return newTime;
    }

    private long checkForPauseAtProposedTime(Date now, long proposedTime) {
        Date futureDate = getFutureDate(now, proposedTime);
        PauseBuilder pause = findPause(futureDate);
        if (pause == null) {
            return proposedTime;
        }

        int endPause = pause.getEndTime();
        int currentTime = Util.getTimeFromDate(now);

        long timeToEndOfPause = Util.milliTimeDiffernce(currentTime, endPause);

        while (timeToEndOfPause < proposedTime) {
            timeToEndOfPause += ONE_DAY;
        }

        if (timeToEndOfPause > MAX_INTERVAL_MILLISECONDS) {
            LOG.error("maximum pause interval exceeded! project perpetually paused?");
            return MAX_INTERVAL_MILLISECONDS;
        }

        return timeToEndOfPause + ONE_MINUTE;
    }

    private Date getFutureDate(Date now, long delay) {
        long futureMillis = now.getTime() + delay;
        Date futureDate = new Date(futureMillis);
        return futureDate;
    }

    public void setInterval(long intervalBetweenModificationChecks) {
        interval = intervalBetweenModificationChecks * ONE_SECOND;
    }

    public long getInterval() {
        return interval;
    }

    public void validate() throws CruiseControlException {
        if (builders.size() == 0) {
            throw new CruiseControlException("schedule element requires at least one nested builder element");
        }

        if (interval > ONE_YEAR) {
            throw new CruiseControlException(
                "maximum interval value is " + MAX_INTERVAL_SECONDS + " (one year)");
        }
        
        if (hasOnlyTimeBuilders()) {
            LOG.warn("schedule has all time based builders: interval value will be ignored.");
        }
    }

}

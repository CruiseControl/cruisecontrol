/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sourceforge.cruisecontrol.util.DateUtil;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

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
    private final DateFormat timeFormatter = new SimpleDateFormat("HH:mm");

    public void add(Builder builder) {
        checkParamNotNull("builder", builder);
        builders.add(builder);
    }
    
    public void add(PauseBuilder pause) {
        checkParamNotNull("pauseBuilder", pause);
        pauseBuilders.add(pause);        
    }

    /**
     *  Determine if CruiseControl should run a build, given the current time.
     *
     *  @param now The current date
     *  @return true if CruiseControl is currently paused (no build should run).
     */
    public boolean isPaused(Date now) {
        checkParamNotNull("date", now);
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
        return timeFormatter.format(cal.getTime());
    }

    PauseBuilder findPause(Date date) {
        checkParamNotNull("date", date);
        Iterator pauseBuilderIterator = pauseBuilders.iterator();
        while (pauseBuilderIterator.hasNext()) {
            PauseBuilder pause = (PauseBuilder) pauseBuilderIterator.next();
            if (pause.isPaused(date)) {
                return pause;
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
     * @param properties Properties that would need to be passed in to the actual build tool.
     * @param buildTarget the build target to use instead of the configured one (pass in null if no override is needed)
     *
     *  @return JDOM Element representation of build log.
     */
    public Element build(int buildNumber, Date lastBuild, Date now, Map properties, String buildTarget)
      throws CruiseControlException {
        Builder builder = selectBuilder(buildNumber, lastBuild, now);
        if (buildTarget != null) {
            LOG.info("Overriding build target with \"" + buildTarget + "\"");
            return builder.buildWithTarget(properties, buildTarget);
        } 
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
                int nowTime = DateUtil.getTimeFromDate(now);
                boolean isAfterBuildTime = buildTime <= nowTime;
                boolean isValidDay = builder.isValidDay(now);
                if (didntBuildToday && isAfterBuildTime && isValidDay) {
                    return builder;
                }
            } else if (builder.getMultiple() > 0) {
                if (builder.isValidDay(now)) {
                    if ((buildNumber % builder.getMultiple()) == 0) {
                        return builder;
                    }
                }
            } else {
                throw new CruiseControlException("The selected Builder is not properly configured");
            }
        }
               
        return null;
    }

    boolean builderDidntBuildToday(Date lastBuild, Date now, int buildTime) {
        int time = DateUtil.getTimeFromDate(now);
        long timeMillis = DateUtil.convertToMillis(time);
        long startOfToday = now.getTime() - timeMillis;
        boolean lastBuildYesterday = lastBuild.getTime() < startOfToday;
        boolean lastBuildTimeBeforeBuildTime = DateUtil.getTimeFromDate(lastBuild) < buildTime;
        return lastBuildYesterday || lastBuildTimeBeforeBuildTime;
    }

    long getTimeToNextBuild(Date now, long sleepInterval) {
        return getTimeToNextBuild(now, sleepInterval, 0);
    }

    private long getTimeToNextBuild(Date now, long sleepInterval, long priorPauseAdjustment) {
        long timeToNextBuild = sleepInterval;
        LOG.debug("getTimeToNextBuild: initial timeToNextBuild = " + timeToNextBuild);
        timeToNextBuild = checkMultipleBuilders(now, timeToNextBuild);
        LOG.debug("getTimeToNextBuild: after checkMultipleBuilders = " + timeToNextBuild);
        timeToNextBuild = checkTimeBuilders(now, timeToNextBuild);
        LOG.debug("getTimeToNextBuild: after checkTimeBuilders = " + timeToNextBuild);
        long timeTillNotPaused = checkPauseBuilders(now, timeToNextBuild);
        LOG.debug("getTimeToNextBuild: after checkPauseBuilders = " + timeToNextBuild);
        
        if (timeToNextBuild != timeTillNotPaused) {
            boolean atMaxTime = timeTillNotPaused >= MAX_INTERVAL_MILLISECONDS
                              || priorPauseAdjustment >= MAX_INTERVAL_MILLISECONDS;
            if (hasOnlyTimeBuilders() && !atMaxTime) {
                Date dateAfterPause = getFutureDate(now, timeTillNotPaused);
                long adjustmentFromEndOfPause = getTimeToNextBuild(dateAfterPause, 
                                                                                0,
                                         priorPauseAdjustment + timeTillNotPaused);
                timeToNextBuild = timeTillNotPaused + adjustmentFromEndOfPause;
                timeToNextBuild = checkMaximumInterval(timeToNextBuild);
            } else {
                timeToNextBuild = timeTillNotPaused;                
            }
        }
        
        return timeToNextBuild;
    }

    private long checkMultipleBuilders(Date now, long interval) {
        if (hasOnlyTimeBuilders()) {
            LOG.debug("has only time builders, so no correction for multiple builders.");
            return interval;
        }
        
        Date then = getFutureDate(now, interval);
        
        List buildersForOtherDays = new ArrayList();
        Iterator iterator = builders.iterator();
        while (iterator.hasNext()) {
            Builder builder = (Builder) iterator.next();
            boolean isTimeBuilder = builder.getTime() != Builder.NOT_SET;
            if (!isTimeBuilder) {
                if (builder.getMultiple() == 1) {
                    if (builder.isValidDay(then)) {
                        LOG.debug("multiple=1 builder found that could run on " + then);
                        return interval;
                    } else {
                        buildersForOtherDays.add(builder);
                    }
                }
            }
        }
        
        if (buildersForOtherDays.size() == 0) {
            LOG.error("configuration error: has some multiple builders but no multiple=1 builders found!");
            return interval;
        } else {
            LOG.debug("no multiple=1 builders found for " + then + ". checking other days");
        }
        
        for (int i = 1; i < 7; i++) {
            long daysPastInitialInterval = i * ONE_DAY;
            then = getFutureDate(now, interval + daysPastInitialInterval);
            iterator = builders.iterator();
            while (iterator.hasNext()) {
                Builder builder = (Builder) iterator.next();
                if (builder.isValidDay(then)) {
                    LOG.debug("multiple=1 builder found that could run on " + then);
                    long correctionToMidnight = getTimePastMidnight(then);
                    return interval + daysPastInitialInterval - correctionToMidnight;
                }
            }            
        }
        
        LOG.error("configuration error? could not find appropriate multiple=1 builder.");
        return interval;
    }

    private long getTimePastMidnight(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        long time = 60 * ONE_MINUTE * cal.get(Calendar.HOUR_OF_DAY);
        time += ONE_MINUTE * cal.get(Calendar.MINUTE);
        return time;
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
        int nowTime = DateUtil.getTimeFromDate(now);
        Iterator builderIterator = builders.iterator();
        while (builderIterator.hasNext()) {
            Builder builder = (Builder) builderIterator.next();
            int thisBuildTime = builder.getTime();
            boolean isTimeBuilder = thisBuildTime != Builder.NOT_SET;
            if (isTimeBuilder) {
                long timeToThisBuild = Long.MAX_VALUE;
                Calendar cal = Calendar.getInstance();
                long oneYear = 365;
                for (int daysInTheFuture = 0; daysInTheFuture < oneYear; daysInTheFuture++) {
                    cal.setTime(now);
                    cal.add(Calendar.DATE, daysInTheFuture);
                    Date future = cal.getTime();
                    boolean dayIsValid = builder.isValidDay(future);
                    if (dayIsValid) {
                        boolean timePassedToday = (daysInTheFuture == 0) && (nowTime > thisBuildTime);
                        if (!timePassedToday) {
                            int buildHour = thisBuildTime / 100;
                            int buildMinute = thisBuildTime % 100;
                            cal.set(Calendar.HOUR_OF_DAY, buildHour);
                            cal.set(Calendar.MINUTE, buildMinute);
                            future = cal.getTime();
                            timeToThisBuild = future.getTime() - now.getTime();
                            break;
                        }
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
        int currentTime = DateUtil.getTimeFromDate(now);

        long timeToEndOfPause = DateUtil.milliTimeDifference(currentTime, endPause);

        while (timeToEndOfPause < proposedTime) {
            timeToEndOfPause += ONE_DAY;
        }

        timeToEndOfPause = checkMaximumInterval(timeToEndOfPause);

        return timeToEndOfPause == MAX_INTERVAL_MILLISECONDS ? timeToEndOfPause : timeToEndOfPause + ONE_MINUTE;
    }

    private long checkMaximumInterval(long timeToEndOfPause) {
        if (timeToEndOfPause > MAX_INTERVAL_MILLISECONDS) {
            LOG.error("maximum interval exceeded! project perpetually paused?");
            return MAX_INTERVAL_MILLISECONDS;
        }
        return timeToEndOfPause;
    }

    private Date getFutureDate(Date now, long delay) {
        long futureMillis = now.getTime() + delay;
        return new Date(futureMillis);
    }

    public void setInterval(long intervalBetweenModificationChecks) {
        if (intervalBetweenModificationChecks < 0) {
            throw new IllegalArgumentException("interval can't be less than zero");
        }
        interval = intervalBetweenModificationChecks * ONE_SECOND;
    }

    public long getInterval() {
        return interval;
    }

    public void validate() throws CruiseControlException {
        ValidationHelper.assertTrue(builders.size() > 0,
            "schedule element requires at least one nested builder element");

        ValidationHelper.assertFalse(interval > ONE_YEAR,
            "maximum interval value is " + MAX_INTERVAL_SECONDS + " (one year)");
        
        if (hasOnlyTimeBuilders()) {
            LOG.warn("schedule has all time based builders: interval value will be ignored.");
            ValidationHelper.assertFalse(checkWithinPause(new ArrayList(builders)), "all build times during pauses.");
        }

        //Validate the child builders, since no one else seems to be doing it.
        for (Iterator iterator = builders.iterator(); iterator.hasNext();) {
            Builder next = (Builder) iterator.next();
            next.validate();
        }
    }

    private boolean checkWithinPause(List timeBuilders) {
        for (int i = 0; i < timeBuilders.size(); i++) {
            Builder builder = (Builder) timeBuilders.get(i);
            for (int j = 0; j < pauseBuilders.size(); j++) {
                PauseBuilder pauseBuilder = (PauseBuilder) pauseBuilders.get(j);
                if (buildDaySameAsPauseDay(builder, pauseBuilder) && buildTimeWithinPauseTime(builder, pauseBuilder)) {
                    timeBuilders.remove(builder);
                    StringBuffer message = new StringBuffer();
                    message.append("time Builder for time ");
                    message.append(Integer.toString(builder.getTime()));
                    if (builder.getDay() != Builder.NOT_SET) {
                        message.append(" and day of ");
                        message.append(getDayString(builder.getDay()));
                    }
                    message.append(" is always within a pause and will never build");
                    LOG.error(message.toString());
                }
            }
        }
        return timeBuilders.isEmpty();
    }

    /** 
     * I can't believe this method doesn't already exist in the JDK...
     * am I just missing it?!? Probably some locale thing I need to use.
     * @param day int value
     * @return english string value
     */
    private String getDayString(int day) {
        switch(day) {
        case Calendar.SUNDAY:
            return "sunday";
        case Calendar.MONDAY:
            return "monday";
        case Calendar.TUESDAY:
            return "tuesday";
        case Calendar.WEDNESDAY:
            return "wednesday";
        case Calendar.THURSDAY:
            return "thursday";
        case Calendar.FRIDAY:
            return "friday";
        case Calendar.SATURDAY:
            return "saturday";
        default:
            return "invalid day " + day;
        }
    }

    private boolean buildDaySameAsPauseDay(Builder builder, PauseBuilder pauseBuilder) {
        return pauseBuilder.getDay() == PauseBuilder.NOT_SET
                || pauseBuilder.getDay() == builder.getDay();
    }

    private boolean buildTimeWithinPauseTime(Builder builder, PauseBuilder pauseBuilder) {
        return pauseBuilder.getStartTime() < builder.getTime() 
                && builder.getTime() < pauseBuilder.getEndTime();
    }

    /**
     * utility method to check method parameters and ensure they're not null
     * @param paramName name of the paramter to check
     * @param param parameter to check
     */
    private void checkParamNotNull(String paramName, Object param) {
        if (param == null) {
            throw new IllegalArgumentException(paramName + " can't be null");
        }
    }

    public List getBuilders() {
        return builders;
    }
}

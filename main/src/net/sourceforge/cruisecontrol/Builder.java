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

package net.sourceforge.cruisecontrol;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import net.sourceforge.cruisecontrol.util.PerDayScheduleItem;
import net.sourceforge.cruisecontrol.util.ValidationHelper;

import org.jdom.Element;

public abstract class Builder extends PerDayScheduleItem implements Comparable {

    private int time = NOT_SET;
    private int multiple = 1;
    private boolean multipleSet = false;
    private boolean showProgress = true;

    /** Build property name of property that is pass to all builders. */
    public static final String BUILD_PROP_PROJECTNAME = "projectname";

    /**
     * Execute a build.
     * @param properties build properties
     * @param progress callback to provide progress updates
     * @return the log resulting from executing the build
     * @throws CruiseControlException if something breaks
     */
    public abstract Element build(Map<String, String> properties, Progress progress) throws CruiseControlException;
    /**
     * Execute a build with the given target.
     * @param properties build properties
     * @param target the build target to call, overrides target defined in config
     * @param progress callback to provide progress updates
     * @return the log resulting from executing the build
     * @throws CruiseControlException if something breaks
     */
    public abstract Element buildWithTarget(Map<String, String> properties, String target, Progress progress)
            throws CruiseControlException;

    public void validate() throws CruiseControlException {
        boolean timeSet = time != NOT_SET;
        
        if (timeSet) {
          ValidationHelper.assertFalse(time < 0, "negative values for time are not allowed");
        }

        ValidationHelper.assertFalse(timeSet && multipleSet,
            "Only one of 'time' or 'multiple' are allowed on builders.");
    }

    public int getTime() {
        return time;
    }

    /**
     * can use ScheduleItem.NOT_SET to reset.
     * @param timeString new time integer
     */
    public void setTime(String timeString) {
        time = Integer.parseInt(timeString);
    }

    /**
     * can use Builder.NOT_SET to reset.
     * @param multiple new multiple
     */
    public void setMultiple(int multiple) {
        multipleSet = multiple != NOT_SET;
        this.multiple = multiple;
    }

    public int getMultiple() {
        boolean timeSet = time != NOT_SET;
        if (timeSet && !multipleSet) {
            return NOT_SET;
        }
        return multiple;
    }

    public void setShowProgress(final boolean showProgress) {
        this.showProgress = showProgress;
    }
    public boolean getShowProgress() {
        return showProgress;
    }

    /**
     * Is this the correct day to be running this builder?
     * @param now the current date
     * @return true if this this the correct day to be running this builder 
     */
    public boolean isValidDay(Date now) {
        if (getDay() < 0) {
            return true;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        return cal.get(Calendar.DAY_OF_WEEK) == getDay();
    }

    /**
     *  used to sort builders.  we're only going to care about sorting builders based on build number,
     *  so we'll sort based on the multiple attribute.
     */
    public int compareTo(Object o) {
        Builder builder = (Builder) o;
        Integer integer = new Integer(multiple);
        Integer integer2 = new Integer(builder.getMultiple());
        return integer2.compareTo(integer); //descending order
    }

    public boolean isTimeBuilder() {
        return time != NOT_SET;
    }

}

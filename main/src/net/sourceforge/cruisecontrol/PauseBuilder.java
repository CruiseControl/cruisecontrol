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

import java.util.Calendar;
import java.util.Date;

/**
 *  Used by <code>Schedule</code> to define periods of time when CruiseControl
 *  should not even attempt a build.  Useful for making sure CruiseControl does
 *  not run during server backup times, etc.
 *
 *  @author alden almagro, ThoughtWorks, Inc. 2001-2
 */
public class PauseBuilder {

    private int _day = -1;
    private int _startTime;
    private int _endTime;

    public void setDay(String dayString) {
        if(dayString.equalsIgnoreCase("sunday")) {
            _day = Calendar.SUNDAY;
        } else if(dayString.equalsIgnoreCase("monday")) {
            _day = Calendar.MONDAY;
        } else if(dayString.equalsIgnoreCase("tuesday")) {
            _day = Calendar.TUESDAY;
        } else if(dayString.equalsIgnoreCase("wednesday")) {
            _day = Calendar.WEDNESDAY;
        } else if(dayString.equalsIgnoreCase("thursday")) {
            _day = Calendar.THURSDAY;
        } else if(dayString.equalsIgnoreCase("friday")) {
            _day = Calendar.FRIDAY;
        } else if(dayString.equalsIgnoreCase("saturday")) {
            _day = Calendar.SATURDAY;
        }
    }

    public void setStartTime(int time) {
        _startTime = time;
    }

    public void setEndTime(int time) {
        _endTime = time;
    }

    public int getStartTime() {
        return _startTime;
    }

    public int getEndTime() {
        return _endTime;
    }

    /**
     *  Determine if <code>now</code is valid for today.
     *
     *  @param now The current date
     *  @return true if the date matches the day set in <code>setDay(String dayString)</code>
     */
    public boolean isValidDay(Date now) {
        if(_day < 0)
            return true;

        Calendar cal = Calendar.getInstance();
        cal.setTime(now);
        return cal.get(Calendar.DAY_OF_WEEK) == (_day + 1);
    }
}

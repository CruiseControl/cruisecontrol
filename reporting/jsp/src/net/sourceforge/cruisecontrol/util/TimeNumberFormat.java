/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2004, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A custom formatter that will take numbers and display them as if they were timestamps (<em>not</em> date stamps).
 * To achieve this, it actually delegates down to a DateFormatter (using a format of HH:mm). This date formatter is
 * configured to work on UTC time, as a number value of 0 represents midnight.
 *
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public final class TimeNumberFormat extends NumberFormat {
        private static final String UTC_TIME_ZONE = "+0:00";
        private static final String HOURS_MINUTE = "HH:mm";

        private final DateFormat realFormat;

        /** Initialise the formatter */
        public TimeNumberFormat() {
            SimpleDateFormat format = new SimpleDateFormat(HOURS_MINUTE);
            format.setTimeZone(TimeZone.getTimeZone(UTC_TIME_ZONE));
            realFormat = format;
        }

        /** Delegates down to date format */
        public StringBuffer format(double arg0, StringBuffer arg1,
                FieldPosition arg2) {
            return realFormat.format(new Date((long) arg0), arg1, arg2);
        }

        /** Delegates down to date format */
        public StringBuffer format(long arg0, StringBuffer arg1,
                FieldPosition arg2) {
            return realFormat.format(new Date(arg0), arg1, arg2);
        }

        /** Delegates down to date format */
        public Number parse(String arg0, ParsePosition arg1) {
            return new Long(realFormat.parse(arg0, arg1).getTime());
        }
}
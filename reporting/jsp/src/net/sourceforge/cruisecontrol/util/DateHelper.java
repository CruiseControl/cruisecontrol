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
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Helper class for date-related functions.
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public final class DateHelper {

    private static final char YEAR = 'y';
    private static final char DAY = 'd';
    private static final char MONTH = 'M';
    private static final String TWENTY_FOUR_HOUR = " HH:mm:ss";
    private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy/MM/dd" + TWENTY_FOUR_HOUR);
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd/MM/yyyy" + TWENTY_FOUR_HOUR);
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MM/dd/yyyy" + TWENTY_FOUR_HOUR);

    private DateHelper() {
        // private constructor for utility class.
    }

    /**
     * Create a date format for the locale provided.
     * @param locale    the locale.
     * @return  the date format.
     */
    public static SimpleDateFormat createDateFormat(Locale locale) {
        SimpleDateFormat standardFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale);
        String standardPattern = standardFormat.toPattern();
        char firstLetterInPattern = standardPattern.charAt(0);
        switch (firstLetterInPattern) {
            case MONTH:
                return MONTH_FORMAT;
            case DAY:
                return DAY_FORMAT;
            case YEAR:
                return YEAR_FORMAT;
            default:
                return new SimpleDateFormat(standardPattern + TWENTY_FOUR_HOUR);
        }
    }
}

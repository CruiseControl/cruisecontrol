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

import java.util.Locale;

import junit.framework.TestCase;

/**
 * Test case for the utility date class.
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 */
public class DateHelperTest extends TestCase {

    /** Create a formatter for US */
    public void testCreateUsDateFormatter() {
        String expectedPattern = "MM/dd/yyyy HH:mm:ss";
        String actualPattern = DateHelper.createDateFormat(Locale.US).toPattern();

        assertEquals(expectedPattern, actualPattern);
    }

    /** Create a formatter for UK */
    public void testCreateUkDateFormatter() {
        String expectedPattern = "dd/MM/yyyy HH:mm:ss";
        String actualPattern = DateHelper.createDateFormat(Locale.UK).toPattern();

        assertEquals(expectedPattern, actualPattern);
    }

    /** Create a formatter for Other */
    public void testCreateOtherDateFormatter() {
        String expectedPattern = "yyyy/MM/dd HH:mm:ss";
        String actualPattern = DateHelper.createDateFormat(Locale.CANADA_FRENCH).toPattern();

        assertEquals(expectedPattern, actualPattern);
    }
}

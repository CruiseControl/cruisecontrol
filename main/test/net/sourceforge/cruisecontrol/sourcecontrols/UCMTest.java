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

import java.text.SimpleDateFormat;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;

/**
 * @author <a href="mailto:kevin.lee@buildmeister.com">Kevin Lee</a>
 */
public class UCMTest extends TestCase {

    public static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

//    private static final String WINDOWS_LOG = "ucm-history.txt";
//    private static final String UNIX_LOG = "ucm-history-alt.txt";
//    private static final String WINDOWS_XML = "ucm-history.xml";
//    private static final String UNIX_XML = "ucm-history-alt.xml";
//
//    private UCM ucm;
//    private List mods;
//
//    private InputStream loadTestLog(String name) {
//        InputStream testStream = getClass().getResourceAsStream(name);
//        assertNotNull("failed to load resource " + name + " in class " + getClass().getName(), testStream);
//        return testStream;
//    }

    public void testValidate() {
        UCM ucmV = new UCM();

        try {
            ucmV.validate();
            fail("UCM should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        ucmV.setViewPath("path");
        ucmV.setStream("branch");

        try {
            ucmV.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("UCM should not throw exceptions when required attributes are set.");
        }
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(UCMTest.class);
    }

}

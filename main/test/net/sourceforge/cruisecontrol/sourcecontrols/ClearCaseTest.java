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
package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.Modification;
import net.sourceforge.cruisecontrol.CruiseControlException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author Eric Lefevre
 */
public class ClearCaseTest extends TestCase {

    private ClearCase _clearCase;
    private String clearCaseStream;

    private final static String USERID = "userid";
    private final static String DATE = "20010808.023456";
    private final static String FILENAME =
            "c:" + File.separator + "path" + File.separator + "filename@@"
            + File.separator + "main" + File.separator + "vob";
    private final static String CHECKIN = "checkin";
    private final static String COMMENT = "This is a \nsample\n\ncomment";
    private static Date date1;

    private final static String DATE2 = "20221218.143456";
    private final static String COMMENT2 = "\n\n";
    private static Date date2;


    public ClearCaseTest(String name) {
        super(name);
    }

    protected void setUp() {
        // Set up so that this element will match all tests.
        _clearCase = new ClearCase();
        String DELIMITER = ClearCase.DELIMITER;
        clearCaseStream = "";
        clearCaseStream += USERID + DELIMITER + DATE + DELIMITER;
        clearCaseStream += FILENAME + DELIMITER + CHECKIN + DELIMITER;
        clearCaseStream += COMMENT + ClearCase.END_OF_STRING_DELIMITER + "\n";
        clearCaseStream += USERID + DELIMITER + DATE2 + DELIMITER;
        clearCaseStream += FILENAME + DELIMITER + CHECKIN + DELIMITER;
        clearCaseStream += COMMENT2 + ClearCase.END_OF_STRING_DELIMITER + "\n";

        Calendar calendar = Calendar.getInstance();
        calendar.set(2001, 7, 8, 2, 34, 56);
        calendar.set(Calendar.MILLISECOND, 0);
        date1 = calendar.getTime();
        calendar.set(2022, 11, 18, 14, 34, 56);
        date2 = calendar.getTime();
    }

    /**
     * Tests the streams of bytes that can be returned by the ClearCase server.
     */
    public void testClearCaseStream() throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(clearCaseStream.getBytes());
        List list = _clearCase.parseStream(stream);
        Modification mod = (Modification) list.get(0);
        assertEquals(CHECKIN, mod.type);
        assertEquals(File.separator + "filename", mod.fileName);
        assertEquals(File.separator + "path", mod.folderName);
        assertEquals(date1, mod.modifiedTime);
        assertEquals(USERID, mod.userName);
        assertEquals("This is a sample comment", mod.comment);

        mod = (Modification) list.get(1);
        assertEquals(CHECKIN, mod.type);
        assertEquals(File.separator + "filename", mod.fileName);
        assertEquals(File.separator + "path", mod.folderName);
        assertEquals(date2, mod.modifiedTime);
        assertEquals(USERID, mod.userName);
        assertEquals("", mod.comment);
    }

    public void testValidate() {
        ClearCase cc = new ClearCase();

        try {
            cc.validate();
            fail("ClearCase should throw exceptions when required attributes are not set.");
        } catch (CruiseControlException e) {
            assertTrue(true);
        }

        cc.setViewpath("path");
        cc.setBranch("branch");

        try {
            cc.validate();
            assertTrue(true);
        } catch (CruiseControlException e) {
            fail("ClearCase should not throw exceptions when required attributes are set.");
        }
    }

    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(ClearCaseTest.class);
    }

}
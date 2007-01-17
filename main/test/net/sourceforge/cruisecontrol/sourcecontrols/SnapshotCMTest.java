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
package net.sourceforge.cruisecontrol.sourcecontrols;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.CruiseControlException;
import net.sourceforge.cruisecontrol.Modification;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;


/**
 * Things that could/should be improved:
 * <ul>
 * <li> test using a server lying in a different timezone. Right now it's not handled.
 * </ul>
 * @author <a href="mailto:jerome@coffeebreaks.org">Jerome Lacoste</a>
 */
public class SnapshotCMTest extends TestCase {

    private Date parseSnaphotOutDateFormat(String dateString) throws ParseException {
        return new SimpleDateFormat(SnapshotCM.OUT_DATE_FORMAT).parse(dateString);
    }

    public void testValidate() {
        SnapshotCM snaphotCM = new SnapshotCM();

        try {
            snaphotCM.validate();
            fail("SnapshotCM should throw exceptions when required fields are not set.");
        } catch (CruiseControlException e) {
        }

        snaphotCM.setSourcePath("thePath");

        try {
            snaphotCM.validate();
        } catch (CruiseControlException e) {
            fail("SnapshotCM should not throw exceptions when required fields are set.");
        }

        // test validity of the path format?
    }

    private InputStream loadTestLog(String name) {
        InputStream testStream = getClass().getResourceAsStream(name);
        assertNotNull("failed to load resource " + name + " in class " + getClass().getName(), testStream);
        return testStream;
    }

    public void testParseStream() throws IOException, ParseException {
        SnapshotCM snaphotCM = new SnapshotCM();

        BufferedInputStream input =
                new BufferedInputStream(loadTestLog("snapshotcm-history.txt"));
        List modifications = snaphotCM.parseStream(input);
        input.close();
        Collections.sort(modifications);

        assertEquals("Should have returned 3 modifications.",
                3,
                modifications.size());

        Modification mod1 = new Modification("Content");
        Modification.ModifiedFile mod1file = mod1.createModifiedFile("build.xml", "/xxx/yyy/cccc/dddd");
        mod1file.action = "modified";
        mod1.revision = "18";
        mod1.modifiedTime = parseSnaphotOutDateFormat("2004/01/06 15:49:38");
        mod1.userName = "pacon";
        mod1.comment =
                "Corrected capitalization for all parameters";

        Modification mod2 = new Modification("Content");
        Modification.ModifiedFile mod2file = mod2.createModifiedFile("build.xml", "/xxx/yyy/cccc/dddd");
        mod2file.action = "modified";
        mod2.revision = "19";
        mod2.modifiedTime = parseSnaphotOutDateFormat("2004/01/06 17:00:40");
        mod2.userName = "pacon";
        mod2.comment = "Removed -D param from SnapshotCM wco and wci commands.";

        Modification mod3 = new Modification("Content");
        Modification.ModifiedFile mod3file =
            mod3.createModifiedFile("wallawalla", "/xxx/yyy/zzzz/scripts/sbin/init.d/");
        mod3file.action = "modified";
        mod3.revision = "8";
        mod3.modifiedTime = parseSnaphotOutDateFormat("2004/01/07 09:51:34");
        mod3.userName = "pacon";
        mod3.comment = "remove obsolete comment";

        assertEquals(mod1, modifications.get(0));
        assertEquals(mod2, modifications.get(1));
        assertEquals(mod3, modifications.get(2));
    }
}

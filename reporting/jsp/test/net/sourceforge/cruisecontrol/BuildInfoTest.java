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
package net.sourceforge.cruisecontrol;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test that we can determine build information correctly.
 * @author <a href="mailto:robertdw@users.sourceforge.net">Robert Watkins</a>
 * @author <a href="mailto:hak@2mba.dk">Hack Kampbjorn</a>
 */
public class BuildInfoTest extends TestCase {
    public static Test suite() {
        return new LogFileSetupDecorator(new TestSuite(BuildInfoTest.class));
    }

    public void testCreationFailedBuild() throws ParseException {
        Date buildDate = new GregorianCalendar(2002, Calendar.FEBRUARY, 22, 12, 15, 30).getTime();
        BuildInfo buildInfo = new BuildInfo("log20020222121530.xml");
        assertEquals(buildDate, buildInfo.getBuildDate());
        assertFalse(buildInfo.isSuccessful());
        assertNull(buildInfo.getLabel());
    }

    public void testCreationCompressedFailedBuild() throws ParseException {
        Date buildDate = new GregorianCalendar(2004, Calendar.OCTOBER, 28, 15, 24, 19).getTime();
        BuildInfo buildInfo = new BuildInfo("log20041028152419.xml.gz");
        assertEquals(buildDate, buildInfo.getBuildDate());
        assertFalse(buildInfo.isSuccessful());
        assertNull(buildInfo.getLabel());
    }

    public void testCreationGoodBuild() throws ParseException {
        Date buildDate = new GregorianCalendar(2002, Calendar.FEBRUARY, 23, 12, 0, 0).getTime();
        BuildInfo buildInfo = new BuildInfo("log20020223120000LBuild.1.xml");
        assertEquals(buildDate, buildInfo.getBuildDate());
        assertTrue(buildInfo.isSuccessful());
        assertEquals("Build.1", buildInfo.getLabel());
    }

    public void testCreationCompressedGoodBuild() throws ParseException {
        Date buildDate = new GregorianCalendar(2004, Calendar.OCTOBER, 28, 15, 56, 4).getTime();
        BuildInfo buildInfo = new BuildInfo("log20041028155604LBuild.2.xml.gz");
        assertEquals(buildDate, buildInfo.getBuildDate());
        assertTrue(buildInfo.isSuccessful());
        assertEquals("Build.2", buildInfo.getLabel());
    }

    public void testCreationGoodBuildWithEmptyLabel() throws ParseException {
        Date buildDate = new GregorianCalendar(2002, Calendar.FEBRUARY, 23, 12, 0, 0).getTime();
        BuildInfo buildInfo = new BuildInfo("log20020223120000L.xml");
        assertEquals(buildDate, buildInfo.getBuildDate());
        assertTrue(buildInfo.isSuccessful());
        assertEquals("", buildInfo.getLabel());
    }

    public void testLoadBuildInfo() throws ParseException {
        // use the BuildInfoHelper to load up the list of BuildInfo objects
        // verify the build date for each.
        // verify the build successful state for each
        // verify the label for each (if any)
        BuildInfo[] expected = { new BuildInfo("log20020222120000.xml"),
                                 new BuildInfo("log20020223120000LBuild.1.xml"),
                                 new BuildInfo("log20020224120000.xml"),
                                 new BuildInfo("log20020225120000LBuild.2.xml"),
                                 new BuildInfo("log20041018160000.xml.gz"),
                                 new BuildInfo("log20041018170000LBuild.3.xml.gz")};

        BuildInfoSummary results = BuildInfo.loadFromDir(LogFileSetupDecorator.LOG_DIR);
        assertEquals(3, results.getNumBrokenBuilds());
        assertEquals(3, results.getNumSuccessfulBuilds());
        BuildInfo[] resultsArray = results.asArray();
        for (int i = 0; i < expected.length; i++) {
            BuildInfo expectedResult = expected[i];
            BuildInfo actualResult = resultsArray[i];
            validateBuildInfo(expectedResult, actualResult);
        }
    }

    private void validateBuildInfo(BuildInfo expected, BuildInfo actual) {
        assertEquals(expected.getBuildDate(), actual.getBuildDate());
        assertEquals(expected.getLabel(), actual.getLabel());
        assertEquals(expected.isSuccessful(), actual.isSuccessful());

    }
}

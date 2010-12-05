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

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.cruisecontrol.testutil.TestUtil;
import net.sourceforge.cruisecontrol.util.BuildOutputLogger;
import net.sourceforge.cruisecontrol.util.PerDayScheduleItem;

import org.jdom.Element;

import junit.framework.TestCase;

/**
 * @author jfredrick
 */
public class BuilderTest extends TestCase {

    private Builder builder;
    private final Calendar nov22nd2001;

    public BuilderTest(String name) {
        super(name);
        nov22nd2001 = Calendar.getInstance();
        nov22nd2001.set(2001, Calendar.NOVEMBER, 22, 10, 1, 1);
    }

    protected void setUp() throws Exception {
        builder = new TestBuilder();
    }

    public void testLiveOutput() {
        assertTrue(builder.isLiveOutput());
        builder.setLiveOutput(false);
        assertFalse(builder.isLiveOutput());
    }

    public void testGetBuildOutputConsumerNulls() {
        assertNotSame("Null project name results in no cached buildOutputLogger.",
                builder.getBuildOutputConsumer(null, null, null),
                BuildOutputLoggerManager.INSTANCE.lookup(null));
    }

    public void testGetBuildOutputConsumerSpecifyLogFile() {
        final File outputFile = new File(TestUtil.getTargetDir(), "testLiveOutputFile.xyz");
        assertFalse(outputFile.exists());

        final BuildOutputLogger buildOutputLogger
                = builder.getBuildOutputConsumer(null, outputFile.getParentFile(), outputFile.getName());
        assertFalse(outputFile.exists());

        final String testLogData = "testLogData";
        buildOutputLogger.consumeLine(testLogData);
        assertTrue(outputFile.exists());
        assertEquals(testLogData, buildOutputLogger.retrieveLines(0)[0]);
        assertTrue(outputFile.delete());
    }

    public void testGetBuildOutputConsumerLiveOutputOff() {
        final String projectName = "testProjectName";
        builder.setLiveOutput(false);
        assertNull(builder.getBuildOutputConsumer(projectName, null, null));
    }

    public void testGetBuildOutputConsumer() {
        final String projectName = "testProjectName";
        assertSame(builder.getBuildOutputConsumer(projectName, null, null),
                BuildOutputLoggerManager.INSTANCE.lookup(projectName));

        assertSame(BuildOutputLoggerManager.INSTANCE.lookup(projectName),
                builder.getBuildOutputConsumer(projectName, null, null));
    }

    public void testGetBuildOutputConsumerProjectNameWithSlash() {
        final String projectNameWithSlash = "testProjectName/trunk";
        assertSame(builder.getBuildOutputConsumer(projectNameWithSlash, null, null),
                BuildOutputLoggerManager.INSTANCE.lookup(projectNameWithSlash));
    }

    public void testValidate() throws CruiseControlException {
        try {
            builder.validate();
        } catch (CruiseControlException e) {
            fail("no required attributes");
        }

        builder.setTime("0000");
        builder.validate();

        builder.setMultiple(1);
        try {
            builder.validate();
            fail("can't set both time and multiple");
        } catch (CruiseControlException e) {
            // should throw exception
        }

        builder.setTime("-1000");
        try {
          builder.validate();
          fail("time can't be negative");
        } catch (CruiseControlException e) {
            // should throw exception
        }
        
        builder.setTime(String.valueOf(PerDayScheduleItem.NOT_SET));
        builder.validate();
  }

    public void testIsValidDay() {
        Date thursday = nov22nd2001.getTime();

        assertTrue(builder.isValidDay(thursday));

        builder.setDay("wednesday");
        assertTrue(!builder.isValidDay(thursday));

        builder.setDay("thursday");
        assertTrue(builder.isValidDay(thursday));
    }

    public void testGetMultiple() {
        assertEquals(1, builder.getMultiple());
        builder.setTime("0100");
        assertEquals(-1, builder.getMultiple());
    }

    class TestBuilder extends Builder {
        public Element build(Map properties, Progress progress) throws CruiseControlException {
            return null;
        }

        public Element buildWithTarget(Map properties, String target, Progress progress) throws CruiseControlException {
            return null;
        }
    }

    public static Map<String, String> createPropsWithProjectName(final String projectName) {
        final Map<String, String> buildProperties = new HashMap<String, String>();
        buildProperties.put(Builder.BUILD_PROP_PROJECTNAME, projectName);
        return buildProperties;
    }
}

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
package net.sourceforge.cruisecontrol;

import junit.framework.TestCase;
import net.sourceforge.cruisecontrol.labelincrementers.DefaultLabelIncrementer;
import org.jdom.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.List;

public class ProjectXMLHelperTest extends TestCase {

    private File configFile;

    private static final int ONE_SECOND = 1000;

    public void testDateFormat() throws Exception {
        String originalFormat = DateFormatFactory.getFormat();
        assertEquals("MM/dd/yyyy HH:mm:ss", originalFormat);

        Element projectElement = new Element("project");
        Element dateFormatElement = new Element("dateformat");
        dateFormatElement.setAttribute("format", "yyyy/MM/dd hh:mm:ss a");
        projectElement.addContent(dateFormatElement);

        ProjectXMLHelper helper = new ProjectXMLHelper();
        helper.setDateFormat(projectElement);

        assertEquals("yyyy/MM/dd hh:mm:ss a", DateFormatFactory.getFormat());

        DateFormatFactory.setFormat(originalFormat);
    }

    public void testGetBootstrappers() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        List bootstrappers = helper.getBootstrappers();
        assertEquals(0, bootstrappers.size());

        helper = new ProjectXMLHelper(configFile, "project2");
        bootstrappers = helper.getBootstrappers();
        assertEquals(1, bootstrappers.size());
    }

    public void testGetSchedule() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        try {
            helper.getSchedule();
            fail("schedule should be a required element");
        } catch (CruiseControlException e) {
        }

        helper = new ProjectXMLHelper(configFile, "project2");
        Schedule schedule = helper.getSchedule();
        assertEquals(20 * ONE_SECOND, schedule.getInterval());
    }

    public void testGetModificationSet() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        try {
            helper.getModificationSet();
            fail("modificationset should be a required element");
        } catch (CruiseControlException e) {
        }

        helper = new ProjectXMLHelper(configFile, "project2");
        ModificationSet modSet = helper.getModificationSet();
        assertEquals(10 * ONE_SECOND, modSet.getQuietPeriod());
    }

    public void testGetLabelIncrementer() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project2");
        DefaultLabelIncrementer incrementer =
            (DefaultLabelIncrementer) helper.getLabelIncrementer();
        assertTrue(incrementer.isValidLabel("build#9"));

        helper = new ProjectXMLHelper(configFile, "project1");
        incrementer = (DefaultLabelIncrementer) helper.getLabelIncrementer();
        assertFalse(incrementer.isValidLabel("build#9"));
    }

    public void testGetLog() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        assertEquals("logs" + File.separatorChar + "project1", helper.getLog().getLogDir());
        helper = new ProjectXMLHelper(configFile, "project2");
        assertEquals("c:/foo", helper.getLog().getLogDir());
        helper = new ProjectXMLHelper(configFile, "project3");
        assertEquals("logs" + File.separatorChar + "project3", helper.getLog().getLogDir());

        assertNull(helper.getLog().getLogXmlEncoding());
        helper = new ProjectXMLHelper(configFile, "project2");
        assertEquals("utf-8", helper.getLog().getLogXmlEncoding());
    }

    public void testGetAuxLogs() throws CruiseControlException {
        ProjectXMLHelper helper = new ProjectXMLHelper(configFile, "project1");
        assertEquals(0, helper.getAuxLogs().size());
        helper = new ProjectXMLHelper(configFile, "project2");
        assertEquals(1, helper.getAuxLogs().size());
    }

    protected void setUp() throws Exception {
        configFile = File.createTempFile("tempConfig", "xml");
        File tempDirectory = configFile.getParentFile();

        String config =
            "<cruisecontrol>"
                + "  <project name='project1' />"
                + "  <project name='project2' >"
                + "    <bootstrappers>"
                + "      <vssbootstrapper vsspath='foo' localdirectory='"
                + tempDirectory.getAbsolutePath()
                + "' />"
                + "    </bootstrappers>"
                + "    <schedule interval='20' >"
                + "      <ant multiple='1' buildfile='c:/foo/bar.xml' target='baz' />"
                + "    </schedule>"
                + "    <modificationset quietperiod='10' >"
                + "      <vss vsspath='c:/foo/bar' login='login' />"
                + "    </modificationset>"
                + "    <log dir='c:/foo' encoding='utf-8' >"
                + "      <merge file='blah' />"
                + "    </log>"
                + "    <labelincrementer separator='#' />"
                + "  </project>"
                + "  <project name='project3' >"
                + "    <log/>"
                + "  </project>"
                + "</cruisecontrol>";

        Writer writer = new FileWriter(configFile);
        writer.write(config);
        writer.close();
    }

    protected void tearDown() throws Exception {
        configFile = null;
    }

}

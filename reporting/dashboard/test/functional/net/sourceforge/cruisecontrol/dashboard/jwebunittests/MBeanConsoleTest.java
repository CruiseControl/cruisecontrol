/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2007, ThoughtWorks, Inc.
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
package net.sourceforge.cruisecontrol.dashboard.jwebunittests;

import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;
import org.apache.commons.lang.StringUtils;

public class MBeanConsoleTest extends BaseFunctionalTest {

    protected void onSetUp() throws Exception {
        setConfigFileAndSubmitForm(DataUtils.getConfigXmlAsFile().getAbsolutePath());
    }

    public void testShouldShowMBeanConsoleForServer() throws Exception {
        tester.beginAt("/admin/mx4j");
        tester.assertTextPresent("JMX Console for CruiseControl");
        assertTrue(StringUtils.contains(tester.getPageSource(), "set_jmx_console_url(8000, \"\")"));
    }

    public void testShouldShowMBeanConsoleForSpecificProject() throws Exception {
        tester.beginAt("/admin/mx4j/project1");
        tester.assertTextPresent("JMX Console for project1");
        assertTrue(StringUtils
                .contains(tester.getPageSource(),
                "set_jmx_console_url(8000, \"mbean?objectname=CruiseControl+Project%3Aname%3Dproject1\")"));
    }
}

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
package net.sourceforge.cruisecontrol.dashboard.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.sourceforge.cruisecontrol.dashboard.BuildDetail;
import net.sourceforge.cruisecontrol.dashboard.Configuration;
import net.sourceforge.cruisecontrol.dashboard.testhelpers.DataUtils;

import org.apache.commons.lang.StringUtils;
import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

public class WidgetPluginServiceTest extends MockObjectTestCase {

    private WidgetPluginService service;

    private BuildDetail buildDetail;

    private Mock configurationMock;

    protected void setUp() throws Exception {
        configurationMock =
                mock(Configuration.class, new Class[] {ConfigXmlFileService.class}, new Object[] {null});
        service = new WidgetPluginService((Configuration) configurationMock.proxy());
        Map props = new HashMap();
        props.put("projectname", "project1");
        props.put("logfile", new File(DataUtils.getConfigXmlAsFile().getParent()
                + "/logs/project1/log20051209122104.xml"));
        buildDetail = new BuildDetail(props);
    }

    protected void tearDown() throws Exception {
        buildDetail.getPluginOutputs().clear();
    }

    public void testShouldBeAbleToReturnEmptyListWhenCfgIsNotDefined() throws Exception {
        invokeGetCCHome(1, "");
        service.mergePluginOutput(buildDetail, new HashMap());
        assertEquals(0, buildDetail.getPluginOutputs().size());
    }

    public void testShouldIgnoreNonExistentClassAndContinueInitializingTheResult() throws Exception {
        invokeGetCCHome(5, "test/data");
        service.mergePluginOutput(buildDetail, new HashMap());
        assertEquals(4, buildDetail.getPluginOutputs().size());
    }

    public void testShouldBeAbleToReturnInitializedServiceWhenCfgIsDefined() throws Exception {
        invokeGetCCHome(5, "test/data");
        service.mergePluginOutput(buildDetail, new HashMap());
        assertEquals(4, buildDetail.getPluginOutputs().size());
        assertTrue(buildDetail.getPluginOutputs().containsKey("Merged Check Style"));
        String content = (String) buildDetail.getPluginOutputs().get("Merged Check Style");
        assertTrue(StringUtils.contains(content, "Line has trailing spaces."));
    }

    public void testShouldNOTThrowExceptionWhenLineStartedWithHash() throws Exception {
        try {
            service.assemblePlugin(buildDetail, new HashMap(), "#this is a comment");
        } catch (Exception e) {
            fail();
        }
    }

    private void invokeGetCCHome(int times, String path) {
        for (int i = 0; i < times; i++) {
            configurationMock.expects(once()).method("getCCHome").will(returnValue(new File(path)));
        }
    }
}
